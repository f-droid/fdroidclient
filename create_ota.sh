#!/bin/bash
#
# Script to prepare an update.zip containing F-Droid

set -e

PROG_DIR=$(dirname $(realpath $0))

TMP_DIR=$(mktemp -d -t fdroidclient.tmp.XXXXXXXX)
trap "rm -rf $TMP_DIR" EXIT

function error() {
    echo "*** ERROR: " $@
    usage
}

function usage() {
    cat << EOFU
Usage: $0 variant
where:
 - variant is one of: debug, release, or binary
EOFU
    exit 1
}

# Parse input
VARIANT="$1"
[[ -z "$VARIANT" ]] && error "Missing variant"

VERSIONCODE=$2

GPG="gpg --keyring $PROG_DIR/f-droid.org-signing-key.gpg --no-default-keyring --trust-model always"

GITVERSION=$(git describe --tags --always)

FDROID_APK=F-Droid.apk

# Collect files
mkdir -p $TMP_DIR/META-INF/com/google/android/
cp app/src/main/scripts/update-binary $TMP_DIR/META-INF/com/google/android/

if [ $VARIANT == "binary" ] ; then
    if [ -z $VERSIONCODE ]; then
        curl -L https://f-droid.org/$FDROID_APK > $TMP_DIR/$FDROID_APK
        curl -L https://f-droid.org/${FDROID_APK}.asc > $TMP_DIR/${FDROID_APK}.asc
    else
        GITVERSION=$VERSIONCODE
        DL_APK=org.fdroid.fdroid_${VERSIONCODE}.apk
        curl -L https://f-droid.org/repo/$DL_APK > $TMP_DIR/$FDROID_APK
        curl -L https://f-droid.org/repo/${DL_APK}.asc > $TMP_DIR/${FDROID_APK}.asc
    fi
    $GPG --verify $TMP_DIR/${FDROID_APK}.asc
    rm $TMP_DIR/${FDROID_APK}.asc
else
    cd $PROG_DIR
    ./gradlew assemble$(echo $VARIANT | tr 'dr' 'DR')
    OUT_DIR=$PROG_DIR/app/build/outputs/apk
    if [ $VARIANT == "debug" ]; then
        cp $OUT_DIR/app-${VARIANT}.apk \
           $TMP_DIR/$FDROID_APK
    elif [ -f $OUT_DIR/app-${VARIANT}-signed.apk ]; then
        cp $OUT_DIR/app-${VARIANT}-signed.apk \
           $TMP_DIR/$FDROID_APK
    else
        cp $OUT_DIR/app-${VARIANT}-unsigned.apk \
           $TMP_DIR/$FDROID_APK
    fi
fi

# Make zip
if [ $VARIANT == "binary" ] ; then
    ZIPBASE=F-DroidFromBinaries-${GITVERSION}
else
    ZIPBASE=F-Droid-${GITVERSION}
fi
if [ $VARIANT == "debug" ]; then
    ZIP=${ZIPBASE}-debug.zip
else
    ZIP=${ZIPBASE}.zip
fi
OUT_DIR=$PROG_DIR/app/build/distributions
mkdir -p $OUT_DIR
[ -f $OUT_DIR/$ZIP ] && rm -f $OUT_DIR/$ZIP
pushd $TMP_DIR
zip -r $OUT_DIR/$ZIP .
popd
