#!/usr/bin/env python3

# Remove extra translations

import glob
import os
import sys
import re
from xml.etree import ElementTree

formatRe = re.compile(r'(%%|%[^%](\$.)?)')

simpleFormatRe = re.compile(r'%[ds]')
numberedFormatRe = re.compile(r'%[0-9]+\$')
validFormatRe = re.compile(r'^(%%|%[sd]|%[0-9]\$[sd])$')
oddQuotingRe = re.compile(r'^"\s*(.+?)\s*"$')

resdir = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res')

count = 0

for d in sorted(glob.glob(os.path.join(resdir, 'values-*'))):

    str_path = os.path.join(d, 'strings.xml')
    if not os.path.exists(str_path):
        continue

    with open(str_path, encoding='utf-8') as fp:
        fulltext = fp.read()

    tree = ElementTree.parse(str_path)
    root = tree.getroot()

    for e in root.findall('.//string') + root.findall('.//item'):
        if e.tag == "item" and e.text is None:
            continue

        found_simple_format = False  # %s
        found_numbered_format = False  # %1$s
        if e.text is None:
            fulltext = re.sub(r' *<string name="' + e.attrib['name'] + r'"> *</string> *\n?', '', fulltext)
            print('%s: %s is blank!' % (str_path, e.attrib['name']))
            count += 1
            continue
        for m in formatRe.finditer(e.text):
            s = m.group(0)
            if simpleFormatRe.match(s):
                found_simple_format = True
            if numberedFormatRe.match(s):
                found_numbered_format = True
            if validFormatRe.match(s):
                continue
            count += 1
            print('%s: Invalid format "%s" in "%s"' % (str_path, s, e.text))

        if found_simple_format and found_numbered_format:
            count += 1
            print('%s: Invalid mixed formats "%s" in "%s"' % (str_path, s, e.text))

        m = oddQuotingRe.search(e.text)
        if m:
            print('%s: odd quoting in %s' % (str_path, e.tag))
            print('found', fulltext.rfind(e.text))
            fulltext = fulltext.replace(e.text, m.group(1))
            count += 1
            if e.text != m.group(1):
                print(e.text, '-=<' + m.group(1) + '>=-')

    with open(str_path, 'w', encoding='utf-8') as fp:
        fp.write(fulltext)

if count > 0:
    print("%d misformatted strings found!" % count)
    sys.exit(1)

