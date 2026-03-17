package com.oscar.sincarnet

import android.content.Context

internal data class JuzgadoAtestadoData(
    val ccaaId: Int? = null,
    val ccaaNombre: String = "",
    val provinciaId: Int? = null,
    val provinciaNombre: String = "",
    val municipioNombre: String = "",
    val sedeId: Int? = null,
    val sedeNombre: String = "",
    val sedeDireccion: String = "",
    val sedeTelefono: String = "",
    val sedeCodigoPostal: String = "",
    val numeroDiligencias: String = "",
    val tipoJuicio: String = "",
    val fechaJuicioRapido: String = "",
    val horaJuicioRapido: String = ""
)

internal class JuzgadoAtestadoStorage(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadCurrent(): JuzgadoAtestadoData = JuzgadoAtestadoData(
        ccaaId = prefs.takeIf { it.contains(KEY_CCAA_ID) }?.getInt(KEY_CCAA_ID, 0),
        ccaaNombre = prefs.getString(KEY_CCAA_NOMBRE, "").orEmpty(),
        provinciaId = prefs.takeIf { it.contains(KEY_PROVINCIA_ID) }?.getInt(KEY_PROVINCIA_ID, 0),
        provinciaNombre = prefs.getString(KEY_PROVINCIA_NOMBRE, "").orEmpty(),
        municipioNombre = prefs.getString(KEY_MUNICIPIO_NOMBRE, "").orEmpty(),
        sedeId = prefs.takeIf { it.contains(KEY_SEDE_ID) }?.getInt(KEY_SEDE_ID, 0),
        sedeNombre = prefs.getString(KEY_SEDE_NOMBRE, "").orEmpty(),
        sedeDireccion = prefs.getString(KEY_SEDE_DIRECCION, "").orEmpty(),
        sedeTelefono = prefs.getString(KEY_SEDE_TELEFONO, "").orEmpty(),
        sedeCodigoPostal = prefs.getString(KEY_SEDE_CP, "").orEmpty(),
        numeroDiligencias = prefs.getString(KEY_NUM_DILIGENCIAS, "").orEmpty(),
        tipoJuicio = prefs.getString(KEY_TIPO_JUICIO, "").orEmpty(),
        fechaJuicioRapido = prefs.getString(KEY_FECHA_RAPIDO, "").orEmpty(),
        horaJuicioRapido = prefs.getString(KEY_HORA_RAPIDO, "").orEmpty()
    )

    fun saveCurrent(data: JuzgadoAtestadoData) {
        prefs.edit()
            .putIntOrRemove(KEY_CCAA_ID, data.ccaaId)
            .putString(KEY_CCAA_NOMBRE, data.ccaaNombre)
            .putIntOrRemove(KEY_PROVINCIA_ID, data.provinciaId)
            .putString(KEY_PROVINCIA_NOMBRE, data.provinciaNombre)
            .putString(KEY_MUNICIPIO_NOMBRE, data.municipioNombre)
            .putIntOrRemove(KEY_SEDE_ID, data.sedeId)
            .putString(KEY_SEDE_NOMBRE, data.sedeNombre)
            .putString(KEY_SEDE_DIRECCION, data.sedeDireccion)
            .putString(KEY_SEDE_TELEFONO, data.sedeTelefono)
            .putString(KEY_SEDE_CP, data.sedeCodigoPostal)
            .putString(KEY_NUM_DILIGENCIAS, data.numeroDiligencias)
            .putString(KEY_TIPO_JUICIO, data.tipoJuicio)
            .putString(KEY_FECHA_RAPIDO, data.fechaJuicioRapido)
            .putString(KEY_HORA_RAPIDO, data.horaJuicioRapido)
            .apply()
    }

    fun clearCurrent() {
        prefs.edit()
            .remove(KEY_CCAA_ID)
            .remove(KEY_CCAA_NOMBRE)
            .remove(KEY_PROVINCIA_ID)
            .remove(KEY_PROVINCIA_NOMBRE)
            .remove(KEY_MUNICIPIO_NOMBRE)
            .remove(KEY_SEDE_ID)
            .remove(KEY_SEDE_NOMBRE)
            .remove(KEY_SEDE_DIRECCION)
            .remove(KEY_SEDE_TELEFONO)
            .remove(KEY_SEDE_CP)
            .remove(KEY_NUM_DILIGENCIAS)
            .remove(KEY_TIPO_JUICIO)
            .remove(KEY_FECHA_RAPIDO)
            .remove(KEY_HORA_RAPIDO)
            .apply()
    }

    private fun android.content.SharedPreferences.Editor.putIntOrRemove(
        key: String,
        value: Int?
    ): android.content.SharedPreferences.Editor {
        return if (value != null) putInt(key, value) else remove(key)
    }

    private companion object {
        const val PREFS_NAME = "juzgado_atestado_storage"
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

