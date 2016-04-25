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
suggestions on how to improve them, open a merge request like you would if you
were making code changes. This way the changes can be reviewed before the
source strings on Weblate are changed.

## Code Style

We follow the [Android Java style](https://source.android.com/source/code-style.html).
Some key points:

* Four space indentation
* UTF-8 source files
* Exactly one top-level class per file
* No wildcard imports
* One statement per line
* K&R spacings with braces and parenthesis
* Commented fallthroughs
* Braces are always used after if, for and while

The current code base doesn't follow it entirely, but new code should follow
it. We enforce some of these, but not all, via checkstyle.

## Debugging

To get all the logcat messages by F-Droid, you can run:

    adb logcat | grep `adb shell ps | grep org.fdroid.fdroid | cut -c10-15`

## Building tips

* Use gradle with `--daemon` if you are going to build F-Droid multiple times.
* If you get a message like `Could not find com.android.support:support-...`,
  make sure that you have the latest Android support maven repository.
* When building as part of AOSP with `Android.mk`, make sure you have a
  recent version of Gradle installed as `gradlew` will not be used.

## Running the test suite

In order to run the F-Droid test suite, you will need to have either a real device
connected via `adb`, or an emulator running. Then, execute the following from the
command line:

    ./gradlew check

Note that the CI already runs the tests on an emulator, so you don't
necessarily have to do this yourself if you open a merge request as the tests
will get run.

## Versioning

Each stable version follows the `X.Y` pattern. Hotfix releases - i.e. when a
stable has an important bug that needs immediate fixing - will follow the
`X.Y.Z` pattern.

Before each stable release, a number of alpha releases will be released. They
will follow the pattern `X.Y-alphaN`, where `N` is the current alpha number.
These will usually include changes and new features that have not been tested
enough for a stable release, so use at your own risk. Testers and reporters
are very welcome.

The version codes use a number of digits per each of these keys: `XXXYYYZNN`.
So for example, 1.3.1 would be `1003150` and 0.95-alpha13 would be `95013`
(leading zeros are omitted).

Note that we use a trailing `50` for actual stable releases, so alphas are
limited to `-alpha49`.

This is an example of a release process for **0.95**:

* We are currently at stable **0.94**
* **0.95-alpha1** is released
* **0.95-alpha2** is released
* **0.95-alpha3** is released
* `stable-v0.95` is branched and frozen
* **0.95** is released
* A bug is reported on the stable release and fixed
* **0.95.1** is released with only that fix

As soon as a stable is tagged, master will move on to `-alpha0` on the next
version. This is a temporary measure - until `-alpha1` is released - so that
moving from stable to master doesn't require a downgrade. `-alpha0` versions
will not be tagged nor released.
