## For changing the package name so that your app can be installed alongside 
## F-Droid. This script also changes the app name, but DOESN'T change the 
## URLs of the default repos, or the icons.

#! /bin/sh
NEW_PACKAGE=org.your.fdroid
NEW_PATH=org/your/fdroid
NEW_PROJ_NAME="Your App"
mkdir -p src/org/your
perl -pi -e"s|org/fdroid/fdroid/R.java|$NEW_PATH/R.java|g" build.xml
find src/org/fdroid res -type f |xargs -n 1 perl -pi -e"s/org.fdroid.fdroid(?=\W)/$NEW_PACKAGE/g"
perl -pi -e"s|org.fdroid.fdroid|$NEW_PACKAGE|g" AndroidManifest.xml
mv src/org/fdroid/fdroid src/$NEW_PATH
perl -pi -e"s|FDroid|$NEW_PROJ_NAME|g" build.xml
find res/ -type f -print0 | xargs -0 sed -i 's/F-Droid/Your App/g'
ant $@

