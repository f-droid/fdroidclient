package org.fdroid.database

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import org.fdroid.index.v2.FeatureV2
import org.fdroid.index.v2.ManifestV2
import org.fdroid.index.v2.MetadataV2
import org.fdroid.index.v2.PackageVersionV2
import org.fdroid.index.v2.RepoV2
import org.junit.Assert
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

internal object TestUtils {

    fun assertTimestampRecent(timestamp: Long?) {
        assertNotNull(timestamp)
        assertTrue(System.currentTimeMillis() - timestamp < 2000)
    }

    fun assertRepoEquals(repoV2: RepoV2, repo: Repository) {
        val repoId = repo.repoId
        // mirrors
        val expectedMirrors = repoV2.mirrors.map { it.toMirror(repoId) }.toSet()
        Assert.assertEquals(expectedMirrors, repo.mirrors.toSet())
        // anti-features
        val expectedAntiFeatures = repoV2.antiFeatures.toRepoAntiFeatures(repoId).toSet()
        assertEquals(expectedAntiFeatures, repo.antiFeatures.toSet())
        // categories
        val expectedCategories = repoV2.categories.toRepoCategories(repoId).sortedBy { it.id }
        assertEquals(expectedCategories, repo.categories.sortedBy { it.id })
        // release channels
        val expectedReleaseChannels = repoV2.releaseChannels.toRepoReleaseChannel(repoId).toSet()
        assertEquals(expectedReleaseChannels, repo.releaseChannels.toSet())
        // core repo
        val coreRepo = repoV2.toCoreRepository(
            version = repo.repository.version!!.toLong(),
            formatVersion = repo.repository.formatVersion,
            certificate = repo.repository.certificate,
        ).copy(repoId = repoId)
        assertEquals(coreRepo, repo.repository)
    }

    internal fun App.toMetadataV2() = MetadataV2(
        added = metadata.added,
        lastUpdated = metadata.lastUpdated,
        name = metadata.name,
        summary = metadata.summary,
        description = metadata.description,
        webSite = metadata.webSite,
        changelog = metadata.changelog,
        license = metadata.license,
        sourceCode = metadata.sourceCode,
        issueTracker = metadata.issueTracker,
        translation = metadata.translation,
        preferredSigner = metadata.preferredSigner,
        video = metadata.video,
        authorName = metadata.authorName,
        authorEmail = metadata.authorEmail,
        authorWebSite = metadata.authorWebSite,
        authorPhone = metadata.authorPhone,
        donate = metadata.donate ?: emptyList(),
        liberapayID = metadata.liberapayID,
        liberapay = metadata.liberapay,
        openCollective = metadata.openCollective,
        bitcoin = metadata.bitcoin,
        litecoin = metadata.litecoin,
        flattrID = metadata.flattrID,
        categories = metadata.categories ?: emptyList(),
        icon = icon,
        featureGraphic = featureGraphic,
        promoGraphic = promoGraphic,
        tvBanner = tvBanner,
        screenshots = screenshots,
    )

    fun AppVersion.toPackageVersionV2() = PackageVersionV2(
        added = added,
        file = file,
        src = src,
        manifest = ManifestV2(
            versionName = manifest.versionName,
            versionCode = manifest.versionCode,
            usesSdk = manifest.usesSdk,
            maxSdkVersion = manifest.maxSdkVersion,
            signer = manifest.signer,
            usesPermission = usesPermission.sortedBy { it.name },
            usesPermissionSdk23 = usesPermissionSdk23.sortedBy { it.name },
            nativecode = manifest.nativecode?.sorted() ?: emptyList(),
            features = manifest.features?.map { FeatureV2(it) } ?: emptyList(),
        ),
        releaseChannels = releaseChannels,
        antiFeatures = version.antiFeatures ?: emptyMap(),
        whatsNew = version.whatsNew ?: emptyMap(),
    )

    fun <T> LiveData<T>.getOrAwaitValue(): T? {
        val data = arrayOfNulls<Any>(1)
        val latch = CountDownLatch(1)
        val observer: Observer<T> = object : Observer<T> {
            override fun onChanged(value: T) {
                data[0] = value
                latch.countDown()
                removeObserver(this)
            }
        }
        observeForever(observer)
        latch.await(2, TimeUnit.SECONDS)
        @Suppress("UNCHECKED_CAST")
        return data[0] as T?
    }

    fun <T> LiveData<T>.getOrFail(): T {
        return getOrAwaitValue() ?: fail()
    }
}
