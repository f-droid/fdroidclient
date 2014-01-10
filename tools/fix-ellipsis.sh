#!/bin/bash -x

# Fix TypographyEllipsis programmatically

find res -name strings.xml -type f | xargs -n 1 sed -i 's/\.\.\./…/g'
