package org.fdroid.basic.ui.main.details

import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.core.os.LocaleListCompat
import org.fdroid.basic.ui.main.apps.MinimalApp
import org.fdroid.database.AppMetadata
import org.fdroid.database.AppVersion
import org.fdroid.index.v2.FileV2
import java.util.concurrent.TimeUnit
import kotlin.random.Random

data class AppDetailsItem(
    val app: IApp,
    val whatsNew: String? = null,
    val versions: List<AppVersion>? = null,
    val installedVersion: AppVersion? = null,
    val suggestedVersion: AppVersion? = null,
    val antiFeatures: List<AntiFeature>? = null,
) {
    private val localeList = LocaleListCompat.getDefault()
    val description: String? get() = app.getDescription(localeList)
    val icon: FileV2? get() = app.getIcon(localeList)
    val video: String? get() = app.getVideo(localeList)
    val featureGraphic: FileV2? get() = app.getFeatureGraphic(localeList)
    val phoneScreenshots: List<FileV2> get() = app.getPhoneScreenshots(localeList)
    val installedVersionCode: Long? get() = installedVersion?.manifest?.versionCode
    val installedVersionName: String? get() = installedVersion?.manifest?.versionName
}

data class AntiFeature(
    val id: String,
    val icon: FileV2?,
    val name: String,
    val reason: String? = null,
)

interface IApp : MinimalApp {
    val repoId: Long
    override val packageName: String
    override val name: String?
    val summary: String?
    val metadata: AppMetadata

    fun getDescription(localeList: LocaleListCompat): String?
    fun getVideo(localeList: LocaleListCompat): String?
    fun getIcon(localeList: LocaleListCompat): FileV2?
    fun getFeatureGraphic(localeList: LocaleListCompat): FileV2?
    fun getPromoGraphic(localeList: LocaleListCompat): FileV2?
    fun getTvBanner(localeList: LocaleListCompat): FileV2?
    fun getPhoneScreenshots(localeList: LocaleListCompat): List<FileV2>
    fun getSevenInchScreenshots(localeList: LocaleListCompat): List<FileV2>
    fun getTenInchScreenshots(localeList: LocaleListCompat): List<FileV2>
    fun getTvScreenshots(localeList: LocaleListCompat): List<FileV2>
    fun getWearScreenshots(localeList: LocaleListCompat): List<FileV2>
}

private val long_min = TimeUnit.DAYS.toMillis(1)
private val long_max = TimeUnit.DAYS.toMillis(365 * 3)

data class TestApp(
    override val repoId: Long = -1,
    override val packageName: String,
    override val name: String?,
    override val summary: String?,
    val description: String? = null,
    val icon: FileV2? = null,
    val featureGraphic: FileV2? = null,
    val phoneScreenshots: List<FileV2> = emptyList(),
    override val metadata: AppMetadata = AppMetadata(
        repoId = repoId,
        packageName = packageName,
        added = System.currentTimeMillis() - Random.nextLong(),
        lastUpdated = System.currentTimeMillis() - Random.nextLong(long_min, long_max),
        isCompatible = true,
    )
) : IApp {
    override fun getIcon(localeList: LocaleListCompat): FileV2? = icon
    override fun getDescription(localeList: LocaleListCompat): String? = description
    override fun getFeatureGraphic(localeList: LocaleListCompat): FileV2? = featureGraphic
    override fun getPhoneScreenshots(localeList: LocaleListCompat): List<FileV2> = phoneScreenshots

    override fun getVideo(localeList: LocaleListCompat): String? = null
    override fun getPromoGraphic(localeList: LocaleListCompat): FileV2? = null
    override fun getTvBanner(localeList: LocaleListCompat): FileV2? = null

    override fun getSevenInchScreenshots(localeList: LocaleListCompat): List<FileV2> = emptyList()
    override fun getTenInchScreenshots(localeList: LocaleListCompat): List<FileV2> = emptyList()
    override fun getTvScreenshots(localeList: LocaleListCompat): List<FileV2> = emptyList()
    override fun getWearScreenshots(localeList: LocaleListCompat): List<FileV2> = emptyList()
}

val newPipeApp = TestApp(
    packageName = "org.schabi.newpipe",
    name = "NewPipe",
    summary = "Lightweight YouTube frontend",
    description = "NewPipe does not use any Google framework libraries, or the YouTube API. " +
        "It only parses the website in order to gain the information it needs. " +
        "Therefore this app can be used on devices without Google Services installed. " +
        "Also, you don't need a YouTube account to use NewPipe, and it's FLOSS.\n\n" +
        LoremIpsum(256).values.joinToString(" "),
    metadata = AppMetadata(
        repoId = 1,
        packageName = "org.schabi.newpipe",
        added = 1441756800000,
        lastUpdated = 1747214796000,
        webSite = "https://newpipe.net",
        changelog = "https://github.com/TeamNewPipe/NewPipe/releases",
        license = "GPL-3.0-or-later",
        sourceCode = "https://github.com/TeamNewPipe/NewPipe",
        issueTracker = "https://github.com/TeamNewPipe/NewPipe/issues",
        translation = "https://hosted.weblate.org/projects/newpipe/",
        preferredSigner = "cb84069bd68116bafae5ee4ee5b08a567aa6d898404e7cb12f9e756df5cf5cab",
        video = null,
        authorName = "Team NewPipe",
        authorEmail = "team@newpipe.net",
        authorWebSite = null,
        authorPhone = null,
        donate = listOf("https://newpipe.net/donate"),
        liberapayID = null,
        liberapay = "TeamNewPipe",
        openCollective = null,
        bitcoin = null,
        litecoin = null,
        flattrID = null,
        categories = listOf("Internet", "Multimedia"),
        isCompatible = true,
    ),
    icon = FileV2(name = "https://f-droid.org/repo/org.schabi.newpipe/en-US/icon_OHy4y1W-fJCNhHHOBCM9V_cxZNJJgbcNkB-x7UDTY9Q=.png"),
    featureGraphic = FileV2("https://f-droid.org/repo/org.schabi.newpipe/en-US/featureGraphic_aeQS-fmo5GZhNiioPTtd8Sjgb53Mvvg6LOV2ww-irYA=.png"),
    phoneScreenshots = listOf(
        FileV2("https://f-droid.org/repo/org.schabi.newpipe/en-US/phoneScreenshots/00.png"),
        FileV2("https://f-droid.org/repo/org.schabi.newpipe/en-US/phoneScreenshots/01.png"),
        FileV2("https://f-droid.org/repo/org.schabi.newpipe/en-US/phoneScreenshots/02.png"),
        FileV2("https://f-droid.org/repo/org.schabi.newpipe/en-US/phoneScreenshots/03.png"),
        FileV2("https://f-droid.org/repo/org.schabi.newpipe/en-US/phoneScreenshots/04.png"),
        FileV2("https://f-droid.org/repo/org.schabi.newpipe/en-US/phoneScreenshots/05.png"),
        FileV2("https://f-droid.org/repo/org.schabi.newpipe/en-US/phoneScreenshots/06.png"),
        FileV2("https://f-droid.org/repo/org.schabi.newpipe/en-US/phoneScreenshots/07.png"),
        FileV2("https://f-droid.org/repo/org.schabi.newpipe/en-US/phoneScreenshots/08.png"),
        FileV2("https://f-droid.org/repo/org.schabi.newpipe/en-US/phoneScreenshots/shot_01.png"),
        FileV2("https://f-droid.org/repo/org.schabi.newpipe/en-US/phoneScreenshots/shot_02.png"),
        FileV2("https://f-droid.org/repo/org.schabi.newpipe/en-US/phoneScreenshots/shot_03.png"),
        FileV2("https://f-droid.org/repo/org.schabi.newpipe/en-US/phoneScreenshots/shot_04.png"),
        FileV2("https://f-droid.org/repo/org.schabi.newpipe/en-US/phoneScreenshots/shot_05.png"),
        FileV2("https://f-droid.org/repo/org.schabi.newpipe/en-US/phoneScreenshots/shot_06.png"),
        FileV2("https://f-droid.org/repo/org.schabi.newpipe/en-US/phoneScreenshots/shot_07.png"),
        FileV2("https://f-droid.org/repo/org.schabi.newpipe/en-US/phoneScreenshots/shot_08.png"),
        FileV2("https://f-droid.org/repo/org.schabi.newpipe/en-US/phoneScreenshots/shot_09.png"),
        FileV2("https://f-droid.org/repo/org.schabi.newpipe/en-US/phoneScreenshots/shot_10.png"),
    ),
)

val newPipe = AppDetailsItem(
    app = newPipeApp,
    antiFeatures = listOf(
        AntiFeature(
            id = "NonFreeNet",
            icon = FileV2("https://f-droid.org/repo/icons/ic_antifeature_nonfreenet.png"),
            name = "This app promotes or depends entirely on a non-free network service",
            reason = "Depends on Youtube for videos.",
        )
    ),
    whatsNew = "This release fixes YouTube only providing a 360p stream.\n\n" +
        "Note that the solution employed in this version is likely temporary, " +
        "and in the long run the SABR video protocol needs to be implemented, " +
        "but TeamNewPipe members are currently busy so any help would be greatly appreciated! " +
        "https://github.com/TeamNewPipe/NewPipe/issues/12248",
)
