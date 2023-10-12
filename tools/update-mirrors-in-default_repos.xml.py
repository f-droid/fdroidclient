#!/usr/bin/env python3
#
# Fetch the official mirrors from f-droid.org and update default_repos.xml.

import os
import requests
import lxml.etree as ET

os.chdir(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

r = requests.get('https://f-droid.org/repo/index-v2.json')
mirrors = []
for mirror in r.json()['repo']['mirrors']:
    mirrors.append(mirror['url'])

default_repos = 'app/src/main/res/values/default_repos.xml'
tree = ET.parse(default_repos)
root = tree.getroot()
i = 0
indent = '\n            '
for item in root.iter('item'):
    if i == 1:
        item.text = indent + (indent.join(mirrors).replace('/repo', '/archive')) + '\n        '
    elif i == 8:
        item.text = indent + (indent.join(mirrors)) + '\n        '
    i += 1

tree.write(default_repos)
