#!/bin/sh
scp fdroid@f-droid.org:/home/fdroid/public_html/translate/po/fdroidclient/*.po locale
a2po import
