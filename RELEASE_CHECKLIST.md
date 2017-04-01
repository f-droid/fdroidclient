
# Release Checklist

This is the things that need to happen for all releases, alpha or stable:

* pull translations from Weblate: ./tools/pull-trans.sh

* rebase Weblate in its web interface, since we squash commits

* update `versionCode` in _app/build.gradle_

* make signed tag with version name

* update _metadata/org.fdroid.fdroid.txt_ in _fdroiddata_

## Stable releases

For stable releases, there are a couple more steps to do __before__
making the release tag:

* update CHANGELOG.md

* run `./tools/trim-incomplete-translations-for-release.py`
