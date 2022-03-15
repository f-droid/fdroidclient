package org.fdroid.database.test

import org.fdroid.database.test.TestRepoUtils.getRandomLocalizedFileV2
import org.fdroid.database.test.TestRepoUtils.getRandomLocalizedTextV2
import org.fdroid.database.test.TestUtils.getRandomList
import org.fdroid.database.test.TestUtils.getRandomString
import org.fdroid.database.test.TestUtils.orNull
import org.fdroid.index.v2.Author
import org.fdroid.index.v2.Donation
import org.fdroid.index.v2.LocalizedFileListV2
import org.fdroid.index.v2.MetadataV2
import org.fdroid.index.v2.Screenshots
import kotlin.random.Random
import kotlin.test.assertEquals

internal object TestAppUtils {

    fun getRandomMetadataV2() = MetadataV2(
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
        author = getRandomAuthor().orNull(),
        donation = getRandomDonation().orNull(),
        icon = getRandomLocalizedFileV2().orNull(),
        featureGraphic = getRandomLocalizedFileV2().orNull(),
        promoGraphic = getRandomLocalizedFileV2().orNull(),
        tvBanner = getRandomLocalizedFileV2().orNull(),
        categories = getRandomList { getRandomString() }.orNull()
            ?: emptyList(),
        screenshots = getRandomScreenshots().orNull(),
    )

    fun getRandomAuthor() = Author(
        name = getRandomString().orNull(),
        email = getRandomString().orNull(),
        website = getRandomString().orNull(),
        phone = getRandomString().orNull(),
    )

    fun getRandomDonation() = Donation(
        url = getRandomString().orNull(),
        liberapay = getRandomString().orNull(),
        liberapayID = getRandomString().orNull(),
        openCollective = getRandomString().orNull(),
        bitcoin = getRandomString().orNull(),
        litecoin = getRandomString().orNull(),
        flattrID = getRandomString().orNull(),
    )

    fun getRandomScreenshots() = Screenshots(
        phone = getRandomLocalizedFileListV2().orNull(),
        sevenInch = getRandomLocalizedFileListV2().orNull(),
        tenInch = getRandomLocalizedFileListV2().orNull(),
        wear = getRandomLocalizedFileListV2().orNull(),
        tv = getRandomLocalizedFileListV2().orNull(),
    ).takeIf { !it.isNull }

    fun getRandomLocalizedFileListV2() = TestUtils.getRandomMap(Random.nextInt(1, 3)) {
        getRandomString() to getRandomList(Random.nextInt(1,
            7)) { TestRepoUtils.getRandomFileV2() }
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
