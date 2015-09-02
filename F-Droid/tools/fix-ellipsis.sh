#!/bin/bash -x

# Fix TypographyEllipsis programmatically

sed -r -i 's/(\.\.\.|&#8230\;)/…/g' res/values*/*.xml
