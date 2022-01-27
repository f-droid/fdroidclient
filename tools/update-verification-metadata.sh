#!/bin/sh -ex

TOOLS_DIR=$(cd "$(dirname "$0")"; pwd)

cd "$TOOLS_DIR"/..
./gradlew --write-verification-metadata pgp,sha256 \
  build \
  assembleFullDebug \
  loadKtlintReporters \
  -x :app:test \
  -x :app:lint \
  -x :download:nativeTest

printf "\nIf you changed dependencies related to tests, also add 'test' or 'connectedCheck'.\n\n"
printf "\nPlease review the following diff:\n\n"

git diff gradle/verification-metadata.xml
