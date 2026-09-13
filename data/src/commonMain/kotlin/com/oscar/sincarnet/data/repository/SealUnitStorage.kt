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

    /** Indica si el sello debe estamparse en el atestado completo. */
    fun loadSealEnabled(): Boolean = storage.getBoolean(KEY_SEAL_ENABLED, true)

    /** Persiste la visibilidad del sello para futuras generaciones. */
    fun saveSealEnabled(value: Boolean) {
        storage.putBoolean(KEY_SEAL_ENABLED, value)
    }

    private companion object {
        const val KEY_SEAL_UNIT_TEXT = "seal_unit_text"
        const val KEY_SEAL_ENABLED = "seal_enabled"
    }
}
