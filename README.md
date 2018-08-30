# F-Droid Client

[![build status](https://gitlab.com/fdroid/fdroidclient/badges/master/build.svg)](https://gitlab.com/fdroid/fdroidclient/builds)
[![Translation status](https://hosted.weblate.org/widgets/f-droid/-/svg-badge.svg)](https://hosted.weblate.org/engage/f-droid/)

Client for [F-Droid](https://f-droid.org), the Free Software repository system
for Android.

## Building with Gradle

    ./gradlew assembleRelease

## Direct download

You can [download the application](https://f-droid.org/FDroid.apk) directly
from our site or [browse it in the repo](https://f-droid.org/app/org.fdroid.fdroid).

## Contributing

See our [Contributing doc](CONTRIBUTING.md) for information on how to report
issues, translate the app into your language or help with development.

## IRC

We are on `#fdroid` and `#fdroid-dev` on Freenode. We hold weekly dev meetings
on `#fdroid-dev` on Thursdays at 11:30h UTC, which usually last half an hour.

## FAQ

* Why does F-Droid require "Unknown Sources" to install apps by default?

Because a regular Android app cannot act as a package manager on its
own. To do so, it would require system privileges (see below), similar
to what Google Play does.

* Can I avoid enabling "Unknown Sources" by installing F-Droid as a
  privileged system app?

This used to be the case, but no longer is. Now the [Privileged
Extension](https://gitlab.com/fdroid/privileged-extension) is the one that should be placed in
the system. It can be bundled with a ROM or installed via a zip.
## License

This program is Free Software: You can use, study share and improve it at your
will. Specifically you can redistribute and/or modify it under the terms of the
[GNU General Public License](https://www.gnu.org/licenses/gpl.html) as
published by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Some icons are made by [Picol](http://www.flaticon.com/authors/picol),
[Icomoon](http://www.flaticon.com/authors/icomoon) or
[Dave Gandy](http://www.flaticon.com/authors/dave-gandy) from
[Flaticon](http://www.flaticon.com) or by Google and are licensed by
[Creative Commons BY 3.0](https://creativecommons.org/licenses/by/3.0/).

Other icons are from the
[Material Design Icon set](https://github.com/google/material-design-icons)
released under an
[Attribution 4.0 International license](https://creativecommons.org/licenses/by/4.0/).


## Translation

Everything can be translated.  See
[Translation and Localization](https://f-droid.org/docs/Translation_and_Localization)
for more info.
[![translation status](https://hosted.weblate.org/widgets/f-droid/-/f-droid/multi-auto.svg)](https://hosted.weblate.org/engage/f-droid/?utm_source=widget)
