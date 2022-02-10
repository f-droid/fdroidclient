package org.fdroid.test

import kotlin.random.Random

public object TestUtils {

    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    public fun getRandomString(length: Int = Random.nextInt(1, 128)): String = (1..length)
        .map { Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")

    public fun <T> getRandomList(
        size: Int = Random.nextInt(0, 23),
        factory: () -> T,
    ): List<T> = if (size == 0) emptyList() else buildList {
        repeat(size) {
            add(factory())
        }
    }

    public fun <A, B> getRandomMap(
        size: Int = Random.nextInt(0, 23),
        factory: () -> Pair<A, B>,
    ): Map<A, B> = if (size == 0) emptyMap() else buildMap {
        repeat(size) {
            val pair = factory()
            put(pair.first, pair.second)
        }
    }

    public fun <T> T.orNull(): T? {
        return if (Random.nextBoolean()) null else this
    }

}
