#!/bin/bash -x

# Remove extra translations

sed -n 's@res/values-[^/]\+/\([^\.]\+\)\.xml:.*Error: "\([^"]*\)" is translated here but not found in default locale.*@\1 \2@p' < build/outputs/lint-results.txt | \
while read file name; do
	if [[ $file == strings ]]; then
		sed -i "/name=\"$name\"/d" res/values-*/strings.xml
	elif [[ $file == array ]]; then
		sed -i "/<string-array name=\"$name\"/,/<\/string-array/d" res/values-*/array.xml
	fi
done
