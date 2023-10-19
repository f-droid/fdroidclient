package org.fdroid.repo

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import kotlinx.coroutines.runBlocking
import org.fdroid.fdroid.data.DBHelper
import org.fdroid.fdroid.net.DownloaderFactory
import org.fdroid.index.RepoManager
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory.getLogger
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
internal class RepoManagerAddAllIntegrationTest {

    @get:Rule
    var folder: TemporaryFolder = TemporaryFolder()

    private val repos = listOf(
        "https://raw.githubusercontent.com/2br-2b/Fdroid-repo/master/fdroid/repo",
        "https://anonymousmessenger.ly/fdroid/repo",
        "https://fdroid.beocode.eu/fdroid/repo",
        "https://mobileapp.bitwarden.com/fdroid/repo",
        "https://briarproject.org/fdroid/repo",
        "https://fdroid.bromite.org/fdroid/repo",
        "https://fdroid.gitlab.io/ccc/fdroid/repo",
        "https://www.collaboraoffice.com/downloads/fdroid/repo",
        "https://bubu1.eu/cctg/fdroid/repo",
        "https://static.cryptomator.org/android/fdroid/repo",
        "https://lucaapp.gitlab.io/fdroid-repository/fdroid/repo",
        "https://divestos.org/apks/official/fdroid/repo",
        "https://divestos.org/apks/unofficial/fdroid/repo",
        "https://raw.githubusercontent.com/efreak/auto-daily-fdroid/main/fdroid/repo",
        "https://bubu1.eu/fdroidclassic/fdroid/repo",
        "https://f5a.typed.icu/fdroid/repo",
        "https://fdroid.fedilab.app/repo",
        "https://raw.githubusercontent.com/Tobi823/ffupdaterrepo/master/fdroid/repo",
        "https://rfc2822.gitlab.io/fdroid-firefox/fdroid/repo",
        "https://raw.githubusercontent.com/Five-Prayers/fdroid-repo-stable/main/fdroid/repo",
        "https://codeberg.org/florian-obernberger/fdroid-repo/raw/branch/main/repo",
        "https://fdroid.frostnerd.com/fdroid/repo",
        "https://pili.qi0.de/fdroid/repo",
        "https://gitjournal.io/fdroid/repo",
        "https://guardianproject.info/fdroid/repo",
        "https://s3.amazonaws.com/guardianproject/fdroid/repo",
        "https://guardianproject.info/fdroid/repo",
        "https://f-droid.i2p.io/repo",
        "https://iitc.app/fdroid/repo",
        "https://jhass.github.io/insporation/fdroid/repo",
        "https://raw.githubusercontent.com/iodeOS/fdroid/master/fdroid/repo",
        "https://apt.izzysoft.de/fdroid/repo",
        "https://android.izzysoft.de/repo",
        "https://jak-linux.org/fdroid/repo",
        "https://julianfairfax.gitlab.io/fdroid-repo/fdroid/repo",
        "https://kaffeemitkoffein.de/fdroid/repo",
        "https://store.nethunter.com/repo",
        "https://cdn.kde.org/android/stable-releases/fdroid/repo",
        "https://repo.kuschku.de/fdroid/repo",
        "https://fdroid.libretro.com/repo",
        "https://fdroid.ltheinrich.de/fdroid/repo",
        "https://ltt.rs/fdroid/repo",
        "https://pili.qi0.de/fdroid/repo",
        "https://fdroid.metatransapps.com/fdroid/repo",
        "https://microg.org/fdroid/repo",
        "https://fdroid.mm20.de/repo",
        "https://repo.mobilsicher.de/fdroid/repo",
        "https://molly.im/fdroid/repo",
        "https://molly.im/fdroid/foss/fdroid/repo",
        "https://f-droid.monerujo.io/fdroid/repo",
        "https://releases.nailyk.fr/repo",
        "https://nanolx.org/fdroid/repo",
        "https://www.nanolx.org/fdroid/repo",
        "https://repo.netsyms.com/fdroid/repo",
        "https://archive.newpipe.net/fdroid/repo",
        "https://repo.nononsenseapps.com/fdroid/repo",
        "https://fdroid.novy.software/repo",
        "https://raw.githubusercontent.com/nucleus-ffm/Nucleus-F-Droid-Repo/master/fdroid/repo",
        "https://obfusk.ch/fdroid/repo",
        "https://ouchadam.github.io/fdroid-repository/repo",
        "https://fdroid.partidopirata.com.ar/fdroid/repo",
        "https://thecapslock.gitlab.io/fdroid-patched-apps/fdroid/repo",
        "https://fdroid.i2pd.xyz/fdroid/repo",
        "https://fdroid.rami.io/fdroid/repo",
        "https://thedoc.eu.org/fdroid/repo",
        "https://repo.samourai.io/fdroid/repo",
        "https://fdroid.a3.pm/seabear/repo",
        "https://raw.githubusercontent.com/jackbonadies/seekerandroid/fdroid/fdroid/repo",
        "https://fdroid.getsession.org/fdroid/repo",
        "https://raw.githubusercontent.com/simlar/simlar-fdroid-repo/master/fdroid/repo",
        "https://s2.spiritcroc.de/fdroid/repo",
        "https://haagch.frickel.club/files/fdroid/repo",
        "https://submarine.strangled.net/fdroid/repo",
        "https://service.tagesschau.de/app/repo",
        "https://fdroid-repo.calyxinstitute.org/fdroid/repo",
        "https://releases.threema.ch/fdroid/repo",
        "https://raw.githubusercontent.com/chrisgch/tca/master/fdroid/repo",
        "https://fdroid.twinhelix.com/fdroid/repo",
        "https://secfirst.org/fdroid/repo",
        "https://fdroid.videlibri.de/repo",
        "https://guardianproject-wind.s3.amazonaws.com/fdroid/repo",
        "https://raw.githubusercontent.com/xarantolus/fdroid/main/fdroid/repo",
        "https://zimbelstern.eu/fdroid/repo",
    )

    private val log = getLogger(this::class.java.simpleName)
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db = DBHelper.getDb(context) // real DB
    private val httpManager = DownloaderFactory.HTTP_MANAGER
    private val downloaderFactory = DownloaderFactory.INSTANCE

    private val repoManager = RepoManager(context, db, downloaderFactory, httpManager)

    @Before
    fun optIn() {
        // Careful! This will add lots of repos to your live DB
        assumeTrue(false) // don't run integration tests with real repos all the time
    }

    @Test
    fun addAllTheThings() = runBlocking {
        repos.forEach { addRepo(it) }
    }

    private suspend fun addRepo(url: String) {
        log.info("Fetching $url")
        repoManager.fetchRepositoryPreview(url = url, proxy = null)
        repoManager.addRepoState.test(timeout = 15.seconds) {
            val fetchState = awaitFinalFetchState()
            if (fetchState is Fetching && fetchState.canAdd) {
                repoManager.addFetchedRepository()
                val item = awaitItem()
                if (item is Adding) {
                    // await final state
                    assertIs<Added>(awaitItem())
                } else {
                    // was already final state
                    assertIs<Added>(item)
                }
                log.info("  Added")
            } else if (fetchState is AddRepoError) {
                log.error("  $fetchState $url")
            }
            repoManager.abortAddingRepository()
            assertIs<None>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        log.info("End $url")
    }

    private suspend fun TurbineTestContext<AddRepoState>.awaitFinalFetchState(): AddRepoState {
        var item = awaitItem()
        log.info("  $item")
        while (item is None || (item is Fetching && !item.done)) {
            item = awaitItem()
            log.info("  $item")
        }
        log.info("  final: $item")
        return item
    }

}
