#!/bin/bash -x

# Fix TypographyEllipsis programmatically

find res/values* -name '*.xml' -type f | xargs -n 1 sed -i 's/\.\.\./…/g'
