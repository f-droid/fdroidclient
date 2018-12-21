#!/usr/bin/env python3
#

import glob
import os
import re

locale_pat = re.compile(r'.*values-([a-z][a-z][a-zA-Z-]*)/strings.xml')
translation_pat = re.compile(r'.*name="settings_button"[^>]*>"?([^"<]*).*')
for f in glob.glob('/home/hans/code/android.googlesource.com/packages/apps/Settings/res/values-[a-z][a-z]*/strings.xml'):
    m = locale_pat.search(f)
    if m:
        locale = m.group(1)
        if locale.endswith('-nokeys'):
            continue
    #print(locale)
    with open(f) as fp:
        m = translation_pat.search(fp.read())
        if m:
            word = m.group(1)
            print(locale, '\t', word)
            fdroid = '/home/hans/code/fdroid/client/app/src/main/res/values-' + locale + '/strings.xml'
            if os.path.exists(fdroid):
                with open(fdroid) as fp:
                    data = fp.read()
                with open(fdroid, 'w') as fp:
                    fp.write(re.sub(r'menu_settings">[^<]+</string', 'menu_settings">' + word + '</string', data))
