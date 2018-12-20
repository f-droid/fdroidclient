#!/bin/sh -ex

toolsdir=`cd $(dirname $0); pwd`

cd $toolsdir/..
git remote update -p weblate
git checkout -b weblate weblate/master

firefox https://hosted.weblate.org/projects/f-droid/f-droid/#repository

$toolsdir/fix-ellipsis.sh
$toolsdir/check-format-strings.py
$toolsdir/remove-unused-and-blank-translations.py

git gui
git push origin weblate
