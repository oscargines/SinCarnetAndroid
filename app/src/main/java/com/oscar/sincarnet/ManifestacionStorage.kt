package com.oscar.sincarnet

import android.content.Context

internal data class ManifestacionData(
    val renunciaAsistenciaLetrada: Boolean? = null,
    val deseaDeclarar: Boolean? = null,
    val respuestasPreguntas: Map<Int, String> = emptyMap()
)

internal class ManifestacionStorage(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

