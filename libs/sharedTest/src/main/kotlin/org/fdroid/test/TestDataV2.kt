package org.fdroid.test

import org.fdroid.index.RELEASE_CHANNEL_BETA
import org.fdroid.index.getV1ReleaseChannels
import org.fdroid.index.v2.AntiFeatureV2
import org.fdroid.index.v2.CategoryV2
import org.fdroid.index.v2.FeatureV2
import org.fdroid.index.v2.FileV1
import org.fdroid.index.v2.FileV2
import org.fdroid.index.v2.IndexV2
import org.fdroid.index.v2.ManifestV2
import org.fdroid.index.v2.MetadataV2
import org.fdroid.index.v2.MirrorV2
import org.fdroid.index.v2.PackageV2
import org.fdroid.index.v2.PackageVersionV2
import org.fdroid.index.v2.PermissionV2
import org.fdroid.index.v2.ReleaseChannelV2
import org.fdroid.index.v2.RepoV2
import org.fdroid.index.v2.Screenshots
import org.fdroid.index.v2.SignerV2
import org.fdroid.index.v2.UsesSdkV2

const val LOCALE = "en-US"

fun IndexV2.v1compat() = copy(
    repo = repo.v1compat(),
)

fun RepoV2.v1compat() = copy(
    name = name.filterKeys { it == LOCALE },
    description = description.filterKeys { it == LOCALE },
    icon = icon.filterKeys { it == LOCALE }.mapValues { it.value.v1compat() },
    webBaseUrl = null,
    mirrors = mirrors.map { MirrorV2(it.url) },
    categories = categories.mapValues { CategoryV2(name = mapOf(LOCALE to it.key)) },
    releaseChannels = getV1ReleaseChannels(),
    antiFeatures = antiFeatures.mapValues { AntiFeatureV2(name = mapOf(LOCALE to it.key)) },
)

fun PackageV2.v1compat(overrideLocale: Boolean = false) = copy(
    metadata = metadata.copy(
        name = if (overrideLocale) metadata.name?.filterKeys { it == LOCALE } else metadata.name,
        summary = if (overrideLocale) metadata.summary?.filterKeys { it == LOCALE }
        else metadata.summary,
        description = if (overrideLocale) metadata.description?.filterKeys { it == LOCALE }
        else metadata.description,
        icon = metadata.icon?.mapValues { it.value.v1compat() },
        featureGraphic = metadata.featureGraphic?.mapValues { it.value.v1compat() },
        promoGraphic = metadata.promoGraphic?.mapValues { it.value.v1compat() },
        tvBanner = metadata.tvBanner?.mapValues { it.value.v1compat() },
        screenshots = metadata.screenshots?.copy(
            phone = metadata.screenshots?.phone?.mapValues { list ->
                list.value.map { it.v1compat() }
            },
            sevenInch = metadata.screenshots?.sevenInch?.mapValues { list ->
                list.value.map { it.v1compat() }
            },
            tenInch = metadata.screenshots?.tenInch?.mapValues { list ->
                list.value.map { it.v1compat() }
            },
            wear = metadata.screenshots?.wear?.mapValues { list ->
                list.value.map { it.v1compat() }
            },
            tv = metadata.screenshots?.tv?.mapValues { list ->
                list.value.map { it.v1compat() }
            },
        )
    )
)

fun PackageVersionV2.v1compat() = copy(
    src = src?.v1compat(),
    manifest = manifest.copy(
        signer = if ((manifest.signer?.sha256?.size ?: 0) <= 1) manifest.signer else {
            SignerV2(manifest.signer?.sha256?.subList(0, 1) ?: error(""))
        }
    ),
    releaseChannels = releaseChannels.filter { it == RELEASE_CHANNEL_BETA },
    antiFeatures = antiFeatures.mapValues { emptyMap() }
)

fun FileV2.v1compat() = copy(
    sha256 = null,
    size = null,
)

object TestDataEmptyV2 {

    val repo = RepoV2(
        timestamp = 23,
        address = "https://empty-v1.org",
        name = mapOf(LOCALE to "EmptyV1"),
        icon = mapOf(
            LOCALE to FileV2(
                name = "/icons/empty-v1.png",
                sha256 = "824a109b2352138c3699760e1683385d0ed50ce526fc7982f8d65757743374bf",
                size = 32492,
            ),
        ),
        webBaseUrl = null,
        description = mapOf(LOCALE to "This is a repo with empty data."),
        mirrors = emptyList(),
        antiFeatures = emptyMap(),
        categories = emptyMap(),
        releaseChannels = emptyMap(),
    )
    val index = IndexV2(
        repo = repo,
        packages = emptyMap(),
    )
}

object TestDataMinV2 {

    val repo = RepoV2(
        timestamp = 42,
        name = mapOf(LOCALE to "MinV1"),
        icon = mapOf(
            LOCALE to
                FileV2(
                    name = "/icons/min-v1.png",
                    sha256 = "74758e480ae76297c8947f107db9ea03d2933c9d5c110d02046977cf78d43def",
                    size = 0,
                ),
        ),
        address = "https://min-v1.org",
        description = mapOf(LOCALE to "This is a repo with minimal data."),
    )

    const val packageName = "org.fdroid.min1"
    val version = PackageVersionV2(
        added = 0,
        file = FileV1(
            name = "/${packageName}_23.apk",
            sha256 = "824a109b2352138c3699760e1683385d0ed50ce526fc7982f8d65757743374bf",
            size = 1337,
        ),
        manifest = ManifestV2(
            versionCode = 1,
            versionName = "0",
        ),
    )
    val app = PackageV2(
        metadata = MetadataV2(
            added = 0,
            lastUpdated = 0,
            license = "", // not really needed, but easier for v1 conversion testing
        ),
        versions = mapOf(
            version.file.sha256 to version,
        ),
    )
    val packages = mapOf(packageName to app)

    val index = IndexV2(
        repo = repo,
        packages = packages,
    )
}

object TestDataMidV2 {

    val repo = RepoV2(
        timestamp = 1337,
        name = mapOf(
            LOCALE to "MidV1",
            "de" to "MitteV1",
        ),
        icon = mapOf(
            LOCALE to FileV2(
                name = "/icons/mid-v1.png",
                sha256 = "74758e480ae76297c7947f107db9ea03d2933c9d5c110d02046977cf78d43def",
                size = 234232352235,
            ),
            "de" to FileV2(
                name = "/icons/mitte-v1.png",
                sha256 = "34758e480ae76297c7947f107db9ea03d2933c9d5c110d02046977cf78d43def",
                size = 132352235,
            ),
        ),
        address = "https://mid-v1.org",
        description = mapOf(
            LOCALE to "This is a repo with medium data.",
            "de" to "Dies ist ein Repo mit mittlerer Datendichte.",
        ),
        mirrors = listOf(MirrorV2("https://mid-v1.com")),
        categories = mapOf(
            "Cat1" to CategoryV2(
                name = mapOf(LOCALE to "Cat1"),
                icon = mapOf(LOCALE to FileV2(
                    name = "/icons/cat2.png",
                    sha256 = "54758e480ae76297c7947f107db9ea03d2933c9d5c110d02046977cf78d43def",
                    size = Long.MAX_VALUE,
                )),
            ),
            "System" to CategoryV2(
                name = emptyMap(),
            ),
        ),
        antiFeatures = mapOf(
            "AntiFeature" to AntiFeatureV2(
                name = mapOf(LOCALE to "AntiFeature"),
                description = mapOf(LOCALE to "A bad anti-feature, we can't show to users."),
            )
        )
    )
    val repoCompat = repo.v1compat()

    const val packageName1 = TestDataMinV1.packageName
    const val packageName2 = "org.fdroid.fdroid"
    val version1_1 = PackageVersionV2(
        added = 2342,
        file = FileV1(
            name = "/${packageName1}_23_2.apk",
            sha256 = "824a109b2352138c3699760e1683385d0ed50ce526fc7982f8d65757743374bf",
            size = 1338,
        ),
        src = FileV2(
            name = "/${packageName1}_23_2.zip",
            sha256 = "824a109b2352138c3699760e1683385d0ed50ce526fc7982f8d65757743374bf",
            size = 1338,
        ),
        manifest = ManifestV2(
            versionCode = 1,
            versionName = "1",
            usesPermission = listOf(PermissionV2("perm")),
            nativecode = listOf("x86"),
            features = listOf(FeatureV2("feature")),
        ),
        antiFeatures = mapOf("AntiFeature" to mapOf(LOCALE to "reason")),
    )
    val version1_1Compat = version1_1.v1compat()
    val version1_2 = PackageVersionV2(
        added = 0,
        file = FileV1(
            name = "/${packageName1}_42.apk",
            sha256 = "824a109b2352138c3699760e1683385d0ed50ce526fc7982f8d65757743374bf",
            size = 1337,
        ),
        manifest = ManifestV2(
            versionCode = 24,
            versionName = "24",
            usesSdk = UsesSdkV2(
                minSdkVersion = 21,
                targetSdkVersion = 32,
            ),
            maxSdkVersion = 4568,
            signer = SignerV2(
                sha256 = listOf("824a109b2352138c3699760e1683385d0ed50ce526fc7982f8d65757743374bf"),
            ),
        ),
        src = FileV2(
            name = "/${packageName1}_42.zip",
            sha256 = "824a109b2352138c3699760e1683385d0ed50ce526fc7982f8d65757743374bf",
            size = 1338,
        ),
        antiFeatures = mapOf("AntiFeature" to mapOf(LOCALE to "reason")),
        releaseChannels = listOf("Beta")
    )
    val version1_2Compat = version1_2.v1compat().copy(
        antiFeatures = mapOf("AntiFeature" to emptyMap()),
    )

    val app1 = PackageV2(
        metadata = MetadataV2(
            added = 1234567890,
            categories = listOf(TestDataMidV1.categories[0]),
            summary = mapOf(
                LOCALE to "App1 summary",
                "de" to "App1 Zusammenfassung",
            ),
            description = mapOf(
                LOCALE to "App1 description",
                "de" to "App1 beschreibung",
            ),
            name = mapOf(
                LOCALE to "App1",
                "de" to "app 1 name",
            ),
            authorName = "App1 author",
            license = "GPLv3",
            webSite = "http://min1.test.org",
            icon = mapOf(
                LOCALE to FileV2(
                    name = "/icons/icon-min1.png",
                    sha256 = "824a109b2352138c3699760e1683385d0ed50ce526fc7982f8d65757743374bf",
                    size = 1337,
                ),
            ),
            lastUpdated = 1234567891,
        ),
        versions = mapOf(
            version1_1.file.sha256 to version1_1,
            version1_2.file.sha256 to version1_2,
        ),
    )
    val app1Compat = app1.v1compat(true).copy(
        versions = mapOf(
            version1_1.file.sha256 to version1_1Compat,
            version1_2.file.sha256 to version1_2Compat,
        ),
    )

    val version2_1 = PackageVersionV2(
        added = 1643250075000,
        file = FileV1(
            name = "/org.fdroid.fdroid_1014050.apk",
            sha256 = "8c89ce2f42f4a89af8ca6e1ea220f9dfdee220724d8a9cc067d510ac6f3e0d06",
            size = 8165518,
        ),
        manifest = ManifestV2(
            versionCode = 1014050,
            versionName = "1.14",
            usesSdk = UsesSdkV2(
                minSdkVersion = 22,
                targetSdkVersion = 25,
            ),
            signer = SignerV2(
                sha256 = listOf("43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab"),
            ),
            usesPermission = listOf(
                PermissionV2(name = "android.permission.INTERNET"),
                PermissionV2(name = "android.permission.ACCESS_NETWORK_STATE"),
                PermissionV2(name = "android.permission.ACCESS_WIFI_STATE"),
                PermissionV2(name = "android.permission.CHANGE_WIFI_MULTICAST_STATE"),
                PermissionV2(name = "android.permission.CHANGE_NETWORK_STATE"),
                PermissionV2(name = "android.permission.CHANGE_WIFI_STATE"),
                PermissionV2(name = "android.permission.BLUETOOTH"),
                PermissionV2(name = "android.permission.BLUETOOTH_ADMIN"),
                PermissionV2(name = "android.permission.RECEIVE_BOOT_COMPLETED"),
                PermissionV2(name = "android.permission.READ_EXTERNAL_STORAGE"),
                PermissionV2(name = "android.permission.WRITE_EXTERNAL_STORAGE"),
                PermissionV2(name = "android.permission.WRITE_SETTINGS"),
                PermissionV2(name = "android.permission.NFC"),
                PermissionV2(name = "android.permission.USB_PERMISSION", maxSdkVersion = 22),
                PermissionV2(name = "android.permission.WAKE_LOCK"),
                PermissionV2(name = "android.permission.FOREGROUND_SERVICE")
            ),
            usesPermissionSdk23 = listOf(
                PermissionV2(name = "android.permission.ACCESS_COARSE_LOCATION")
            ),
        ),
        src = FileV2(
            name = "/org.fdroid.fdroid_1014050_src.tar.gz",
            sha256 = "8c89ce2f42f4a89af8ca6e1ea220f9dfdee220724d8a9cc067d510ac6f3e0d07",
            size = 8165519,
        ),
        whatsNew = mapOf(
            LOCALE to "* Overhaul Share menu to use built-in options like Nearby",
            "eo" to "• rekonstruita menuo “Kunhavigi” por uzi enkonstruitajn eblojn, ekz.",
        ),
    )
    val version2_1Compat = version2_1.v1compat()

    val version2_2 = PackageVersionV2(
        added = 1642785071000,
        file = FileV1(
            name = "/org.fdroid.fdroid_1014005.apk",
            sha256 = "b4282febf5558d43c7c51a00478961f6df1b6d59e0a6674974cdacb792683e5d",
            size = 8382606,
        ),
        manifest = ManifestV2(
            versionCode = 1014005,
            versionName = "1.14-alpha5",
            usesSdk = UsesSdkV2(
                minSdkVersion = 22,
                targetSdkVersion = 25,
            ),
            signer = SignerV2(
                sha256 = listOf("43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab"),
            ),
            usesPermission = listOf(
                PermissionV2(name = "android.permission.INTERNET"),
                PermissionV2(name = "android.permission.ACCESS_NETWORK_STATE"),
                PermissionV2(name = "android.permission.ACCESS_WIFI_STATE"),
                PermissionV2(name = "android.permission.CHANGE_WIFI_MULTICAST_STATE"),
                PermissionV2(name = "android.permission.CHANGE_NETWORK_STATE"),
                PermissionV2(name = "android.permission.CHANGE_WIFI_STATE"),
                PermissionV2(name = "android.permission.BLUETOOTH"),
                PermissionV2(name = "android.permission.BLUETOOTH_ADMIN"),
                PermissionV2(name = "android.permission.RECEIVE_BOOT_COMPLETED"),
                PermissionV2(name = "android.permission.READ_EXTERNAL_STORAGE"),
                PermissionV2(name = "android.permission.WRITE_EXTERNAL_STORAGE"),
                PermissionV2(name = "android.permission.WRITE_SETTINGS"),
                PermissionV2(name = "android.permission.NFC"),
                PermissionV2(name = "android.permission.USB_PERMISSION", maxSdkVersion = 22),
                PermissionV2(name = "android.permission.WAKE_LOCK"),
                PermissionV2(name = "android.permission.FOREGROUND_SERVICE")
            ),
            usesPermissionSdk23 = listOf(
                PermissionV2(name = "android.permission.ACCESS_COARSE_LOCATION")
            ),
            nativecode = listOf("fakeNativeCode"),
            features = listOf(FeatureV2("fake feature")),
        ),
        src = FileV2(
            name = "/org.fdroid.fdroid_1014005_src.tar.gz",
            sha256 = "8c89ce2f42f4a89af8ca6e1ea220f9dfdee220724d8a9cc067d510ac6f3e0d07",
            size = 8165519,
        ),
        antiFeatures = mapOf("FakeAntiFeature" to emptyMap()),
    )
    val version2_2Compat = version2_2.v1compat()

    val version2_3 = PackageVersionV2(
        added = 1635169849000,
        file = FileV1(
            name = "/org.fdroid.fdroid_1014003.apk",
            sha256 = "c062a9642fde08aacabbfa4cab1ab5773c83f4e6b81551ffd92027d2b20f37d3",
            size = 8276110,
        ),
        manifest = ManifestV2(
            versionCode = 1014003,
            versionName = "1.14-alpha3",
            usesSdk = UsesSdkV2(
                minSdkVersion = 22,
                targetSdkVersion = 25,
            ),
            signer = SignerV2(
                sha256 = listOf("43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab"),
            ),
            usesPermission = listOf(
                PermissionV2(name = "android.permission.INTERNET"),
                PermissionV2(name = "android.permission.ACCESS_NETWORK_STATE"),
                PermissionV2(name = "android.permission.ACCESS_WIFI_STATE"),
                PermissionV2(name = "android.permission.CHANGE_WIFI_MULTICAST_STATE"),
                PermissionV2(name = "android.permission.CHANGE_NETWORK_STATE"),
                PermissionV2(name = "android.permission.CHANGE_WIFI_STATE"),
                PermissionV2(name = "android.permission.BLUETOOTH"),
                PermissionV2(name = "android.permission.BLUETOOTH_ADMIN"),
                PermissionV2(name = "android.permission.RECEIVE_BOOT_COMPLETED"),
                PermissionV2(name = "android.permission.READ_EXTERNAL_STORAGE"),
                PermissionV2(name = "android.permission.WRITE_EXTERNAL_STORAGE"),
                PermissionV2(name = "android.permission.WRITE_SETTINGS"),
                PermissionV2(name = "android.permission.NFC"),
                PermissionV2(name = "android.permission.USB_PERMISSION", maxSdkVersion = 22),
                PermissionV2(name = "android.permission.WAKE_LOCK"),
                PermissionV2(name = "android.permission.FOREGROUND_SERVICE")
            ),
            usesPermissionSdk23 = listOf(
                PermissionV2(name = "android.permission.ACCESS_COARSE_LOCATION")
            ),
        ),
        src = FileV2(
            name = "/org.fdroid.fdroid_1014003_src.tar.gz",
            sha256 = "8c89ce2f42f4a89af8ca6e1ea220f9dfdee220724d8a9cb067d510ac6f3e0d07",
            size = 8165519,
        ),
    )
    val version2_3Compat = version2_3.v1compat()

    val version2_4 = PackageVersionV2(
        added = 1632281731000,
        file = FileV1(
            name = "/org.fdroid.fdroid_1014002.apk",
            sha256 = "3243c24ee95be0fce0830d72e7d2605e3e24f6ccf4ee72a7c8e720fccd7621a1",
            size = 8284386,
        ),
        manifest = ManifestV2(
            versionCode = 1014002,
            versionName = "1.14-alpha2",
            usesSdk = UsesSdkV2(
                minSdkVersion = 22,
                targetSdkVersion = 25,
            ),
            signer = SignerV2(
                sha256 = listOf("43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab"),
            ),
            usesPermission = listOf(
                PermissionV2(name = "android.permission.INTERNET"),
                PermissionV2(name = "android.permission.ACCESS_NETWORK_STATE"),
                PermissionV2(name = "android.permission.ACCESS_WIFI_STATE"),
                PermissionV2(name = "android.permission.CHANGE_WIFI_MULTICAST_STATE"),
                PermissionV2(name = "android.permission.CHANGE_NETWORK_STATE"),
                PermissionV2(name = "android.permission.CHANGE_WIFI_STATE"),
                PermissionV2(name = "android.permission.BLUETOOTH"),
                PermissionV2(name = "android.permission.BLUETOOTH_ADMIN"),
                PermissionV2(name = "android.permission.RECEIVE_BOOT_COMPLETED"),
                PermissionV2(name = "android.permission.READ_EXTERNAL_STORAGE"),
                PermissionV2(name = "android.permission.WRITE_EXTERNAL_STORAGE"),
                PermissionV2(name = "android.permission.WRITE_SETTINGS"),
                PermissionV2(name = "android.permission.NFC"),
                PermissionV2(name = "android.permission.USB_PERMISSION", maxSdkVersion = 22),
                PermissionV2(name = "android.permission.WAKE_LOCK"),
                PermissionV2(name = "android.permission.FOREGROUND_SERVICE")
            ),
            usesPermissionSdk23 = listOf(
                PermissionV2(name = "android.permission.ACCESS_COARSE_LOCATION")
            ),
        ),
        src = FileV2(
            name = "/org.fdroid.fdroid_1014002_src.tar.gz",
            sha256 = "7c89ce2f42f4a89af8ca6e1ea220f9dfdee220724d8a9cb067d510ac6f3e0d07",
            size = 7165519,
        ),
    )
    val version2_4Compat = version2_4.v1compat()

    val version2_5 = PackageVersionV2(
        added = 1632281729000,
        file = FileV1(
            name = "/org.fdroid.fdroid_1014001.apk",
            sha256 = "7ebfd5eb76f9ec95ba955e549260fe930dc38fb99ed3532f92c93b879aca5610",
            size = 8272166,
        ),
        manifest = ManifestV2(
            versionCode = 1014001,
            versionName = "1.14-alpha1",
            usesSdk = UsesSdkV2(
                minSdkVersion = 22,
                targetSdkVersion = 25,
            ),
            signer = SignerV2(
                sha256 = listOf("43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab"),
            ),
            usesPermission = listOf(
                PermissionV2(name = "android.permission.INTERNET"),
                PermissionV2(name = "android.permission.ACCESS_NETWORK_STATE"),
                PermissionV2(name = "android.permission.ACCESS_WIFI_STATE"),
                PermissionV2(name = "android.permission.CHANGE_WIFI_MULTICAST_STATE"),
                PermissionV2(name = "android.permission.CHANGE_NETWORK_STATE"),
                PermissionV2(name = "android.permission.CHANGE_WIFI_STATE"),
                PermissionV2(name = "android.permission.BLUETOOTH"),
                PermissionV2(name = "android.permission.BLUETOOTH_ADMIN"),
                PermissionV2(name = "android.permission.RECEIVE_BOOT_COMPLETED"),
                PermissionV2(name = "android.permission.READ_EXTERNAL_STORAGE"),
                PermissionV2(name = "android.permission.WRITE_EXTERNAL_STORAGE"),
                PermissionV2(name = "android.permission.WRITE_SETTINGS"),
                PermissionV2(name = "android.permission.NFC"),
                PermissionV2(name = "android.permission.USB_PERMISSION", maxSdkVersion = 22),
                PermissionV2(name = "android.permission.WAKE_LOCK"),
                PermissionV2(name = "android.permission.FOREGROUND_SERVICE")
            ),
            usesPermissionSdk23 = listOf(
                PermissionV2(name = "android.permission.ACCESS_COARSE_LOCATION")
            ),
        ),
        src = FileV2(
            name = "/org.fdroid.fdroid_1014001_src.tar.gz",
            sha256 = "6c89ce2f42f4a89af8ca6e1ea220f9dfdee220724d8a9cb067d510ac6f3e0d07",
            size = 6165519,
        ),
    )
    val version2_5Compat = version2_5.v1compat()

    val app2 = PackageV2(
        metadata = MetadataV2(
            added = 1295222400000,
            categories = listOf("System"),
            name = mapOf(
                "af" to "-درويد",
                "be" to "F-Droid",
                "bn" to "এফ-ড্রয়েড",
                "ca" to "F-Droid",
                "cs" to "F-Droid",
                "cy" to "F-Droid",
                "el" to "F-Droid",
                "en-US" to "F-Droid",
                "eo" to "F-Droid",
            ),
            summary = mapOf(
                "af" to "متجر التطبيقات الذي يحترم الحرية والخصوصية)",
                "be" to "Крама праграм, якая паважае свабоду і прыватнасць",
                "bg" to "Магазинът за приложения, който уважава независимостта и поверителността",
                "bn" to "যে অ্যাপ স্টোর স্বাধীনতা ও গোপনীয়তা সম্মান করে",
                "bo" to "རང་དབང་དང་གསང་དོན་ལ་གུས་བརྩི་ཞུས་མཁན་གྱི་མཉེན་ཆས་ཉར་ཚགས་ཁང་།",
                "ca" to "La botiga d'aplicacions que respecta la llibertat i la privacitat",
                "cs" to "Zdroj aplikací který respektuje vaši svobodu a soukromí",
                "cy" to "Yr ystorfa apiau sy'n parchu rhyddid a phreifatrwydd",
                "de" to "Der App-Store, der Freiheit und Privatsphäre respektiert",
                "el" to "Το κατάστημα εφαρμογών που σέβεται την ελευθερία και την ιδιωτικότητα",
                "en-US" to "The app store that respects freedom and privacy",
                "eo" to "Aplikaĵa vendejo respektanta liberecon kaj privatecon",
            ),
            description = mapOf(
                "af" to "F-Droid is 'n installeerbare katalogus van gratis sagteware",
                "bo" to "ཨེཕ་རོཌ་ནི་ཨེན་ཀྲོཌ་བབ་སྟེགས་ཀྱི་ཆེད་དུ་FOSS",
                "ca" to "F-Droid és un catàleg instal·lable d'aplicacions de software lliure",
                "cs" to "F-Droid je instalovatelný katalog softwarových libre",
                "cy" to "Mae F-Droid yn gatalog y gellir ei osod o apiau meddalwedd " +
                    "rhyddar gyfer Android.",
                "de" to "F-Droid ist ein installierbarer Katalog mit Libre SoftwareAndroid-Apps.",
                "el" to "Το F-Droid είναι ένας κατάλογος εφαρμογών ελεύθερου λογισμικού",
                "en-US" to "F-Droid is an installable catalogue of libre software",
                "eo" to "F-Droid estas instalebla katalogo de liberaj aplikaĵoj por Android."
            ),
            changelog = "https://gitlab.com/fdroid/fdroidclient/raw/HEAD/CHANGELOG.md",
            translation = "https://hosted.weblate.org/projects/f-droid/f-droid",
            issueTracker = "https://gitlab.com/fdroid/fdroidclient/issues",
            sourceCode = "https://gitlab.com/fdroid/fdroidclient",
            donate = listOf("https://f-droid.org/donate"),
            liberapayID = "27859",
            openCollective = "F-Droid-Euro",
            flattrID = "343053",
            preferredSigner = "43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab",
            license = "GPL-3.0-or-later",
            webSite = "https://f-droid.org",
            icon = mapOf(
                LOCALE to FileV2(
                    name = "/icons/org.fdroid.fdroid.1014050.png",
                    sha256 = "224a109b2352138c3699760e1683385d0ed50ce526fc7982f8d65757743374bf",
                    size = 1237,
                ),
            ),
            featureGraphic = mapOf(
                LOCALE to FileV2(
                    name = "/org.fdroid.fdroid/en-US/" +
                        "featureGraphic_PTun9TO4cMFOeiqbvQSrkdcxNUcOFQCymMIaj9UJOAY=.jpg",
                    sha256 = "424a109b2352138c3699760e1683385d0ed50ce526fc7982f8d65757743374bf",
                    size = 4237,
                ),
            ),
            screenshots = Screenshots(
                phone = mapOf(
                    LOCALE to listOf(
                        FileV2(
                            name = "/org.fdroid.fdroid/en-US/phoneScreenshots/" +
                                "screenshot-app-details.png",
                            sha256 = "424a109b2352138c3699760e1683385d" +
                                "0ed50ce526fc7982f8d65757743374bf",
                            size = 4237,
                        ),
                        FileV2(
                            name = "/org.fdroid.fdroid/en-US/phoneScreenshots/" +
                                "screenshot-dark-details.png",
                            sha256 = "424a109b2352138c3699760e1683385d" +
                                "0ed50ce526fc7982f8d65757743374bf",
                            size = 44287,
                        ),
                        FileV2(
                            name = "/org.fdroid.fdroid/en-US/phoneScreenshots/" +
                                "screenshot-dark-home.png",
                            sha256 = "424a109b2352138c3699760e1673385d" +
                                "0ed50ce526fc7982f8d65757743374bf",
                            size = 4587,
                        ),
                        FileV2(
                            name = "/org.fdroid.fdroid/en-US/phoneScreenshots/" +
                                "screenshot-dark-knownvuln.png",
                            sha256 = "424a109b2352138c3699760e1683385d" +
                                "0ed50ce526fc7982f8d65757743374bf",
                            size = 445837,
                        ),
                        FileV2(
                            name = "/org.fdroid.fdroid/en-US/phoneScreenshots/" +
                                "screenshot-knownvuln.png",
                            sha256 = "424a109b2352138c4599760e1683385d" +
                                "0ed50ce526fc7982f8d65757743374bf",
                            size = 4287,
                        ),
                        FileV2(
                            name = "/org.fdroid.fdroid/en-US/phoneScreenshots/" +
                                "screenshot-search.png",
                            sha256 = "424a109b2352138c3694760e1683385d" +
                                "0ed50ce526fc7982f8d65757743374bf",
                            size = 2857,
                        ),
                        FileV2(
                            name = "/org.fdroid.fdroid/en-US/phoneScreenshots/" +
                                "screenshot-updates.png",
                            sha256 = "424a109b2352138c3699750e1683385d" +
                                "0ed50ce526fc7982f8d65757743374bf",
                            size = 485287,
                        ),
                    ),
                ),
            ),
            lastUpdated = 1643250075000,
        ),
        versions = mapOf(
            version2_1.file.sha256 to version2_1,
            version2_2.file.sha256 to version2_2,
            version2_3.file.sha256 to version2_3,
            version2_4.file.sha256 to version2_4,
            version2_5.file.sha256 to version2_5,
        ),
    )
    val app2Compat = app2.v1compat().copy(
        versions = mapOf(
            version2_1.file.sha256 to version2_1Compat,
            version2_2.file.sha256 to version2_2Compat,
            version2_3.file.sha256 to version2_3Compat,
            version2_4.file.sha256 to version2_4Compat,
            version2_5.file.sha256 to version2_5Compat,
        ),
    )
    val packages = mapOf(packageName1 to app1, packageName2 to app2)

    val index = IndexV2(
        repo = repo,
        packages = packages,
    )
    val indexCompat = index.copy(
        repo = repoCompat,
        packages = mapOf(
            packageName1 to app1Compat,
            packageName2 to app2Compat,
        ),
    )
}

object TestDataMaxV2 {

    val repo = RepoV2(
        timestamp = Long.MAX_VALUE,
        name = mapOf(LOCALE to "MaxV1", "de_DE" to "MaximumV1"),
        icon = mapOf(
            LOCALE to FileV2(
                name = "/icons/max-v1.png",
                sha256 = "14758e480ae76297c7947f107db9ea03d2933c9d5c110d02046977cf78d43def",
                size = Long.MAX_VALUE,
            ),
            "de_DE" to FileV2(
                name = "/icons/maximal-v1.png",
                sha256 = "12758e480ae76297c7947f107db9ea03d2933c9d5c110d02046977cf78d43def",
                size = Long.MAX_VALUE - 1,
            ),
        ),
        address = "https://max-v1.org",
        webBaseUrl = "https://www.max-v1.org",
        description = mapOf(
            LOCALE to "This is a repo with maximum data.",
            "de" to "Dies ist ein Repo mit maximaler Datendichte.",
        ),
        mirrors = listOf(
            MirrorV2("https://max-v1.com", "us"),
            MirrorV2("https://max-v1.org", "nl"),
        ),
        antiFeatures = mapOf(
            "VeryBad" to AntiFeatureV2(
                name = emptyMap(),
            ),
            "Dont,Show,This" to AntiFeatureV2(
                name = emptyMap(),
            ),
            "NotNice" to AntiFeatureV2(
                name = emptyMap(),
            ),
            "AntiFeature" to AntiFeatureV2(
                icon = mapOf(LOCALE to FileV2(
                    name = "/icons/antifeature.png",
                    sha256 = "24758e480ae66297c7947f107db9ea03d2933c9d5c110d02046977cf78d43def",
                    size = 254916,
                )),
                name = mapOf(LOCALE to "AntiFeature"),
                description = mapOf(LOCALE to "A bad anti-feature, we can't show to users."),
            ),
            "NonFreeNet" to AntiFeatureV2(
                name = mapOf(LOCALE to "NonFreeNet"),
            ),
            "AddOne" to AntiFeatureV2(
                name = mapOf(LOCALE to "AddOne anti feature"),
                description = mapOf(LOCALE to "A bad anti-feature, that was added in an update."),
            ),
        ),
        categories = mapOf(
            "Cat3" to CategoryV2(
                name = mapOf(LOCALE to "Cat3"),
                icon = mapOf(LOCALE to FileV2(
                    name = "/icons/cat3.png",
                    sha256 = "54758e480ae76297c7947f107db9ea03d2933c9d5c110d02046977cf78d43def",
                    size = Long.MAX_VALUE,
                )),
                description = mapOf(LOCALE to "Cat3"),
            ),
            "Cat2" to CategoryV2(
                name = mapOf(LOCALE to "Cat3"),
                icon = mapOf(LOCALE to FileV2(
                    name = "/icons/cat2.png",
                    sha256 = "54758e480ae76297c7947f107db9ea03d2933c9d5c110d02046977cf78d43def",
                    size = Long.MAX_VALUE,
                )),
                description = mapOf(LOCALE to "Cat3"),
            ),
            "Cat1" to CategoryV2(
                name = mapOf(LOCALE to "Cat1"),
                icon = mapOf(LOCALE to FileV2(
                    name = "/icons/cat1.png",
                    sha256 = "54758e480ae76297c7947f107db9ea03d2933c9d5c110d02046977cf78d43def",
                    size = Long.MAX_VALUE,
                )),
                description = mapOf(LOCALE to "Cat1"),
            ),
            "NoMoreSystem" to CategoryV2(
                name = emptyMap(),
            ),
            "OneMore" to CategoryV2(
                name = emptyMap(),
            ),
        ),
        releaseChannels = mapOf(
            "Alpha" to ReleaseChannelV2(
                name = mapOf("de" to "channel name alpha"),
                description = mapOf("de-DE" to "channel desc alpha"),
            ),
            "Beta" to ReleaseChannelV2(
                name = mapOf(LOCALE to "channel name"),
                description = mapOf(LOCALE to "channel desc"),
            ),
        )
    )
    val repoCompat = repo.v1compat().copy(
        description = mapOf(LOCALE to "This is a repo with medium data."),
        categories = mapOf(
            "Cat1" to CategoryV2(name = emptyMap()),
            "System" to CategoryV2(name = emptyMap()),
        ),
        antiFeatures = mapOf(
            "AntiFeature" to AntiFeatureV2(name = emptyMap())
        ),
    )

    const val packageName1 = TestDataMidV2.packageName1
    const val packageName2 = TestDataMidV2.packageName2
    const val packageName3 = "Haoheiseeshai2que2Che0ooSa6aikeemoo2ap9Aequoh4ju5chooYuPhiev8moo" +
        "dahlonu2oht5Eikahvushapeum5aefo6xig4aghahyaaNuezoo4eexee1Goo5Ung" +
        "ohGha6quaeghe8uCh9iex9Oowa9aiyohzoo2ij5miifiegaeth8nie9jae6raeph" +
        "oowishoor1Ien5vahGhahm7eidaiy2AeCaej9iexahyooshu2ic9tea1ool8tu4Y"

    val version2_2 = TestDataMidV2.version2_2.copy(
        manifest = TestDataMidV2.version2_2.manifest.copy(
            usesPermission = emptyList(),
            usesPermissionSdk23 = emptyList(),
            nativecode = emptyList(),
            features = emptyList(),
        ),
        antiFeatures = mapOf(
            "AddOne" to mapOf(LOCALE to "was added this update"),
        ),
        releaseChannels = listOf(RELEASE_CHANNEL_BETA),
    )
    val version2_3 = TestDataMidV2.version2_3.copy(
        manifest = TestDataMidV2.version2_3.manifest.copy(
            versionCode = 1014003,
            versionName = "1.14-alpha3",
            usesSdk = UsesSdkV2(
                minSdkVersion = 22,
                targetSdkVersion = 25,
            ),
            signer = SignerV2(
                sha256 = listOf("43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab"),
            ),
            usesPermission = listOf(
                PermissionV2(name = "android.permission.ACCESS_MEDIA"),
                PermissionV2(name = "android.permission.CHANGE_WIFI_STATE", maxSdkVersion = 32),
                PermissionV2(name = "android.permission.READ_EXTERNAL_STORAGE"),
                PermissionV2(name = "android.permission.WRITE_EXTERNAL_STORAGE"),
                PermissionV2(name = "android.permission.NFC"),
                PermissionV2(name = "android.permission.USB_PERMISSION", maxSdkVersion = 22),
                PermissionV2(name = "android.permission.WAKE_LOCK"),
                PermissionV2(name = "android.permission.READ_MY_ASS"),
                PermissionV2(name = "android.permission.FOREGROUND_SERVICE"),
            ),
            usesPermissionSdk23 = listOf(
                PermissionV2(name = "android.permission.ACCESS_FINE_LOCATION"),
                PermissionV2(name = "android.permission.ACCESS_COARSE_LOCATION", maxSdkVersion = 3),
            ),
        ),
        file = TestDataMidV2.version2_3.file.copy(
            size = 8276110,
        ),
        src = TestDataMidV2.version2_3.src?.copy(
            name = "/org.fdroid.fdroid_1014003_src.tar.gz",
        ),
        antiFeatures = mapOf(
            "AddOne" to mapOf(LOCALE to "was added this update"),
        ),
        whatsNew = mapOf(
            "ch" to "This is new!",
            "de" to "das ist neu",
            "en-US" to "this is new",
        ),
    )
    val version2_4 = TestDataMidV2.version2_4.copy(
        antiFeatures = mapOf(
            "AddOne" to emptyMap(),
        ),
    )
    val version2_5 = TestDataMidV2.version2_5.copy(
        antiFeatures = mapOf(
            "AddOne" to mapOf(LOCALE to "was added this update"),
        ),
    )
    val app2 = TestDataMidV2.app2.copy(
        metadata = TestDataMidV2.app2.metadata.copy(
            categories = listOf("NoMoreSystem", "OneMore"),
            summary = mapOf(
                LOCALE to "new summary in en-US",
                "ch" to "new summary",
                "de" to "Der App-Store, der Freiheit und Privatsphäre respektiert",
            ),
            description = mapOf(
                LOCALE to "F-Droid is an installable catalogue of libre software",
                "ch" to "new desc",
            ),
            webSite = "https://fdroid.org",
            name = mapOf(
                LOCALE to "F-DroidX",
                "ch" to "new name",
            ),
            icon = mapOf(
                LOCALE to FileV2(
                    name = "/org.fdroid.fdroid/en-US/new icon",
                    sha256 = "324a109b2352138c3699760e1683385d1ed50ce526fc7982f8d65757743374ba",
                    size = 2233,
                )
            ),
            screenshots = TestDataMidV2.app2.metadata.screenshots?.copy(
                phone = mapOf(
                    LOCALE to listOf(
                        FileV2(
                            name = "/org.fdroid.fdroid/en-US/phoneScreenshots/" +
                                "screenshot-app-details.png",
                            sha256 = "424a109b2352138c3699760e1683385d" +
                                "0ed50ce526fc7982f8d65757743374bf",
                            size = 4237,
                        ),
                        FileV2(
                            name = "/org.fdroid.fdroid/en-US/phoneScreenshots/" +
                                "screenshot-dark-details.png",
                            sha256 = "424a109b2352138c3699760e1683385d" +
                                "0ed50ce526fc7982f8d65757743374bf",
                            size = 44287,
                        ),
                        FileV2(
                            name = "/org.fdroid.fdroid/en-US/phoneScreenshots/" +
                                "screenshot-dark-home.png",
                            sha256 = "424a109b2352138c3699760e1673385d" +
                                "0ed50ce526fc7982f8d65757743374bf",
                            size = 4587,
                        ),
                        FileV2(
                            name = "/org.fdroid.fdroid/en-US/phoneScreenshots/" +
                                "screenshot-search.png",
                            sha256 = "424a109b2352138c3694760e1683385d" +
                                "0ed50ce526fc7982f8d65757743374bf",
                            size = 2857,
                        ),
                        FileV2(
                            name = "/org.fdroid.fdroid/en-US/phoneScreenshots/" +
                                "screenshot-updates.png",
                            sha256 = "424a109b2352138c3699750e1683385d" +
                                "0ed50ce526fc7982f8d65757743374bf",
                            size = 485286,
                        ),
                    ),
                ),
                tenInch = mapOf(
                    "ch" to listOf(
                        FileV2(
                            name = "/org.fdroid.fdroid/ch/tenInchScreenshots/new screenshots",
                            sha256 = "54758e380ae76297c7947f107db9ea03" +
                                "d2933c9d5c110d02046977cf78d43def",
                            size = Long.MIN_VALUE,
                        )
                    ),
                ),
                wear = null,
            ),
        ),
        versions = mapOf(
            // remove one and replace two
            version2_2.file.sha256 to version2_2,
            version2_3.file.sha256 to version2_3,
            version2_4.file.sha256 to version2_4,
            version2_5.file.sha256 to version2_5,
        ),
    )
    private val app2CompatPre = app2.v1compat(true)
    val app2Compat = app2CompatPre.copy(
        // due to locale overrides
        metadata = app2CompatPre.metadata.copy(
            summary = mapOf(LOCALE to "new summary"),
            description = mapOf(LOCALE to "new description"),
        ),
        versions = mapOf(
            // remove one and replace two
            version2_2.file.sha256 to version2_2.v1compat(),
            version2_3.file.sha256 to version2_3.v1compat(),
            version2_4.file.sha256 to version2_4.v1compat(),
            version2_5.file.sha256 to version2_5.v1compat(),
        ),
    )

    val version3_1 = PackageVersionV2(
        added = 1643250075000,
        file = FileV1(
            name = "/org.fdroid.fdroid_1014050.apk",
            sha256 = "8c89ce2f42f4a89af8ca6e1ea220f9dfdee220724d8a9cc067d510ac6f3e0d06",
            size = 8165518,
        ),
        src = FileV2(
            name = "/org.fdroid.fdroid_1014050_src.tar.gz",
            sha256 = "8c89ce2f42f4a89af8ca6e1ea220f9dfdee220724d8a9cc067d510ac6f3e0d06",
            size = 8165518,
        ),
        manifest = ManifestV2(
            versionName = "1.14",
            versionCode = 1014050,
            usesSdk = UsesSdkV2(
                minSdkVersion = 22,
                targetSdkVersion = 25,
            ),
            maxSdkVersion = Int.MAX_VALUE,
            signer = SignerV2(
                sha256 = listOf(
                    "43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab",
                    "33238d512c1e3eb2d6569f4a3bfbf5523418b22e0a3ed1552770abb9a9c9ccvb",
                ),
            ),
            usesPermission = listOf(
                PermissionV2(name = "android.permission.INTERNET"),
                PermissionV2(name = "android.permission.ACCESS_NETWORK_STATE"),
                PermissionV2(name = "android.permission.ACCESS_WIFI_STATE"),
                PermissionV2(name = "android.permission.CHANGE_WIFI_MULTICAST_STATE"),
                PermissionV2(name = "android.permission.CHANGE_NETWORK_STATE"),
                PermissionV2(name = "android.permission.CHANGE_WIFI_STATE"),
                PermissionV2(name = "android.permission.BLUETOOTH"),
                PermissionV2(name = "android.permission.BLUETOOTH_ADMIN"),
                PermissionV2(name = "android.permission.RECEIVE_BOOT_COMPLETED"),
                PermissionV2(name = "android.permission.READ_EXTERNAL_STORAGE"),
                PermissionV2(name = "android.permission.WRITE_EXTERNAL_STORAGE"),
                PermissionV2(name = "android.permission.WRITE_SETTINGS"),
                PermissionV2(name = "android.permission.NFC"),
                PermissionV2(name = "android.permission.USB_PERMISSION", maxSdkVersion = 22),
                PermissionV2(name = "android.permission.WAKE_LOCK"),
                PermissionV2(name = "android.permission.FOREGROUND_SERVICE"),
            ),
            usesPermissionSdk23 = listOf(
                PermissionV2(name = "android.permission.ACCESS_COARSE_LOCATION"),
                PermissionV2(
                    name = "android.permission.USB_PERMISSION",
                    maxSdkVersion = Int.MAX_VALUE,
                ),
            ),
            nativecode = listOf("x86", "x86_64"),
            features = listOf(FeatureV2("feature"), FeatureV2("feature2")),
        ),
        releaseChannels = listOf("Beta", "Alpha"),
        antiFeatures = mapOf(
            "AntiFeature" to emptyMap(),
            "NonFreeNet" to emptyMap(),
            "NotNice" to emptyMap(),
            "VeryBad" to emptyMap(),
            "Dont,Show,This" to emptyMap(),
            "anti-feature" to mapOf(LOCALE to "bla", "de" to "blubb"),
            "anti-feature2" to mapOf("de" to "blabla"),
        ),
    )
    val app3 = PackageV2(
        metadata = MetadataV2(
            name = mapOf(
                LOCALE to "App3",
                "en" to "en ",
            ),
            summary = mapOf(
                LOCALE to "App3 summary",
                "en" to "en ",
            ),
            description = mapOf(
                LOCALE to "App3 description",
                "en" to "en ",
            ),
            added = 1234567890,
            lastUpdated = Long.MAX_VALUE,
            webSite = "http://min1.test.org",
            changelog = "changeLog3",
            license = "GPLv3",
            sourceCode = "source code3",
            issueTracker = "tracker3",
            translation = "translation3",
            preferredSigner = "43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab",
            categories = listOf("Cat1", "Cat2", "Cat3"),
            authorName = "App3 author",
            authorEmail = "email",
            authorWebSite = "website",
            authorPhone = "phone",
            donate = listOf("donate"),
            liberapayID = "liberapayID",
            liberapay = "liberapay",
            openCollective = "openCollective",
            bitcoin = "bitcoin",
            litecoin = "litecoin",
            flattrID = "flattrID",
            icon = mapOf(
                "en" to FileV2(
                    name = "/Haoheiseeshai2que2Che0ooSa6aikeemoo2ap9Aequoh4ju5chooYuPhiev8moodahl" +
                        "onu2oht5Eikahvushapeum5aefo6xig4aghahyaaNuezoo4eexee1Goo5UngohGha6quaegh" +
                        "e8uCh9iex9Oowa9aiyohzoo2ij5miifiegaeth8nie9jae6raephoowishoor1Ien5vahGha" +
                        "hm7eidaiy2AeCaej9iexahyooshu2ic9tea1ool8tu4Y/en/en ",
                    sha256 = "32758e380aeg6297c7947f107db9ea03d2933c9d5c110d02046977cf78d43def",
                    size = 2253245,
                ),
            ),
            featureGraphic = mapOf(
                "en" to FileV2(
                    name = "/Haoheiseeshai2que2Che0ooSa6aikeemoo2ap9Aequoh4ju5chooYuPhiev8moodahl" +
                        "onu2oht5Eikahvushapeum5aefo6xig4aghahyaaNuezoo4eexee1Goo5UngohGha6quaegh" +
                        "e8uCh9iex9Oowa9aiyohzoo2ij5miifiegaeth8nie9jae6raephoowishoor1Ien5vahGha" +
                        "hm7eidaiy2AeCaej9iexahyooshu2ic9tea1ool8tu4Y/en/en ",
                    sha256 = "54758e380ae762f7c7947f107db9ea03d2933c9d5c110d02046977cf78d43def",
                    size = 32453245,
                ),
            ),
            promoGraphic = mapOf(
                "en" to FileV2(
                    name = "/Haoheiseeshai2que2Che0ooSa6aikeemoo2ap9Aequoh4ju5chooYuPhiev8moodahl" +
                        "onu2oht5Eikahvushapeum5aefo6xig4aghahyaaNuezoo4eexee1Goo5UngohGha6quaegh" +
                        "e8uCh9iex9Oowa9aiyohzoo2ij5miifiegaeth8nie9jae6raephoowishoor1Ien5vahGha" +
                        "hm7eidaiy2AeCaej9iexahyooshu2ic9tea1ool8tu4Y/en/en ",
                    sha256 = "54758e380aee6297c7947f107db9ea03d2933c9d5c110d02046977cf78d43def",
                    size = 4523325,
                ),
            ),
            tvBanner = mapOf(
                "en" to FileV2(
                    name = "/Haoheiseeshai2que2Che0ooSa6aikeemoo2ap9Aequoh4ju5chooYuPhiev8moodahl" +
                        "onu2oht5Eikahvushapeum5aefo6xig4aghahyaaNuezoo4eexee1Goo5UngohGha6quaegh" +
                        "e8uCh9iex9Oowa9aiyohzoo2ij5miifiegaeth8nie9jae6raephoowishoor1Ien5vahGha" +
                        "hm7eidaiy2AeCaej9iexahyooshu2ic9tea1ool8tu4Y/en/en ",
                    sha256 = "54758e380aeh6297c7947f107db9ea03d2933c9d5c110d02046977cf78d43def",
                    size = 32453245,
                ),
            ),
            video = mapOf(
                "en" to "en ",
            ),
            screenshots = Screenshots(
                phone = mapOf(
                    "en" to listOf(
                        FileV2(
                            name = "/Haoheiseeshai2que2Che0ooSa6aikeemoo2ap9Aequoh4ju5chooYuPhiev" +
                                "8moodahlonu2oht5Eikahvushapeum5aefo6xig4aghahyaaNuezoo4eexee1Goo" +
                                "5UngohGha6quaeghe8uCh9iex9Oowa9aiyohzoo2ij5miifiegaeth8nie9jae6r" +
                                "aephoowishoor1Ien5vahGhahm7eidaiy2AeCaej9iexahyooshu2ic9tea1ool8" +
                                "tu4Y/en/phoneScreenshots/en phoneScreenshots",
                            sha256 = "54758e380aee6297c7947f107db9ea03" +
                                "d2933c9d5c110d02046977cf78d43def",
                            size = 0,
                        ),
                        FileV2(
                            name = "/Haoheiseeshai2que2Che0ooSa6aikeemoo2ap9Aequoh4ju5chooYuPhiev" +
                                "8moodahlonu2oht5Eikahvushapeum5aefo6xig4aghahyaaNuezoo4eexee1Goo" +
                                "5UngohGha6quaeghe8uCh9iex9Oowa9aiyohzoo2ij5miifiegaeth8nie9jae6r" +
                                "aephoowishoor1Ien5vahGhahm7eidaiy2AeCaej9iexahyooshu2ic9tea1ool8" +
                                "tu4Y/en/phoneScreenshots/en phoneScreenshots2",
                            sha256 = "54758e380aee6297c7947f107db9ea03" +
                                "d2933c9d5c110d02046977cf78d43def",
                            size = 0,
                        ),
                    )
                ),
                sevenInch = mapOf(
                    "en" to listOf(
                        FileV2(
                            name = "/Haoheiseeshai2que2Che0ooSa6aikeemoo2ap9Aequoh4ju5chooYuPhiev" +
                                "8moodahlonu2oht5Eikahvushapeum5aefo6xig4aghahyaaNuezoo4eexee1Goo" +
                                "5UngohGha6quaeghe8uCh9iex9Oowa9aiyohzoo2ij5miifiegaeth8nie9jae6r" +
                                "aephoowishoor1Ien5vahGhahm7eidaiy2AeCaej9iexahyooshu2ic9tea1ool8" +
                                "tu4Y/en/sevenInchScreenshots/en sevenInchScreenshots",
                            sha256 = "54758e380aee6297c7947f107db9ea03" +
                                "d2933c9d5c110d02046977cf78d43def",
                            size = 0,
                        ),
                        FileV2(
                            name = "/Haoheiseeshai2que2Che0ooSa6aikeemoo2ap9Aequoh4ju5chooYuPhiev" +
                                "8moodahlonu2oht5Eikahvushapeum5aefo6xig4aghahyaaNuezoo4eexee1Goo" +
                                "5UngohGha6quaeghe8uCh9iex9Oowa9aiyohzoo2ij5miifiegaeth8nie9jae6r" +
                                "aephoowishoor1Ien5vahGhahm7eidaiy2AeCaej9iexahyooshu2ic9tea1ool8" +
                                "tu4Y/en/sevenInchScreenshots/en sevenInchScreenshots2",
                            sha256 = "54758e380aee6297c7947f107db9ea03" +
                                "d2933c9d5c110d02046977cf78d43def",
                            size = 0,
                        ),
                    ),
                ),
                tenInch = mapOf(
                    "en" to listOf(
                        FileV2(
                            name = "/Haoheiseeshai2que2Che0ooSa6aikeemoo2ap9Aequoh4ju5chooYuPhiev" +
                                "8moodahlonu2oht5Eikahvushapeum5aefo6xig4aghahyaaNuezoo4eexee1Goo" +
                                "5UngohGha6quaeghe8uCh9iex9Oowa9aiyohzoo2ij5miifiegaeth8nie9jae6r" +
                                "aephoowishoor1Ien5vahGhahm7eidaiy2AeCaej9iexahyooshu2ic9tea1ool8" +
                                "tu4Y/en/tenInchScreenshots/en tenInchScreenshots",
                            sha256 = "54758e380aee6297c7947f107db9ea03" +
                                "d2933c9d5c110d02046977cf78d43def",
                            size = 0,
                        ),
                        FileV2(
                            name = "/Haoheiseeshai2que2Che0ooSa6aikeemoo2ap9Aequoh4ju5chooYuPhiev" +
                                "8moodahlonu2oht5Eikahvushapeum5aefo6xig4aghahyaaNuezoo4eexee1Goo" +
                                "5UngohGha6quaeghe8uCh9iex9Oowa9aiyohzoo2ij5miifiegaeth8nie9jae6r" +
                                "aephoowishoor1Ien5vahGhahm7eidaiy2AeCaej9iexahyooshu2ic9tea1ool8" +
                                "tu4Y/en/tenInchScreenshots/en tenInchScreenshots2",
                            sha256 = "54758e380aee6297c7947f107db9ea03" +
                                "d2933c9d5c110d02046977cf78d43def",
                            size = 0,
                        ),
                    ),
                ),
                wear = mapOf(
                    "en" to listOf(
                        FileV2(
                            name = "/Haoheiseeshai2que2Che0ooSa6aikeemoo2ap9Aequoh4ju5chooYuPhiev" +
                                "8moodahlonu2oht5Eikahvushapeum5aefo6xig4aghahyaaNuezoo4eexee1Goo" +
                                "5UngohGha6quaeghe8uCh9iex9Oowa9aiyohzoo2ij5miifiegaeth8nie9jae6r" +
                                "aephoowishoor1Ien5vahGhahm7eidaiy2AeCaej9iexahyooshu2ic9tea1ool8" +
                                "tu4Y/en/wearScreenshots/en wearScreenshots",
                            sha256 = "54758e380aee6297c7947f107db9ea03" +
                                "d2933c9d5c110d02046977cf78d43def",
                            size = 0,
                        ),
                        FileV2(
                            name = "/Haoheiseeshai2que2Che0ooSa6aikeemoo2ap9Aequoh4ju5chooYuPhiev" +
                                "8moodahlonu2oht5Eikahvushapeum5aefo6xig4aghahyaaNuezoo4eexee1Goo" +
                                "5UngohGha6quaeghe8uCh9iex9Oowa9aiyohzoo2ij5miifiegaeth8nie9jae6r" +
                                "aephoowishoor1Ien5vahGhahm7eidaiy2AeCaej9iexahyooshu2ic9tea1ool8" +
                                "tu4Y/en/wearScreenshots/en wearScreenshots2",
                            sha256 = "54758e380aee6297c7947f107db9ea03" +
                                "d2933c9d5c110d02046977cf78d43def",
                            size = 0,
                        ),
                    ),
                ),
                tv = mapOf(
                    "en" to listOf(
                        FileV2(
                            name = "/Haoheiseeshai2que2Che0ooSa6aikeemoo2ap9Aequoh4ju5chooYuPhiev" +
                                "8moodahlonu2oht5Eikahvushapeum5aefo6xig4aghahyaaNuezoo4eexee1Goo" +
                                "5UngohGha6quaeghe8uCh9iex9Oowa9aiyohzoo2ij5miifiegaeth8nie9jae6r" +
                                "aephoowishoor1Ien5vahGhahm7eidaiy2AeCaej9iexahyooshu2ic9tea1ool8" +
                                "tu4Y/en/tvScreenshots/en tvScreenshots",
                            sha256 = "54758e380aee6297c7947f107db9ea03" +
                                "d2933c9d5c110d02046977cf78d43def",
                            size = 0),
                        FileV2(
                            name = "/Haoheiseeshai2que2Che0ooSa6aikeemoo2ap9Aequoh4ju5chooYuPhiev" +
                                "8moodahlonu2oht5Eikahvushapeum5aefo6xig4aghahyaaNuezoo4eexee1Goo" +
                                "5UngohGha6quaeghe8uCh9iex9Oowa9aiyohzoo2ij5miifiegaeth8nie9jae6r" +
                                "aephoowishoor1Ien5vahGhahm7eidaiy2AeCaej9iexahyooshu2ic9tea1ool8" +
                                "tu4Y/en/tvScreenshots/en tvScreenshots2",
                            sha256 = "54758e380aee6297c7947f107db9ea03" +
                                "d2933c9d5c110d02046977cf78d43def",
                            size = 0),
                    ),
                ),
            ),
        ),
        versions = mapOf(
            version3_1.file.sha256 to version3_1,
        ),
    )
    val app3Compat = app3.v1compat(true).copy(
        versions = mapOf(
            version3_1.file.sha256 to version3_1.v1compat(),
        ),
    )

    val index = IndexV2(
        repo = repo,
        packages = mapOf(
            TestDataMidV2.packageName1 to TestDataMidV2.app1,
            TestDataMidV2.packageName2 to app2,
            packageName3 to app3,
        ),
    )
    val indexCompat = index.v1compat().copy(
        packages = mapOf(
            TestDataMidV2.packageName1 to TestDataMidV2.app1Compat,
            TestDataMidV2.packageName2 to app2Compat,
            packageName3 to app3Compat,
        ),
    )
}
