#!/bin/bash -ex

android update lib-project --path extern/UniversalImageLoader/library
android update lib-project --path extern/AndroidPinning
android update lib-project --path extern/MemorizingTrustManager
android update project --path . --name F-Droid

# technically optional, needed for the tests
cd test
android update test-project --path . --main ..
