#!/bin/bash -x

# Fix TypographyEllipsis programmatically

sed -i -e 's/\.\.\./…/g' -e 's/ …/…/g' res/values*/*.xml
