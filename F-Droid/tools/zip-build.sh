#!/bin/bash -ex

# Copyright 2014 Ron Rieve
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

function create-update-zip {
    APK_NAME=$1
    rm -rf "bin/zip/${APK_NAME}"*
    mkdir -p bin/zip/${APK_NAME}
    cp bin/${APK_NAME}.apk bin/zip/${APK_NAME}/FDroid.apk
    mkdir -p bin/zip/${APK_NAME}/META-INF/com/google/android
    cp tools/zip-installer bin/zip/${APK_NAME}/META-INF/com/google/android/update-binary
    (cd bin/zip/${APK_NAME}; zip -r -X ../${APK_NAME}.zip FDroid.apk META-INF/com/google/android/update-binary)
}

for apk in bin/*.apk
do
    apk=${apk##*/}
    apk=${apk%%\.apk}
    create-update-zip $apk
done

rm -rf bin/zip/F-Droid-remove*
mkdir -p bin/zip/F-Droid-remove/META-INF/com/google/android
cp tools/zip-uninstaller bin/zip/F-Droid-remove/META-INF/com/google/android/update-binary
(cd bin/zip/F-Droid-remove; zip -r -X ../F-Droid-remove.zip FDroid.apk META-INF/com/google/android/update-binary)
