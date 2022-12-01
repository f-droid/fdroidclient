package org.fdroid.test

import org.fdroid.index.v2.Entry
import org.fdroid.index.v2.EntryFileV2

object TestDataEntry {

    val empty = Entry(
        timestamp = 23,
        version = 20001,
        index = EntryFileV2(
            name = "index-v2.json",
            sha256 = "746ecda085ed53adab25f761a9dbf4c09d59e5bff9c9d5530814d56445ae30f1",
            size = 42,
            numPackages = 1,
        ),
    )

    val emptyToMin = Entry(
        timestamp = 42,
        version = 20001,
        maxAge = 7,
        index = EntryFileV2(
            name = "../index-min-v2.json",
            sha256 = "851ecda085ed53adab25f761a9dbf4c09d59e5bff9c9d5530814d56445ae30f2",
            size = 912,
            numPackages = 1,
        ),
        diffs = mapOf(
            "23" to EntryFileV2(
                name = "/23.json",
                sha256 = "b7fc69156cbd42aef1ec3f0a5a943868ccb4b62775bce71fa8cc06cc63ad425b",
                size = 911,
                numPackages = 1,
            ),
        ),
    )

    val emptyToMid = Entry(
        timestamp = 1337,
        version = 20001,
        index = EntryFileV2(
            name = "../index-mid-v2.json",
            sha256 = "561630a90ec9bcc29bc133cbd14b2d14d94124bb043c8d48effbad9d18d482fb",
            size = 22756,
            numPackages = 2,
        ),
        diffs = mapOf(
            "23" to EntryFileV2(
                name = "/23.json",
                sha256 = "1e19080fa0bdf37c7ea71106e97f0b0452da89edf37f638229582dc9a871e7c9",
                size = 22732,
                numPackages = 2,
            ),
            "42" to EntryFileV2(
                name = "/42.json",
                sha256 = "26203adba4fb64bbaf39743dfb8fc8d3d9f17ed809461beb6b7215dce278f263",
                size = 22594,
                numPackages = 2,
            ),
        ),
    )

    val emptyToMax = Entry(
        timestamp = Long.MAX_VALUE,
        version = Long.MAX_VALUE,
        maxAge = Int.MAX_VALUE,
        index = EntryFileV2(
            name = "../index-max-v2.json",
            sha256 = "36cbdb2f3134d94a210e457332e1945a237d8b8e642ae1276ffa419a9375665a",
            size = 29863,
            numPackages = 3,
        ),
        diffs = mapOf(
            "23" to EntryFileV2(
                name = "/23.json",
                sha256 = "521fe7e85ad77d0611e71bb5b96736d614ba8e62af43921eb05f05f30673f0c0",
                size = 29739,
                numPackages = 3,
            ),
            "42" to EntryFileV2(
                name = "/42.json",
                sha256 = "363aaaf7289828b94b684f2b1e2b8c2b9e4f1ad1f9ea3ffb99d53a87918a5a69",
                size = 29601,
                numPackages = 3,
            ),
            "1337" to EntryFileV2(
                name = "/1337.json",
                sha256 = "d0f14afad781d10b9d8f3e163ff26f99f769cb273a6a9d9757eac3f11d1a5d04",
                size = 20395,
                numPackages = 2,
            ),
        ),
    )

}
