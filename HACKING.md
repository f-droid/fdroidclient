F-Droid Client Hacking Doc
==========================

Style
-----

We follow the [Google Java style](https://google-styleguide.googlecode.com/svn/trunk/javaguide.html).
Some of its points are:

 * Four space indentation
 * UTF-8 source files
 * Exactly one top-level class per file
 * No wildcard imports
 * Braces are always used and follow K&R
 * Commented fallthroughs

We didn't follow any style before but generally it was more or less like
Google's. As we make the switch, we encourage devs to follow this new format.

Building
--------

You have three options:

 * Build with gradle
 * Build with gradle from source
 * Build with ant from source

Any will work, but if you use gradle, remember that you can use --daemon to
not have to watch gradle load every time.
