#!/bin/sh
a2po export
scp locale/*.po* fdroid@f-droid.org:/home/fdroid/public_html/translate/po/fdroidclient/
