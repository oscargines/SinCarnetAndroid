package com.oscar.sincarnet.data.repository

import com.oscar.sincarnet.data.PlatformStorage
import com.oscar.sincarnet.domain.model.JuzgadoAtestadoData

class JuzgadoAtestadoStorage(private val storage: PlatformStorage) {

    fun loadCurrent(): JuzgadoAtestadoData = JuzgadoAtestadoData(
        ccaaId = if (storage.contains(KEY_CCAA_ID)) storage.getInt(KEY_CCAA_ID, 0) else null,
        ccaaNombre = storage.getString(KEY_CCAA_NOMBRE, ""),
        provinciaId = if (storage.contains(KEY_PROVINCIA_ID)) storage.getInt(KEY_PROVINCIA_ID, 0) else null,
        provinciaNombre = storage.getString(KEY_PROVINCIA_NOMBRE, ""),
        municipioNombre = storage.getString(KEY_MUNICIPIO_NOMBRE, ""),
        sedeId = if (storage.contains(KEY_SEDE_ID)) storage.getInt(KEY_SEDE_ID, 0) else null,
        sedeNombre = storage.getString(KEY_SEDE_NOMBRE, ""),
        sedeDireccion = storage.getString(KEY_SEDE_DIRECCION, ""),
        sedeTelefono = storage.getString(KEY_SEDE_TELEFONO, ""),
        sedeCodigoPostal = storage.getString(KEY_SEDE_CP, ""),
        numeroDiligencias = storage.getString(KEY_NUM_DILIGENCIAS, ""),
        tipoJuicio = storage.getString(KEY_TIPO_JUICIO, ""),
        fechaJuicioRapido = storage.getString(KEY_FECHA_RAPIDO, ""),
        horaJuicioRapido = storage.getString(KEY_HORA_RAPIDO, "")
    )

    fun saveCurrent(data: JuzgadoAtestadoData) {
        putIntOrRemove(KEY_CCAA_ID, data.ccaaId)
        storage.putString(KEY_CCAA_NOMBRE, data.ccaaNombre)
        putIntOrRemove(KEY_PROVINCIA_ID, data.provinciaId)
        storage.putString(KEY_PROVINCIA_NOMBRE, data.provinciaNombre)
        storage.putString(KEY_MUNICIPIO_NOMBRE, data.municipioNombre)
        putIntOrRemove(KEY_SEDE_ID, data.sedeId)
        storage.putString(KEY_SEDE_NOMBRE, data.sedeNombre)
        storage.putString(KEY_SEDE_DIRECCION, data.sedeDireccion)
        storage.putString(KEY_SEDE_TELEFONO, data.sedeTelefono)
        storage.putString(KEY_SEDE_CP, data.sedeCodigoPostal)
        storage.putString(KEY_NUM_DILIGENCIAS, data.numeroDiligencias)
        storage.putString(KEY_TIPO_JUICIO, data.tipoJuicio)
        storage.putString(KEY_FECHA_RAPIDO, data.fechaJuicioRapido)
        storage.putString(KEY_HORA_RAPIDO, data.horaJuicioRapido)
    }

    fun clearCurrent() {
        storage.remove(KEY_CCAA_ID)
        storage.remove(KEY_CCAA_NOMBRE)
        storage.remove(KEY_PROVINCIA_ID)
        storage.remove(KEY_PROVINCIA_NOMBRE)
        storage.remove(KEY_MUNICIPIO_NOMBRE)
        storage.remove(KEY_SEDE_ID)
        storage.remove(KEY_SEDE_NOMBRE)
        storage.remove(KEY_SEDE_DIRECCION)
        storage.remove(KEY_SEDE_TELEFONO)
        storage.remove(KEY_SEDE_CP)
        storage.remove(KEY_NUM_DILIGENCIAS)
        storage.remove(KEY_TIPO_JUICIO)
        storage.remove(KEY_FECHA_RAPIDO)
        storage.remove(KEY_HORA_RAPIDO)
    }

    private fun putIntOrRemove(key: String, value: Int?) {
        if (value != null) storage.putInt(key, value) else storage.remove(key)
    }

    private companion object {
        const val KEY_CCAA_ID = "ccaa_id"
        const val KEY_CCAA_NOMBRE = "ccaa_nombre"
        const val KEY_PROVINCIA_ID = "provincia_id"
        const val KEY_PROVINCIA_NOMBRE = "provincia_nombre"
        const val KEY_MUNICIPIO_NOMBRE = "municipio_nombre"
        const val KEY_SEDE_ID = "sede_id"
        const val KEY_SEDE_NOMBRE = "sede_nombre"
        const val KEY_SEDE_DIRECCION = "sede_direccion"
        const val KEY_SEDE_TELEFONO = "sede_telefono"
        const val KEY_SEDE_CP = "sede_codigo_postal"
        const val KEY_NUM_DILIGENCIAS = "num_diligencias"
        const val KEY_TIPO_JUICIO = "tipo_juicio"
        const val KEY_FECHA_RAPIDO = "fecha_juicio_rapido"
        const val KEY_HORA_RAPIDO = "hora_juicio_rapido"
    }
}
