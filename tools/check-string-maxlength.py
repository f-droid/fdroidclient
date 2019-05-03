#!/usr/bin/env python3

# Remove extra translations

import glob
import os
import sys
import re
from xml.etree import ElementTree

maxlengths = {
    "menu_install": 20,
    "menu_uninstall": 20,
    "nearby_splash__find_people_button": 30,
    "nearby_splash__request_permission": 30,
    "update_all": 20,
    "updates__hide_updateable_apps": 35,
    "updates__show_updateable_apps": 35,
}


resdir = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res')

count = 0

for d in sorted(glob.glob(os.path.join(resdir, 'values-*'))):
    locale = d.split('/')[-1][7:]

    str_path = os.path.join(d, 'strings.xml')
    if not os.path.exists(str_path):
        continue

    with open(str_path, encoding='utf-8') as fp:
        fulltext = fp.read()

    tree = ElementTree.parse(str_path)
    root = tree.getroot()

    for e in root.findall('.//string'):

        if maxlengths.get(e.attrib['name']) is not None \
           and len(e.text) > maxlengths.get(e.attrib['name']):
            print(e.attrib['name'], locale, str(len(e.text)) + ':\t\t"' + e.text + '"')

if count > 0:
    print("%d over-long strings found!" % count)
    sys.exit(count)

