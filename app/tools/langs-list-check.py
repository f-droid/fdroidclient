#!/usr/bin/env python3

# List supported languages missing from the preference array

import glob
import os
import sys
import re
from xml.etree import ElementTree

prefs = set([''])
trans = set(['', 'en'])

donottranslate = os.path.join('src', 'main', 'res', 'values', 'donottranslate.xml')

for e in ElementTree.parse(donottranslate).getroot().findall('.//string-array'):
    if e.attrib['name'] != 'languageValues':
        continue
    for i in e.findall('.//item'):
        lang = i.text
        if not lang:
            continue
        prefs.add(lang)

for d in glob.glob(os.path.join('src', 'main', 'res', 'values-*')):
    lang = d[len(os.path.join('src', 'main', 'res', 'values-')):]
    if not lang:
        continue
    if re.match('^v[0-9]+$', lang):
        continue
    if lang == 'ldrtl':
        continue
    if os.path.islink(d):
        continue
    trans.add(lang)

print("In the settings array: %s" % ' '.join(prefs))
print("Actually translated:   %s" % ' '.join(trans))

missing = []
for lang in trans:
    if lang not in prefs:
        missing.append(lang)

if missing:
    print("Missing:")
    for lang in missing:
        print("  %s" % lang)

extra = []
for lang in prefs:
    if lang not in trans:
        extra.append(lang)

if extra:
    print("Extra:")
    for lang in extra:
        print("  %s" % lang)

if not missing and not extra:
    print("All good.")
else:
    sys.exit(1)
