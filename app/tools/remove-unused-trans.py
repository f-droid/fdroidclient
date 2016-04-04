#!/usr/bin/env python3

# Remove extra translations

import glob
import os
import re
from xml.etree import ElementTree

strings = set()

for e in ElementTree.parse(os.path.join('src', 'main', 'res', 'values', 'strings.xml')).getroot().findall('.//string'):
    name = e.attrib['name']
    strings.add(name)

for d in glob.glob(os.path.join('src', 'main', 'res', 'values-*')):

    str_path = os.path.join(d, 'strings.xml')
    if os.path.exists(str_path):
        header = ''
        with open(str_path, 'r') as f:
            header = f.readline()
        tree = ElementTree.parse(str_path)
        root = tree.getroot()

        elems = root.findall('.//string')
        for e in elems:
            name = e.attrib['name']
            if name not in strings:
                root.remove(e)
            if not e.text:
                root.remove(e)

        result = re.sub(r' />', r'/>', ElementTree.tostring(root, encoding='utf-8').decode('utf-8'))

        with open(str_path, 'w+') as f:
            f.write(header)
            f.write(result)
            f.write('\n')
