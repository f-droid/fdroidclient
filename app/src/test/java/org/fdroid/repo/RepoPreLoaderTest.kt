package org.fdroid.repo

import android.content.Context
import android.content.res.AssetManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.InitialRepository
import org.fdroid.database.RepositoryDao
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

internal class RepoPreLoaderTest {

  @get:Rule val tmpFolder: TemporaryFolder = TemporaryFolder()

  private val context: Context = mockk(relaxed = true)
  private val assetManager: AssetManager = mockk(relaxed = true)
  private val db: FDroidDatabase = mockk(relaxed = true)
  private val repositoryDao: RepositoryDao = mockk(relaxed = true)
  private val fileFactory: (String) -> File = mockk()
  private val repoPreLoader = RepoPreLoader(context, fileFactory)

  private val packageName = "org.fdroid.basic"

  init {
    every { context.assets } returns assetManager
    every { context.packageName } returns packageName
    every { db.getRepositoryDao() } returns repositoryDao
    // By default, the asset file is empty (no repos)
    every { assetManager.open("default_repos.json") } returns "[]".byteInputStream()
    // By default, every path doesn't existent, so no ROM repos are loaded
    every { fileFactory(any()) } returns File("does-not-exist")
  }

  private val testReposJson =
    """
    [
      {
        "name": "CalyxOS Local Repository",
        "address": "file:///product/fdroid/repo/",
        "description": "This is a repository of apps to be used with CalyxOS. It is installed to the system partition, and meant to be used to initially install apps when offline. It contains a limited number of applications deemed suitable for inclusion with CalyxOS.",
        "certificate": "30820503308202eba00302010202046d902e92300d06092a864886f70d01010b050030323110300e060355040b1307462d44726f6964311e301c060355040313156c6f63616c686f73742e6c6f63616c646f6d61696e301e170d3139303131303134303235355a170d3436303532383134303235355a30323110300e060355040b1307462d44726f6964311e301c060355040313156c6f63616c686f73742e6c6f63616c646f6d61696e30820222300d06092a864886f70d01010105000382020f003082020a028202010087fc14522eb8d57da8e05121574345edf69574973d64585df2292c23acf81bc42c98f1cbfdf9fe7a1976bc575d28f6b606dbf3228b110cfc7ddc823722a279cd69b0f846ae5500ecd9884556209eacbbab30159a6ea1eaf2f64967849369a10adba65c8738b2c82b676e1367bb7b43a62dc1a5438a7c46ae2d971eafebca606a4960e0b7b1795a2a314e25759d06a20755b36b7bf0d9a6868c08aae63e389dd68f450ee093b02b28e830018dbcbbfd48ac757d8cda87549f9c41836608595e9ebfa09c128acc3c7dfd1d17a67eb6a5c99281fba69652ec27f4df3406b886e00881e6d6a4feb8fd1c5b84a5c773a631b1b2d6eab5c5ebe503c599d40f15d3de313b0d0c96d3c63802ef4346036791b2b793b3874ca73a70565ba7a768c3062679aab0e289e98b9ba16a77c8747b80820618863fe2028f02afc55914c8d6c4bcc13dd0dda61834b728875b9682ee9724589bbe216cf2b0655f62976dbf07c91514e1c342e8e397ba4458eaa5aa98703517264ef47a0972458fee928d67e06c34ecdbf7307a157928567d799c34f2a657cee034bd3fbbb717387f12e70871f2dc378687b7889bb727b92d69c8a9996257b8404e93e53eb187c807a154d95b5690eb053c249613cedad9edea857b168d41864c892c33cfecbb969cc6199e7215e82dc5810a1785ebdad509e0254daa2b2acda1093bda4fb389a8a2db5f526c5b23c10203010001a321301f301d0603551d0e04160414ba8990d4010764f81c86c769df075400c198f15e300d06092a864886f70d01010b05000382020100489ce26311568b78e4fa07951f5fcf77322ff1f4e688594e2821a20e3986d8c433b092f360930fed95d9ff206cee9070120e3ddceca1ce2221c5493dc892f1df87ae3f9a45f3e3f29dd41f852daaec9cd7b9bbe754cab9c18b0a4d0f8687915befadb27cce7ca9fd4f6061b43295568792eabd82a885ddd34ced64e9b3473b82069de6571f1bf8c292e5c599fbd37ce1103f8f95c0f644b091ba227706c53a1952959a1685a410221d374924079144d8da9536a4bbab8e9af570468c81059a78a59d212b6a07883bc5f04adf019ab922e2ab1ee23ebb3cba0e4e2987e81538b385fa8dd28486a05d53f128dfc9d18ab6bd1f2b7c92abf447eab70d3f4a73279c5fac6ec0e499cb07f4c03613836361f39cffdb75a09744b4bb37bf8d54967d0bee745bb0f39a7397faab9a79cd7b81fc2ee089814a8c18198fbf3d5d7a0e7823305bfc5339e4c61ccb64eee822acf9bc6a82e79fbce091ec91daac508970ef20e8bd4b3c2aa3dc3cd5af676d0fcfa2f4339f68a52a4f81087a2807fc3aa701bbbf80f92e8e1a3e458fe558c99d34ae94de21b211f6402606daaa1791c3be5f94730b3fa9d3e99ae34fc5682127c58fd4eb4d5b1e8f8b2848b3dbb0c1556d2c6043cceee5952e0a4f2b83c2b21ed472fd596d0f3b74e56d640f65b7cf471959c1d90d46986e598b49cd799d0793d6397f8e295a908301728291a68a14df561735",
        "mirrors": [],
        "enabled": true
      },
      {
        "name": "Guardian Project",
        "address": "https://guardianproject.info/fdroid/repo",
        "description": "The official app repository of The Guardian Project. Applications in this repository are official binaries build by the original application developers and signed by the same key as the APKs that are released in the Google Play store.",
        "certificate": "308205d8308203c0020900a397b4da7ecda034300d06092a864886f70d01010505003081ad310b30090603550406130255533111300f06035504080c084e657720596f726b3111300f06035504070c084e657720596f726b31143012060355040b0c0b4644726f6964205265706f31193017060355040a0c10477561726469616e2050726f6a656374311d301b06035504030c14677561726469616e70726f6a6563742e696e666f3128302606092a864886f70d0109011619726f6f7440677561726469616e70726f6a6563742e696e666f301e170d3134303632363139333931385a170d3431313131303139333931385a3081ad310b30090603550406130255533111300f06035504080c084e657720596f726b3111300f06035504070c084e657720596f726b31143012060355040b0c0b4644726f6964205265706f31193017060355040a0c10477561726469616e2050726f6a656374311d301b06035504030c14677561726469616e70726f6a6563742e696e666f3128302606092a864886f70d0109011619726f6f7440677561726469616e70726f6a6563742e696e666f30820222300d06092a864886f70d01010105000382020f003082020a0282020100b3cd79121b9b883843be3c4482e320809106b0a23755f1dd3c7f46f7d315d7bb2e943486d61fc7c811b9294dcc6b5baac4340f8db2b0d5e14749e7f35e1fc211fdbc1071b38b4753db201c314811bef885bd8921ad86facd6cc3b8f74d30a0b6e2e6e576f906e9581ef23d9c03e926e06d1f033f28bd1e21cfa6a0e3ff5c9d8246cf108d82b488b9fdd55d7de7ebb6a7f64b19e0d6b2ab1380a6f9d42361770d1956701a7f80e2de568acd0bb4527324b1e0973e89595d91c8cc102d9248525ae092e2c9b69f7414f724195b81427f28b1d3d09a51acfe354387915fd9521e8c890c125fc41a12bf34d2a1b304067ab7251e0e9ef41833ce109e76963b0b256395b16b886bca21b831f1408f836146019e7908829e716e72b81006610a2af08301de5d067c9e114a1e5759db8a6be6a3cc2806bcfe6fafd41b5bc9ddddb3dc33d6f605b1ca7d8a9e0ecdd6390d38906649e68a90a717bea80fa220170eea0c86fc78a7e10dac7b74b8e62045a3ecca54e035281fdc9fe5920a855fde3c0be522e3aef0c087524f13d973dff3768158b01a5800a060c06b451ec98d627dd052eda804d0556f60dbc490d94e6e9dea62ffcafb5beffbd9fc38fb2f0d7050004fe56b4dda0a27bc47554e1e0a7d764e17622e71f83a475db286bc7862deee1327e2028955d978272ea76bf0b88e70a18621aba59ff0c5993ef5f0e5d6b6b98e68b70203010001300d06092a864886f70d0101050500038202010079c79c8ef408a20d243d8bd8249fb9a48350dc19663b5e0fce67a8dbcb7de296c5ae7bbf72e98a2020fb78f2db29b54b0e24b181aa1c1d333cc0303685d6120b03216a913f96b96eb838f9bff125306ae3120af838c9fc07ebb5100125436bd24ec6d994d0bff5d065221871f8410daf536766757239bf594e61c5432c9817281b985263bada8381292e543a49814061ae11c92a316e7dc100327b59e3da90302c5ada68c6a50201bda1fcce800b53f381059665dbabeeb0b50eb22b2d7d2d9b0aa7488ca70e67ac6c518adb8e78454a466501e89d81a45bf1ebc350896f2c3ae4b6679ecfbf9d32960d4f5b493125c7876ef36158562371193f600bc511000a67bdb7c664d018f99d9e589868d103d7e0994f166b2ba18ff7e67d8c4da749e44dfae1d930ae5397083a51675c409049dfb626a96246c0015ca696e94ebb767a20147834bf78b07fece3f0872b057c1c519ff882501995237d8206b0b3832f78753ebd8dcbd1d3d9f5ba733538113af6b407d960ec4353c50eb38ab29888238da843cd404ed8f4952f59e4bbc0035fc77a54846a9d419179c46af1b4a3b7fc98e4d312aaa29b9b7d79e739703dc0fa41c7280d5587709277ffa11c3620f5fba985b82c238ba19b17ebd027af9424be0941719919f620dd3bb3c3f11638363708aa11f858e153cf3a69bce69978b90e4a273836100aa1e617ba455cd00426847f",
        "mirrors": ["https://s3.amazonaws.com/guardianproject/fdroid/repo"],
        "enabled": false
      }
    ]
    """
      .trimIndent()

  private val allRomRepoPaths =
    listOf("/system", "/system_ext", "/product", "/vendor").flatMap { root ->
      listOf(packageName, "fdroid").map { subdir -> "$root/etc/$subdir/additional_repos.json" }
    }

  @Test
  fun `adds default repos from assets`() {
    // Override default empty JSON with two real repos
    every { assetManager.open("default_repos.json") } returns testReposJson.byteInputStream()

    val capturedRepoList = mutableListOf<InitialRepository>()
    repoPreLoader.addPreloadedRepositories(db)
    verify(exactly = 2) { repositoryDao.insert(capture(capturedRepoList)) }

    val names = capturedRepoList.map { it.name }
    assertTrue("CalyxOS Local Repository" in names)
    assertTrue("Guardian Project" in names)
  }

  @Test
  fun `picks up file from system etc fdroid`() {
    mockFileAtPath("/system/etc/fdroid/additional_repos.json")

    val capturedRepoList = mutableListOf<InitialRepository>()
    repoPreLoader.addPreloadedRepositories(db)
    verify(exactly = 2) { repositoryDao.insert(capture(capturedRepoList)) }

    assertTrue(capturedRepoList.any { it.name == "CalyxOS Local Repository" })
    assertTrue(capturedRepoList.any { it.name == "Guardian Project" })
  }

  @Test
  fun `picks up file from system etc package name`() {
    mockFileAtPath("/system/etc/$packageName/additional_repos.json")

    val capturedRepoList = mutableListOf<InitialRepository>()
    repoPreLoader.addPreloadedRepositories(db)
    verify(exactly = 2) { repositoryDao.insert(capture(capturedRepoList)) }

    assertTrue(capturedRepoList.any { it.name == "CalyxOS Local Repository" })
    assertTrue(capturedRepoList.any { it.name == "Guardian Project" })
  }

  @Test
  fun `picks up file from system_ext etc fdroid`() {
    mockFileAtPath("/system_ext/etc/fdroid/additional_repos.json")

    val capturedRepoList = mutableListOf<InitialRepository>()
    repoPreLoader.addPreloadedRepositories(db)
    verify(exactly = 2) { repositoryDao.insert(capture(capturedRepoList)) }

    assertTrue(capturedRepoList.any { it.name == "CalyxOS Local Repository" })
  }

  @Test
  fun `picks up file from system_ext etc package name`() {
    mockFileAtPath("/system_ext/etc/$packageName/additional_repos.json")

    val capturedRepoList = mutableListOf<InitialRepository>()
    repoPreLoader.addPreloadedRepositories(db)
    verify(exactly = 2) { repositoryDao.insert(capture(capturedRepoList)) }

    assertTrue(capturedRepoList.any { it.name == "Guardian Project" })
  }

  @Test
  fun `picks up file from product etc fdroid`() {
    mockFileAtPath("/product/etc/fdroid/additional_repos.json")

    val capturedRepoList = mutableListOf<InitialRepository>()
    repoPreLoader.addPreloadedRepositories(db)
    verify(exactly = 2) { repositoryDao.insert(capture(capturedRepoList)) }

    assertTrue(capturedRepoList.any { it.name == "CalyxOS Local Repository" })
  }

  @Test
  fun `picks up file from product etc package name`() {
    mockFileAtPath("/product/etc/$packageName/additional_repos.json")

    val capturedRepoList = mutableListOf<InitialRepository>()
    repoPreLoader.addPreloadedRepositories(db)
    verify(exactly = 2) { repositoryDao.insert(capture(capturedRepoList)) }

    assertTrue(capturedRepoList.any { it.name == "Guardian Project" })
  }

  @Test
  fun `picks up file from vendor etc fdroid`() {
    mockFileAtPath("/vendor/etc/fdroid/additional_repos.json")

    val capturedRepoList = mutableListOf<InitialRepository>()
    repoPreLoader.addPreloadedRepositories(db)
    verify(exactly = 2) { repositoryDao.insert(capture(capturedRepoList)) }

    assertTrue(capturedRepoList.any { it.name == "CalyxOS Local Repository" })
  }

  @Test
  fun `picks up file from vendor etc package name`() {
    mockFileAtPath("/vendor/etc/$packageName/additional_repos.json")

    val capturedRepoList = mutableListOf<InitialRepository>()
    repoPreLoader.addPreloadedRepositories(db)
    verify(exactly = 2) { repositoryDao.insert(capture(capturedRepoList)) }

    assertTrue(capturedRepoList.any { it.name == "Guardian Project" })
  }

  @Test
  fun `picks up files from all 8 possible locations`() {
    mockAllRomRepoPaths()

    val capturedRepoList = mutableListOf<InitialRepository>()
    repoPreLoader.addPreloadedRepositories(db)
    // 8 locations * 2 repos per file = 16 inserts (plus 0 from empty default_repos.json)
    verify(exactly = 16) { repositoryDao.insert(capture(capturedRepoList)) }

    // Every path should have contributed both repos
    assertEquals(16, capturedRepoList.size)
    assertEquals(8, capturedRepoList.count { it.name == "CalyxOS Local Repository" })
    assertEquals(8, capturedRepoList.count { it.name == "Guardian Project" })
  }

  @Test
  fun `maps all fields from json to InitialRepository`() {
    mockFileAtPath("/product/etc/fdroid/additional_repos.json")

    val capturedRepoList = mutableListOf<InitialRepository>()
    repoPreLoader.addPreloadedRepositories(db)
    verify(exactly = 2) { repositoryDao.insert(capture(capturedRepoList)) }

    val calyx = capturedRepoList.first { it.name == "CalyxOS Local Repository" }
    assertEquals("CalyxOS Local Repository", calyx.name)
    assertEquals("file:///product/fdroid/repo/", calyx.address)
    assertEquals(
      "This is a repository of apps to be used with CalyxOS. It is installed to the system partition, and meant to be used to initially install apps when offline. It contains a limited number of applications deemed suitable for inclusion with CalyxOS.",
      calyx.description,
    )
    assertEquals(emptyList(), calyx.mirrors)
    assertTrue(calyx.certificate.isNotEmpty())
    assertEquals(true, calyx.enabled)

    val guardian = capturedRepoList.first { it.name == "Guardian Project" }
    assertEquals("Guardian Project", guardian.name)
    assertEquals("https://guardianproject.info/fdroid/repo", guardian.address)
    assertEquals(
      "The official app repository of The Guardian Project. Applications in this repository are official binaries build by the original application developers and signed by the same key as the APKs that are released in the Google Play store.",
      guardian.description,
    )
    assertEquals(listOf("https://s3.amazonaws.com/guardianproject/fdroid/repo"), guardian.mirrors)
    assertTrue(guardian.certificate.isNotEmpty())
    assertEquals(false, guardian.enabled)
  }

  @Test
  fun `does not insert when no rom repos files are present`() {
    // fileFactory returns a non-existent file for all paths and default_repos.json is empty
    repoPreLoader.addPreloadedRepositories(db)

    verify(exactly = 0) { repositoryDao.insert(any<InitialRepository>()) }
  }

  @Test
  fun `handles invalid json gracefully without crashing`() {
    val badFile = tmpFolder.newFile().apply { writeText("{ not valid json at all }") }

    every { fileFactory("/vendor/etc/fdroid/additional_repos.json") } returns badFile

    // Should not throw, the error is logged and the bad file is skipped
    repoPreLoader.addPreloadedRepositories(db)

    verify(exactly = 0) { repositoryDao.insert(any<InitialRepository>()) }
  }

  @Test
  fun `defaultRepoAddresses returns addresses from default_repos json`() {
    every { assetManager.open("default_repos.json") } returns testReposJson.byteInputStream()

    val addresses = repoPreLoader.defaultRepoAddresses

    assertEquals(
      setOf("file:///product/fdroid/repo/", "https://guardianproject.info/fdroid/repo"),
      addresses,
    )
  }

  @Test
  fun `defaultRepoAddresses is empty when default_repos json is empty`() {
    // default is already "[]" from setUp
    assertTrue(repoPreLoader.defaultRepoAddresses.isEmpty())
  }

  /** Mocks a file at the specific ROM repo path filled with our [testReposJson]. */
  private fun mockFileAtPath(path: String) {
    val tempFile = tmpFolder.newFile().apply { writeText(testReposJson) }
    every { fileFactory(path) } returns tempFile
  }

  /** Mocks a file at *all* possible ROM repo paths filled with our [testReposJson]. */
  private fun mockAllRomRepoPaths() {
    val tempFile = tmpFolder.newFile().apply { writeText(testReposJson) }
    allRomRepoPaths.forEach { path -> every { fileFactory(path) } returns tempFile }
  }
}
