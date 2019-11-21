#!/usr/bin/env python3
#

import glob
import os
import re
from xml.etree import ElementTree

android_dir = os.path.join(os.path.dirname(__file__),'../../../android.googlesource.com')
fdroidclient_dir = os.path.join(os.path.dirname(__file__), '..')

res_glob = 'res/values-[a-z][a-z]*/strings.xml'

locale_pat = re.compile(r'.*values-([a-z][a-z][a-zA-Z-]*)/strings.xml')
translation_pat = re.compile(r'.*name="([a-zA-Z0-9_]+)"[^>]*>"?([^"<]*).*')

keymap = {
    'launch': 'menu_launch',
    'permissions_label': 'permissions',
    'print_menu_item_search': 'menu_search',
    'radioInfo_data_connecting': 'swap_connecting',
    'settings_button': 'menu_settings',
}


tree = ElementTree.parse(os.path.join(fdroidclient_dir, 'app/src/main/res/values/strings.xml'))
root = tree.getroot()
keys = set()
for e in root.findall('.//string'):
    if e.text is None:
        continue
    keys.add(e.attrib['name'])

# remove the false friends
for k in (
        'app_name',
        'install_error_unknown',
        'notification_content_single_installing',
        'uninstall_error_unknown',):
    keys.remove(k)

for f in sorted(glob.glob(android_dir + '/frameworks/base/packages/*/' + res_glob)
                + glob.glob(android_dir + '/packages/apps/*/' + res_glob)):
    m = locale_pat.search(f)
    if m:
        locale = m.group(1)
        if locale.endswith('nokeys') or locale.endswith('television') or locale.endswith('watch'):
            continue
    #print(locale)
    with open(f) as fp:
        contents = fp.read()
    for m in translation_pat.finditer(contents):
        key = m.group(1)
        key = keymap.get(key, key)
        if key not in keys:
            continue
        word = m.group(2)
        fdroid = fdroidclient_dir + '/app/src/main/res/values-' + locale + '/strings.xml'
        if os.path.exists(fdroid):
            print(locale, '\t', key, '\t', word)
            with open(fdroid) as fp:
                data = fp.read()
            with open(fdroid, 'w') as fp:
                fp.write(re.sub(r'"' + key + r'">[^<]+</string',
                                r'"' + key + '">' + word + '</string',
                                data))
