#!/usr/bin/env python3

import glob
import json
import os

from xml.etree import ElementTree

os.chdir(os.path.dirname(__file__) + '/..')

for f in sorted(glob.glob('../fdroid-website/_data/*/strings.json')):
    with open(f) as fp:
        strings = json.load(fp)
    banner = strings.get('banners')
    if banner:
        segments = f.split('/')[3].split('_')
        if len(segments) == 1:
            locale = segments[0]
        else:
            locale = f'{segments[0]}-r{segments[1]}'
        strings_xml = f'app/src/main/res/values-{locale}/strings.xml'
        if os.path.exists(strings_xml):
            root = ElementTree.parse(strings_xml)
            print(locale, strings_xml, banner)
            for resources in root.iter('resources'):
                print(resources)
                for k, v in banner.items():
                    new_string = ElementTree.Element("string", name=k)
                    new_string.text = v
                    resources.append(new_string)
                    ElementTree.indent(resources, space='    ')
                break
            root.write(strings_xml, encoding='utf-8', xml_declaration=True)


