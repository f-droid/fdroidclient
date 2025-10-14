package org.fdroid.ui

import android.content.Intent
import android.content.Intent.ACTION_SHOW_APP_INFO
import android.content.Intent.ACTION_VIEW
import android.content.Intent.CATEGORY_BROWSABLE
import android.content.Intent.EXTRA_PACKAGE_NAME
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@Config(sdk = [24]) // needed for ACTION_SHOW_APP_INFO
@RunWith(RobolectricTestRunner::class)
class IntentRouterTest {

    private val backstack: NavBackStack<NavKey> = NavBackStack()
    private val intentRouter = IntentRouter(backstack)

    @Test
    fun testBrowserUris() {
        val packageName = "org.fdroid.fdroid"
        listOf(
            "https://f-droid.org/packages/$packageName/",
            "https://f-droid.org/de/packages/$packageName/",
            "https://f-droid.org/en-US/packages/$packageName",
            "https://cloudflare.f-droid.org/zh_Hans/packages/$packageName",
            "market://details?id=$packageName",
        ).forEach { url ->
            val i = Intent().apply {
                action = ACTION_VIEW
                addCategory(CATEGORY_BROWSABLE)
                data = url.toUri()
            }
            intentRouter.accept(i)
            assertEquals(1, backstack.size)
            assertEquals(NavigationKey.AppDetails(packageName), backstack.removeLastOrNull())
        }
    }

    @Test
    fun testMalformedBrowserUris() {
        val packageName = "fdroid') DROP TABLE apps; --"
        listOf(
            "https://f-droid.org/packages/$packageName/",
            "https://f-droid.org/de/packages/$packageName/",
            "https://f-droid.org/en-US/packages/$packageName",
            "https://cloudflare.f-droid.org/zh_Hans/packages/$packageName",
            "market://details?id=$packageName",
        ).forEach { url ->
            val i = Intent().apply {
                action = ACTION_VIEW
                addCategory(CATEGORY_BROWSABLE)
                data = url.toUri()
            }
            intentRouter.accept(i)
            assertEquals(0, backstack.size)
        }
    }

    @Test
    fun testShowAppInfo() {
        val packageName = "org.fdroid.fdroid"
        val i = Intent().apply {
            action = ACTION_SHOW_APP_INFO
            putExtra(EXTRA_PACKAGE_NAME, packageName)
        }
        intentRouter.accept(i)
        assertEquals(1, backstack.size)
        assertEquals(NavigationKey.AppDetails(packageName), backstack.removeLastOrNull())
    }

    @Test
    fun testShowAppInfoMalformed() {
        val packageName = "fdroid') DROP TABLE apps; --"
        val i = Intent().apply {
            action = ACTION_SHOW_APP_INFO
            putExtra(EXTRA_PACKAGE_NAME, packageName)
        }
        intentRouter.accept(i)
        assertEquals(0, backstack.size)
    }

    @Test
    fun testRepoUris() {
        listOf(
            "fdroidrepos://example.org/repo",
            "FDROIDREPOS://example.org/repo",
            "https://fdroid.link/#repo=https://f-droid.org/repo",
            "https://fdroid.link/#foo/bar",
        ).forEach { uri ->
            val i = Intent(ACTION_VIEW).apply {
                data = uri.toUri()
            }
            intentRouter.accept(i)
            assertEquals(1, backstack.size)
            assertEquals(NavigationKey.AddRepo(uri), backstack.removeLastOrNull())
        }
    }

}
