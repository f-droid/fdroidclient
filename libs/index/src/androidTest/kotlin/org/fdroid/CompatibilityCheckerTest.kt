package org.fdroid

import android.content.pm.FeatureInfo
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.fdroid.index.v2.PackageManifest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class CompatibilityCheckerTest {

    private val sdkInt: Int = 30
    private val supportedAbis = arrayOf("x86")
    private val packageManager: PackageManager = mockk()

    init {
        every { packageManager.systemAvailableFeatures } returns arrayOf(
            FeatureInfo().apply { name = "foo bar" },
            FeatureInfo().apply { name = "1337" },
        )
    }

    private val checker = CompatibilityCheckerImpl(
        packageManager = packageManager,
        forceTouchApps = false,
        sdkInt = sdkInt,
        supportedAbis = supportedAbis,
    )

    @Test
    fun emptyManifestIsCompatible() {
        val manifest = Manifest()
        assertTrue(checker.isCompatible(manifest))
    }

    @Test
    fun minSdkIsRespected() {
        // smaller or equal minSdks are compatible
        val manifest1 = Manifest(minSdkVersion = 1)
        assertTrue(checker.isCompatible(manifest1))
        val manifest2 = Manifest(minSdkVersion = sdkInt)
        assertTrue(checker.isCompatible(manifest2))
        // a minSdk higher than the system is not compatible
        val manifest3 = Manifest(minSdkVersion = sdkInt + 1)
        assertFalse(checker.isCompatible(manifest3))
    }

    @Test
    fun maxSdkIsRespected() {
        // smaller maxSdks are not compatible
        val manifest1 = Manifest(maxSdkVersion = sdkInt - 1)
        assertFalse(checker.isCompatible(manifest1))
        // higher or equal are compatible
        val manifest2 = Manifest(maxSdkVersion = sdkInt)
        assertTrue(checker.isCompatible(manifest2))
        val manifest3 = Manifest(maxSdkVersion = sdkInt + 1)
        assertTrue(checker.isCompatible(manifest3))
    }

    @Test
    fun emptyNativeCodeIsCompatible() {
        val manifest = Manifest(nativecode = emptyList())
        assertTrue(checker.isCompatible(manifest))
    }

    @Test
    fun nativeCodeMustBeAvailable() {
        val manifest1 = Manifest(nativecode = listOf("x86"))
        assertTrue(checker.isCompatible(manifest1))
        val manifest2 = Manifest(nativecode = listOf("x86", "armeabi-v7a"))
        assertTrue(checker.isCompatible(manifest2))
        val manifest3 = Manifest(nativecode = listOf("arm64-v8a", "armeabi-v7a"))
        assertFalse(checker.isCompatible(manifest3))
    }

    @Test
    fun featuresMustBeAvailable() {
        val manifest1 = Manifest(featureNames = listOf("foo bar"))
        assertTrue(checker.isCompatible(manifest1))
        val manifest2 = Manifest(featureNames = listOf("1337", "foo bar"))
        assertTrue(checker.isCompatible(manifest2))
        val manifest3 = Manifest(featureNames = listOf("1337", "foo bar", "42"))
        assertFalse(checker.isCompatible(manifest3))
        val manifest4 = Manifest(featureNames = listOf("foo", "bar"))
        assertFalse(checker.isCompatible(manifest4))
    }

    @Test
    fun forceTouchScreenIsRespected() {
        val checkerForce = CompatibilityCheckerImpl(packageManager, true, sdkInt, supportedAbis)

        // when forced, apps that need touchscreen on non-touchscreen device are compatible
        val manifest1 = Manifest(featureNames = listOf("android.hardware.touchscreen"))
        assertTrue(checkerForce.isCompatible(manifest1))
        val manifest2 = Manifest(featureNames = listOf("android.hardware.touchscreen"))
        assertTrue(checkerForce.isCompatible(manifest2))
        // when not forced, apps that need touchscreen on non-touchscreen device are not compatible
        val manifest3 = Manifest(featureNames = listOf("android.hardware.touchscreen"))
        assertFalse(checker.isCompatible(manifest3))
        val manifest4 = Manifest(featureNames = listOf("android.hardware.touchscreen"))
        assertFalse(checker.isCompatible(manifest4))
    }

    private data class Manifest(
        override val minSdkVersion: Int? = null,
        override val maxSdkVersion: Int? = null,
        override val featureNames: List<String>? = null,
        override val nativecode: List<String>? = null,
    ) : PackageManifest

}
