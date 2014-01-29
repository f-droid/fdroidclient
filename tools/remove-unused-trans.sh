#!/bin/bash -x

# Remove extra translations

lint . --quiet --check ExtraTranslation --nolines | \
	sed -n 's/.*Error: "\([^"]*\)" is translated here but not found in default locale.*/\1/p' | \
	while read name; do
		sed -i "/name=\"$name\"/d" res/values-*/strings.xml
	done
