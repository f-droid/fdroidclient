# Licenses

To see which license any one of these libraries is under, consult the extern/ directory.
If you have checked out the source for all dependencies (`git submodule update --init` from the root directory),
then you should be able to find the relevant LICENSE file for each, or else you can consult upstream.


# Building libraries from source

As a matter of principle, and also to comply with the GPLv3+, F-Droid should always be able to be built
from source, _including all of its dependencies_.

For practical reasons, developers often don't want the overhead of having to fetch the source of each
dependency though. Also, they may not want their build script to have to take the time to configure and
check that each subproject is up-to-date. Building from binary dependencies is _much_ faster, requires
less downloading of external source, and should import into Android Studio and other IDE's more easily.

To deal with these two goals (building dependencies from source, and relying on prebuilt binaries), the
build script can be run in two modes.


## Gradle commands

`gradle build` will build _F-Droid_ from source, but _not the depenencies_ from source. It will instead depend
on binaries from jcenter where possible, and .jar and .aar files in F-Droid/libs/ elsewhere.

`gradle -PsourceDeps build` will include all dependencies and build the entire F-Droid binary
and all of its dependencies from source.

`gradle -PsourceDeps binaryDeps` will build all dependencies from source, and then copy the ones which
are not found in jcenter to F-Droid/libs/, making sure that the latest versions of the libraries are available
for developers wishing to build F-Droid from binary dependencies.

`gradle -PsourceDeps cleanBinaryDeps` will remove all binary dependencies. It shouldn't be neccesary,
because the `binaryDeps` will copy fresh .jar and .aar files to the F-Droid/libs/ directory when they change.
It may be handy if you are updating the build script though, as a nice way to empty the F-Droid/libs/ directory.


# Adding new dependencies

When adding a new dependency, *DON'T* copy the .jar or .aar file into F-Droid/libs/. This will get deleted
when somebody runs `gradle -PsourceDeps cleanBinaryDeps`. Also, the version of F-Droid built for distribution
on https://f-droid.org will be build from source depednencies, so adding a binary is not enough.

Instead, you should add the source repo as a submodule in the extern/ diretory. You will also need to modify
the F-Droid/build.gradle file, adding both the source dependency on the project in the extern/ directory, and
a dependency on its jcenter/mavenCentral artifact. If that artifact is not available, then you should add
the library to those to be copied using `gradler -PsourceDeps binaryDeps`. Then, you can commit that binary so that
anyone who clones the F-Droid repo will have all of the dependencies ready in either jcetner or the
F-Droid/libs/ directory.
