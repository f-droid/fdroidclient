F-Droid Client
==============

Client for [F-Droid](https://fdroid.org), the Free Software repository system
for Android.


Building from source
--------------------

The only required tools are the Android SDK and Apache Ant.

```
git submodule update --init
android update project -p . --name F-droid
android update lib-project -p extern/Universal-Image-Loader/library
android update lib-project -p extern/AndroidPinning
android update lib-project -p extern/MemorizingTrustManager
ant clean release
```


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
