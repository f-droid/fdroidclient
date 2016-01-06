#!/bin/sh

# This script pulls translations from weblate.
#
# It squashes all the changes into a single commit. This removes authorship
# from the changes, which is given to the Translatebot, so to keep the names
# they are grabbed from git log and added to the commit message.
#
# Note that this will apply changes and commit them! Make sure to not have
# uncommited changes when running this script.

REMOTE="weblate"
REMOTE_URL="git://git.weblate.org/f-droid.git"
REMOTE_BRANCH="master"

AUTHOR="F-Droid Translatebot <team@f-droid.org>"

if ! git ls-remote --exit-code $REMOTE >/dev/null 2>/dev/null; then
	echo "Remote doesn't exist! Try the following:"
	echo "  git remote add $REMOTE $REMOTE_URL"
	echo "  git fetch $REMOTE"
	exit 1
fi

ref="${REMOTE}/${REMOTE_BRANCH}"
diff="HEAD...$ref -- */values-*/strings.xml"

authors=$(git log --format="%s %an" $diff | \
	sed 's/Translated using Weblate (\(.*\)) \(.*\)/\2||\1/' | sort -f -u | column -s '||' -t)

git diff $diff | git apply

git add */values-*/strings.xml

git commit --author "$AUTHOR" -m "Pull translation updates from Weblate

Translators:

$authors"
