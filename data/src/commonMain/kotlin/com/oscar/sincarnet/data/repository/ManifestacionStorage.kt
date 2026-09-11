package com.oscar.sincarnet.data.repository

import com.oscar.sincarnet.data.PlatformStorage
import com.oscar.sincarnet.domain.model.ManifestacionData

class ManifestacionStorage(private val storage: PlatformStorage) {

    fun loadCurrent(): ManifestacionData {
        val respuestas = buildMap {
            for (questionId in QUESTION_IDS) {
                put(questionId, storage.getString(questionKey(questionId), ""))
            }
        }
        return ManifestacionData(
            renunciaAsistenciaLetrada = getNullableBoolean(KEY_RENUNCIA_ASISTENCIA_LETRADA),
            deseaDeclarar = getNullableBoolean(KEY_DESEA_DECLARAR),
            respuestasPreguntas = respuestas
        )
    }

    fun saveCurrent(data: ManifestacionData) {
        putBooleanOrRemove(KEY_RENUNCIA_ASISTENCIA_LETRADA, data.renunciaAsistenciaLetrada)
        putBooleanOrRemove(KEY_DESEA_DECLARAR, data.deseaDeclarar)
        data.respuestasPreguntas.forEach { (questionId, respuesta) ->
            storage.putString(questionKey(questionId), respuesta)
        }
    }

    fun clearCurrent() {
        storage.remove(KEY_RENUNCIA_ASISTENCIA_LETRADA)
        storage.remove(KEY_DESEA_DECLARAR)
        QUESTION_IDS.forEach { storage.remove(questionKey(it)) }
    }

    private fun getNullableBoolean(key: String): Boolean? {
        return if (storage.contains(key)) storage.getBoolean(key, false) else null
    }

    private fun putBooleanOrRemove(key: String, value: Boolean?) {
        if (value == null) storage.remove(key) else storage.putBoolean(key, value)
    }

    private companion object {
        const val KEY_RENUNCIA_ASISTENCIA_LETRADA = "renuncia_asistencia_letrada"
        const val KEY_DESEA_DECLARAR = "desea_declarar"
        val QUESTION_IDS = 1..8

        fun questionKey(questionId: Int) = "pregunta_$questionId"
    }
}
