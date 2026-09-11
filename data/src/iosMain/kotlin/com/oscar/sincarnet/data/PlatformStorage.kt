package com.oscar.sincarnet.data

import platform.Foundation.NSUserDefaults

actual class PlatformStorage(name: String, context: PlatformContext) {
    private val defaults = NSUserDefaults(suiteName = name) ?: NSUserDefaults.standardUserDefaults

    actual fun getString(key: String, defValue: String): String =
        defaults.stringForKey(key) ?: defValue

    actual fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
        defaults.synchronize()
    }

    actual fun getInt(key: String, defValue: Int): Int =
        defaults.integerForKey(key).let { if (it == 0L && !defaults.objectForKey(key)) defValue else it.toInt() }

    actual fun putInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), forKey = key)
        defaults.synchronize()
    }

    actual fun getBoolean(key: String, defValue: Boolean): Boolean =
        defaults.boolForKey(key).let { if (!it && !defaults.objectForKey(key)) defValue else it }

    actual fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
        defaults.synchronize()
    }

    actual fun remove(key: String) {
        defaults.removeObjectForKey(key)
        defaults.synchronize()
    }

    actual fun contains(key: String): Boolean =
        defaults.objectForKey(key) != null

    actual fun clear() {
        val dict = defaults.dictionaryRepresentation()
        for (key in dict.keys) {
            defaults.removeObjectForKey(key as String)
        }
        defaults.synchronize()
    }
}
