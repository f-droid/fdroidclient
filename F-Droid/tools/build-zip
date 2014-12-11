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

# find the real path to the base of fdroidclient
project_dir=$(cd `echo $0 | sed 's|\(.*\)/.*$|\1|'`/.. && pwd)
cd "$project_dir"

rm -rf "${project_dir}/bin/zip/"

for apk in bin/*.apk
do
    apk=${apk##*/}
    apk=${apk%%\.apk}
    rm -f bin/${apk}.zip
    mkdir -p bin/zip/${apk}
    cp bin/${apk}.apk bin/zip/${apk}/FDroid.apk
    mkdir -p bin/zip/${apk}/META-INF/com/google/android
    cp tools/zip-installer bin/zip/${apk}/META-INF/com/google/android/update-binary
    (cd "${project_dir}/bin/zip/${apk}" && \
        zip -r -X ../../${apk}.zip FDroid.apk META-INF/com/google/android/update-binary)
done

rm -rf "bin/zip/F-Droid-remove*"
mkdir -p bin/zip/F-Droid-remove/META-INF/com/google/android
cp tools/zip-uninstaller bin/zip/F-Droid-remove/META-INF/com/google/android/update-binary
(cd "${project_dir}/bin/zip/F-Droid-remove" && \
    zip -r -X ../../F-Droid-remove.zip FDroid.apk META-INF/com/google/android/update-binary)
