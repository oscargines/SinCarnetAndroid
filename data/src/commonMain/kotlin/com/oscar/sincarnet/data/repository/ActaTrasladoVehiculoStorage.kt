package com.oscar.sincarnet.data.repository

import com.oscar.sincarnet.data.PlatformStorage
import com.oscar.sincarnet.domain.model.ActaTrasladoVehiculoData

/**
 * Persistencia local del acta de traslado de vehículo.
 */
class ActaTrasladoVehiculoStorage(private val storage: PlatformStorage) {

    fun loadCurrent(): ActaTrasladoVehiculoData = ActaTrasladoVehiculoData(
        sector = storage.getString(KEY_SECTOR, ""),
        subsector = storage.getString(KEY_SUBSECTOR, ""),
        destacamento = storage.getString(KEY_DESTACAMENTO, ""),
        vehiculoTipo = storage.getString(KEY_VEHICULO_TIPO, ""),
        vehiculoMarca = storage.getString(KEY_VEHICULO_MARCA, ""),
        vehiculoModelo = storage.getString(KEY_VEHICULO_MODELO, ""),
        vehiculoColor = storage.getString(KEY_VEHICULO_COLOR, ""),
        vehiculoMatricula = storage.getString(KEY_VEHICULO_MATRICULA, ""),
        esTitular = storage.getBoolean(KEY_ES_TITULAR, true),
        esConductor = storage.getBoolean(KEY_ES_CONDUCTOR, false),
        nombre = storage.getString(KEY_NOMBRE, ""),
        primerApellido = storage.getString(KEY_PRIMER_APELLIDO, ""),
        segundoApellido = storage.getString(KEY_SEGUNDO_APELLIDO, ""),
        dniNie = storage.getString(KEY_DNI_NIE, ""),
        telefono = storage.getString(KEY_TELEFONO, ""),
        inicioAuxilioAccidente = storage.getBoolean(KEY_INICIO_AUXILIO_ACCIDENTE, false),
        inicioInfraccion = storage.getBoolean(KEY_INICIO_INFRACCION, false),
        inicioOtroMotivo = storage.getString(KEY_INICIO_OTRO_MOTIVO, ""),
        circunstanciaFactoresAtmosfericos = storage.getBoolean(KEY_CIRC_FACTORES_ATMOSFERICOS, false),
        circunstanciaMalaVisibilidad = storage.getBoolean(KEY_CIRC_MALA_VISIBILIDAD, false),
        circunstanciaConfiguracionVia = storage.getString(KEY_CIRC_CONFIGURACION_VIA, ""),
        circunstanciaOtras = storage.getString(KEY_CIRC_OTRAS, ""),
        inicioLugar = storage.getString(KEY_INICIO_LUGAR, ""),
        inicioHora = storage.getString(KEY_INICIO_HORA, ""),
        inicioFecha = storage.getString(KEY_INICIO_FECHA, ""),
        finLugar = storage.getString(KEY_FIN_LUGAR, ""),
        finHora = storage.getString(KEY_FIN_HORA, ""),
        finFecha = storage.getString(KEY_FIN_FECHA, ""),
        unidadResponsable = storage.getString(KEY_UNIDAD_RESPONSABLE, ""),
        unidadTelefono = storage.getString(KEY_UNIDAD_TELEFONO, ""),
        agenteTip = storage.getString(KEY_AGENTE_TIP, ""),
        agenteUnidad = storage.getString(KEY_AGENTE_UNIDAD, ""),
        consentimientoTraslado = storage.getBooleanOrNull(KEY_CONSENTIMIENTO_TRASLADO),
        autorizaTitular = storage.getBoolean(KEY_AUTORIZA_TITULAR, true),
        autorizaConductor = storage.getBoolean(KEY_AUTORIZA_CONDUCTOR, false),
        firmanteAutorizaTraslado = storage.getBooleanOrNull(KEY_FIRMANTE_AUTORIZA_TRASLADO),
        incidenciaDuranteTraslado = storage.getBooleanOrNull(KEY_INCIDENCIA_DURANTE_TRASLADO),
        entregaLlaves = storage.getBooleanOrNull(KEY_ENTREGA_LLAVES)
    )

    fun saveCurrent(data: ActaTrasladoVehiculoData) {
        storage.putString(KEY_SECTOR, data.sector)
        storage.putString(KEY_SUBSECTOR, data.subsector)
        storage.putString(KEY_DESTACAMENTO, data.destacamento)
        storage.putString(KEY_VEHICULO_TIPO, data.vehiculoTipo)
        storage.putString(KEY_VEHICULO_MARCA, data.vehiculoMarca)
        storage.putString(KEY_VEHICULO_MODELO, data.vehiculoModelo)
        storage.putString(KEY_VEHICULO_COLOR, data.vehiculoColor)
        storage.putString(KEY_VEHICULO_MATRICULA, data.vehiculoMatricula)
        storage.putBoolean(KEY_ES_TITULAR, data.esTitular)
        storage.putBoolean(KEY_ES_CONDUCTOR, data.esConductor)
        storage.putString(KEY_NOMBRE, data.nombre)
        storage.putString(KEY_PRIMER_APELLIDO, data.primerApellido)
        storage.putString(KEY_SEGUNDO_APELLIDO, data.segundoApellido)
        storage.putString(KEY_DNI_NIE, data.dniNie)
        storage.putString(KEY_TELEFONO, data.telefono)
        storage.putBoolean(KEY_INICIO_AUXILIO_ACCIDENTE, data.inicioAuxilioAccidente)
        storage.putBoolean(KEY_INICIO_INFRACCION, data.inicioInfraccion)
        storage.putString(KEY_INICIO_OTRO_MOTIVO, data.inicioOtroMotivo)
        storage.putBoolean(KEY_CIRC_FACTORES_ATMOSFERICOS, data.circunstanciaFactoresAtmosfericos)
        storage.putBoolean(KEY_CIRC_MALA_VISIBILIDAD, data.circunstanciaMalaVisibilidad)
        storage.putString(KEY_CIRC_CONFIGURACION_VIA, data.circunstanciaConfiguracionVia)
        storage.putString(KEY_CIRC_OTRAS, data.circunstanciaOtras)
        storage.putString(KEY_INICIO_LUGAR, data.inicioLugar)
        storage.putString(KEY_INICIO_HORA, data.inicioHora)
        storage.putString(KEY_INICIO_FECHA, data.inicioFecha)
        storage.putString(KEY_FIN_LUGAR, data.finLugar)
        storage.putString(KEY_FIN_HORA, data.finHora)
        storage.putString(KEY_FIN_FECHA, data.finFecha)
        storage.putString(KEY_UNIDAD_RESPONSABLE, data.unidadResponsable)
        storage.putString(KEY_UNIDAD_TELEFONO, data.unidadTelefono)
        storage.putString(KEY_AGENTE_TIP, data.agenteTip)
        storage.putString(KEY_AGENTE_UNIDAD, data.agenteUnidad)
        storage.putBooleanOrNull(KEY_CONSENTIMIENTO_TRASLADO, data.consentimientoTraslado)
        storage.putBoolean(KEY_AUTORIZA_TITULAR, data.autorizaTitular)
        storage.putBoolean(KEY_AUTORIZA_CONDUCTOR, data.autorizaConductor)
        storage.putBooleanOrNull(KEY_FIRMANTE_AUTORIZA_TRASLADO, data.firmanteAutorizaTraslado)
        storage.putBooleanOrNull(KEY_INCIDENCIA_DURANTE_TRASLADO, data.incidenciaDuranteTraslado)
        storage.putBooleanOrNull(KEY_ENTREGA_LLAVES, data.entregaLlaves)
    }

    fun clearCurrent() {
        ALL_KEYS.forEach(storage::remove)
    }

    private fun PlatformStorage.getBooleanOrNull(key: String): Boolean? {
        val marker = getString("${key}_set", "")
        if (marker != "1") return null
        return getBoolean(key, false)
    }

    private fun PlatformStorage.putBooleanOrNull(key: String, value: Boolean?) {
        if (value == null) {
            remove(key)
            remove("${key}_set")
            return
        }
        putBoolean(key, value)
        putString("${key}_set", "1")
    }

    private companion object {
        const val KEY_SECTOR = "sector"
        const val KEY_SUBSECTOR = "subsector"
        const val KEY_DESTACAMENTO = "destacamento"
        const val KEY_VEHICULO_TIPO = "vehiculo_tipo"
        const val KEY_VEHICULO_MARCA = "vehiculo_marca"
        const val KEY_VEHICULO_MODELO = "vehiculo_modelo"
        const val KEY_VEHICULO_COLOR = "vehiculo_color"
        const val KEY_VEHICULO_MATRICULA = "vehiculo_matricula"
        const val KEY_ES_TITULAR = "es_titular"
        const val KEY_ES_CONDUCTOR = "es_conductor"
        const val KEY_NOMBRE = "nombre"
        const val KEY_PRIMER_APELLIDO = "primer_apellido"
        const val KEY_SEGUNDO_APELLIDO = "segundo_apellido"
        const val KEY_DNI_NIE = "dni_nie"
        const val KEY_TELEFONO = "telefono"
        const val KEY_INICIO_AUXILIO_ACCIDENTE = "inicio_auxilio_accidente"
        const val KEY_INICIO_INFRACCION = "inicio_infraccion"
        const val KEY_INICIO_OTRO_MOTIVO = "inicio_otro_motivo"
        const val KEY_CIRC_FACTORES_ATMOSFERICOS = "circ_factores_atmosfericos"
        const val KEY_CIRC_MALA_VISIBILIDAD = "circ_mala_visibilidad"
        const val KEY_CIRC_CONFIGURACION_VIA = "circ_configuracion_via"
        const val KEY_CIRC_OTRAS = "circ_otras"
        const val KEY_INICIO_LUGAR = "inicio_lugar"
        const val KEY_INICIO_HORA = "inicio_hora"
        const val KEY_INICIO_FECHA = "inicio_fecha"
        const val KEY_FIN_LUGAR = "fin_lugar"
        const val KEY_FIN_HORA = "fin_hora"
        const val KEY_FIN_FECHA = "fin_fecha"
        const val KEY_UNIDAD_RESPONSABLE = "unidad_responsable"
        const val KEY_UNIDAD_TELEFONO = "unidad_telefono"
        const val KEY_AGENTE_TIP = "agente_tip"
        const val KEY_AGENTE_UNIDAD = "agente_unidad"
        const val KEY_CONSENTIMIENTO_TRASLADO = "consentimiento_traslado"
        const val KEY_AUTORIZA_TITULAR = "autoriza_titular"
        const val KEY_AUTORIZA_CONDUCTOR = "autoriza_conductor"
        const val KEY_FIRMANTE_AUTORIZA_TRASLADO = "firmante_autoriza_traslado"
        const val KEY_INCIDENCIA_DURANTE_TRASLADO = "incidencia_durante_traslado"
        const val KEY_ENTREGA_LLAVES = "entrega_llaves"

        val ALL_KEYS = listOf(
            KEY_SECTOR,
            KEY_SUBSECTOR,
            KEY_DESTACAMENTO,
            KEY_VEHICULO_TIPO,
            KEY_VEHICULO_MARCA,
            KEY_VEHICULO_MODELO,
            KEY_VEHICULO_COLOR,
            KEY_VEHICULO_MATRICULA,
            KEY_ES_TITULAR,
            KEY_ES_CONDUCTOR,
            KEY_NOMBRE,
            KEY_PRIMER_APELLIDO,
            KEY_SEGUNDO_APELLIDO,
            KEY_DNI_NIE,
            KEY_TELEFONO,
            KEY_INICIO_AUXILIO_ACCIDENTE,
            KEY_INICIO_INFRACCION,
            KEY_INICIO_OTRO_MOTIVO,
            KEY_CIRC_FACTORES_ATMOSFERICOS,
            KEY_CIRC_MALA_VISIBILIDAD,
            KEY_CIRC_CONFIGURACION_VIA,
            KEY_CIRC_OTRAS,
            KEY_INICIO_LUGAR,
            KEY_INICIO_HORA,
            KEY_INICIO_FECHA,
            KEY_FIN_LUGAR,
            KEY_FIN_HORA,
            KEY_FIN_FECHA,
            KEY_UNIDAD_RESPONSABLE,
            KEY_UNIDAD_TELEFONO,
            KEY_AGENTE_TIP,
            KEY_AGENTE_UNIDAD,
            KEY_CONSENTIMIENTO_TRASLADO,
            "${KEY_CONSENTIMIENTO_TRASLADO}_set",
            KEY_AUTORIZA_TITULAR,
            KEY_AUTORIZA_CONDUCTOR,
            KEY_FIRMANTE_AUTORIZA_TRASLADO,
            "${KEY_FIRMANTE_AUTORIZA_TRASLADO}_set",
            KEY_INCIDENCIA_DURANTE_TRASLADO,
            "${KEY_INCIDENCIA_DURANTE_TRASLADO}_set",
            KEY_ENTREGA_LLAVES,
            "${KEY_ENTREGA_LLAVES}_set"
        )
    }
}
