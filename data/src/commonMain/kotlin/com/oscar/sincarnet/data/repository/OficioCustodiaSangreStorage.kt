package com.oscar.sincarnet.data.repository

import com.oscar.sincarnet.data.PlatformStorage
import com.oscar.sincarnet.domain.model.OficioCustodiaSangreData

/**
 * Persistencia del oficio de custodia de sangre en SharedPreferences.
 *
 * Sigue el mismo patrón que [ActaTrasladoCentroSanitarioStorage]:
 * almacena cada campo como una clave individual y expone
 * [loadCurrent] / [saveCurrent] / [clearCurrent] como operación atómica.
 */
class OficioCustodiaSangreStorage(private val storage: PlatformStorage) {

    fun loadCurrent(): OficioCustodiaSangreData = OficioCustodiaSangreData(
        unidad = storage.getString(KEY_UNIDAD, ""),
        centroDestinatario = storage.getString(KEY_CENTRO_DESTINATARIO, ""),
        fechaHoraSolicitud = storage.getString(KEY_FECHA_HORA_SOLICITUD, ""),
        tipSolicitante = storage.getString(KEY_TIP_SOLICITANTE, ""),
        empleoSolicitante = storage.getString(KEY_EMPLEO_SOLICITANTE, ""),

        nombreApellidos = storage.getString(KEY_NOMBRE_APELLIDOS, ""),
        numeroDocumento = storage.getString(KEY_NUMERO_DOCUMENTO, ""),
        fechaHoraSiniestro = storage.getString(KEY_FECHA_HORA_SINIESTRO, ""),
        carretera = storage.getString(KEY_CARRETERA, ""),
        puntoKilometrico = storage.getString(KEY_PUNTO_KILOMETRICO, ""),
        municipioOcurrencia = storage.getString(KEY_MUNICIPIO_OCURRENCIA, ""),
        partidoJudicial = storage.getString(KEY_PARTIDO_JUDICIAL, ""),
        catalogacionHecho = storage.getString(KEY_CATALOGACION_HECHO, ""),
        juzgadoCompetente = storage.getString(KEY_JUZGADO_COMPETENTE, ""),
        numeroDiligencia = storage.getString(KEY_NUMERO_DILIGENCIA, "")
    )

    fun saveCurrent(data: OficioCustodiaSangreData) {
        storage.putString(KEY_UNIDAD, data.unidad)
        storage.putString(KEY_CENTRO_DESTINATARIO, data.centroDestinatario)
        storage.putString(KEY_FECHA_HORA_SOLICITUD, data.fechaHoraSolicitud)
        storage.putString(KEY_TIP_SOLICITANTE, data.tipSolicitante)
        storage.putString(KEY_EMPLEO_SOLICITANTE, data.empleoSolicitante)

        storage.putString(KEY_NOMBRE_APELLIDOS, data.nombreApellidos)
        storage.putString(KEY_NUMERO_DOCUMENTO, data.numeroDocumento)
        storage.putString(KEY_FECHA_HORA_SINIESTRO, data.fechaHoraSiniestro)
        storage.putString(KEY_CARRETERA, data.carretera)
        storage.putString(KEY_PUNTO_KILOMETRICO, data.puntoKilometrico)
        storage.putString(KEY_MUNICIPIO_OCURRENCIA, data.municipioOcurrencia)
        storage.putString(KEY_PARTIDO_JUDICIAL, data.partidoJudicial)
        storage.putString(KEY_CATALOGACION_HECHO, data.catalogacionHecho)
        storage.putString(KEY_JUZGADO_COMPETENTE, data.juzgadoCompetente)
        storage.putString(KEY_NUMERO_DILIGENCIA, data.numeroDiligencia)
    }

    fun clearCurrent() {
        ALL_KEYS.forEach { storage.remove(it) }
    }

    companion object {
        private const val KEY_UNIDAD = "ocs_unidad"
        private const val KEY_CENTRO_DESTINATARIO = "ocs_centro_destinatario"
        private const val KEY_FECHA_HORA_SOLICITUD = "ocs_fecha_hora_solicitud"
        private const val KEY_TIP_SOLICITANTE = "ocs_tip_solicitante"
        private const val KEY_EMPLEO_SOLICITANTE = "ocs_empleo_solicitante"

        private const val KEY_NOMBRE_APELLIDOS = "ocs_nombre_apellidos"
        private const val KEY_NUMERO_DOCUMENTO = "ocs_numero_documento"
        private const val KEY_FECHA_HORA_SINIESTRO = "ocs_fecha_hora_siniestro"
        private const val KEY_CARRETERA = "ocs_carretera"
        private const val KEY_PUNTO_KILOMETRICO = "ocs_punto_kilometrico"
        private const val KEY_MUNICIPIO_OCURRENCIA = "ocs_municipio_ocurrencia"
        private const val KEY_PARTIDO_JUDICIAL = "ocs_partido_judicial"
        private const val KEY_CATALOGACION_HECHO = "ocs_catalogacion_hecho"
        private const val KEY_JUZGADO_COMPETENTE = "ocs_juzgado_competente"
        private const val KEY_NUMERO_DILIGENCIA = "ocs_numero_diligencia"

        private val ALL_KEYS = listOf(
            KEY_UNIDAD, KEY_CENTRO_DESTINATARIO, KEY_FECHA_HORA_SOLICITUD,
            KEY_TIP_SOLICITANTE, KEY_EMPLEO_SOLICITANTE,
            KEY_NOMBRE_APELLIDOS, KEY_NUMERO_DOCUMENTO, KEY_FECHA_HORA_SINIESTRO,
            KEY_CARRETERA, KEY_PUNTO_KILOMETRICO, KEY_MUNICIPIO_OCURRENCIA,
            KEY_PARTIDO_JUDICIAL, KEY_CATALOGACION_HECHO,
            KEY_JUZGADO_COMPETENTE, KEY_NUMERO_DILIGENCIA
        )
    }
}
