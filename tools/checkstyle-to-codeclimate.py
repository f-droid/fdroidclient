#!/usr/bin/env python3
"""Convert a Checkstyle XML report to a Code Climate JSON report.

Usage examples:
  # Basic (defaults)
  python tools/checkstyle-to-codeclimate.py

  # Explicit input/output
  python tools/checkstyle-to-codeclimate.py -i build/reports/checkstyle-result.xml -o gl-code-quality-report.json

  # Using positional file path(s) (new):
  python tools/checkstyle-to-codeclimate.py build/reports/checkstyle-result.xml another-report.xml

  # Read from stdin (single '-') and write pretty JSON
  cat checkstyle-result.xml | python tools/checkstyle-to-codeclimate.py - -o report.json --pretty

  # Strip a common base directory from paths (useful for CI where absolute paths leak)
  python tools/checkstyle-to-codeclimate.py --base-dir "$PWD" -i build/reports/checkstyle-result.xml

If no input is provided via positional arguments or -i/--input, defaults to 'checkstyle-result.xml'.
If --output is omitted, defaults to 'gl-code-quality-report.json'.

The resulting JSON is an array of issue objects compatible with GitLab's
Code Quality widget (Code Climate format subset).
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
import pathlib
import xml.etree.ElementTree as ET
from typing import Dict, List, Any, Iterable, Iterator, Optional, Union

# NOTE: Keep this script compatible with Python 3.8+ (avoid PEP 604 union syntax).

# Severity mapping from Checkstyle -> Code Climate (GitLab)
CONVERSION_MAP: Dict[str, str] = {
    "ignore": "info",
    "info": "minor",
    "warning": "major",
    "error": "critical",
}

Issue = Dict[str, Any]


def parse_args(argv: List[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Convert Checkstyle XML to Code Climate JSON"
    )
    parser.add_argument(
        "-i",
        "--input",
        metavar="FILE",
        help="Path to checkstyle XML input file (deprecated in favor of positional FILEs).",
    )
    parser.add_argument(
        "-o",
        "--output",
        metavar="FILE",
        default="gl-code-quality-report.json",
        help="Path to output JSON file (default: gl-code-quality-report.json)",
    )
    parser.add_argument(
        "--pretty", action="store_true", help="Pretty-print JSON output"
    )
    parser.add_argument(
        "--base-dir",
        metavar="DIR",
        help="If set, emitted issue paths are made relative to this directory when possible.",
    )
    parser.add_argument(
        "files",
        nargs="*",
        metavar="FILE",
        help="One or more Checkstyle XML files. Use '-' to read from stdin (only once).",
    )
    return parser.parse_args(argv)


def compute_fingerprint(
    path: str, line: str, message: str, severity_raw: str, source: Optional[str]
) -> str:
    """Compute a stable fingerprint for an issue.

    Combining multiple attributes reduces collision risk across similar messages.
    """
    base = f"{path}:{line}:{severity_raw}:{message}:{source or ''}".encode("utf-8")
    return hashlib.sha256(base).hexdigest()


def _relativize(path: str, base_dir: Optional[str]) -> str:
    if not base_dir:
        return path
    try:
        p = pathlib.Path(path)
        base = pathlib.Path(base_dir)
        return str(p.relative_to(base))
    except Exception:
        return path  # Fallback to original if not relative


def _stream_issues_from_file(
    path: pathlib.Path, base_dir: Optional[str]
) -> Iterator[Issue]:
    """Memory-efficient streaming parser using iterparse.

    We use start/end events to track the current <file> context and emit issues
    when encountering <error> end events. Elements are cleared after their
    subtrees are processed to free memory for very large reports.
    """
    current_file: Optional[str] = None
    rel_file: Optional[str] = None
    for event, elem in ET.iterparse(str(path), events=("start", "end")):
        if event == "start" and elem.tag == "file":
            current_file = elem.get("name") or None
            if current_file:
                rel_file = _relativize(current_file, base_dir)
        elif event == "end" and elem.tag == "error" and current_file and rel_file:
            line = elem.get("line") or "1"
            message = elem.get("message") or "No description provided by Checkstyle."
            severity_raw = (elem.get("severity") or "").lower()
            severity = CONVERSION_MAP.get(severity_raw, "info")
            source = elem.get("source")
            fingerprint = compute_fingerprint(
                rel_file, line, message, severity_raw, source
            )
            yield {
                "description": message,
                "fingerprint": fingerprint,
                "severity": severity,
                "location": {
                    "path": rel_file,
                    "lines": {"begin": int(line) if line.isdigit() else 1},
                },
            }
            elem.clear()
        elif event == "end" and elem.tag == "file":
            elem.clear()
            current_file = None
            rel_file = None


def _issues_from_stdin(base_dir: Optional[str]) -> List[Issue]:
    data = sys.stdin.read()
    try:
        root = ET.fromstring(data)
    except ET.ParseError as e:
        raise SystemExit(f"Failed to parse XML from stdin: {e}") from e
    issues: List[Issue] = []
    for file_elem in root.findall("file"):
        file_path = file_elem.get("name")
        if not file_path:
            continue
        rel_path = _relativize(file_path, base_dir)
        for err in file_elem.findall("error"):
            line = err.get("line") or "1"
            message = err.get("message") or "No description provided by Checkstyle."
            severity_raw = (err.get("severity") or "").lower()
            severity = CONVERSION_MAP.get(severity_raw, "info")
            source = err.get("source")
            fingerprint = compute_fingerprint(
                rel_path, line, message, severity_raw, source
            )
            issues.append(
                {
                    "description": message,
                    "fingerprint": fingerprint,
                    "severity": severity,
                    "location": {
                        "path": rel_path,
                        "lines": {"begin": int(line) if line.isdigit() else 1},
                    },
                }
            )
    return issues


def convert_paths(
    inputs: Iterable[Union[pathlib.Path, str]], base_dir: Optional[str]
) -> List[Issue]:
    issues: List[Issue] = []
    for inp in inputs:
        if inp == "-":
            issues.extend(_issues_from_stdin(base_dir))
            continue
        path = pathlib.Path(str(inp))
        if not path.is_file():
            raise FileNotFoundError(f"Input file not found: {path}")
        try:
            for issue in _stream_issues_from_file(path, base_dir):
                issues.append(issue)
        except ET.ParseError as e:
            raise SystemExit(f"Failed to parse XML '{path}': {e}") from e
    return issues


def write_output(issues: List[Issue], output_path: pathlib.Path, pretty: bool) -> None:
    indent = 2 if pretty else None
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8") as f:
        json.dump(issues, f, indent=indent)
        if pretty:
            f.write("\n")


def main(argv: List[str]) -> int:
    args = parse_args(argv)

    # Resolve input list precedence: positional > -i > default
    input_files: List[str] = []
    if args.files:
        input_files.extend(args.files)
    elif args.input:
        input_files.append(args.input)
    else:
        input_files.append("checkstyle-result.xml")

    # Basic validation: only one '-' allowed
    if input_files.count("-") > 1:
        print("Only one stdin ('-') source allowed.", file=sys.stderr)
        return 4

    base_dir = args.base_dir
    if base_dir:
        base_dir = os.path.abspath(base_dir)

    try:
        issues = convert_paths(input_files, base_dir)
    except FileNotFoundError as e:
        print(str(e), file=sys.stderr)
        return 2
    except SystemExit as e:  # parse error forwarded
        print(str(e), file=sys.stderr)
        return 3
    except Exception as e:  # unexpected
        print(f"Unexpected error: {e}", file=sys.stderr)
        return 1

    output_path = pathlib.Path(args.output)
    write_output(issues, output_path, args.pretty)

    print(
        f"Converted {len(issues)} issue(s) from {len(input_files)} input file(s) -> '{output_path}'."
    )
    return 0


if __name__ == "__main__":  # pragma: no cover
    raise SystemExit(main(sys.argv[1:]))
