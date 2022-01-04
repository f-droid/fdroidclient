#!/bin/sh -ex

TOOLS_DIR=$(cd "$(dirname "$0")"; pwd)

cd "$TOOLS_DIR"/..
./gradlew --write-verification-metadata pgp,sha256 assemble assembleFullDebug

printf "\nPlease review the following diff:\n\n"

git diff gradle/verification-metadata.xml
