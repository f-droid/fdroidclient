#!/bin/bash -ex

android update lib-project --path extern/UniversalImageLoader/library
android update lib-project --path extern/AndroidPinning
android update lib-project --path extern/MemorizingTrustManager
android update lib-project --path extern/libsuperuser/libsuperuser
android update lib-project --path extern/zxing-core
android update lib-project --path extern/android-support-v4-preferencefragment
android update lib-project --path extern/Support/v7/appcompat --target android-19
android update project --path . --name F-Droid

{ echo -e "\nSuccessfully updated the main project.\n"; } 2>/dev/null

# technically optional, needed for the tests
cd test
android update test-project --path . --main ..

{ echo -e "\nSuccessfully updated the test project.\n"; } 2>/dev/null
