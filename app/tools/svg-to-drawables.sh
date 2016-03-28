#!/bin/bash

# Originally by https://github.com/vitriolix/storymaker-art/blob/3ee3b4aad8db4fd24290b0173e8129a3d0e5299d/original/convert.sh,
# then modified to make it generic and able to be used by any Android project.
#
# Requires ImageMagick to be installed.
# Some builds of ImageMagick on OSX have problems generating the images correctly.
#
# This script scales and creates images at the correct dpi level for Android.
# When creating svg files set the image size to the size that you want your hdpi images to be.

function usage {
	echo $1
	echo ""
	echo "USAGE: svg-to-drawable.sh svg-image res-directory [manually-scaled-res-directory]"
	echo "  svg-image                      Path to the .svg image to convert"
	echo "  res-directory                  Usually \"res\" in your android project"
	echo "  manually-scaled-res-directory  Put manually scaled images in this dir, useful for, e.g. differing levels of details"
	exit
}

SVG_FILE=$1
OUTPUT_RES_DIR=$2
SCALED_RES_DIR=$3

RES_NORMAL=drawable
RES_XXXHDPI=drawable-xxxhdpi
RES_XXHDPI=drawable-xxhdpi
RES_XHDPI=drawable-xhdpi
RES_HDPI=drawable-hdpi
RES_MDPI=drawable-mdpi
RES_LDPI=drawable-ldpi

if (( $# < 2 ))
then
	usage "ERROR: Requires at least svg-image and res-directory to be passed to script"
elif [ ! -d $OUTPUT_RES_DIR ]
then
	usage "ERROR: $OUTPUT_RES_DIR is not a directory"
fi

function convert_drawable {
	DIR=$1
	FILE_PATH=$2
	FILE_NAME=`basename $FILE_PATH`
	SCALE=$3
	PNG_FILE=${FILE_NAME/.svg}.png
	DRAWABLE_DIR=$OUTPUT_RES_DIR/$DIR
	OUTPUT_PATH=$DRAWABLE_DIR/$PNG_FILE

	if [ ! -d $DRAWABLE_DIR ]; then
		mkdir $DRAWABLE_DIR
	fi

	if [ -f $OUTPUT_PATH ]; then
		rm $OUTPUT_PATH
	fi

	INFO=""
	if [ -f $SCALED_RES_DIR/$DIR/$FILE_NAME ]; then
		INFO=" (Using manually scaled file from $DIR/$FILE_NAME)"
		convert -background none $SCALED_RES_DIR/$DIR/$FILE $OUTPUT_PATH || exit
	else
		INFO=" (Scaled by $SCALE%)"
		convert -background none $FILE_PATH[$SCALE%] $OUTPUT_PATH || exit
	fi
	echo "  $OUTPUT_PATH$INFO"

}

echo "Processing $SVG_FILE" 
convert_drawable $RES_NORMAL  $SVG_FILE 100
convert_drawable $RES_XXXHDPI $SVG_FILE 150
convert_drawable $RES_XXHDPI  $SVG_FILE 125
convert_drawable $RES_XHDPI   $SVG_FILE 100
convert_drawable $RES_HDPI    $SVG_FILE 75
convert_drawable $RES_MDPI    $SVG_FILE 50
convert_drawable $RES_LDPI    $SVG_FILE 37.5
