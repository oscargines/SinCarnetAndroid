package com.oscar.sincarnet

import android.nfc.Tag
import android.util.Log
import java.util.concurrent.atomic.AtomicReference

internal data class NfcTagDebugInfo(
    val hasTag: Boolean,
    val uid: String,
    val techList: String,
    val ageMs: Long,
    val capturedAtMillis: Long
)

internal object NfcTagRepository {
    private const val TAG = "NfcTagRepo"
    private val latestTagRef = AtomicReference<Tag?>(null)
    private val latestTagMillisRef = AtomicReference<Long>(0L)

    fun update(tag: Tag?) {
        if (tag != null) {
            val now = System.currentTimeMillis()
            latestTagRef.set(tag)
            latestTagMillisRef.set(now)
            val uid = tag.id?.joinToString(":") { "%02X".format(it) }.orEmpty()
            Log.i(TAG, "update() uid=$uid techs=${tag.techList.joinToString()} timestamp=$now")
        } else {
            Log.w(TAG, "update() recibido tag=null (ignorado)")
        }
    }

    fun getLatest(): Tag? {
        val tag = latestTagRef.get()
        val info = debugInfo()
        Log.d(TAG, "getLatest() hasTag=${info.hasTag} uid=${info.uid} ageMs=${info.ageMs} capturedAt=${info.capturedAtMillis} techs=${info.techList}")
        return tag
    }

    fun clear() {
        latestTagRef.set(null)
        latestTagMillisRef.set(0L)
        Log.i(TAG, "clear() repositorio NFC reiniciado")
    }

    fun debugInfo(): NfcTagDebugInfo {
        val tag = latestTagRef.get()
        val ts = latestTagMillisRef.get()
        val now = System.currentTimeMillis()
        val uid = tag?.id?.joinToString(":") { "%02X".format(it) }.orEmpty()
        val techs = tag?.techList?.joinToString().orEmpty()
        val age = if (ts > 0L) now - ts else -1L
        return NfcTagDebugInfo(
            hasTag = tag != null,
            uid = uid,
            techList = techs,
            ageMs = age,
            capturedAtMillis = ts
        )
    }
}

