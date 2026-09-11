package com.oscar.sincarnet.data.repository

import com.oscar.sincarnet.data.PlatformStorage
import com.oscar.sincarnet.domain.model.ActaTrasladoCentroSanitarioData

/**
 * Persistencia local del acta de traslado a Centro Sanitario.
 */
class ActaTrasladoCentroSanitarioStorage(private val storage: PlatformStorage) {

    fun loadCurrent(): ActaTrasladoCentroSanitarioData = ActaTrasladoCentroSanitarioData(
        subsector = storage.getString(KEY_SUBSECTOR, ""),
        unidadInterviniente = storage.getString(KEY_UNIDAD_INTERVINIENTE, ""),
        numeroExpediente = storage.getString(KEY_NUMERO_EXPEDIENTE, ""),
        lugarIntervencion = storage.getString(KEY_LUGAR_INTERVENCION, ""),
        motivo = storage.getString(KEY_MOTIVO, ""),
        fechaHora = storage.getString(KEY_FECHA_HORA, ""),
        nombreApellidos = storage.getString(KEY_NOMBRE_APELLIDOS, ""),
        dniNiePasaporte = storage.getString(KEY_DNI_NIE_PASAPORTE, ""),
        facultativoColegiado = storage.getString(KEY_FACULTATIVO_COLEGIADO, ""),
        pruebaSangre = storage.getBoolean(KEY_PRUEBA_SANGRE, false),
        pruebaOtroTipo = storage.getBoolean(KEY_PRUEBA_OTRO_TIPO, false),
        pruebaOtroDetalle = storage.getString(KEY_PRUEBA_OTRO_DETALLE, ""),
        centroSanitario = storage.getString(KEY_CENTRO_SANITARIO, ""),
        extraccionColegiado = storage.getString(KEY_EXTRACCION_COLEGIADO, ""),
        fechaHoraExtraccion = storage.getString(KEY_FECHA_HORA_EXTRACCION, ""),
        precintosSeguridad = storage.getString(KEY_PRECINTOS_SEGURIDAD, "PRECINTOS DE SEGURIDAD"),
        datoMuestra = storage.getString(KEY_DATO_MUESTRA, ""),
        juzgadoOrganismo = storage.getString(KEY_JUZGADO_ORGANISMO, ""),
        otrasObservaciones = storage.getString(KEY_OTRAS_OBSERVACIONES, ""),
        numTip = storage.getString(KEY_NUM_TIP, "")
    )

    fun saveCurrent(data: ActaTrasladoCentroSanitarioData) {
        storage.putString(KEY_SUBSECTOR, data.subsector)
        storage.putString(KEY_UNIDAD_INTERVINIENTE, data.unidadInterviniente)
        storage.putString(KEY_NUMERO_EXPEDIENTE, data.numeroExpediente)
        storage.putString(KEY_LUGAR_INTERVENCION, data.lugarIntervencion)
        storage.putString(KEY_MOTIVO, data.motivo)
        storage.putString(KEY_FECHA_HORA, data.fechaHora)
        storage.putString(KEY_NOMBRE_APELLIDOS, data.nombreApellidos)
        storage.putString(KEY_DNI_NIE_PASAPORTE, data.dniNiePasaporte)
        storage.putString(KEY_FACULTATIVO_COLEGIADO, data.facultativoColegiado)
        storage.putBoolean(KEY_PRUEBA_SANGRE, data.pruebaSangre)
        storage.putBoolean(KEY_PRUEBA_OTRO_TIPO, data.pruebaOtroTipo)
        storage.putString(KEY_PRUEBA_OTRO_DETALLE, data.pruebaOtroDetalle)
        storage.putString(KEY_CENTRO_SANITARIO, data.centroSanitario)
        storage.putString(KEY_EXTRACCION_COLEGIADO, data.extraccionColegiado)
        storage.putString(KEY_FECHA_HORA_EXTRACCION, data.fechaHoraExtraccion)
        storage.putString(KEY_PRECINTOS_SEGURIDAD, data.precintosSeguridad)
        storage.putString(KEY_DATO_MUESTRA, data.datoMuestra)
        storage.putString(KEY_JUZGADO_ORGANISMO, data.juzgadoOrganismo)
        storage.putString(KEY_OTRAS_OBSERVACIONES, data.otrasObservaciones)
        storage.putString(KEY_NUM_TIP, data.numTip)
    }

    fun clearCurrent() {
        ALL_KEYS.forEach(storage::remove)
    }

    private companion object {
        const val KEY_SUBSECTOR = "subsector"
        const val KEY_UNIDAD_INTERVINIENTE = "unidad_interviniente"
        const val KEY_NUMERO_EXPEDIENTE = "numero_expediente"
        const val KEY_LUGAR_INTERVENCION = "lugar_intervencion"
        const val KEY_MOTIVO = "motivo"
        const val KEY_FECHA_HORA = "fecha_hora"
        const val KEY_NOMBRE_APELLIDOS = "nombre_apellidos"
        const val KEY_DNI_NIE_PASAPORTE = "dni_nie_pasaporte"
        const val KEY_FACULTATIVO_COLEGIADO = "facultativo_colegiado"
        const val KEY_PRUEBA_SANGRE = "prueba_sangre"
        const val KEY_PRUEBA_OTRO_TIPO = "prueba_otro_tipo"
        const val KEY_PRUEBA_OTRO_DETALLE = "prueba_otro_detalle"
        const val KEY_CENTRO_SANITARIO = "centro_sanitario"
        const val KEY_EXTRACCION_COLEGIADO = "extraccion_colegiado"
        const val KEY_FECHA_HORA_EXTRACCION = "fecha_hora_extraccion"
        const val KEY_PRECINTOS_SEGURIDAD = "precintos_seguridad"
        const val KEY_DATO_MUESTRA = "dato_muestra"
        const val KEY_JUZGADO_ORGANISMO = "juzgado_organismo"
        const val KEY_OTRAS_OBSERVACIONES = "otras_observaciones"
        const val KEY_NUM_TIP = "num_tip"

        val ALL_KEYS = listOf(
            KEY_SUBSECTOR,
            KEY_UNIDAD_INTERVINIENTE,
            KEY_NUMERO_EXPEDIENTE,
            KEY_LUGAR_INTERVENCION,
            KEY_MOTIVO,
            KEY_FECHA_HORA,
            KEY_NOMBRE_APELLIDOS,
            KEY_DNI_NIE_PASAPORTE,
            KEY_FACULTATIVO_COLEGIADO,
            KEY_PRUEBA_SANGRE,
            KEY_PRUEBA_OTRO_TIPO,
            KEY_PRUEBA_OTRO_DETALLE,
            KEY_CENTRO_SANITARIO,
            KEY_EXTRACCION_COLEGIADO,
            KEY_FECHA_HORA_EXTRACCION,
            KEY_PRECINTOS_SEGURIDAD,
            KEY_DATO_MUESTRA,
            KEY_JUZGADO_ORGANISMO,
            KEY_OTRAS_OBSERVACIONES,
            KEY_NUM_TIP
        )
    }
}
