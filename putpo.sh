#!/bin/sh
a2po export
scp locale/*.po* fdroid@f-droid.org:/home/fdroid/public_html/translate/po/fdroidclient/
ssh fdroid@f-droid.org -C "/home/fdroid/public_html/translate/manage.py update_stores"
ssh fdroid@f-droid.org -C "chmod 640 /home/fdroid/public_html/translate/po/fdroidclient/*"
ssh fdroid@f-droid.org -C "chown fdroid:www-data /home/fdroid/public_html/translate/po/fdroidclient/*"
