package com.oscar.sincarnet

import android.content.Context

/**
 * Modelo que almacena datos sobre la manifestación del investigado.
 *
 * Incluye su decisión de renunciar a asistencia letrada, desear declarar,
 * y las respuestas a cada una de las 8 preguntas formuladas.
 *
 * @property renunciaAsistenciaLetrada true si renuncia a asistencia letrada, false si la acepta, null si no se responde.
 * @property deseaDeclarar true si desea declarar, false si no, null si no se responde.
 * @property respuestasPreguntas Mapa de ID de pregunta (1-8) a respuesta de texto.
 */
internal data class ManifestacionData(
    val renunciaAsistenciaLetrada: Boolean? = null,
    val deseaDeclarar: Boolean? = null,
    val respuestasPreguntas: Map<Int, String> = emptyMap()
)

/**
 * Gestor de almacenamiento persistente para datos de manifestación del investigado.
 *
 * @constructor Crea un nuevo gestor de almacenamiento.
 * @param context Contexto de la aplicación.
 */
internal class ManifestacionStorage(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Carga los datos actuales de manifestación.
     *
     * @return Estructura [ManifestacionData] con datos almacenados o valores por defecto.
     */
    fun loadCurrent(): ManifestacionData {
        val respuestas = buildMap {
            for (questionId in QUESTION_IDS) {
                put(questionId, prefs.getString(questionKey(questionId), "").orEmpty())
            }
        }
        return ManifestacionData(
            renunciaAsistenciaLetrada = prefs.getNullableBoolean(KEY_RENUNCIA_ASISTENCIA_LETRADA),
            deseaDeclarar = prefs.getNullableBoolean(KEY_DESEA_DECLARAR),
            respuestasPreguntas = respuestas
        )
    }

    /**
     * Guarda los datos actuales de manifestación.
     *
     * @param data Estructura [ManifestacionData] a guardar.
     */
    fun saveCurrent(data: ManifestacionData) {
        prefs.edit()
            .putBooleanOrRemove(KEY_RENUNCIA_ASISTENCIA_LETRADA, data.renunciaAsistenciaLetrada)
            .putBooleanOrRemove(KEY_DESEA_DECLARAR, data.deseaDeclarar)
            .apply {
                data.respuestasPreguntas.forEach { (questionId, respuesta) ->
                    putString(questionKey(questionId), respuesta)
                }
            }
            .apply()
    }

    /**
     * Limpia todos los datos de manifestación.
     */
    fun clearCurrent() {
        prefs.edit()
            .remove(KEY_RENUNCIA_ASISTENCIA_LETRADA)
            .remove(KEY_DESEA_DECLARAR)
            .apply {
                QUESTION_IDS.forEach { remove(questionKey(it)) }
            }
            .apply()
    }

    private fun android.content.SharedPreferences.getNullableBoolean(key: String): Boolean? {
        return if (contains(key)) getBoolean(key, false) else null
    }

    private fun android.content.SharedPreferences.Editor.putBooleanOrRemove(
        key: String,
        value: Boolean?
    ): android.content.SharedPreferences.Editor {
        return if (value == null) remove(key) else putBoolean(key, value)
    }

    private companion object {
        const val PREFS_NAME = "manifestacion_storage"
        const val KEY_RENUNCIA_ASISTENCIA_LETRADA = "renuncia_asistencia_letrada"
        const val KEY_DESEA_DECLARAR = "desea_declarar"
        val QUESTION_IDS = 1..8

        fun questionKey(questionId: Int) = "pregunta_$questionId"
    }
}

