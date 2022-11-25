package org.fdroid.test

import org.fdroid.index.v1.AppV1
import org.fdroid.index.v1.IndexV1
import org.fdroid.index.v1.Localized
import org.fdroid.index.v1.PackageV1
import org.fdroid.index.v1.PermissionV1
import org.fdroid.index.v1.RepoV1
import org.fdroid.index.v1.Requests

object TestDataEmptyV1 {
    val repo = RepoV1(
        timestamp = 23,
        version = 23,
        name = "EmptyV1",
        icon = "empty-v1.png",
        address = "https://empty-v1.org",
        description = "This is a repo with empty data.",
    )
    val index = IndexV1(
        repo = repo,
    )
}

object TestDataMinV1 {

    val repo = RepoV1(
        timestamp = 42,
        version = 1,
        name = "MinV1",
        icon = "min-v1.png",
        address = "https://min-v1.org",
        description = "This is a repo with minimal data.",
    )

    const val packageName = "org.fdroid.min1"
    val app = AppV1(
        packageName = packageName,
        categories = emptyList(),
        antiFeatures = emptyList(),
        license = "",
    )
    val apps = listOf(app)

    val version = PackageV1(
        packageName = packageName,
        apkName = "${packageName}_23.apk",
        hash = "824a109b2352138c3699760e1683385d0ed50ce526fc7982f8d65757743374bf",
        hashType = "sha256",
        size = 1337,
        versionName = "0",
    )
    val versions = listOf(version)
    val packages = mapOf(packageName to versions)

    val index = IndexV1(
        repo = repo,
        requests = Requests(emptyList(), emptyList()),
        apps = apps,
        packages = packages,
    )
}

object TestDataMidV1 {

    val repo = RepoV1(
        timestamp = 1337,
        version = 1,
        maxAge = 23,
        name = "MidV1",
        icon = "mid-v1.png",
        address = "https://mid-v1.org",
        description = "This is a repo with medium data.",
        mirrors = listOf("https://mid-v1.com"),
    )

    const val packageName1 = TestDataMinV1.packageName
    const val packageName2 = "org.fdroid.fdroid"
    val categories = listOf("Cat1", "Cat2", "Cat3")
    val app1 = TestDataMinV1.app.copy(
        packageName = packageName1,
        categories = listOf(categories[0]),
        antiFeatures = listOf("AntiFeature"),
        summary = "App1 summary",
        description = "App1 description",
        name = "App1",
        authorName = "App1 author",
        license = "GPLv3",
        webSite = "http://min1.test.org",
        added = 1234567890,
        icon = "icon-min1.png",
        lastUpdated = 1234567891,
        localized = mapOf(
            "de" to Localized(
                description = "App1 beschreibung",
                name = "app 1 name",
                summary = "App1 Zusammenfassung",
            ),
        ),
    )
    val app2 = AppV1(
        categories = listOf("System"),
        antiFeatures = emptyList(),
        changelog = "https://gitlab.com/fdroid/fdroidclient/raw/HEAD/CHANGELOG.md",
        translation = "https://hosted.weblate.org/projects/f-droid/f-droid",
        issueTracker = "https://gitlab.com/fdroid/fdroidclient/issues",
        sourceCode = "https://gitlab.com/fdroid/fdroidclient",
        donate = "https://f-droid.org/donate",
        liberapayID = "27859",
        openCollective = "F-Droid-Euro",
        flattrID = "343053",
        suggestedVersionName = "1.14",
        suggestedVersionCode = "1014050",
        license = "GPL-3.0-or-later",
        webSite = "https://f-droid.org",
        added = 1295222400000,
        icon = "org.fdroid.fdroid.1014050.png",
        packageName = "org.fdroid.fdroid",
        lastUpdated = 1643250075000,
        localized = mapOf(
            "af" to Localized(
                description = "F-Droid is 'n installeerbare katalogus van gratis sagteware",
                name = "-درويد",
                summary = "متجر التطبيقات الذي يحترم الحرية والخصوصية)",
            ),
            "be" to Localized(
                name = "F-Droid",
                summary = "Крама праграм, якая паважае свабоду і прыватнасць",
            ),
            "bg" to Localized(
                summary = "Магазинът за приложения, който уважава независимостта и поверителността",
            ),
            "bn" to Localized(
                name = "এফ-ড্রয়েড",
                summary = "যে অ্যাপ স্টোর স্বাধীনতা ও গোপনীয়তা সম্মান করে"
            ),
            "bo" to Localized(
                description = "ཨེཕ་རོཌ་ནི་ཨེན་ཀྲོཌ་བབ་སྟེགས་ཀྱི་ཆེད་དུ་FOSS",
                summary = "རང་དབང་དང་གསང་དོན་ལ་གུས་བརྩི་ཞུས་མཁན་གྱི་མཉེན་ཆས་ཉར་ཚགས་ཁང་།",
            ),
            "ca" to Localized(
                description = "F-Droid és un catàleg instal·lable d'aplicacions de software lliure",
                name = "F-Droid",
                summary = "La botiga d'aplicacions que respecta la llibertat i la privacitat",
            ),
            "cs" to Localized(
                description = "F-Droid je instalovatelný katalog softwarových libre",
                name = "F-Droid",
                summary = "Zdroj aplikací který respektuje vaši svobodu a soukromí",
            ),
            "cy" to Localized(
                description = "Mae F-Droid yn gatalog y gellir ei osod o apiau meddalwedd rhydd" +
                    "ar gyfer Android.",
                name = "F-Droid",
                summary = "Yr ystorfa apiau sy'n parchu rhyddid a phreifatrwydd",
            ),
            "de" to Localized(
                description = "F-Droid ist ein installierbarer Katalog mit Libre Software" +
                    "Android-Apps.",
                summary = "Der App-Store, der Freiheit und Privatsphäre respektiert",
            ),
            "el" to Localized(
                description = "Το F-Droid είναι ένας κατάλογος εφαρμογών ελεύθερου λογισμικού",
                name = "F-Droid",
                summary = "Το κατάστημα εφαρμογών που σέβεται την ελευθερία και την ιδιωτικότητα",
            ),
            "en-US" to Localized(
                description = "F-Droid is an installable catalogue of libre software",
                name = "F-Droid",
                whatsNew = "* Overhaul Share menu to use built-in options like Nearby",
                phoneScreenshots = listOf(
                    "screenshot-app-details.png",
                    "screenshot-dark-details.png",
                    "screenshot-dark-home.png",
                    "screenshot-dark-knownvuln.png",
                    "screenshot-knownvuln.png",
                    "screenshot-search.png",
                    "screenshot-updates.png",
                ),
                featureGraphic = "featureGraphic_PTun9TO4cMFOeiqbvQSrkdcxNUcOFQCymMIaj9UJOAY=.jpg",
                summary = "The app store that respects freedom and privacy",
            ),
            "eo" to Localized(
                description = "F-Droid estas instalebla katalogo de liberaj aplikaĵoj por Android.",
                name = "F-Droid",
                whatsNew = "• rekonstruita menuo “Kunhavigi” por uzi enkonstruitajn eblojn, ekz.",
                summary = "Aplikaĵa vendejo respektanta liberecon kaj privatecon",
            ),
        ),
    )
    val apps = listOf(app1, app2)

    val version1_1 = TestDataMinV1.version.copy(
        added = 2342,
        apkName = "${packageName1}_23_2.apk",
        size = 1338,
        srcName = "${packageName1}_23_2.zip",
        usesPermission = listOf(PermissionV1("perm")),
        usesPermission23 = emptyList(),
        versionCode = 1,
        versionName = "1",
        nativeCode = listOf("x86"),
        features = listOf("feature"),
        antiFeatures = listOf("anti-feature"),
    )
    val version1_2 = PackageV1(
        packageName = packageName1,
        apkName = "${packageName1}_42.apk",
        hash = "824a109b2352138c3699760e1683385d0ed50ce526fc7982f8d65757743374bf",
        hashType = "sha256",
        minSdkVersion = 21,
        maxSdkVersion = 4568,
        targetSdkVersion = 32,
        sig = "old",
        signer = "824a109b2352138c3699760e1683385d0ed50ce526fc7982f8d65757743374bf",
        size = 1337,
        srcName = "${packageName1}_42.zip",
        versionCode = 24,
        versionName = "24",
    )

    val version2_1 = PackageV1(
        added = 1643250075000,
        apkName = "org.fdroid.fdroid_1014050.apk",
        hash = "8c89ce2f42f4a89af8ca6e1ea220f9dfdee220724d8a9cc067d510ac6f3e0d06",
        hashType = "sha256",
        minSdkVersion = 22,
        targetSdkVersion = 25,
        packageName = "org.fdroid.fdroid",
        sig = "9063aaadfff9cfd811a9c72fb5012f28",
        signer = "43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab",
        size = 8165518,
        srcName = "org.fdroid.fdroid_1014050_src.tar.gz",
        usesPermission = listOf(
            PermissionV1(name = "android.permission.INTERNET"),
            PermissionV1(name = "android.permission.ACCESS_NETWORK_STATE"),
            PermissionV1(name = "android.permission.ACCESS_WIFI_STATE"),
            PermissionV1(name = "android.permission.CHANGE_WIFI_MULTICAST_STATE"),
            PermissionV1(name = "android.permission.CHANGE_NETWORK_STATE"),
            PermissionV1(name = "android.permission.CHANGE_WIFI_STATE"),
            PermissionV1(name = "android.permission.BLUETOOTH"),
            PermissionV1(name = "android.permission.BLUETOOTH_ADMIN"),
            PermissionV1(name = "android.permission.RECEIVE_BOOT_COMPLETED"),
            PermissionV1(name = "android.permission.READ_EXTERNAL_STORAGE"),
            PermissionV1(name = "android.permission.WRITE_EXTERNAL_STORAGE"),
            PermissionV1(name = "android.permission.WRITE_SETTINGS"),
            PermissionV1(name = "android.permission.NFC"),
            PermissionV1(name = "android.permission.USB_PERMISSION", maxSdk = 22),
            PermissionV1(name = "android.permission.WAKE_LOCK"),
            PermissionV1(name = "android.permission.FOREGROUND_SERVICE")
        ),
        usesPermission23 = listOf(PermissionV1(name = "android.permission.ACCESS_COARSE_LOCATION")),
        versionCode = 1014050,
        versionName = "1.14",
    )
    val version2_2 = PackageV1(
        added = 1642785071000,
        apkName = "org.fdroid.fdroid_1014005.apk",
        hash = "b4282febf5558d43c7c51a00478961f6df1b6d59e0a6674974cdacb792683e5d",
        hashType = "sha256",
        minSdkVersion = 22,
        targetSdkVersion = 25,
        packageName = "org.fdroid.fdroid",
        sig = "9063aaadfff9cfd811a9c72fb5012f28",
        signer = "43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab",
        size = 8382606,
        srcName = "org.fdroid.fdroid_1014005_src.tar.gz",
        usesPermission = listOf(
            PermissionV1(name = "android.permission.INTERNET"),
            PermissionV1(name = "android.permission.ACCESS_NETWORK_STATE"),
            PermissionV1(name = "android.permission.ACCESS_WIFI_STATE"),
            PermissionV1(name = "android.permission.CHANGE_WIFI_MULTICAST_STATE"),
            PermissionV1(name = "android.permission.CHANGE_NETWORK_STATE"),
            PermissionV1(name = "android.permission.CHANGE_WIFI_STATE"),
            PermissionV1(name = "android.permission.BLUETOOTH"),
            PermissionV1(name = "android.permission.BLUETOOTH_ADMIN"),
            PermissionV1(name = "android.permission.RECEIVE_BOOT_COMPLETED"),
            PermissionV1(name = "android.permission.READ_EXTERNAL_STORAGE"),
            PermissionV1(name = "android.permission.WRITE_EXTERNAL_STORAGE"),
            PermissionV1(name = "android.permission.WRITE_SETTINGS"),
            PermissionV1(name = "android.permission.NFC"),
            PermissionV1(name = "android.permission.USB_PERMISSION", maxSdk = 22),
            PermissionV1(name = "android.permission.WAKE_LOCK"),
            PermissionV1(name = "android.permission.FOREGROUND_SERVICE")
        ),
        usesPermission23 = listOf(PermissionV1(name = "android.permission.ACCESS_COARSE_LOCATION")),
        versionCode = 1014005,
        versionName = "1.14-alpha5",
        nativeCode = listOf("fakeNativeCode"),
        features = listOf("fake feature"),
        antiFeatures = listOf("FakeAntiFeature")
    )
    val version2_3 = PackageV1(
        added = 1635169849000,
        apkName = "org.fdroid.fdroid_1014003.apk",
        hash = "c062a9642fde08aacabbfa4cab1ab5773c83f4e6b81551ffd92027d2b20f37d3",
        hashType = "sha256",
        minSdkVersion = 22,
        targetSdkVersion = 25,
        packageName = "org.fdroid.fdroid",
        sig = "9063aaadfff9cfd811a9c72fb5012f28",
        signer = "43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab",
        size = 8276110,
        srcName = "org.fdroid.fdroid_1014003_src.tar.gz",
        usesPermission = listOf(
            PermissionV1(name = "android.permission.INTERNET"),
            PermissionV1(name = "android.permission.ACCESS_NETWORK_STATE"),
            PermissionV1(name = "android.permission.ACCESS_WIFI_STATE"),
            PermissionV1(name = "android.permission.CHANGE_WIFI_MULTICAST_STATE"),
            PermissionV1(name = "android.permission.CHANGE_NETWORK_STATE"),
            PermissionV1(name = "android.permission.CHANGE_WIFI_STATE"),
            PermissionV1(name = "android.permission.BLUETOOTH"),
            PermissionV1(name = "android.permission.BLUETOOTH_ADMIN"),
            PermissionV1(name = "android.permission.RECEIVE_BOOT_COMPLETED"),
            PermissionV1(name = "android.permission.READ_EXTERNAL_STORAGE"),
            PermissionV1(name = "android.permission.WRITE_EXTERNAL_STORAGE"),
            PermissionV1(name = "android.permission.WRITE_SETTINGS"),
            PermissionV1(name = "android.permission.NFC"),
            PermissionV1(name = "android.permission.USB_PERMISSION", maxSdk = 22),
            PermissionV1(name = "android.permission.WAKE_LOCK"),
            PermissionV1(name = "android.permission.FOREGROUND_SERVICE"),
        ),
        usesPermission23 = listOf(PermissionV1(name = "android.permission.ACCESS_COARSE_LOCATION")),
        versionCode = 1014003,
        versionName = "1.14-alpha3",
    )
    val version2_4 = PackageV1(
        added = 1632281731000,
        apkName = "org.fdroid.fdroid_1014002.apk",
        hash = "3243c24ee95be0fce0830d72e7d2605e3e24f6ccf4ee72a7c8e720fccd7621a1",
        hashType = "sha256",
        minSdkVersion = 22,
        targetSdkVersion = 25,
        packageName = "org.fdroid.fdroid",
        sig = "9063aaadfff9cfd811a9c72fb5012f28",
        signer = "43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab",
        size = 8284386,
        srcName = "org.fdroid.fdroid_1014002_src.tar.gz",
        usesPermission = listOf(
            PermissionV1(name = "android.permission.INTERNET"),
            PermissionV1(name = "android.permission.ACCESS_NETWORK_STATE"),
            PermissionV1(name = "android.permission.ACCESS_WIFI_STATE"),
            PermissionV1(name = "android.permission.CHANGE_WIFI_MULTICAST_STATE"),
            PermissionV1(name = "android.permission.CHANGE_NETWORK_STATE"),
            PermissionV1(name = "android.permission.CHANGE_WIFI_STATE"),
            PermissionV1(name = "android.permission.BLUETOOTH"),
            PermissionV1(name = "android.permission.BLUETOOTH_ADMIN"),
            PermissionV1(name = "android.permission.RECEIVE_BOOT_COMPLETED"),
            PermissionV1(name = "android.permission.READ_EXTERNAL_STORAGE"),
            PermissionV1(name = "android.permission.WRITE_EXTERNAL_STORAGE"),
            PermissionV1(name = "android.permission.WRITE_SETTINGS"),
            PermissionV1(name = "android.permission.NFC"),
            PermissionV1(name = "android.permission.USB_PERMISSION", maxSdk = 22),
            PermissionV1(name = "android.permission.WAKE_LOCK"),
            PermissionV1(name = "android.permission.FOREGROUND_SERVICE"),
        ),
        usesPermission23 = listOf(PermissionV1(name = "android.permission.ACCESS_COARSE_LOCATION")),
        versionCode = 1014002,
        versionName = "1.14-alpha2",
    )
    val version2_5 = PackageV1(
        added = 1632281729000,
        apkName = "org.fdroid.fdroid_1014001.apk",
        hash = "7ebfd5eb76f9ec95ba955e549260fe930dc38fb99ed3532f92c93b879aca5610",
        hashType = "sha256",
        minSdkVersion = 22,
        targetSdkVersion = 25,
        packageName = "org.fdroid.fdroid",
        sig = "9063aaadfff9cfd811a9c72fb5012f28",
        signer = "43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab",
        size = 8272166,
        srcName = "org.fdroid.fdroid_1014001_src.tar.gz",
        usesPermission = listOf(
            PermissionV1(name = "android.permission.INTERNET"),
            PermissionV1(name = "android.permission.ACCESS_NETWORK_STATE"),
            PermissionV1(name = "android.permission.ACCESS_WIFI_STATE"),
            PermissionV1(name = "android.permission.CHANGE_WIFI_MULTICAST_STATE"),
            PermissionV1(name = "android.permission.CHANGE_NETWORK_STATE"),
            PermissionV1(name = "android.permission.CHANGE_WIFI_STATE"),
            PermissionV1(name = "android.permission.BLUETOOTH"),
            PermissionV1(name = "android.permission.BLUETOOTH_ADMIN"),
            PermissionV1(name = "android.permission.RECEIVE_BOOT_COMPLETED"),
            PermissionV1(name = "android.permission.READ_EXTERNAL_STORAGE"),
            PermissionV1(name = "android.permission.WRITE_EXTERNAL_STORAGE"),
            PermissionV1(name = "android.permission.WRITE_SETTINGS"),
            PermissionV1(name = "android.permission.NFC"),
            PermissionV1(name = "android.permission.USB_PERMISSION", maxSdk = 22),
            PermissionV1(name = "android.permission.WAKE_LOCK"),
            PermissionV1(name = "android.permission.FOREGROUND_SERVICE"),
        ),
        usesPermission23 = listOf(PermissionV1(name = "android.permission.ACCESS_COARSE_LOCATION")),
        versionCode = 1014001,
        versionName = "1.14-alpha1",
    )
    val versions1 = listOf(version1_1, version1_2)
    val versions2 = listOf(version2_1, version2_2, version2_3, version2_4, version2_5)
    val packages = mapOf(
        packageName1 to versions1,
        packageName2 to versions2,
    )

    val index = IndexV1(
        repo = repo,
        requests = Requests(listOf("installThis"), listOf("uninstallThis")),
        apps = apps,
        packages = packages,
    )

}

object TestDataMaxV1 {

    val repo = RepoV1(
        timestamp = Long.MAX_VALUE,
        version = Int.MAX_VALUE,
        maxAge = Int.MAX_VALUE,
        name = "MaxV1",
        icon = "max-v1.png",
        address = "https://max-v1.org",
        description = "This is a repo with maximum data.",
        mirrors = listOf("https://max-v1.com", "https://max-v1.org"),
    )

    const val packageName1 = TestDataMidV1.packageName1
    const val packageName2 = TestDataMidV1.packageName2
    const val packageName3 = "Haoheiseeshai2que2Che0ooSa6aikeemoo2ap9Aequoh4ju5chooYuPhiev8moo" +
        "dahlonu2oht5Eikahvushapeum5aefo6xig4aghahyaaNuezoo4eexee1Goo5Ung" +
        "ohGha6quaeghe8uCh9iex9Oowa9aiyohzoo2ij5miifiegaeth8nie9jae6raeph" +
        "oowishoor1Ien5vahGhahm7eidaiy2AeCaej9iexahyooshu2ic9tea1ool8tu4Y"
    val categories = listOf("Cat1", "Cat2", "Cat3")

    val app2 = TestDataMidV1.app2.copy(
        categories = listOf("NoMoreSystem", "OneMore"),
        antiFeatures = listOf("AddOne"),
        summary = "new summary",
        description = "new description",
        webSite = "https://fdroid.org",
        binaries = "https://fdroid.org/binaries",
        name = "F-DroidX",
        suggestedVersionCode = "1014003",
        localized = mapOf(
            "ch" to Localized(
                description = "new desc",
                name = "new name",
                summary = "new summary",
                tenInchScreenshots = listOf("new screenshots"),
                whatsNew = "This is new!"
            ),
            "de" to Localized(
                summary = "Der App-Store, der Freiheit und Privatsphäre respektiert",
                whatsNew = "das ist neu",
            ),
            "en-US" to Localized(
                description = "F-Droid is an installable catalogue of libre software",
                summary = "new summary in en-US",
                phoneScreenshots = listOf(
                    "screenshot-app-details.png",
                    "screenshot-dark-details.png",
                    "screenshot-dark-home.png",
                    "screenshot-search.png",
                    "screenshot-updates.png",
                ),
                featureGraphic = "featureGraphic_PTun9TO4cMFOeiqbvQSrkdcxNUcOFQCymMIaj9UJOAY=.jpg",
                icon = "new icon",
                whatsNew = "this is new",
            ),
        ),
    )
    val app3 = AppV1(
        packageName = packageName3,
        categories = categories,
        antiFeatures = listOf("AntiFeature", "NonFreeNet", "NotNice", "VeryBad", "Dont,Show,This"),
        summary = "App3 summary",
        description = "App3 description",
        changelog = "changeLog3",
        translation = "translation3",
        issueTracker = "tracker3",
        sourceCode = "source code3",
        binaries = "binaries3",
        name = "App3",
        authorName = "App3 author",
        authorEmail = "email",
        authorWebSite = "website",
        authorPhone = "phone",
        donate = "donate",
        liberapayID = "liberapayID",
        liberapay = "liberapay",
        openCollective = "openCollective",
        bitcoin = "bitcoin",
        litecoin = "litecoin",
        flattrID = "flattrID",
        suggestedVersionName = "1.0",
        suggestedVersionCode = Long.MIN_VALUE.toString(),
        license = "GPLv3",
        webSite = "http://min1.test.org",
        added = 1234567890,
        icon = "icon-max1.png",
        lastUpdated = Long.MAX_VALUE,
        localized = mapOf(
            LOCALE to Localized(
                whatsNew = "this is new",
            ),
            "de" to Localized(
                whatsNew = "das ist neu",
            ),
            "en" to Localized(
                description = "en ",
                name = "en ",
                icon = "en ",
                video = "en ",
                phoneScreenshots = listOf("en phoneScreenshots", "en phoneScreenshots2"),
                sevenInchScreenshots = listOf("en sevenInchScreenshots",
                    "en sevenInchScreenshots2"),
                tenInchScreenshots = listOf("en tenInchScreenshots", "en tenInchScreenshots2"),
                wearScreenshots = listOf("en wearScreenshots", "en wearScreenshots2"),
                tvScreenshots = listOf("en tvScreenshots", "en tvScreenshots2"),
                featureGraphic = "en ",
                promoGraphic = "en ",
                tvBanner = "en ",
                summary = "en ",
            )
        ),
        allowedAPKSigningKeys = listOf("key1, key2"),
    )
    val apps = listOf(TestDataMidV1.app1, app2, app3)

    val version2_2 = TestDataMidV1.version2_2.copy(
        usesPermission = emptyList(),
        usesPermission23 = emptyList(),
        nativeCode = emptyList(),
        features = emptyList(),
        antiFeatures = emptyList(),
    )
    val version2_3 = TestDataMidV1.version2_3.copy(
        minSdkVersion = 22,
        targetSdkVersion = 25,
        packageName = "org.fdroid.fdroid",
        sig = "9063aaadfff9cfd811a9c72fb5012f28",
        signer = "43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab",
        size = 8276110,
        srcName = "org.fdroid.fdroid_1014003_src.tar.gz",
        usesPermission = listOf(
            PermissionV1(name = "android.permission.ACCESS_MEDIA"),
            PermissionV1(name = "android.permission.CHANGE_WIFI_STATE", maxSdk = 32),
            PermissionV1(name = "android.permission.READ_EXTERNAL_STORAGE"),
            PermissionV1(name = "android.permission.WRITE_EXTERNAL_STORAGE"),
            PermissionV1(name = "android.permission.NFC"),
            PermissionV1(name = "android.permission.USB_PERMISSION", maxSdk = 22),
            PermissionV1(name = "android.permission.WAKE_LOCK"),
            PermissionV1(name = "android.permission.READ_MY_ASS"),
            PermissionV1(name = "android.permission.FOREGROUND_SERVICE"),
        ),
        usesPermission23 = listOf(
            PermissionV1(name = "android.permission.ACCESS_FINE_LOCATION"),
            PermissionV1(name = "android.permission.ACCESS_COARSE_LOCATION", maxSdk = 3),
        ),
        versionCode = 1014003,
        versionName = "1.14-alpha3",
    )
    val version3_1 = PackageV1(
        added = 1643250075000,
        apkName = "org.fdroid.fdroid_1014050.apk",
        hash = "8c89ce2f42f4a89af8ca6e1ea220f9dfdee220724d8a9cc067d510ac6f3e0d06",
        hashType = "sha256",
        minSdkVersion = 22,
        maxSdkVersion = Int.MAX_VALUE,
        targetSdkVersion = 25,
        packageName = "org.fdroid.fdroid",
        sig = "9063aaadfff9cfd811a9c72fb5012f28",
        signer = "43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab",
        size = 8165518,
        srcName = "org.fdroid.fdroid_1014050_src.tar.gz",
        usesPermission = listOf(
            PermissionV1(name = "android.permission.INTERNET"),
            PermissionV1(name = "android.permission.ACCESS_NETWORK_STATE"),
            PermissionV1(name = "android.permission.ACCESS_WIFI_STATE"),
            PermissionV1(name = "android.permission.CHANGE_WIFI_MULTICAST_STATE"),
            PermissionV1(name = "android.permission.CHANGE_NETWORK_STATE"),
            PermissionV1(name = "android.permission.CHANGE_WIFI_STATE"),
            PermissionV1(name = "android.permission.BLUETOOTH"),
            PermissionV1(name = "android.permission.BLUETOOTH_ADMIN"),
            PermissionV1(name = "android.permission.RECEIVE_BOOT_COMPLETED"),
            PermissionV1(name = "android.permission.READ_EXTERNAL_STORAGE"),
            PermissionV1(name = "android.permission.WRITE_EXTERNAL_STORAGE"),
            PermissionV1(name = "android.permission.WRITE_SETTINGS"),
            PermissionV1(name = "android.permission.NFC"),
            PermissionV1(name = "android.permission.USB_PERMISSION", maxSdk = 22),
            PermissionV1(name = "android.permission.WAKE_LOCK"),
            PermissionV1(name = "android.permission.FOREGROUND_SERVICE")
        ),
        usesPermission23 = listOf(
            PermissionV1(name = "android.permission.ACCESS_COARSE_LOCATION"),
            PermissionV1(name = "android.permission.USB_PERMISSION", maxSdk = Int.MAX_VALUE),
        ),
        versionCode = 1014050,
        versionName = "1.14",
        nativeCode = listOf("x86", "x86_64"),
        features = listOf("feature", "feature2"),
        antiFeatures = listOf("anti-feature", "anti-feature2"),
    )
    val versions1 = TestDataMidV1.versions1
    val versions2 = listOf(
        version2_2, version2_3, TestDataMidV1.version2_4, TestDataMidV1.version2_5
    )
    val versions3 = listOf(version3_1)
    val packages = mapOf(
        packageName1 to versions1,
        packageName2 to versions2,
        packageName3 to versions3,
    )

    val index = IndexV1(
        repo = repo,
        requests = Requests(listOf("installThis", "installThat"), listOf("uninstallThis")),
        apps = apps,
        packages = packages,
    )
}
