package org.fdroid.test

import kotlin.random.Random

public object DiffUtils {

    /**
     * Create a map diff by adding or removing keys. Note that this does not change keys.
     */
    public fun <T> Map<String, T?>.randomDiff(factory: () -> T): Map<String, T?> = buildMap {
        if (this@randomDiff.isNotEmpty()) {
            // remove random keys
            while (Random.nextBoolean()) put(this@randomDiff.keys.random(), null)
            // Note: we don't replace random keys, because we can't easily diff inside T
        }
        // add random keys
        while (Random.nextBoolean()) put(TestUtils.getRandomString(), factory())
    }

    public fun <T> Map<String, T>.applyDiff(diff: Map<String, T?>): Map<String, T> =
        toMutableMap().apply {
            diff.entries.forEach { (key, value) ->
                if (value == null) remove(key)
                else set(key, value)
            }
        }

}
