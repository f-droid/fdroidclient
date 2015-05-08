F-Droid Client Hacking Doc
==========================

Style
-----

We loosely follow the [Google Java style](https://google-styleguide.googlecode.com/svn/trunk/javaguide.html).
Some of the points we follow the most are:

 * Four space indentation
 * UTF-8 source files
 * Exactly one top-level class per file
 * No wildcard imports
 * One statement per line
 * K&R spacings with braces and parenthesis

Some other interesting additions we might use in the future:

 * Commented fallthroughs
 * Braces are always used after if, for and while

We don't strictly follow the entire style spec, but when in doubt you should
follow it.

Building
--------

You have three options:

 * Build with gradle
 * Build with gradle from source
 * Build with ant from source

Any will work, but if you use gradle, remember that you can use --daemon to
not have to watch gradle load every time.

Debugging
---------

To get all the logcat messages by F-Droid, you can run:

    adb logcat | grep `adb shell ps | grep org.fdroid.fdroid | cut -c10-15`
