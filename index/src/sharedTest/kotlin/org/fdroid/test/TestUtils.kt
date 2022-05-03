package org.fdroid.test

import org.fdroid.index.v2.IndexV2
import org.fdroid.index.v2.MetadataV2
import org.fdroid.index.v2.Screenshots
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

    public fun IndexV2.sorted(): IndexV2 = copy(
        packages = packages.toSortedMap().mapValues { entry ->
            entry.value.copy(
                metadata = entry.value.metadata.sort(),
                versions = entry.value.versions.mapValues {
                    val pv = it.value
                    pv.copy(
                        manifest = pv.manifest.copy(
                            usesPermission = pv.manifest.usesPermission.sortedBy { p -> p.name },
                            usesPermissionSdk23 = pv.manifest.usesPermissionSdk23.sortedBy { p ->
                                p.name
                            }
                        )
                    )
                }
            )
        }
    )

    public fun MetadataV2.sort(): MetadataV2 = copy(
        name = name?.toSortedMap(),
        summary = summary?.toSortedMap(),
        description = description?.toSortedMap(),
        icon = icon?.toSortedMap(),
        screenshots = screenshots?.sort(),
    )

    public fun Screenshots.sort(): Screenshots = copy(
        phone = phone?.toSortedMap(),
        sevenInch = sevenInch?.toSortedMap(),
        tenInch = tenInch?.toSortedMap(),
        wear = wear?.toSortedMap(),
        tv = tv?.toSortedMap(),
    )

}
