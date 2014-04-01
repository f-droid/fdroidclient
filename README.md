F-Droid Client
==============

Client for [F-Droid](https://fdroid.org), the Free Software repository system
for Android.


Building from source
--------------------

The only required tools are the Android SDK and Apache Ant.

Once you have checked out the version you wish to build, run:

```
git submodule update --init
./ant-prepare.sh # This runs 'android update' on the libs and the main project
ant clean release
```

The project itself supports Gradle, but some of the libraries it uses don't.
Hence it is currently not possible to build F-Droid with Gradle in a clean way
without manual interaction.

Building as part of a ROM
-------------------------

Add the following lines to your repo manifest:

```
<remote name="fdroid" fetch="https://git.gitorious.org/f-droid" />
<remote name="github" fetch="https://github.com/" />

<project path="packages/apps/fdroidclient" name="fdroidclient.git" remote="fdroid" revision="edd2de49c317ecd59ab7c9c193c7a3b7cc6edc3d" />

<project path="packages/apps/fdroidclient/extern/UniversalImageLoader" name="nostra13/Android-Universal-Image-Loader" remote="github" revision="b1b49e51f2c43b119edca44691daf9ab6c751158" />
<project path="packages/apps/fdroidclient/extern/AndroidPinning" name="binaryparadox/AndroidPinning" remote="github" revision="ce84a19e753bbcc3304525f763edb7d7f3b62429" />
<project path="packages/apps/fdroidclient/extern/MemorizingTrustManager" name="ge0rg/MemorizingTrustManager" remote="github" revision="a705441ac53b9e1aba9f00f3f59aab81da6fbc9e" />
```

Adding F-Droid is then just a matter of adding `F-Droid` to your `PRODUCT_PACKAGES`.

Direct download
---------------

You can [download the application](https://f-droid.org/FDroid.apk) directly
from our site or [browse it in the
repo](https://f-droid.org/app/org.fdroid.fdroid).


Contributing
------------

You are welcome to submit Merge Requests via the Gitorious web interface. You
can also follow our [Issue tracker](https://f-droid.org/repository/issues/)
and our [Forums](https://f-droid.org/forums/).


Translating
-----------

The `res/values-*` dirs are kept up to date automatically via [MediaWiki's
Translate Extension](http://www.mediawiki.org/wiki/Extension:Translate). See
[our translation page](https://f-droid.org/wiki/page/Special:Translate) if you
would like to contribute.


Running the test suite
----------------------

FDroid client includes a embedded Android Test Project for running tests.  It
is in the `test/` subfolder.  To run the tests from the command line, do:

```
git submodule update --init
./ant-prepare.sh # This runs 'android update' on the libs and the main project
ant clean emma debug install test
```

You can also run the tests in Eclipse. Here's how:

1. Choose *File* -> *Import* -> *Android* -> *Existing Android Code Into Workspace* for the `fdroidclient/` directory.
2. Choose *File* -> *Import* -> *Android* -> *Existing Android Code Into Workspace* for the `fdroidclient/test/` directory
3. If **fdroid-test** has errors, right-click on it, select *Properties*, the
*Java Build Path*, then click on the *Projects* tab.
4. Click on the *Add...* button and select `fdroidclient/`
5. Right-click on the **fdroid-test** project, then *Run As...* -> *Android JUnit Test*


License
-------

This program is Free Software: You can use, study share and improve it at your
will. Specifically you can redistribute and/or modify it under the terms of the
[GNU General Public License](https://www.gnu.org/licenses/gpl.html) as
published by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
