package org.fdroid.repo

/**
 * A map from canonical repo URL to lower-case fingerprint of this repo.
 * When adding new repos here, please test that adding the repo still works.
 */
internal val knownRepos = mapOf(
    "https://apt.izzysoft.de/fdroid/repo" to
        "3bf0d6abfeae2f401707b6d966be743bf0eee49c2561b9ba39073711f628937a",
    "https://archive.newpipe.net/fdroid/repo" to
        "e2402c78f9b97c6c89e97db914a2751fda1d02fe2039cc0897a462bdb57e7501",
    "https://briarproject.org/fdroid/repo" to
        "1fb874bee7276d28ecb2c9b06e8a122ec4bcb4008161436ce474c257cbf49bd6",
    "https://guardianproject.info/fdroid/repo" to
        "b7c2eefd8dac7806af67dfcd92eb18126bc08312a7f2d6f3862e46013c7a6135",
    "https://microg.org/fdroid/repo" to
        "9bd06727e62796c0130eb6dab39b73157451582cbd138e86c468acc395d14165",
)
