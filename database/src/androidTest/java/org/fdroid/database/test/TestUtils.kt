package org.fdroid.database.test

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
        repeat(size) {
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

    fun <T> T.orNull(): T? {
        return if (Random.nextBoolean()) null else this
    }

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
