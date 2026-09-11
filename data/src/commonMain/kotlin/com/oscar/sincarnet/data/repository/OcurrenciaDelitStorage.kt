package com.oscar.sincarnet.data.repository

import com.oscar.sincarnet.data.PlatformStorage
import com.oscar.sincarnet.domain.model.OcurrenciaDelitData

class OcurrenciaDelitStorage(private val storage: PlatformStorage) {

    fun loadCurrent(): OcurrenciaDelitData = OcurrenciaDelitData(
        carretera = storage.getString(KEY_CARRETERA, ""),
        pk = storage.getString(KEY_PK, ""),
        localidad = storage.getString(KEY_LOCALIDAD, ""),
        provincia = storage.getString(KEY_PROVINCIA, ""),
        terminoMunicipal = storage.getString(KEY_TERMINO_MUNICIPAL, ""),
        fecha = storage.getString(KEY_FECHA, ""),
        hora = storage.getString(KEY_HORA, ""),
        derechosInformacionMomento = storage.getString(KEY_DERECHOS_INFORMACION_MOMENTO, "")
    )

    fun saveCurrent(data: OcurrenciaDelitData) {
        storage.putString(KEY_CARRETERA, data.carretera)
        storage.putString(KEY_PK, data.pk)
        storage.putString(KEY_LOCALIDAD, data.localidad)
        storage.putString(KEY_PROVINCIA, data.provincia)
        storage.putString(KEY_TERMINO_MUNICIPAL, data.terminoMunicipal)
        storage.putString(KEY_FECHA, data.fecha)
        storage.putString(KEY_HORA, data.hora)
        storage.putString(KEY_DERECHOS_INFORMACION_MOMENTO, data.derechosInformacionMomento)
    }

    fun clearCurrent() {
        storage.remove(KEY_CARRETERA)
        storage.remove(KEY_PK)
        storage.remove(KEY_LOCALIDAD)
        storage.remove(KEY_PROVINCIA)
        storage.remove(KEY_TERMINO_MUNICIPAL)
        storage.remove(KEY_FECHA)
        storage.remove(KEY_HORA)
        storage.remove(KEY_DERECHOS_INFORMACION_MOMENTO)
    }

    private companion object {
        const val KEY_CARRETERA = "carretera"
        const val KEY_PK = "pk"
        const val KEY_LOCALIDAD = "localidad"
        const val KEY_PROVINCIA = "provincia"
        const val KEY_TERMINO_MUNICIPAL = "termino_municipal"
        const val KEY_FECHA = "fecha"
        const val KEY_HORA = "hora"
        const val KEY_DERECHOS_INFORMACION_MOMENTO = "derechos_informacion_momento"
    }
}
