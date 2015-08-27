#!/usr/bin/env python2

# Remove extra translations

import glob
import os
import re
from xml.etree import ElementTree

strings = set()
arrays = set()

for e in ElementTree.parse(os.path.join('res', 'values', 'strings.xml')).getroot().findall('.//string'):
    name = e.attrib['name']
    strings.add(name)

for e in ElementTree.parse(os.path.join('res', 'values', 'array.xml')).getroot().findall('.//string-array'):
    name = e.attrib['name']
    arrays.add(name)

for d in glob.glob(os.path.join('res', 'values-*')):

    str_path = os.path.join(d, 'strings.xml')
    if os.path.exists(str_path):
        tree = ElementTree.parse(str_path)
        root = tree.getroot()

        elems = root.findall('.//string')
        for e in elems:
            name = e.attrib['name']
            if name not in strings:
                root.remove(e)

        result = re.sub(r' />', r'/>', ElementTree.tostring(root, encoding='utf-8'))

        with open(str_path, 'w+') as f:
            f.write('<?xml version="1.0" encoding="utf-8"?>\n')
            f.write(result)
            f.write('\n')

    arr_path = os.path.join(d, 'array.xml')
    if os.path.exists(arr_path):
        tree = ElementTree.parse(arr_path)
        root = tree.getroot()

        elems = root.findall('.//string-array')
        for e in elems:
            name = e.attrib['name']
            if name not in arrays:
                root.remove(e)

        result = re.sub(r' />', r'/>', ElementTree.tostring(root, encoding='utf-8'))

        with open(arr_path, 'w+') as f:
            f.write('<?xml version="1.0" encoding="utf-8"?>\n')
            f.write(result)
            f.write('\n')
