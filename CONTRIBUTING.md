# Contributing

## Reporting issues

If you find an issue in the app, you can use our [Issue
Tracker](https://gitlab.com/fdroid/fdroidclient/issues). Make sure that it
hasn't yet been reported by searching first.

Remember to include the following information:

* Android version
* Device model
* F-Droid version
* Steps to reproduce the issue
* Logcat - see [instructions](https://f-droid.org/wiki/page/Getting_logcat_messages_after_crash)

## Contributing code

Before starting to work on non-trivial code changes,
please post in the related ticket first stating your intention to work on it.
This avoids duplicate work and allows for discussion of the best approach to take.
If no ticket exists, create one first.

## Translating

The strings are translated using [Weblate](https://weblate.org/en/). Follow
[these instructions](https://hosted.weblate.org/engage/f-droid/) if you would
like to contribute.

Please *do not* send merge requests or patches modifying the translations. Use
Weblate instead - it applies a series of fixes and suggestions, plus it keeps
track of modifications and fuzzy translations. Applying translations manually
skips all the fixes and checks, and overrides the fuzzy state of strings.

Note that you cannot change the English strings on Weblate. If you have any
suggestions on how to improve them, open an issue or merge request like you
would if you were making code changes. This way the changes can be reviewed
before the source strings on Weblate are changed.


## Code Style

We follow [ktfmt](https://facebook.github.io/ktfmt/) style with Google flavor.

Run `./gradlew ktfmtFormat` to auto-format your changes before committing them.


## Running the test suite

Before pushing commits to a merge request, make sure this passes:

    ./gradlew check

In order to run the F-Droid test suite, you will need to have either a real device
connected via `adb`, or an emulator running. Then, execute the following from the
command line:

    ./gradlew connectedAndroidTest

For a quick way to run a specific JUnit/Robolectric test:

    ./gradlew testFullDebugUnitTest --tests *LocaleSelectionTest*

For a quick way to run a specific emulator test:

	./gradlew connectedFullDebugAndroidTest \
        -Pandroid.testInstrumentationRunnerArguments.class=org.fdroid.fdroid.MainActivityExpressoTest

## Screenshot tests

F-Droid uses screenshot tests to check that everything displays correctly. If
you've intentionally updated the UI or visible strings, you'll need to update
the screenshots to avoid breaking the tests.

Make sure you've pulled the screenshot files:

    git lfs pull

To update the screenshots:

    ./gradlew :app:updateBasicDefaultDebugScreenshotTest

## Making releases

See https://gitlab.com/fdroid/wiki/-/wikis/Internal/Release-Process#fdroidclient
