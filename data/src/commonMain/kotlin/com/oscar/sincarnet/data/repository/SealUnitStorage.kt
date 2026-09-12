package com.oscar.sincarnet.data.repository

import com.oscar.sincarnet.data.PlatformStorage

/**
 * Persistencia del texto de unidad confirmado para el sello institucional.
 *
 * El valor se solicita y confirma al usuario cada vez que se genera el
 * atestado completo en PDF, y se reutiliza como valor inicial del campo en
 * generaciones posteriores.
 */
class SealUnitStorage(private val storage: PlatformStorage) {

    /** @return Texto de unidad del sello persistido, o cadena vacía si no existe. */
    fun loadSealUnitText(): String = storage.getString(KEY_SEAL_UNIT_TEXT, "")

    /** Persiste el texto de unidad confirmado para el sello. */
    fun saveSealUnitText(value: String) {
        storage.putString(KEY_SEAL_UNIT_TEXT, value.trim())
    }

    private companion object {
        const val KEY_SEAL_UNIT_TEXT = "seal_unit_text"
    }
}
