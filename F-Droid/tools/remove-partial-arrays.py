#!/usr/bin/env python2

# Remove translated arrays that are missing elements

import glob
import os
import re
from xml.etree import ElementTree

number = dict()

for e in ElementTree.parse(os.path.join('res', 'values', 'array.xml')).getroot().findall('.//string-array'):
    name = e.attrib['name']
    count = len(e.findall('item'))
    number[name] = count

for d in glob.glob(os.path.join('res', 'values-*')):

    arr_path = os.path.join(d, 'array.xml')
    if os.path.exists(arr_path):
        tree = ElementTree.parse(arr_path)
        root = tree.getroot()

        elems = root.findall('.//string-array')
        for e in elems:
            name = e.attrib['name']
            count = len(e.findall('item'))
            if count != number[name]:
                root.remove(e)

        result = re.sub(r' />', r'/>', ElementTree.tostring(root, encoding='utf-8'))

        with open(arr_path, 'w+') as f:
            f.write('<?xml version="1.0" encoding="utf-8"?>\n')
            f.write(result)
            f.write('\n')
