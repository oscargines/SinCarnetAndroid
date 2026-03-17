package com.oscar.sincarnet

import android.content.Context

internal data class OcurrenciaDelitData(
    val carretera: String = "",
    val pk: String = "",
    val localidad: String = "",
    val terminoMunicipal: String = "",
    val fecha: String = "",
    val hora: String = ""
)

internal class OcurrenciaDelitStorage(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadCurrent(): OcurrenciaDelitData = OcurrenciaDelitData(
        carretera = prefs.getString(KEY_CARRETERA, "").orEmpty(),
        pk = prefs.getString(KEY_PK, "").orEmpty(),
        localidad = prefs.getString(KEY_LOCALIDAD, "").orEmpty(),
        terminoMunicipal = prefs.getString(KEY_TERMINO_MUNICIPAL, "").orEmpty(),
        fecha = prefs.getString(KEY_FECHA, "").orEmpty(),
        hora = prefs.getString(KEY_HORA, "").orEmpty()
    )

    fun saveCurrent(data: OcurrenciaDelitData) {
        prefs.edit()
            .putString(KEY_CARRETERA, data.carretera)
            .putString(KEY_PK, data.pk)
            .putString(KEY_LOCALIDAD, data.localidad)
            .putString(KEY_TERMINO_MUNICIPAL, data.terminoMunicipal)
            .putString(KEY_FECHA, data.fecha)
            .putString(KEY_HORA, data.hora)
            .apply()
    }

    fun clearCurrent() {
        prefs.edit()
            .remove(KEY_CARRETERA)
            .remove(KEY_PK)
            .remove(KEY_LOCALIDAD)
            .remove(KEY_TERMINO_MUNICIPAL)
            .remove(KEY_FECHA)
            .remove(KEY_HORA)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "ocurrencia_delit_storage"
        const val KEY_CARRETERA = "carretera"
        const val KEY_PK = "pk"
        const val KEY_LOCALIDAD = "localidad"
        const val KEY_TERMINO_MUNICIPAL = "termino_municipal"
        const val KEY_FECHA = "fecha"
        const val KEY_HORA = "hora"
    }
}

