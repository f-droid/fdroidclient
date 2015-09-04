F-Droid Client
==============

[![build status](https://ci.gitlab.com/projects/6571/status.png?ref=master)](https://ci.gitlab.com/projects/6571?ref=master)

Client for [F-Droid](https://f-droid.org), the Free Software repository system
for Android.

Building with Gradle
--------------------

The only required tools are the [Android SDK](http://developer.android.com/sdk/index.html)
and Gradle.

You should use a relatively new version of Gradle, such as 2.4, or use the
gradle wrapper.

Once you have checked out the version you wish to build, run:

	cd F-Droid
	gradle assembleRelease

If you would like to build from source, add `-PsourceDeps`:

	cd F-Droid
	gradle assembleRelease -PsourceDeps

The resulting apk will be in `build/outputs/apk/`.

Android Studio
--------------

From Android Studio: File -> Import Project -> Select the cloned top folder


Building tips
-------------

* Use `gradle --daemon` if you are going to build F-Droid multiple times.
* If you get a message like `Could not find com.android.support:support-...`,
  make sure that you have the latest Android support maven repository

Direct download
---------------

You can [download the application](https://f-droid.org/FDroid.apk) directly
from our site or [browse it in the repo](https://f-droid.org/app/org.fdroid.fdroid).


Contributing
------------

You are welcome to submit
[Merge Requests](https://gitlab.com/fdroid/fdroidclient/merge_requests)
via the Gitlab web interface. You can also follow our
[Issue tracker](https://gitlab.com/fdroid/fdroidclient/issues) and our
[Forums](https://f-droid.org/forums).

Also see our [Contributing doc](CONTRIBUTING.md).


Translating
-----------

The `res/values-*` dirs are kept up to date automatically via [MediaWiki's
Translate Extension](http://www.mediawiki.org/wiki/Extension:Translate). See
[our translation page](https://f-droid.org/wiki/page/Special:Translate) if you
would like to contribute.


Running the test suite
----------------------

In order to run the F-Droid test suite, you will need to have either a real device
connected via `adb`, or an emulator running. Then, execute the following from the
command line:

	gradle connectedCheck

This will build and install F-Droid and the test apk, then execute the entire
test suite on the device or emulator.

See the [Android Gradle user guide](http://tools.android.com/tech-docs/new-build-system/user-guide#TOC-Testing)
for more details, including how to use Android Studio to run tests (which
provides more useful feedback than the command line).


Versioning
----------

Each stable version follows the `X.Y` pattern. Hotfix releases - i.e. when a
stable has an important bug that needs immediate fixing - will follow the
`X.Y.Z` pattern.

Before each stable release, a number of alpha releases will be released. They
will follow the pattern `X.Y-alphaN`, where `N` is the current alpha number.
These will usually include changes and new features that have not been tested
enough for a stable release, so use at your own risk. Testers and reporters
are very welcome.

The version codes use a number of digits per each of these keys: `XYYZNN`.
So for example, 1.3.1 would be `103150` and 0.95-alpha13 would be `95013`
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


License
-------

This program is Free Software: You can use, study share and improve it at your
will. Specifically you can redistribute and/or modify it under the terms of the
[GNU General Public License](https://www.gnu.org/licenses/gpl.html) as
published by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Some icons are made by [Picol](http://www.flaticon.com/authors/picol),
[Icomoon](http://www.flaticon.com/authors/icomoon) or
[Dave Gandy](http://www.flaticon.com/authors/dave-gandy) from
[Flaticon](http://www.flaticon.com) or by Google and are licensed by
[Creative Commons BY 3.0](http://creativecommons.org/licenses/by/3.0/).

Other icons are from the
[Material Design Icon set](https://github.com/google/material-design-icons)
released under an
[Attribution 4.0 International license](http://creativecommons.org/licenses/by/4.0/).
