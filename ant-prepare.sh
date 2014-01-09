#!/bin/bash -ex

android update lib-project -p extern/Universal-Image-Loader/library
android update lib-project -p extern/AndroidPinning -t android-17
android update lib-project -p extern/MemorizingTrustManager
android update project -p . --name F-Droid
