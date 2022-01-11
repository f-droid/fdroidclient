package org.fdroid

import kotlinx.coroutines.runBlocking
import kotlin.random.Random

fun getRandomString(length: Int = Random.nextInt(4, 16)): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}

fun runSuspend(block: suspend () -> Unit) = runBlocking {
    block()
}
