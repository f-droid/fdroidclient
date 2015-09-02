#!/usr/bin/env python2

# List supported languages missing from the preference array

import glob
import os
import re
from xml.etree import ElementTree

prefs = set([''])
trans = set([''])

donottranslate = os.path.join('res', 'values', 'donottranslate.xml')

for e in ElementTree.parse(donottranslate).getroot().findall('.//string-array'):
    if e.attrib['name'] != 'languageValues':
        continue
    for i in e.findall('.//item'):
        lang = i.text
        if not lang:
            continue
        prefs.add(lang)

for d in glob.glob(os.path.join('res', 'values-*')):
    lang = d[len(os.path.join('res', 'values-')):]
    if not lang:
        continue
    if re.match('^v[0-9]+$', lang):
        continue
    trans.add(lang)

for lang in trans:
    if lang not in prefs:
        print lang
