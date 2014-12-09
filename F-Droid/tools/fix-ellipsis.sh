#!/bin/bash -x

# Fix TypographyEllipsis programmatically

sed -i 's/\.\.\./…/g' res/values*/*.xml
