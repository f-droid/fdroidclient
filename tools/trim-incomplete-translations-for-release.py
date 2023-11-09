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


projectbasedir = os.path.dirname(os.path.dirname(__file__))
print(projectbasedir)

repo = git.Repo(projectbasedir)

msg = 'removing all translations less than 70% complete\n\n'

url = 'https://hosted.weblate.org/exports/stats/f-droid/f-droid/?format=csv'
r = requests.get(url)
stats = csv.reader(r.iter_lines(decode_unicode=True), delimiter=',')
next(stats)  # skip CSV header
locales_config = set()
for row in stats:
    if len(row) > 4:
        locale = row[1]
        if float(row[4]) > 70.0:
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
            continue
        if '_' in locale:
            codes = locale.split('_')
            if codes[1] == 'Hans':
                codes[1] = 'CN'
            elif codes[1] == 'Hant':
                codes[1] = 'TW'
            locale = codes[0] + '-r' + codes[1]
        translation_file = 'app/src/main/res/values-' + locale + '/strings.xml'
        percent = str(int(float(row[4]))) + '%'
        print('Removing incomplete file: (' + percent + ')\t',
              translation_file)
        delfile = os.path.join(projectbasedir, translation_file)
        if os.path.exists(delfile):
            os.remove(delfile)
            repo.index.remove([translation_file, ])
        if len(percent) == 2:
            msg += ' '
        msg += percent + '  ' + row[1] + '  ' + row[0] + '\n'

with open('app/src/main/res/xml/locales_config.xml', 'w') as fp:
    fp.write("""<?xml version="1.0" encoding="utf-8"?>
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
    <locale android:name="en-US" />""")
    locales_config.remove('en')
    fp.write('\n')
    for locale in sorted(locales_config):
        fp.write(f'    <locale android:name="{locale}" />\n')
    fp.write('</locale-config>\n')

found = False
for remote in repo.remotes:
    if remote.name == 'weblate':
        remote.fetch()
        found = True

if not found:
    print('ERROR: there must be a weblate remote to preserve incomplete translations!')
    sys.exit(1)

repo.index.commit(msg)
