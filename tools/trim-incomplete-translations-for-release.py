#!/usr/bin/python3
#
# WARNING!  THIS DELETES TRANSLATIONS!
#
# The incomplete translations should be kept by rebasing the weblate
# remote on top of this commit, once its complete.

import csv
import git
import os
import requests
import sys


url = 'https://hosted.weblate.org/api/components/f-droid/f-droid/statistics/'
r = requests.get(url)
stats = r.json()["results"]
locales_config = set()
for row in stats:
    locale = row["code"]
    translated_percent = float(row["translated_percent"])
    percent = str(int(translated_percent)) + '%'
    if translated_percent > 70.0:
        if locale == 'nb_NO':
            locale = 'nb'
        elif locale == 'yue_Hant':
            locale = 'yue'
        elif locale == 'zh_Hans':
            locale = 'zh-CN'
        elif locale == 'zh_Hant':
            locale = 'zh-TW'
        elif locale == 'zh_Hant_HK':
            locale = 'zh-HK'
        locales_config.add(locale.replace('_', '-'))
        print('[+] Adding locale: ' + locale + ' (' + percent + ')')
        continue
    if '_' in locale:
        codes = locale.split('_')
        if codes[1] == 'Hans':
            codes[1] = 'CN'
        elif codes[1] == 'Hant':
            codes[1] = 'TW'
        locale = codes[0] + '-r' + codes[1]
    translation_file = 'app/src/main/res/values-' + locale + '/strings.xml'
    print('[-] Ignoring incomplete locale: ' + locale + ' (' + percent + ')')

if len(locales_config) == 0:
    print("ERROR: Did not get any locales")
    sys.exit(1)

with open('app/src/main/res/xml/locales_config.xml', 'w') as fp:
    fp.write("""<?xml version="1.0" encoding="utf-8"?>
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
    <locale android:name="en-US" />""")
    locales_config.remove('en')
    fp.write('\n')
    for locale in sorted(locales_config):
        fp.write(f'    <locale android:name="{locale}" />\n')
    fp.write('</locale-config>\n')
