#!/bin/bash

#
# Helper script to download icons from https://github.com/google/material-design-icons and
# put the in the relevant drawable-* directories.
#

function usage {
	echo "USAGE: download-material-icon.sh res-directory category icon"
	echo "  res-directory  Usually \"res\" in your android project"
	echo "  category       the grouping seen in the URL below (e.g. action, alert, av, communication, content, etc)"
	echo "  icon           is the name if the icon (see URL below for icons)"
	echo ""
	echo "To see available icons, visit http://google.github.io/material-design-icons/"
}

function download {
	DRAWABLE_DIR=$1
	FILE="ic_${ICON}_24dp.png"
	URL="$BASE_URL/$CATEGORY/$DRAWABLE_DIR/$FILE"
	DIR="$RES_DIR/$DRAWABLE_DIR"

	if [ ! -d $DIR ]
	then
		echo "Creating dir $DIR"
		mkdir $DIR
	fi

	LOCAL_PATH="$DIR/ic_${ICON}.png"

	echo "Downloading to $LOCAL_PATH"
	wget --quiet --output-document=$LOCAL_PATH $URL

	if [ ! -s $LOCAL_PATH ]
	then
		if [ -f $LOCAL_PATH ]
		then
			rm $LOCAL_PATH
		fi

		echo "ERROR: Could not download from $URL to $LOCAL_PATH failed."
		echo ""
		usage
		exit
	fi
}

RES_DIR=$1
CATEGORY=$2
ICON="${3}_white"
BASE_URL="https://raw.githubusercontent.com/google/material-design-icons/master"
SCREENS="mdpi hdpi xhdpi xxhdpi xxxhdpi"

if [ ! -d $RES_DIR ]
then
	echo "ERROR: $RES_DIR is not a directory"
	echo ""
	usage
	exit
fi

for SCREEN in $SCREENS
do
	download "drawable-$SCREEN"
done

echo ""
echo "Please make sure you have the following attribution (or words to this effect) somewhere in your project:"
echo ""
echo "  Some icons are from the Material Design Icon set (https://github.com/google/material-design-icons)"
echo "  released under an Attribution 4.0 International license (http://creativecommons.org/licenses/by/4.0/)"
echo ""
