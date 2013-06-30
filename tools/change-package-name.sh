#!/bin/sh -x

## For changing the package name so that your app can be installed alongside 
## F-Droid. This script also changes the app name, but DOESN'T change the 
## URLs of the default repos, or the icons.

## Arguments: org.your.fdroid "Your FDroid"
## org.your.fdroid will be the new package id
## "Your FDroid" will be the name of the application

FDROID_PACKAGE=${1:-org.your.fdroid}
FDROID_NAME=${2:-Your FDroid}
FDROID_PATH=${FDROID_PACKAGE//./\/}

mkdir -p "src/${FDROID_PATH}"
perl -pi -e"s|org/fdroid/fdroid/R.java|${FDROID_PATH}/R.java|g" build.xml

find src/org/fdroid/ res/ -type f |xargs -n 1 perl -pi -e"s/org.fdroid.fdroid(?=\W)/${FDROID_PACKAGE}/g"
perl -pi -e"s|org.fdroid.fdroid|${FDROID_PACKAGE}|g" AndroidManifest.xml

mv src/org/fdroid/fdroid/* src/${FDROID_PATH}/
rm -rf src/org/fdroid/fdroid/

perl -pi -e"s|FDroid|${FDROID_NAME}|g" build.xml
find res/ -type f -print0 | xargs -0 sed -i "s/F-Droid/${FDROID_NAME}/g"

