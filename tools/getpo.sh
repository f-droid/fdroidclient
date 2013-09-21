#!/bin/sh
ssh fdroid@f-droid.org -C "/home/fdroid/public_html/translate/manage.py sync_stores"
scp fdroid@f-droid.org:/home/fdroid/public_html/translate/po/fdroidclient/*.po locale
a2po import
