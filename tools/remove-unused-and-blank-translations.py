#!/usr/bin/env python3

# This script removes strings from the translated files that are not useful:
#  * translations for strings that are no longer used
#  * empty translated strings, English is better than no text at all

import glob
import os
import re
import sys
from xml.etree import ElementTree

count = 0

resdir = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res')
sourcepath = os.path.join(resdir, 'values', 'strings.xml')

strings = set()
for e in ElementTree.parse(sourcepath).getroot().findall('.//string'):
    name = e.attrib['name']
    strings.add(name)

for d in sorted(glob.glob(os.path.join(resdir, 'values-*'))):

    str_path = os.path.join(d, 'strings.xml')
    if not os.path.exists(str_path):
        continue

    with open(str_path, 'rb') as fp:
        body = fp.read()
    body = re.sub(b'.*<item quantity="[a-z]+"/>.*\n', b'', body)
    # Weblate is not handling plurals right https://github.com/WeblateOrg/weblate/issues/520
    language = os.path.basename(d)[7:9]  # careful, its not the whole locale!
    if language in ('ja', 'ko', 'zh'):
        body = re.sub(b'<item quantity="one">', b'<item quantity="other">', body)
    with open(str_path, 'wb') as fp:
        fp.write(body)

    header = ''
    with open(str_path, 'r') as f:
        header = f.readline()

    # handling XML namespaces in Python is painful, just remove them, they
    # should not be in the translation files anyway
    with open(str_path, 'rb') as fp:
        contents = fp.read()
    contents = contents.replace(b' tools:ignore="UnusedResources"', b'') \
                       .replace(b' xmlns:tools="http://schemas.android.com/tools"', b'')
    root = ElementTree.fromstring(contents)

    for e in root.findall('.//string'):
        name = e.attrib['name']
        if name not in strings:
            root.remove(e)
        if not e.text:
            root.remove(e)

    for e in root.findall('.//plurals'):
        found_other = False
        for item in e.findall('item'):
            if not item.text:
                e.remove(item)
            elif item.attrib['quantity'] == 'other':
                found_other = True
        if not found_other and language not in ('be', 'pl', 'ru'):
            print(os.path.relpath(str_path) + ': Missing "other" string in', e.attrib['name'])
            count += 1

    result = re.sub(r' />', r'/>', ElementTree.tostring(root, encoding='utf-8').decode('utf-8'))

    with open(str_path, 'w+') as f:
        f.write(header)
        f.write(result)
        f.write('\n')

sys.exit(count)
