# F-Droid multi-platform download library

Note that advanced security and privacy features are only available for Android:

   * Rejection of TLS 1.1 and older as well as rejection of weak ciphers
   * No DNS requests when using Tor as a proxy
   * short TLS session timeout to prevent tracking and key re-use

Other platforms besides Android have not been tested and might need additional work.

## How to include in your project

Add this to your `build.gradle` file
and replace `[version]` with the [latest version](gradle.properties):

    implementation 'org.fdroid:download:[version]'

## Development

You can list available gradle tasks by running the following command in the project root.

    ./gradlew :download:tasks

### Making releases

Bump version number in [`gradle.properties`](gradle.properties), ensure you didn't break a public API and run:

    ./gradlew :download:check :download:connectedCheck
    ./gradlew :download:publish
    ./gradlew closeAndReleaseRepository

See https://github.com/vanniktech/gradle-maven-publish-plugin#gradle-maven-publish-plugin for more information.

## License

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
