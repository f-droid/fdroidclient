package org.fdroid.database.test

import org.fdroid.database.test.TestRepoUtils.getRandomFileV2
import org.fdroid.database.test.TestRepoUtils.getRandomLocalizedTextV2
import org.fdroid.database.test.TestUtils.getRandomList
import org.fdroid.database.test.TestUtils.getRandomMap
import org.fdroid.database.test.TestUtils.getRandomString
import org.fdroid.database.test.TestUtils.orNull
import org.fdroid.index.v2.FeatureV2
import org.fdroid.index.v2.FileV1
import org.fdroid.index.v2.ManifestV2
import org.fdroid.index.v2.PackageVersionV2
import org.fdroid.index.v2.PermissionV2
import org.fdroid.index.v2.SignatureV2
import org.fdroid.index.v2.UsesSdkV2
import kotlin.random.Random

internal object TestVersionUtils {

    fun getRandomPackageVersionV2() = PackageVersionV2(
        added = Random.nextLong(),
        file = getRandomFileV2(false).let {
            FileV1(it.name, it.sha256!!, it.size)
        },
        src = getRandomFileV2().orNull(),
        manifest = getRandomManifestV2(),
        releaseChannels = getRandomList { getRandomString() },
        antiFeatures = getRandomMap { getRandomString() to getRandomLocalizedTextV2() },
        whatsNew = getRandomLocalizedTextV2(),
    )

    fun getRandomManifestV2() = ManifestV2(
        versionName = getRandomString(),
        versionCode = Random.nextLong(),
        usesSdk = UsesSdkV2(
            minSdkVersion = Random.nextInt(),
            targetSdkVersion = Random.nextInt(),
        ),
        maxSdkVersion = Random.nextInt().orNull(),
        signer = SignatureV2(getRandomList(Random.nextInt(1, 3)) {
            getRandomString(64)
        }).orNull(),
        usesPermission = getRandomList {
            PermissionV2(getRandomString(), Random.nextInt().orNull())
        },
        usesPermissionSdk23 = getRandomList {
            PermissionV2(getRandomString(), Random.nextInt().orNull())
        },
        nativeCode = getRandomList(Random.nextInt(0, 4)) { getRandomString() },
        features = getRandomList { FeatureV2(getRandomString()) },
    )

}
