package com.oscar.sincarnet

import android.content.Context

/**
 * Modelo que almacena los datos iniciales/modales de un atestado.
 *
 * Contiene información sobre:
 * - Motivo de la infracción
 * - Norma y artículo infringidos
 * - Estado de antecedentes (DGT, internacionales)
 * - Vicisitudes/circunstancias especiales
 * - Jefatura provincial responsable
 * - Tiempo de privación/suspensión
 * - Juzgado que decreta la privación
 *
 * @property motivo Motivo de la denuncia o infracción (p. ej. "Siniestro Vial", "Control de Tráfico").
 * @property norma Norma incumplida (p. ej. "LSV", "LTSV").
 * @property articulo Artículo de la norma (p. ej. "381 CP").
 * @property dgtNoRecord true si no hay antecedentes en DGT.
 * @property internationalNoRecord true si no hay antecedentes internacionales.
 * @property existsRecord true si existe registro previo.
 * @property vicisitudesOption Tipo de vicisitud/circunstancia (p. ej. "Desconocido", "Enfermedad").
 * @property jefaturaProvincial Jefatura provincial responsable.
 * @property tiempoPrivacion Tiempo de privación de conducir (p. ej. "3 meses").
 * @property juzgadoDecreta Juzgado que decreta la privación.
 */
internal data class AtestadoInicioModalData(
    val motivo: String = "",
    val norma: String = "",
    val articulo: String = "",
    val dgtNoRecord: Boolean = false,
    val internationalNoRecord: Boolean = false,
    val existsRecord: Boolean = false,
    val vicisitudesOption: String = "",
    val jefaturaProvincial: String = "",
    val tiempoPrivacion: String = "",
    val juzgadoDecreta: String = ""
)

/**
 * Gestor de almacenamiento persistente para datos iniciales/modales del atestado.
 *
 * @constructor Crea un nuevo gestor de almacenamiento.
 * @param context Contexto de la aplicación.
 */
internal class AtestadoInicioStorage(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Carga los datos actuales del modal inicial del atestado.
     *
     * @return Estructura [AtestadoInicioModalData] con datos almacenados o valores por defecto.
     */
    fun loadCurrent(): AtestadoInicioModalData = AtestadoInicioModalData(
        motivo = prefs.getString(KEY_MOTIVO, "").orEmpty(),
        norma = prefs.getString(KEY_NORMA, "").orEmpty(),
        articulo = prefs.getString(KEY_ARTICULO, "").orEmpty(),
        dgtNoRecord = prefs.getBoolean(KEY_DGT_NO_RECORD, false),
        internationalNoRecord = prefs.getBoolean(KEY_INTERNATIONAL_NO_RECORD, false),
        existsRecord = prefs.getBoolean(KEY_EXISTS_RECORD, false),
        vicisitudesOption = prefs.getString(KEY_VICISITUDES_OPTION, "").orEmpty(),
        jefaturaProvincial = prefs.getString(KEY_JEFATURA_PROVINCIAL, "").orEmpty(),
        tiempoPrivacion = prefs.getString(KEY_TIEMPO_PRIVACION, "").orEmpty(),
        juzgadoDecreta = prefs.getString(KEY_JUZGADO_DECRETA, "").orEmpty()
    )

    /**
     * Guarda los datos del modal inicial del atestado.
     *
     * @param data Estructura [AtestadoInicioModalData] a guardar.
     */
    fun saveCurrent(data: AtestadoInicioModalData) {
        prefs.edit()
            .putString(KEY_MOTIVO, data.motivo)
            .putString(KEY_NORMA, data.norma)
            .putString(KEY_ARTICULO, data.articulo)
            .putBoolean(KEY_DGT_NO_RECORD, data.dgtNoRecord)
            .putBoolean(KEY_INTERNATIONAL_NO_RECORD, data.internationalNoRecord)
            .putBoolean(KEY_EXISTS_RECORD, data.existsRecord)
            .putString(KEY_VICISITUDES_OPTION, data.vicisitudesOption)
            .putString(KEY_JEFATURA_PROVINCIAL, data.jefaturaProvincial)
            .putString(KEY_TIEMPO_PRIVACION, data.tiempoPrivacion)
            .putString(KEY_JUZGADO_DECRETA, data.juzgadoDecreta)
            .commit()
    }

    /**
     * Limpia todos los datos del modal inicial del atestado.
     */
    fun clearCurrent() {
        prefs.edit()
            .remove(KEY_MOTIVO)
            .remove(KEY_NORMA)
            .remove(KEY_ARTICULO)
            .remove(KEY_DGT_NO_RECORD)
            .remove(KEY_INTERNATIONAL_NO_RECORD)
            .remove(KEY_EXISTS_RECORD)
            .remove(KEY_VICISITUDES_OPTION)
            .remove(KEY_JEFATURA_PROVINCIAL)
            .remove(KEY_TIEMPO_PRIVACION)
            .remove(KEY_JUZGADO_DECRETA)
            .commit()
    }

    private companion object {
        const val PREFS_NAME = "atestado_inicio_storage"
        const val KEY_MOTIVO = "motivo"
        const val KEY_NORMA = "norma"
        const val KEY_ARTICULO = "articulo"
        const val KEY_DGT_NO_RECORD = "dgt_no_record"
        const val KEY_INTERNATIONAL_NO_RECORD = "international_no_record"
        const val KEY_EXISTS_RECORD = "exists_record"
        const val KEY_VICISITUDES_OPTION = "vicisitudes_option"
        const val KEY_JEFATURA_PROVINCIAL = "jefatura_provincial"
        const val KEY_TIEMPO_PRIVACION = "tiempo_privacion"
        const val KEY_JUZGADO_DECRETA = "juzgado_decreta"
    }
}
