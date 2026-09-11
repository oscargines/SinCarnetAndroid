package com.oscar.sincarnet.data

actual class PlatformContext(val value: android.content.Context)

fun android.content.Context.toStorage(name: String): PlatformStorage =
    PlatformStorage(name, PlatformContext(this))
