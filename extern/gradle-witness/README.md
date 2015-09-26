# Gradle Witness

A gradle plugin that enables static verification for remote dependencies.

Build systems like gradle and maven allow one to specify dependencies for versioned artifacts. An
Android project might list dependencies like this:

    dependency {
        compile 'com.actionbarsherlock:actionbarsherlock:4.4.0@aar'
        compile 'com.android.support:support-v4:19.0.1'
        compile 'com.google.android.gcm:gcm-client:1.0.2'
        compile 'se.emilsjolander:stickylistheaders:2.2.0'
    }

This allows the sample Android project to very easily make use of versioned third party libraries like
[ActionBarSherlock](http://actionbarsherlock.com/), or [StickyListHeaders](https://github.com/emilsjolander/StickyListHeaders).
During the build process, gradle will automatically retrieve the libraries from the configured
maven repositories and incorporate them into the build.  This makes it easy to manage dependencies
without having to check jars into a project's source tree.

## Dependency Problems

A "published" maven/gradle artifact [looks like this](https://github.com/WhisperSystems/maven/tree/master/gson/releases/org/whispersystems/gson/2.2.4):

    gson-2.2.4.jar
    gson-2.2.4.jar.md5
    gson-2.2.4.jar.sha1
    gson-2.2.4.pom
    gson-2.2.4.pom.md5
    gson-2.2.4.pom.sha1

In the remote directory, the artifact consists of a POM file and a jar or aar, along with md5sum and
sha1sum hash values for those files.

When gradle retrieves the artifact, it will also retrieve the md5sum and sha1sums to verify that
they match the calculated md5sum and sha1sum of the retrieved files.  The problem, obviously, is 
that if someone is able to compromise the remote maven repository and change the jar/aar for a 
dependency to include some malicious functionality, they could just as easily change the md5sum
and sha1sum values the repository advertises as well.

## The Witness Solution

This gradle plugin simply allows the author of a project to statically specify the sha256sum of
the dependencies that it uses.  For our dependency example above, `gradle-witness` would allow
the project to specify:

    dependency {
        compile 'com.actionbarsherlock:actionbarsherlock:4.4.0@aar'
        compile 'com.android.support:support-v4:19.0.1'
        compile 'com.google.android.gcm:gcm-client:1.0.2'
        compile 'se.emilsjolander:stickylistheaders:2.2.0'
    }

    dependencyVerification {
        verify = [
                'com.actionbarsherlock:actionbarsherlock:5ab04d74101f70024b222e3ff9c87bee151ec43331b4a2134b6cc08cf8565819',
                'com.android.support:support-v4:a4268abd6370c3fd3f94d2a7f9e6e755f5ddd62450cf8bbc62ba789e1274d585',
                'com.google.android.gcm:gcm-client:5ff578202f93dcba1c210d015deb4241c7cdad9b7867bd1b32e0a5f4c16986ca',
                'se.emilsjolander:stickylistheaders:89146b46c96fea0e40200474a2625cda10fe94891e4128f53cdb42375091b9b6',
        ]
    }

The `dependency` definition is the same, but `gradle-witness` allows one to also specify a
`dependencyVerification` definition as well.  That definition should include a single list called
`verify` with elements in the format of `group_id:name:sha256sum`.

At this point, running `gradle build` will first verify that all of the listed dependencies have
the specified sha256sums.  If there's a mismatch, the build is aborted.  If the remote repository
is later compromised, an attacker won't be able to undetectably modify these artifacts.

## Using Witness

Unfortunately, it doesn't make sense to publish `gradle-witness` as an artifact, since that
creates a bootstrapping problem.  To use `gradle-witness`, the jar needs to be built and included
in your project:

    $ git clone https://github.com/WhisperSystems/gradle-witness.git
    $ cd gradle-witness
    $ gradle build
    $ cp build/libs/gradle-witness.jar /path/to/your/project/libs/gradle-witness.jar

Then in your project's `build.gradle`, the buildscript needs to add a `gradle-witness` dependency.
It might look something like:

    buildscript {
        repositories {
            mavenCentral()
        }
        dependencies {
            classpath 'com.android.tools.build:gradle:0.9.+'
            classpath files('libs/gradle-witness.jar')
        }
    }

    apply plugin: 'witness'

At this point you can use `gradle-witness` in your project.  If you're feeling "trusting on first
use," you can have `gradle-witness` calculate the sha256sum for all your project's dependencies
(and transitive dependencies!) for you:

    $ gradle -q calculateChecksums

This will print the full `dependencyVerification` definition to include in the project's `build.gradle`.
For a project that has a dependency definition like:

    dependency {
        compile 'com.actionbarsherlock:actionbarsherlock:4.4.0@aar'
        compile 'com.android.support:support-v4:19.0.1'
        compile 'com.google.android.gcm:gcm-client:1.0.2'
        compile 'se.emilsjolander:stickylistheaders:2.2.0'
    }

Running `gradle -q calculateChecksums` will print:

    dependencyVerification {
        verify = [
                'com.actionbarsherlock:actionbarsherlock:5ab04d74101f70024b222e3ff9c87bee151ec43331b4a2134b6cc08cf8565819',
                'com.android.support:support-v4:a4268abd6370c3fd3f94d2a7f9e6e755f5ddd62450cf8bbc62ba789e1274d585',
                'com.google.android.gcm:gcm-client:5ff578202f93dcba1c210d015deb4241c7cdad9b7867bd1b32e0a5f4c16986ca',
                'se.emilsjolander:stickylistheaders:89146b46c96fea0e40200474a2625cda10fe94891e4128f53cdb42375091b9b6',
        ]
    }

...which you can then include directly below the `dependency` definition in the project's `build.gradle`.

And that's it! From then on, running a standard `gradle build` will verify the integrity of
the project's dependencies.
