#!/bin/bash -ex

if ! which android > /dev/null; then
    if [ -z $ANDROID_HOME ]; then
        if [ -e ~/.android/bashrc ]; then
            . ~/.android/bashrc
        else
            echo "'android' not found, ANDROID_HOME must be set!"
            exit
        fi
    else
        export PATH="${ANDROID_HOME}/tools:$PATH"
    fi
fi

# set up test signing keys for any `ant release` runs
if [ -e ~/.android/ant.properties ]; then
    cp ~/.android/ant.properties ./
else
    echo "skipping release ant.properties"
fi


EXTERN=../extern

place_support_v4() {
    mkdir -p $1/libs
    cp libs/android-support-v4.jar $1/libs/
}

android update lib-project --path $EXTERN/UniversalImageLoader/library

android update lib-project --path $EXTERN/AndroidPinning

android update lib-project --path $EXTERN/libsuperuser/libsuperuser

android update lib-project --path $EXTERN/zxing-core

android update lib-project --path $EXTERN/support-v4-preferencefragment
place_support_v4 $EXTERN/support-v4-preferencefragment

android update lib-project --path $EXTERN/Support/v7/appcompat --target android-19
place_support_v4 $EXTERN/Support/v7/appcompat

android update project --path . --name F-Droid

{ echo -e "\nSuccessfully updated the main project.\n"; } 2>/dev/null

# technically optional, needed for the tests
cd test
android update test-project --path . --main ..

{ echo -e "\nSuccessfully updated the test project.\n"; } 2>/dev/null
