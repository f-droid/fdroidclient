#!/bin/bash -x

# Fix TypographyEllipsis programmatically

sed -i 's/\.\.\./…/g' app/src/main/res/values*/*.xml
