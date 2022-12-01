package org.fdroid.test

import org.fdroid.index.v2.FeatureV2
import org.fdroid.index.v2.FileV1
import org.fdroid.index.v2.ManifestV2
import org.fdroid.index.v2.PackageVersionV2
import org.fdroid.index.v2.PermissionV2
import org.fdroid.index.v2.SignerV2
import org.fdroid.index.v2.UsesSdkV2
import org.fdroid.test.TestRepoUtils.getRandomFileV2
import org.fdroid.test.TestRepoUtils.getRandomLocalizedTextV2
import org.fdroid.test.TestUtils.getRandomList
import org.fdroid.test.TestUtils.getRandomMap
import org.fdroid.test.TestUtils.getRandomString
import org.fdroid.test.TestUtils.orNull
import kotlin.random.Random

object TestVersionUtils {

    fun getRandomPackageVersionV2(
        versionCode: Long = Random.nextLong(1, Long.MAX_VALUE),
    ) = PackageVersionV2(
        added = Random.nextLong(),
        file = getRandomFileV2(false).let {
            FileV1(it.name, it.sha256!!, it.size)
        },
        src = getRandomFileV2().orNull(),
        manifest = getRandomManifestV2(versionCode),
        releaseChannels = getRandomList { getRandomString() },
        antiFeatures = getRandomMap { getRandomString() to getRandomLocalizedTextV2() },
        whatsNew = getRandomLocalizedTextV2(),
    )

    fun getRandomManifestV2(versionCode: Long) = ManifestV2(
        versionName = getRandomString(),
        versionCode = versionCode,
        usesSdk = UsesSdkV2(
            minSdkVersion = Random.nextInt(),
            targetSdkVersion = Random.nextInt(),
        ),
        maxSdkVersion = Random.nextInt().orNull(),
        signer = SignerV2(getRandomList(Random.nextInt(1, 3)) {
            getRandomString(64)
        }).orNull(),
        usesPermission = getRandomList {
            PermissionV2(getRandomString(), Random.nextInt().orNull())
        },
        usesPermissionSdk23 = getRandomList {
            PermissionV2(getRandomString(), Random.nextInt().orNull())
        },
        nativecode = getRandomList(Random.nextInt(0, 4)) { getRandomString() },
        features = getRandomList { FeatureV2(getRandomString()) },
    )

}
