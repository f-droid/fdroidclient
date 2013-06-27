#!/bin/sh -x

## For changing the package name so that your app can be installed alongside 
## F-Droid. This script also changes the app name, but DOESN'T change the 
## URLs of the default repos, or the icons.

## Arguments: org.your.fdroid "Your FDroid"
## org.your.fdroid will be the new package id
## "Your FDroid" will be the name of the application

PACKAGE=${1:-org.your.fdroid}
NAME=${2:-Your FDroid}
PATH=${PACKAGE//./\/}

mkdir -p "src/$PATH"
perl -pi -e"s|org/fdroid/fdroid/R.java|$PATH/R.java|g" build.xml

find src/org/fdroid/ res/ -type f |xargs -n 1 perl -pi -e"s/org.fdroid.fdroid(?=\W)/$PACKAGE/g"
perl -pi -e"s|org.fdroid.fdroid|$PACKAGE|g" AndroidManifest.xml

mv src/org/fdroid/fdroid/* src/$PATH/
rm -rf src/org/fdroid/fdroid/

perl -pi -e"s|FDroid|$NAME|g" build.xml
find res/ -type f -print0 | xargs -0 sed -i "s/F-Droid/$NAME/g"

