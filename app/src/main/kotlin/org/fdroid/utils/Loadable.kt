package org.fdroid.utils

sealed class Loadable<T>

class Loading<T> : Loadable<T>()

class Loaded<T>(val value: T) : Loadable<T>()
