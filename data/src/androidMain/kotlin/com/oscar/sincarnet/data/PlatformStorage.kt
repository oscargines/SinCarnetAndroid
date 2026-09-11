package com.oscar.sincarnet.data

actual class PlatformStorage actual constructor(name: String, context: PlatformContext) {
    private val prefs = context.value.getSharedPreferences(name, android.content.Context.MODE_PRIVATE)

    actual fun getString(key: String, defValue: String): String =
        prefs.getString(key, defValue) ?: defValue

    actual fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    actual fun getInt(key: String, defValue: Int): Int =
        prefs.getInt(key, defValue)

    actual fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    actual fun getBoolean(key: String, defValue: Boolean): Boolean =
        prefs.getBoolean(key, defValue)

    actual fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    actual fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    actual fun contains(key: String): Boolean =
        prefs.contains(key)

    actual fun clear() {
        prefs.edit().clear().apply()
    }
}
