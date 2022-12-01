package org.fdroid.test

import org.fdroid.index.v2.FileV2
import org.fdroid.index.v2.LocalizedFileListV2
import org.fdroid.index.v2.MetadataV2
import org.fdroid.index.v2.Screenshots
import org.fdroid.test.TestRepoUtils.getRandomFileV2
import org.fdroid.test.TestRepoUtils.getRandomLocalizedFileV2
import org.fdroid.test.TestRepoUtils.getRandomLocalizedTextV2
import org.fdroid.test.TestUtils.getRandomList
import org.fdroid.test.TestUtils.getRandomString
import org.fdroid.test.TestUtils.orNull
import kotlin.random.Random
import kotlin.test.assertEquals

object TestAppUtils {

    fun getRandomMetadataV2(): MetadataV2 = MetadataV2(
        added = Random.nextLong(),
        lastUpdated = Random.nextLong(),
        name = getRandomLocalizedTextV2().orNull(),
        summary = getRandomLocalizedTextV2().orNull(),
        description = getRandomLocalizedTextV2().orNull(),
        webSite = getRandomString().orNull(),
        changelog = getRandomString().orNull(),
        license = getRandomString().orNull(),
        sourceCode = getRandomString().orNull(),
        issueTracker = getRandomString().orNull(),
        translation = getRandomString().orNull(),
        preferredSigner = getRandomString().orNull(),
        video = getRandomLocalizedTextV2().orNull(),
        authorName = getRandomString().orNull(),
        authorEmail = getRandomString().orNull(),
        authorWebSite = getRandomString().orNull(),
        authorPhone = getRandomString().orNull(),
        donate = getRandomList(Random.nextInt(0, 3)) { getRandomString() },
        liberapay = getRandomString().orNull(),
        liberapayID = getRandomString().orNull(),
        openCollective = getRandomString().orNull(),
        bitcoin = getRandomString().orNull(),
        litecoin = getRandomString().orNull(),
        flattrID = getRandomString().orNull(),
        icon = getRandomLocalizedFileV2().orNull(),
        featureGraphic = getRandomLocalizedFileV2().orNull(),
        promoGraphic = getRandomLocalizedFileV2().orNull(),
        tvBanner = getRandomLocalizedFileV2().orNull(),
        categories = getRandomList { getRandomString() }.orNull()
            ?: emptyList(),
        screenshots = getRandomScreenshots().orNull(),
    )

    fun getRandomScreenshots(): Screenshots? = Screenshots(
        phone = getRandomLocalizedFileListV2().orNull(),
        sevenInch = getRandomLocalizedFileListV2().orNull(),
        tenInch = getRandomLocalizedFileListV2().orNull(),
        wear = getRandomLocalizedFileListV2().orNull(),
        tv = getRandomLocalizedFileListV2().orNull(),
    ).takeIf { !it.isNull }

    fun getRandomLocalizedFileListV2(): Map<String, List<FileV2>> =
        TestUtils.getRandomMap(Random.nextInt(1, 3)) {
            getRandomString() to getRandomList(Random.nextInt(1, 7)) {
                getRandomFileV2()
            }
        }

    /**
     * [Screenshots] include lists which can be ordered differently,
     * so we need to ignore order when comparing them.
     */
    fun assertScreenshotsEqual(s1: Screenshots?, s2: Screenshots?) {
        if (s1 != null && s2 != null) {
            assertLocalizedFileListV2Equal(s1.phone, s2.phone)
            assertLocalizedFileListV2Equal(s1.sevenInch, s2.sevenInch)
            assertLocalizedFileListV2Equal(s1.tenInch, s2.tenInch)
            assertLocalizedFileListV2Equal(s1.wear, s2.wear)
            assertLocalizedFileListV2Equal(s1.tv, s2.tv)
        } else {
            assertEquals(s1, s2)
        }
    }

    private fun assertLocalizedFileListV2Equal(l1: LocalizedFileListV2?, l2: LocalizedFileListV2?) {
        if (l1 != null && l2 != null) {
            l1.keys.forEach { key ->
                assertEquals(l1[key]?.toSet(), l2[key]?.toSet())
            }
        } else {
            assertEquals(l1, l2)
        }
    }

}
