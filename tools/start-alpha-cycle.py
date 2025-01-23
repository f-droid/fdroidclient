#!/usr/bin/env python3
#
# https://gitlab.com/fdroid/wiki/-/wikis/Internal/Release-Process#versioning
# https://gitlab.com/fdroid/wiki/-/wikis/Internal/Release-Process#changelogs

import git
import os
import re
import sys
from pathlib import Path

projectdir = Path(__file__).parent.parent
os.chdir(projectdir)
repo = git.Repo.init('.')

tag = str(sorted(repo.tags, key=lambda t: t.commit.committed_datetime, reverse=True)[0])
if not re.match(r'[0-9]+\.[0-9]+\.[0-9]+\Z', tag):
    print(f'ERROR: most recent tag "{tag}" is not stable release!')
    sys.exit(1)
print(f'Working off of {tag} release')

vc_pat = re.compile(r"versionCode +([1-9][0-9]{6,})")
versionCode = int(vc_pat.search(repo.git.show(f"{tag}:app/build.gradle")).group(1))
print(f"Working off of {tag} release (versionCode {versionCode}).")

if versionCode % 1000 == 0:
    print(versionCode, "is already first alpha")
    sys.exit(1)
elif versionCode % 1000 >= 50:
    print(versionCode, "is stable release")

for f in sorted(projectdir.glob('metadata/*/changelogs/[0-9]*.txt'), reverse=True):
    changelog_version = int(Path(f).name[:-4])
    if versionCode <= changelog_version:
        print(
            f'ERROR: {versionCode} is newer than changelog version {changelog_version}!'
        )
        sys.exit(1)

default_file = 'metadata/en-US/changelogs/default.txt'
if not Path(default_file).exists():
    print(f'ERROR: {default_file} does not exist!')
    sys.exit(1)

newvc = ((versionCode // 1000) + 1) * 1000
print(f'New alpha versionCode: {newvc}')
build_gradle = Path('app/build.gradle')
build_gradle.write_text(vc_pat.sub(f'versionCode {newvc}', build_gradle.read_text()))

for f in projectdir.glob('metadata/*/changelogs/default.txt'):
    vcf = f.parent / f'{versionCode}{f.suffix}'
    repo.git.mv(f, vcf)
