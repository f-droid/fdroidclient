# Contributing

## Reporting issues

If you find an issue in the client, you can use our [Issue
Tracker](https://gitlab.com/fdroid/fdroidclient/issues). Make sure that it
hasn't yet been reported by searching first.

Remember to include the following information:

* Android version
* Device model
* F-Droid version
* Steps to reproduce the issue
* Logcat - see [instructions](https://f-droid.org/wiki/page/Getting_logcat_messages_after_crash)

## Translating

The strings are translated using [Weblate](https://weblate.org/en/). Follow
[these instructions](https://hosted.weblate.org/engage/f-droid/) if you would
like to contribute.

Please *do not* send merge requests or patches modifying the translations. Use
Weblate instead - it applies a series of fixes and suggestions, plus it keeps
track of modifications and fuzzy translations. Applying translations manually
skips all of the fixes and checks, and overrides the fuzzy state of strings.

Note that you cannot change the English strings on Weblate. If you have any
suggestions on how to improve them, open an issue or merge request like you
would if you were making code changes. This way the changes can be reviewed
before the source strings on Weblate are changed.


## Code Style

We follow the default Android Studio code formatter (e.g. `Ctrl-Alt-L`).  This
should be more or less the same as [Android Java
style](https://source.android.com/source/code-style.html).  Some key points:

* Four space indentation
* UTF-8 source files
* Exactly one top-level class per file
* No wildcard imports
* One statement per line
* K&R spacings with braces and parenthesis
* Commented fallthroughs
* Braces are always used after if, for and while

The current code base doesn't follow it entirely, but new code should follow
it. We enforce some of these, but not all, via `./gradlew checkstyle`.


## Running the test suite

Before pushing commits to a merge request, make sure this passes:

    ./gradlew checkstyle pmd lint

In order to run the F-Droid test suite, you will need to have either a real device
connected via `adb`, or an emulator running. Then, execute the following from the
command line:

    ./gradlew check

Many important tests require a device or emulator, but do not work in GitLab CI.
That mean they need to be run locally, and that is usually easiest in Android
Studio rather than the command line.

For a quick way to run a specific JUnit/Robolectric test:

    ./gradlew testFullDebugUnitTest --tests *LocaleSelectionTest*

For a quick way to run a specific emulator test:

	./gradlew connectedFullDebugAndroidTest \
        -Pandroid.testInstrumentationRunnerArguments.class=org.fdroid.fdroid.MainActivityExpressoTest


## Making releases

See https://gitlab.com/fdroid/wiki/-/wikis/Internal/Release-Process#fdroidclient
