Contributing
============

IRC
---

We are on `#fdroid` and `#fdroid-dev` on Freenode. We hold weekly dev meetings
on `#fdroid-dev` on Tuesdays at 21h UTC, which usually last half an hour.

Reporting issues
----------------

Remember to include:

* Android version
* Device model
* F-Droid version
* Steps to reproduce the issue
* Logcat - see [instructions](https://f-droid.org/wiki/page/Getting_logcat_messages_after_crash)

Style
-----

We follow the [Google Java style](https://google-styleguide.googlecode.com/svn/trunk/javaguide.html).
To summarize it:

* Four space indentation
* UTF-8 source files
* Exactly one top-level class per file
* No wildcard imports
* One statement per line
* K&R spacings with braces and parenthesis
* Commented fallthroughs
* Braces are always used after if, for and while

The current code base doesn't follow it entirely, but new code should follow
it.

Debugging
---------

To get all the logcat messages by F-Droid, you can run:

	adb logcat | grep `adb shell ps | grep org.fdroid.fdroid | cut -c10-15`
