package org.fdroid.database

import org.fdroid.index.v2.AntiFeatureV2
import org.fdroid.index.v2.CategoryV2
import org.fdroid.index.v2.FileV2
import org.fdroid.index.v2.LocalizedTextV2
import org.fdroid.index.v2.MirrorV2
import org.fdroid.index.v2.ReleaseChannelV2
import org.fdroid.index.v2.RepoV2
import kotlin.random.Random

object TestUtils {

    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    fun getRandomString(length: Int = Random.nextInt(1, 128)) = (1..length)
        .map { Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")

    fun <T> getRandomList(
        size: Int = Random.nextInt(0, 23),
        factory: () -> T,
    ): List<T> = if (size == 0) emptyList() else buildList {
        repeat(Random.nextInt(0, size)) {
            add(factory())
        }
    }

    fun <A, B> getRandomMap(
        size: Int = Random.nextInt(0, 23),
        factory: () -> Pair<A, B>,
    ): Map<A, B> = if (size == 0) emptyMap() else buildMap {
        repeat(size) {
            val pair = factory()
            put(pair.first, pair.second)
        }
    }

    private fun <T> T.orNull(): T? {
        return if (Random.nextBoolean()) null else this
    }

    fun getRandomMirror() = MirrorV2(
        url = getRandomString(),
        location = getRandomString().orNull()
    )

    fun getRandomLocalizedTextV2(size: Int = Random.nextInt(0, 23)): LocalizedTextV2 = buildMap {
        repeat(size) {
            put(getRandomString(4), getRandomString())
        }
    }

    fun getRandomFileV2() = FileV2(
        name = getRandomString(),
        sha256 = getRandomString(64),
        size = Random.nextLong(-1, Long.MAX_VALUE)
    )

    fun getRandomRepo() = RepoV2(
        name = getRandomString(),
        icon = getRandomFileV2(),
        address = getRandomString(),
        description = getRandomLocalizedTextV2(),
        mirrors = getRandomList { getRandomMirror() },
        timestamp = System.currentTimeMillis(),
        antiFeatures = getRandomMap {
            getRandomString() to AntiFeatureV2(getRandomFileV2(), getRandomLocalizedTextV2())
        },
        categories = getRandomMap {
            getRandomString() to CategoryV2(getRandomFileV2(), getRandomLocalizedTextV2())
        },
        releaseChannels = getRandomMap {
            getRandomString() to ReleaseChannelV2(getRandomLocalizedTextV2())
        },
    )

    /**
     * Create a map diff by adding or removing keys. Note that this does not change keys.
     */
    fun <T> Map<String, T?>.randomDiff(factory: () -> T): Map<String, T?> = buildMap {
        if (this@randomDiff.isNotEmpty()) {
            // remove random keys
            while (Random.nextBoolean()) put(this@randomDiff.keys.random(), null)
            // Note: we don't replace random keys, because we can't easily diff inside T
        }
        // add random keys
        while (Random.nextBoolean()) put(getRandomString(), factory())
    }

    fun <T> Map<String, T>.applyDiff(diff: Map<String, T?>): Map<String, T> = toMutableMap().apply {
        diff.entries.forEach { (key, value) ->
            if (value == null) remove(key)
            else set(key, value)
        }
    }

}
