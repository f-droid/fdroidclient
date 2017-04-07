#!/usr/bin/python3

import csv
import git
import os
import requests


projectbasedir = os.path.dirname(os.path.dirname(__file__))
print(projectbasedir)

repo = git.Repo(projectbasedir)

msg = 'removing all translations less than 75% complete\n\n'

url = 'https://hosted.weblate.org/exports/stats/f-droid/f-droid/?format=csv'
r = requests.get(url)
stats = csv.reader(r.iter_lines(decode_unicode=True), delimiter=',')
next(stats)  # skip CSV header
for row in stats:
    if len(row) > 4:
        if float(row[4]) > 75.0:
            continue
        locale = row[1]
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
        os.remove(os.path.join(projectbasedir, translation_file))
        repo.index.remove([translation_file, ])
        if len(percent) == 2:
            msg += ' '
        msg += percent + '  ' + row[1] + '  ' + row[0] + '\n'

repo.index.commit(msg)
