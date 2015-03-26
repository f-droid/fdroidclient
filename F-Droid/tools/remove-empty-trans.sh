#!/bin/bash -x

# Remove empty translations

sed -i '/<string [^>]*\/>/d' res/values-*/strings.xml
