F-Droid Client
==============

Client for [F-Droid](https://fdroid.org), the Free Software repository system
for Android.


Building from source
--------------------

The only required tools are the Android SDK and Apache Ant.

```
git submodule update --init
./ant-prepare.sh # This runs 'android update' on the libs and the main project
ant clean release
```

The project itself supports Gradle, but some of the libraries it uses don't.
Hence it is currently not possible to build F-Droid with Gradle in a clean way
without manual interaction.


Direct download
---------------

You can [download the application](https://f-droid.org/FDroid.apk) directly
from our site.


Contributing
------------

You are welcome to submit Merge Requests via the Gitorious web interface. You
can also follow our [Issue tracker](https://f-droid.org/repository/issues/)
and our [Forums](https://f-droid.org/forums/).


Translating
-----------

The `locale` dir is automatically updated via the
[android2po](https://github.com/miracle2k/android2po) tool, and translations
are pulled from our Pootle translation server at
[f-droid.org/translate](https://f-droid.org/translate). You should only add or
remove strings in the `res/values/` dir, since all the `res/values-*` dirs are
also generated automatically.


License
-------

This program is Free Software: You can use, study share and improve it at your
will. Specifically you can redistribute and/or modify it under the terms of the
[GNU General Public License](https://www.gnu.org/licenses/gpl.html) as
published by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
