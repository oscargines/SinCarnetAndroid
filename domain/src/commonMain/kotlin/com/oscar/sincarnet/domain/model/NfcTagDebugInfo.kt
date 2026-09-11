package com.oscar.sincarnet.domain.model

data class NfcTagDebugInfo(
    val hasTag: Boolean,
    val uid: String,
    val techList: String,
    val ageMs: Long,
    val capturedAtMillis: Long
)