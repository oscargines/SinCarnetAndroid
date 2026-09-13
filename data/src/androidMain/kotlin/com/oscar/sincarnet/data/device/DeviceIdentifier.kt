package com.oscar.sincarnet.data.device

import android.content.Context
import android.provider.Settings
import java.util.UUID

/** Identificador corto y persistente utilizado para trazabilidad local. */
fun getDeviceIdentifierSuffix(context: Context): String {
    val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    val stored = preferences.getString(KEY_SUFFIX, null)
    if (!stored.isNullOrBlank()) return stored

    val source = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        ?.takeIf { it.isNotBlank() }
        ?: UUID.randomUUID().toString().replace("-", "")
    val suffix = source.takeLast(IDENTIFIER_LENGTH).uppercase()
    preferences.edit().putString(KEY_SUFFIX, suffix).apply()
    return suffix
}

private const val PREFERENCES_NAME = "device_identifier_storage"
private const val KEY_SUFFIX = "device_identifier_suffix"
private const val IDENTIFIER_LENGTH = 4
