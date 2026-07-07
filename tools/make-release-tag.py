#!/usr/bin/env python3

"""A script to check everything is in order, then make the release tag."""

import git
import re
import sys

from pathlib import Path


def main():
    project_dir = Path(__file__).parent.parent

    for line in (project_dir / 'app/build.gradle.kts').open():
        m = re.search(r"""versionName\s*=\s*["']([^"']+)["']""", line)
        if m:
            current_version = m.group(1)
            break

    changelog_file = project_dir / "CHANGELOG.md"
    changelog = changelog_file.read_text()
    versions = list()
    entries = dict()
    for entry in changelog.split('###'):
        if not entry:
            continue
        m = re.match(r" *(\S+) *\([0-9-]+\)\n(.*)", entry, flags=re.DOTALL)
        if not m:
            print('ERROR: broken entry format in CHANGELOG.md:', entry)
            sys.exit(1)
        version = m.group(1)
        versions.append(version)
        entries[version] = m.group(2).strip()

    if current_version not in versions:
        print(f"ERROR: CHANGELOG.md missing entry for {current_version}")
        sys.exit(1)

    repo = git.Repo(project_dir)
    for tag in sorted(
        repo.tags,
        key=lambda t: t.tag.tagged_date if t.tag is not None else 0,
        reverse=True,
    ):
        if tag.name == current_version:
            print(f"ERROR: current version {tag.name} tag exists at {tag.commit}")
            sys.exit(1)

    message = entries[current_version]
    repo.create_tag(current_version, message=message, sign=True)
    print(f'Tagged current version {current_version} with changelog entry:\n\n{message}')


if __name__ == "__main__":
    main()
