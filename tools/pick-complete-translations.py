#!/usr/bin/python3
#
# cherry-pick complete translations from weblate

import git
import os
import re
import requests
import sys


def get_paths_tuple(locale):
    return (
        'metadata/%s/*.txt' % locale,
        'metadata/%s/changelogs/*.txt' % locale,
        'app/src/main/res/values-%s/strings.xml' % re.sub(r'-', r'-r', locale),
    )

projectbasedir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
print(projectbasedir)

repo = git.Repo(projectbasedir)
weblate = repo.remotes.weblate
weblate.fetch()
upstream = repo.remotes.upstream
upstream.fetch()

url = 'https://hosted.weblate.org/exports/stats/f-droid/f-droid/?format=json'
r = requests.get(url)
r.raise_for_status()
app = r.json()

url = 'https://hosted.weblate.org/exports/stats/f-droid/f-droid-metadata/?format=json'
r = requests.get(url)
r.raise_for_status()
metadata = r.json()


#with open('f-droid-metadata.json') as fp:
#    metadata = json.load(fp)

app_locales = dict()
metadata_locales = dict()

merge_locales = []
for locale in app:
    app_locales[locale['code']] = locale
for locale in metadata:
    metadata_locales[locale['code']] = locale

for locale in sorted(app_locales.keys(), reverse=True):
    a = app_locales.get(locale)
    m = metadata_locales.get(locale)
    if m:
        print('%10s' % locale, a['translated_percent'], a['failing'], m['translated_percent'], m['failing'],
              sep='\t')
    else:
        print('%10s' % locale, a['translated_percent'], a['failing'], sep='\t')
    if a is not None and a['translated_percent'] == 100 and a['failing'] == 0 \
       and m is not None and m['translated_percent'] == 100 and m['failing'] == 0:
        print(locale)
        merge_locales.append(locale)

if not merge_locales:
    sys.exit()

if 'merge_weblate' in repo.heads:
    merge_weblate = repo.heads['merge_weblate']
    repo.create_tag('previous_merge_weblate', ref=merge_weblate,
                    message=('Automatically created by %s' % __file__))
else:
    merge_weblate = repo.create_head('merge_weblate')
merge_weblate.set_commit(upstream.refs.master)
merge_weblate.checkout()

email_pattern = re.compile(r'by (.*?) <(.*)>$')

for locale in sorted(merge_locales):
    commits = list(repo.iter_commits(
        str(weblate.refs.master) + '...' + str(upstream.refs.master),
        paths=get_paths_tuple(locale), max_count=10))
    for commit in reversed(commits):
        repo.git.cherry_pick(str(commit))
        m = email_pattern.search(commit.summary)
        if m:
            email = m.group(1) + ' <' + m.group(2) + '>'
