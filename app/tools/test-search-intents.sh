#!/bin/sh

echo "A helper script to send all of the various intents that F-droid should be able to handle via ADB."
echo "Use this to ensure that things which should trigger searches, do trigger searches, and those which should bring up the app details screen, do bring it up."
echo ""

function view {
	DESCRIPTION=$1
	DATA=$2
	wait "$DESCRIPTION"
	CMD="adb shell am start -a android.intent.action.VIEW -d $DATA"
	$CMD
	echo ""
	sleep 1
}

function wait {
	DESCRIPTION=$1
	echo "$DESCRIPTION [Y/n]"

	read -n 1 RESULT
	
	# Lower case the result.
	RESULT=`echo "$RESULT" | tr '[:upper:]' '[:lower:]'`

	echo ""

	if [ "$RESULT" != 'y' ]; then
		exit;
	fi
}

APP_TO_SHOW=org.fdroid.fdroid
SEARCH_QUERY=book+reader

view "Search for '$SEARCH_QUERY' (fdroid web)" http://f-droid.org/repository/browse?fdfilter=$SEARCH_QUERY
view "Search for '$SEARCH_QUERY' (market)" market://search?q=$SEARCH_QUERY
view "Search for '$SEARCH_QUERY' (play)" http://play.google.com/store/search?q=$SEARCH_QUERY
view "Search for '$SEARCH_QUERY' (amazon)" http://amazon.com/gp/mas/dl/android?s=$SEARCH_QUERY
view "Search for '$SEARCH_QUERY' (fdroid)" fdroid.search:$SEARCH_QUERY
view "View '$APP_TO_SHOW' (fdroid web fdid)" http://f-droid.org/repository/browse?fdid=$APP_TO_SHOW
view "View '$APP_TO_SHOW' (fdroid web /app/ path)" http://f-droid.org/app/$APP_TO_SHOW
view "View '$APP_TO_SHOW' (market)" market://details?id=$APP_TO_SHOW
view "View '$APP_TO_SHOW' (play)" http://play.google.com/store/apps/details?id=$APP_TO_SHOW
view "View '$APP_TO_SHOW' (amazon)" amzn://apps/android?p=$APP_TO_SHOW
view "View '$APP_TO_SHOW' (fdroid)" fdroid.app:$APP_TO_SHOW
