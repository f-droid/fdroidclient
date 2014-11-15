#!/bin/bash -x

# Fix StringFormatMatches programmatically

sed -i 's/\(%[0-9]\)%\([a-z]\)/\1$\2/g' res/values*/*.xml
