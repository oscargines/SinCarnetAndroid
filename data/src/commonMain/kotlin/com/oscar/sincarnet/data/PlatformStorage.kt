package com.oscar.sincarnet.data

expect class PlatformStorage(name: String, context: PlatformContext) {
    fun getString(key: String, defValue: String): String
    fun putString(key: String, value: String)

    fun getInt(key: String, defValue: Int): Int
    fun putInt(key: String, value: Int)

    fun getBoolean(key: String, defValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)

    fun remove(key: String)

    fun contains(key: String): Boolean

    fun clear()
}
