#!/usr/bin/env python3
#
# Fetch the official mirrors from f-droid.org and update default_repos.json.

import os
import json
import requests
import lxml.etree as ET

REPO_URL = "https://f-droid.org/repo"
ARCHIVE_URL = "https://f-droid.org/archive"

os.chdir(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# get mirrors from index
r = requests.get('https://f-droid.org/repo/index-v2.json')
mirrors = []
archive_mirrors = []
for mirror in r.json()['repo']['mirrors']:
    if mirror['url'] != REPO_URL:
        mirrors.append(mirror['url'])
        archive_mirrors.append(mirror['url'].replace('/repo', '/archive'))

# default_repos.json
default_repos = 'app/src/main/assets/default_repos.json'
with open(default_repos, 'r') as json_file:
    data = json.load(json_file)
for repo in data:
    if repo["address"] == REPO_URL:
        repo["mirrors"] = mirrors
    elif repo["address"] == ARCHIVE_URL:
        repo["mirrors"] = archive_mirrors
with open(default_repos, 'w') as json_file:
    json.dump(data, json_file, indent=2)

# legacy XML default repos
default_repos_legacy = 'legacy/src/main/res/values/default_repos.xml'
tree = ET.parse(default_repos_legacy)
root = tree.getroot()
i = 0
indent = '\n            '
for item in root.iter('item'):
    if i == 1:
        item.text = indent + (indent.join([REPO_URL] + mirrors)) + '\n        '
    elif i == 8:
        item.text = indent + (indent.join([ARCHIVE_URL] + mirrors).replace('/repo', '/archive')) + '\n        '
    i += 1

tree.write(default_repos_legacy)
