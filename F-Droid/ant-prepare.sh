#!/bin/bash -ex

EXTERN=../extern

place_support_v4() {
    mkdir -p $1/libs
    cp libs/android-support-v4.jar $1/libs/
}

android update lib-project --path $EXTERN/UniversalImageLoader/library

android update lib-project --path $EXTERN/AndroidPinning

android update lib-project --path $EXTERN/MemorizingTrustManager

android update lib-project --path $EXTERN/libsuperuser/libsuperuser

android update lib-project --path $EXTERN/zxing-core

android update lib-project --path $EXTERN/android-support-v4-preferencefragment
place_support_v4 $EXTERN/android-support-v4-preferencefragment

android update lib-project --path $EXTERN/Support/v7/appcompat --target android-19
place_support_v4 $EXTERN/Support/v7/appcompat

android update project --path . --name F-Droid

{ echo -e "\nSuccessfully updated the main project.\n"; } 2>/dev/null

# technically optional, needed for the tests
cd test
android update test-project --path . --main ..

{ echo -e "\nSuccessfully updated the test project.\n"; } 2>/dev/null
