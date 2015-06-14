F-Droid Client
==============

Client for [F-Droid](https://fdroid.org), the Free Software repository system
for Android.

Building from source with Gradle
--------------------------------

Once you have checked out the version you wish to build, install gradle on your system and run:

```
git submodule update --init
gradle build
```

Android Studio
--------------

From Android Studio: File -> Import Project -> Select the cloned top folder

Building from source with Ant
-----------------------------

The only required tools are the [Android SDK](http://developer.android.com/sdk/index.html) and Apache Ant.

Once you have checked out the version you wish to build, run:

```
git submodule update --init
cd F-Droid
./ant-prepare.sh # This runs 'android update' on the libs and the main project
ant clean release
```

Direct download
---------------

You can [download the application](https://f-droid.org/FDroid.apk) directly
from our site or [browse it in the
repo](https://f-droid.org/app/org.fdroid.fdroid).


Contributing
------------

You are welcome to submit
[Merge Requests](https://gitlab.com/fdroid/fdroidclient/merge_requests)
via the Gitlab web interface. You can also follow our
[Issue tracker](https://f-droid.org/repository/issues/) and our
[Forums](https://f-droid.org/forums/).


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


Troubleshooting
---------------

When building F-Droid, the following error may occur:

> Invalid file: extern/UniversalImageLoader/library/build.xml

Check the output of the ./ant-prepare.sh command. This error is often
accompanied by the following message:

> Error: The project either has no target set or the target is invalid.
> Please provide a --target to the 'android update' command.

The most likely cause of this is that your installed Android SDK is missing
the target version specified by one of the dependencies. For example, at the
time of writing this, UniversalImageLoader uses the "android-16" target API,
however the default install of the Android SDK will usually only install the
latest version ("android-20" as of writing). So you will have to install
missings "android-xx" targets via the SDK manager. To get a list of already
installed SDK targets, run:

```
$ android list targets
```

To get a list of targets used by fdroidclient libs, run:

```
$ for i in $(grep "android.library.reference" project.properties | cut -f2 -d'='); do
grep ^target $i/project.properties | cut -f2 -d'=';
done | sort | uniq | paste -s -d',' -
```
to install missing or all needed targets, for example "android-16" and "android-7" run:

```
$ android update sdk -u -t "android-16,android-7"
```

NOTE: While it may be tempting to add "--target=android-19" to the
ant-prepare.sh script, it is not the correct solution. Although it may work,
it can cause strange bugs at runtime.


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
