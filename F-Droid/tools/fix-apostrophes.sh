#!/bin/bash -x

# Fix apostrophes in strings not preceded by a backslash

sed -i "/\\(item\\|string\\)/s/\\([^\\]\\)'/\\1\\\\'/g" res/values*/{strings,array}.xml
