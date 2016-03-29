#!/usr/bin/env python3

# Remove extra translations

import glob
import os
import sys
import re
from xml.etree import ElementTree

formatRe = re.compile(r'(%%|%[^%](\$.)?)')

validFormatRe = re.compile(r'^(%%|%[sd]|%[0-9]\$[sd])$')

count = 0

for d in glob.glob(os.path.join('src', 'main', 'res', 'values-*')):

    str_path = os.path.join(d, 'strings.xml')
    if not os.path.exists(str_path):
        continue

    tree = ElementTree.parse(str_path)
    root = tree.getroot()

    for e in root.findall('.//string'):
        for m in formatRe.finditer(e.text):
            s = m.group(0)
            if validFormatRe.match(s):
                continue
            count += 1
            print('%s: Invalid format "%s" in "%s"' % (str_path, s, e.text))

if count > 0:
    print("%d misformatted strings found!" % count)
    sys.exit(1)

