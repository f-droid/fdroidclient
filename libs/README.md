# F-Droid libraries

Core F-Droid functionality is split into re-usable libraries
to make using F-Droid technology in your own projects as easy as possible.

Note that all libraries are still in alpha stage.
While they work, their public APIs are still subject to change.

* [download](libs/download) library for handling (multi-platform) HTTP download
  of repository indexes, APKs and image assets
* [index](libs/index) library for parsing/verifying/creating repository indexes
* [database](libs/database) library to store and query F-Droid related information
  in a Room-based database on Android

## F-Droid multi-platform download library

[API docs](https://fdroid.gitlab.io/fdroidclient/libs/download/)

Note that advanced security and privacy features are only available for Android:

* Rejection of TLS 1.1 and older as well as rejection of weak ciphers
* No DNS requests when using Tor as a proxy
* short TLS session timeout to prevent tracking and key re-use

Other platforms besides Android have not been tested and might need additional work.

### How to include in your project

Add this to your `build.gradle` file
and replace `[version]` with the [latest version](download/index/gradle.properties):

    implementation 'org.fdroid:download:[version]'

## F-Droid multi-platform index library

[API docs](https://fdroid.gitlab.io/fdroidclient/libs/index/)

Note that some features are only available for Android:

   * index signature verification (`JarFile` is JVM only)
   * index stream processing (`InputStream` is JVM only)
   * index V2 diffing (reflection is JVM only)
   * app device compatibility checking (requires Android)

Other platforms besides Android have not been tested and might need additional work.

### How to include in your project

Add this to your `build.gradle` file
and replace `[version]` with the [latest version](libs/index/gradle.properties):

    implementation 'org.fdroid:index:[version]'

## F-Droid Android database library

[API docs](https://fdroid.gitlab.io/fdroidclient/libs/database/)

An Android-only database library to store and query F-Droid related information
such as repositories, apps and their versions.
This library should bring everything you need to build your own F-Droid client
that persists information.

### How to include in your project

Add this to your `build.gradle` file
and replace `[version]` with the [latest version](libs/database/gradle.properties):

    implementation 'org.fdroid:database:[version]'

# Development

You can list available gradle tasks by running the following command in the project root.

    ./gradlew :libs:download:tasks

Replace `download` with the name of the library you want to view tasks for.

# Making releases

Bump version number in the library's [`gradle.properties`](gradle.properties),
ensure you didn't break a public API and run:

    ./gradlew :libs:download:check :libs:download:connectedCheck
    ./gradlew :libs:download:publish
    ./gradlew closeAndReleaseRepository

Replace `download` with the name of the library you want to publish.

See https://github.com/vanniktech/gradle-maven-publish-plugin#gradle-maven-publish-plugin
for more information.

# License

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
