#!/bin/bash -ex

# Update README repo manifest revisions from git

#LAST_STABLE_TAG=$(git describe --abbrev=0 --tags --match='[0-9]*[0-9]')
#sed -i 's@\(.*name="fdroidclient\.git".*revision="\)[^"]*\(".*\)@\1'$LAST_STABLE_TAG'\2@' README.md

git ls-tree HEAD extern/ | while read _ _ revision path; do
	sed -i 's@\(.*fdroidclient/'$path'".*revision="\)[^"]*\(".*\)@\1'$revision'\2@' README.md
done
