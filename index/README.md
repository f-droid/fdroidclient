# F-Droid multi-platform index library

Note that some features are only available for Android:

   * index signature verification (`JarFile` is JVM only)
   * index stream processing (`InputStream` is JVM only)
   * index V2 diffing (reflection is JVM only)
   * app device compatibility checking (requires Android)

Other platforms besides Android have not been tested and might need additional work.

## How to include in your project

Add this to your `build.gradle` file
and replace `[version]` with the [latest version](gradle.properties):

    implementation 'org.fdroid:index:[version]'

## Development

You can list available gradle tasks by running the following command in the project root.

    ./gradlew :index:tasks

### Making releases

Bump version number in [`gradle.properties`](gradle.properties), ensure you didn't break a public API and run:

    ./gradlew :index:check :index:connectedCheck
    ./gradlew :index:publish
    ./gradlew closeAndReleaseRepository

See https://github.com/vanniktech/gradle-maven-publish-plugin#gradle-maven-publish-plugin for more information.

## License

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
