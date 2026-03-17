package org.fdroid.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.fdroid.index.v2.MetadataV2
import org.fdroid.test.TestRepoUtils.getRandomRepo
import org.junit.Test
import org.junit.runner.RunWith

/** The other side of SearchManagerTest, this time focusing on the DB, not query manipulation. */
@RunWith(AndroidJUnit4::class)
internal class AppSearchItemsTest : DbTest() {

  @Test
  fun findsByName() = runBlocking {
    populateDbWithExtractedApps()
    assertSearchTopResult(
      query = "duckduckgo* browser*",
      packageName = "com.duckduckgo.mobile.android",
    )
    assertSearchTopResult(query = "F-Droid*", packageName = "org.fdroid.fdroid")
  }

  @Test
  fun findsBySummary() = runBlocking {
    populateDbWithExtractedApps()
    assertSearchTopResult(query = "alternative* frontend*", packageName = "com.github.libretube")
  }

  @Test
  fun findsByDescription() = runBlocking {
    populateDbWithExtractedApps()
    assertSearchTopResult(
      query = "privacy* essentials*",
      packageName = "com.duckduckgo.mobile.android",
    )
  }

  @Test
  fun findsByAuthor() = runBlocking {
    populateDbWithExtractedApps()
    assertSearchTopResult(query = "rahul* patel*", packageName = "com.aurora.store")
    assertSearchTopResult(query = "bitfire*", packageName = "at.bitfire.davdroid")
    assertSearchTopResult(query = "艾*", packageName = "com.aistra.hail")
  }

  @Test
  fun findsByPackageName() = runBlocking {
    populateDbWithExtractedApps()
    assertSearchTopResult(query = "org* fdroid*", packageName = "org.fdroid.fdroid")
  }

  @Test
  fun findsCamelCase() = runBlocking {
    populateDbWithExtractedApps()
    assertSearchTopResult(
      // this is the actual query used by the search manager when searching for "game pad"
      query = "game* pad* OR gamepad* OR \"game* pad*\"",
      packageName = "io.github.kitswas.virtualgamepadmobile",
    )
    assertSearchTopResult(
      query = "cal* dav* OR caldav* OR \"cal* dav*\"",
      packageName = "at.bitfire.davdroid",
    )
  }

  @Test
  fun findsGermanText() = runBlocking {
    populateDbWithExtractedApps()
    assertSearchTopResult(
      query = "privatsphäre* vereinfacht*",
      packageName = "com.duckduckgo.mobile.android",
    )
    assertSearchTopResult(query = "installierbar*", packageName = "org.fdroid.fdroid")
    assertSearchTopResult(query = "Synchronisierungs-App*", packageName = "at.bitfire.davdroid")
  }

  @Test
  fun findsPortugueseText() = runBlocking {
    populateDbWithExtractedApps()
    assertSearchTopResult(query = "loja* privacidade*", packageName = "org.fdroid.fdroid")
    assertSearchTopResult(query = "sincronização*", packageName = "at.bitfire.davdroid")
    assertSearchTopResult(query = "catálogo* instalável*", packageName = "org.fdroid.fdroid")
    assertSearchTopResult(query = "catalogo* instalavel*", packageName = "org.fdroid.fdroid")
  }

  @Test
  fun findsChineseText() = runBlocking {
    populateDbWithExtractedApps()
    assertSearchTopResult(query = "地* 图*", packageName = "app.organicmaps")
    assertSearchResultsContain(query = "隐* 私*", packageName = "com.duckduckgo.mobile.android")
    assertSearchResultsContain(query = "隐* 私*", packageName = "org.fdroid.fdroid")
    assertSearchTopResult(query = "阅* 读*", packageName = "com.capyreader.app")
    assertSearchResultsContain(query = "同* 步*", packageName = "com.nextcloud.android.beta")
    assertSearchResultsContain(query = "同* 步*", packageName = "at.bitfire.davdroid")
  }

  @Test
  fun findsJapaneseText() = runBlocking {
    populateDbWithExtractedApps()
    assertSearchTopResult(query = "同* 期*", packageName = "at.bitfire.davdroid")
  }

  @Test
  fun findsKoreanText() = runBlocking {
    populateDbWithExtractedApps()
    assertSearchTopResult(query = "동기* 클라이*", packageName = "at.bitfire.davdroid")
  }

  private suspend fun assertSearchTopResult(query: String, packageName: String) {
    val items = appDao.getAppSearchItems(query)
    assertEquals(packageName, items.firstOrNull()?.packageName)
  }

  private suspend fun assertSearchResultsContain(query: String, packageName: String) {
    val items = appDao.getAppSearchItems(query)
    assertTrue(
      items.any { it.packageName == packageName },
      "Query '$query' did not find $packageName, but ${items.map { it.packageName }}",
    )
  }

  private fun populateDbWithExtractedApps() {
    val repoId = repoDao.insertOrReplace(getRandomRepo())
    apps.forEach { app -> appDao.insert(repoId, app.packageName, app.metadata, locales) }
  }

  private data class TestApp(val packageName: String, val metadata: MetadataV2)

  private val apps =
    listOf(
      TestApp(
        packageName = "org.fdroid.fdroid",
        metadata =
          MetadataV2(
            name = mapOf("en-US" to "F-Droid"),
            summary =
              mapOf(
                "de" to "Der App-Store, der Freiheit und Privatsphäre respektiert",
                "en-US" to "The app store that respects freedom and privacy",
                "ja" to "自由とプライバシーを尊重するアプリストア",
                "pt-BR" to "A loja de apps que respeita a liberdade e a privacidade",
                "zh-CN" to "尊重自由与隐私的应用商店",
                "zh-TW" to "尊重自由與隱私的應用程式商店",
                "ko" to "자유와 개인정보 보호를 존중하는 앱 스토어",
              ),
            description =
              mapOf(
                "en-US" to
                  "F-Droid is an installable catalogue of libre software apps for Android.",
                "de" to
                  "F-Droid ist ein installierbarer Katalog mit Libre Software Apps f\u00fcr Android.",
                "pt-BR" to
                  "O F-Droid \u00e9 um cat\u00e1logo instal\u00e1vel de apps de software livre para Android.",
              ),
            authorName = "F-Droid",
            added = 1,
            lastUpdated = 1,
          ),
      ),
      TestApp(
        packageName = "at.bitfire.davdroid",
        metadata =
          MetadataV2(
            name = mapOf("en-US" to "DAVx5"),
            summary =
              mapOf(
                "de" to "CalDAV/CardDAV-Synchronisierung und -Client",
                "en-US" to "CalDAV/CardDAV Synchronization and Client",
                "ja" to "CalDAV/CardDAV 同期とクライアント",
                "ko" to "CalDAV/CardDAV 동기화 및 클라이언트",
                "pt-BR" to "Sincronização e cliente de CalDAV/CardDAV",
                "zh-TW" to "CalDAV/CardDAV 同步服務和客戶端",
              ),
            description =
              mapOf(
                "en-US" to
                  "DAVx5 is a CalDAV/CardDAV management and synchronization app for Android.",
                "de" to
                  "DAVx5 ist eine CalDAV/CardDAV-Verwaltungs- und Synchronisierungs-App fur Android.",
              ),
            authorName = "bitfire web engineering",
            added = 2,
            lastUpdated = 2,
          ),
      ),
      TestApp(
        packageName = "com.aurora.store",
        metadata =
          MetadataV2(
            name = mapOf("en-US" to "Aurora Store"),
            summary =
              mapOf(
                "en-US" to
                  "An unofficial FOSS client to Google Play with an elegant design and privacy",
                "pt-BR" to "Um cliente FOSS do Google Play com privacidade e um design elegante",
                "zh-CN" to "Google Play的非官方自由/开源软件客户端，拥有优雅的设计和隐私",
                "zh-TW" to "Google Play 非官方 FOSS 客戶端，設計雅致兼具隱私",
              ),
            description =
              mapOf(
                "en-US" to "Aurora Store allows users to download, update, and search for apps."
              ),
            authorName = "Rahul Kumar Patel",
            added = 3,
            lastUpdated = 3,
          ),
      ),
      TestApp(
        packageName = "com.duckduckgo.mobile.android",
        metadata =
          MetadataV2(
            name = mapOf("en-US" to "DuckDuckGo Privacy Browser"),
            summary =
              mapOf(
                "en-US" to "Privacy, simplified",
                "de" to "Privatsphäre vereinfacht",
                "ja" to "プライバシー保護をシンプルに",
                "pt-BR" to "Privacidade, simplificada",
                "zh-CN" to "隐私保护，化繁为简",
                "zh-TW" to "隱私保護，化繁為簡",
              ),
            description =
              mapOf(
                "en-US" to
                  "Our app provides the privacy essentials you need to search and browse the web."
              ),
            authorName = "DuckDuckGo",
            added = 4,
            lastUpdated = 4,
          ),
      ),
      TestApp(
        packageName = "com.github.libretube",
        metadata =
          MetadataV2(
            name = mapOf("en-US" to "LibreTube"),
            summary = mapOf("en-US" to "Alternative frontend for YouTube with focus on privacy"),
            description =
              mapOf(
                "en-US" to
                  "LibreTube is an alternative frontend for YouTube with better watching experience."
              ),
            authorName = "Team LibreTube",
            added = 5,
            lastUpdated = 5,
          ),
      ),
      TestApp(
        packageName = "app.organicmaps",
        metadata =
          MetadataV2(
            name =
              mapOf(
                "zh-CN" to "Organic Maps・离线地图与导航 & GPS",
                "en-US" to "Organic Maps・Offline Map & GPS",
                "zh-TW" to "Organic Maps・離線地圖與導航 GPS",
              ),
            summary =
              mapOf(
                "zh-CN" to "获取可靠的离线地图及 GPS 导航，用于徒步、骑行和旅行。",
                "en-US" to
                  "Get reliable offline maps with GPS navigation for hiking, cycling, and travel.",
                "zh-TW" to "獲取可靠的離線地圖，搭配 GPS 導航，適用於徒步、騎行與旅行。",
              ),
            description =
              mapOf(
                "zh-CN" to
                  "Organic Maps 是一款快速、详细的离线地图和 GPS 导航应用，适用于旅行、徒步、远足和骑行探险。规划、导入和记录路线，在不耗电且无需网络连接的情况下享受顺畅的导航体验。在户外探险和全球旅行中安全地探索地点并导航。应用提供无限地图下载和每两周更新。基于 OpenStreetMap，由志愿者制作，隐私优先，100% 离线，100% 免费。\n\n<b>离线:</b> 应用 100% 离线工作——包括搜索和导航。无需网络即可探索地图和导航，节省移动数据和漫游费用。并且由于没有后台数据传输，您的电池续航更长。\n\n<b>快速:</b> 缩放、搜索和路线计算明显快速。\n\n<b>详细 & 更新:</b> 地图信息丰富，每两周更新一次以反映真实世界的情况。与许多知名竞争对手不同，它们包含建筑标签、长椅、徒步路径及其他有用信息。\n\n<b>基于 OpenStreetMap:</b> OpenStreetMap 是一个 wiki 风格的世界地图，由数百万志愿者维护——当地人最了解他们的区域。该社区努力带来了高度详细的地图，在地图覆盖较少的地区通常比商业地图更准确。Organic Maps 每两周从 OpenStreetMap 更新数据，保持地图新鲜可靠。如果发现缺失内容，可将其添加到 OpenStreetMap，帮助改善地图。\n\n<b>隐私优先:</b> 我们不收集任何数据或身份信息。这意味着没有数据泄露风险，也没有广告或嘈杂通知的干扰——仅提供干净、私密的地图体验。\n\n<b>重要功能</b>\n\n<b>探索地点:</b> 可按地址、名称或类别搜索，发现数百万兴趣点，查看详细信息，如开放时间、维基百科文章、轮椅可达性等。\n\n<b>GPS 导航:</b> 享受语音引导的逐步导航。创建步行、徒步、骑行或驾车路线——包括城市间、国家间及多个途经点的路线。找到地铁通勤和出行的最佳路线。支持 Android Auto。\n\n<b>全球徒步 & 骑行路线:</b> 启用特殊地图图层，发现全球流行的徒步和骑行路线，均来源于 OpenStreetMap。切换图层查看彩色自行车及 MTB 路线，以及官方徒步路径。\n\n<b>公共交通:</b> 查看站点的公共交通线路号。实时交通时刻表和路线规划正在开发中。\n\n<b>地图样式:</b> 选择适合您活动的地图图层——户外、等高线、徒步、骑行或地铁/轻轨。\n\n<b>路线记录:</b> 记录、导入、导出并分享您的路线和收藏地点。支持 KML、KMZ、GPX 和 GeoJSON 格式。\n\n<b>维基百科文章:</b> 探索地图上发现地点的维基百科文章。在搜索框中输入 “?wiki” 可查找所有带有维基百科文章的地点。\n\n<b>地图编辑:</b> 通过添加缺失地点或直接从 Organic Maps 编辑地图，为 OpenStreetMap 做贡献。\n\n<b>深色模式:</b> 切换到深色主题，以便夜间舒适浏览。\n\n尝试应用——它 100% 免费，无广告，快速且完全离线。体验可靠的 GPS 导航，用于徒步、骑行、远足和全球旅行！享受快速、可靠、私密的地图体验，并与他人分享！\n\nOrganic Maps 是一个独立、社区驱动、开源项目，拥有数百名贡献者，他们帮助开发新功能、修复问题、翻译及推动项目发展。\n\n加入社区 Telegram: @OrganicMapsApp\n或了解支持项目的其他方式: https://organicmaps.app/contribute/\n\n您在商店的诚实反馈和评论激励我们！我们很乐意听到您的意见。如有问题，请访问 organicmaps.app 网站获取更多详细信息和常见问题。",
                "en-US" to
                  "Organic Maps is a fast, detailed offline map and GPS navigation app for travel, hiking, trekking, and cycling adventures. Plan, import, and record routes, and enjoy smooth guidance without draining your battery or using an internet connection. Explore places and navigate safely during your outdoor adventures and travels around the world. The app offers unlimited map downloads and biweekly updates. OpenStreetMap-based, volunteer-made, privacy-first, 100% offline, and 100% free.\n\n<b>OFFLINE:</b> The app works 100% offline — including search and navigation. You can explore the map and navigate without an internet connection, saving on mobile data and roaming fees. And with no background data transfers, your battery lasts much longer.\n\n<b>FAST:</b> Zooming, search, and route calculations work noticeably fast.\n\n<b>DETAILED & UPDATED:</b> The maps are rich in detail and refreshed twice a month to reflect real-world conditions. Unlike many well-known competitors, they include building labels, benches, hiking paths, and other helpful information.\n\n<b>OPENSTREETMAP-BASED:</b> OpenStreetMap is a wiki-style world map built and maintained by millions of volunteers — locals who know their areas best. This community effort results in highly detailed maps, often more accurate than commercial ones in less-mapped regions. Organic Maps updates its data from OpenStreetMap every two weeks, keeping your maps fresh and reliable. And if you notice something missing, you can add it to OpenStreetMap and help improve the map for everyone.\n\n<b>PRIVACY-FIRST:</b> We don’t collect any data or identifiers. This means there’s no risk of data leaks and no distractions like ads or noisy notifications — just a clean, private map experience.\n\n<b>IMPORTANT FEATURES</b>\n\n<b>EXPLORE PLACES:</b> Search by address, name, or category, discover millions of POIs, and view detailed information such as opening hours, Wikipedia articles, wheelchair accessibility, and other helpful details.\n\n<b>GPS NAVIGATION:</b> Enjoy voice-guided, turn-by-turn directions. Create walking, hiking, cycling, or driving routes — including routes between cities, countries, and multiple waypoints. Find the best routes for your metro journeys and commutes. Android Auto is supported.\n\n<b>HIKING & CYCLING TRAILS FROM AROUND THE WORLD:</b> Enable special map layers to discover popular hiking and cycling routes worldwide, all sourced from OpenStreetMap. Switch between layers to see colored bike and MTB routes, as well as official hiking paths.\n\n<b>PUBLIC TRANSPORT:</b> See public transport route numbers at stops. Live transport schedules and routing are currently in development.\n\n<b>MAP STYLES:</b> Choose map layers that fit your activity — Outdoors, Contour Lines, Hiking, Cycling, or Metro/Subway.\n\n<b>TRACK RECORDER:</b> Record, import, export, and share your routes and favorite places. Supports KML, KMZ, GPX, and GeoJSON formats.\n\n<b>WIKIPEDIA ARTICLES:</b> Explore Wikipedia articles for places you discover on the map. Type “?wiki” in the search to find all places with Wikipedia articles.\n\n<b>MAP EDITOR:</b> Contribute to OpenStreetMap by adding missing places or editing the map directly from Organic Maps.\n\n<b>DARK MODE:</b> Switch to a dark theme for comfortable viewing at night.\n\nTry the app — it’s 100% free, ad-free, fast, and fully offline. Experience reliable GPS navigation for hiking, biking, trekking, and travel anywhere in the world! Enjoy a fast, reliable, and private map experience, and share it with others!\n\nOrganic Maps is an indie, community-driven, open-source project with hundreds of contributors who help develop new features, fix issues, translate, and move the project forward.\n\nJoin the community on Telegram: @OrganicMapsApp\nOr learn other ways to support the project: https://organicmaps.app/contribute/\n\nYour honest feedback and reviews in the store motivate us! We love hearing from you there. Have questions? Please visit the organicmaps.app website for additional details and FAQ",
              ),
            added = 6,
            lastUpdated = 6,
          ),
      ),
      TestApp(
        packageName = "io.github.kitswas.virtualgamepadmobile",
        metadata =
          MetadataV2(
            name = mapOf("en-US" to "VirtualGamePad Mobile"),
            summary = mapOf("en-US" to "Android phone as a gamepad for your PC"),
            added = 7,
            lastUpdated = 7,
          ),
      ),
      TestApp(
        packageName = "com.capyreader.app",
        metadata =
          MetadataV2(
            name = mapOf("zh-CN" to "Capy Reader"),
            summary = mapOf("zh-CN" to "小型 RSS 阅读器"),
            added = 8,
            lastUpdated = 8,
          ),
      ),
      TestApp(
        packageName = "com.capyreader.app",
        metadata =
          MetadataV2(
            name = mapOf("zh-CN" to "Capy Reader"),
            summary = mapOf("zh-CN" to "小型 RSS 阅读器"),
            added = 9,
            lastUpdated = 9,
          ),
      ),
      TestApp(
        packageName = "com.nextcloud.android.beta",
        metadata =
          MetadataV2(
            name = mapOf("en-US" to "Nextcloud Dev"),
            summary = mapOf("zh-CN" to "同步客户端"),
            added = 10,
            lastUpdated = 10,
          ),
      ),
      TestApp(
        packageName = "com.aistra.hail",
        metadata =
          MetadataV2(
            name = mapOf("en-US" to "Hail"),
            authorName = "艾星 Aistra",
            added = 11,
            lastUpdated = 11,
          ),
      ),
    )
}
