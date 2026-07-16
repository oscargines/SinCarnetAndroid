package com.oscar.sincarnet.data.repository

import com.oscar.sincarnet.data.PlatformStorage
import com.oscar.sincarnet.domain.model.SolicitudCustodiaJuzgadoData

class SolicitudCustodiaJuzgadoStorage(private val storage: PlatformStorage) {

    fun loadCurrent(): SolicitudCustodiaJuzgadoData = SolicitudCustodiaJuzgadoData(
        unidad = storage.getString(KEY_UNIDAD, ""),
        fechaHoraSolicitud = storage.getString(KEY_FECHA_HORA_SOLICITUD, ""),
        tipSolicitante = storage.getString(KEY_TIP_SOLICITANTE, ""),
        empleoSolicitante = storage.getString(KEY_EMPLEO_SOLICITANTE, ""),
        numeroDiligencia = storage.getString(KEY_NUMERO_DILIGENCIA, ""),
        juzgadoNumero = storage.getString(KEY_JUZGADO_NUMERO, ""),

        nombreApellidos = storage.getString(KEY_NOMBRE_APELLIDOS, ""),
        numeroDocumento = storage.getString(KEY_NUMERO_DOCUMENTO, ""),
        matricula = storage.getString(KEY_MATRICULA, ""),
        fechaHoraSiniestro = storage.getString(KEY_FECHA_HORA_SINIESTRO, ""),
        carretera = storage.getString(KEY_CARRETERA, ""),
        puntoKilometrico = storage.getString(KEY_PUNTO_KILOMETRICO, ""),
        municipioOcurrencia = storage.getString(KEY_MUNICIPIO_OCURRENCIA, ""),
        partidoJudicial = storage.getString(KEY_PARTIDO_JUDICIAL, ""),
        catalogacionHecho = storage.getString(KEY_CATALOGACION_HECHO, ""),
        centroSanitario = storage.getString(KEY_CENTRO_SANITARIO, ""),
        fechaEntregaOficio = storage.getString(KEY_FECHA_ENTREGA_OFICIO, "")
    )

    fun saveCurrent(data: SolicitudCustodiaJuzgadoData) {
        storage.putString(KEY_UNIDAD, data.unidad)
        storage.putString(KEY_FECHA_HORA_SOLICITUD, data.fechaHoraSolicitud)
        storage.putString(KEY_TIP_SOLICITANTE, data.tipSolicitante)
        storage.putString(KEY_EMPLEO_SOLICITANTE, data.empleoSolicitante)
        storage.putString(KEY_NUMERO_DILIGENCIA, data.numeroDiligencia)
        storage.putString(KEY_JUZGADO_NUMERO, data.juzgadoNumero)

        storage.putString(KEY_NOMBRE_APELLIDOS, data.nombreApellidos)
        storage.putString(KEY_NUMERO_DOCUMENTO, data.numeroDocumento)
        storage.putString(KEY_MATRICULA, data.matricula)
        storage.putString(KEY_FECHA_HORA_SINIESTRO, data.fechaHoraSiniestro)
        storage.putString(KEY_CARRETERA, data.carretera)
        storage.putString(KEY_PUNTO_KILOMETRICO, data.puntoKilometrico)
        storage.putString(KEY_MUNICIPIO_OCURRENCIA, data.municipioOcurrencia)
        storage.putString(KEY_PARTIDO_JUDICIAL, data.partidoJudicial)
        storage.putString(KEY_CATALOGACION_HECHO, data.catalogacionHecho)
        storage.putString(KEY_CENTRO_SANITARIO, data.centroSanitario)
        storage.putString(KEY_FECHA_ENTREGA_OFICIO, data.fechaEntregaOficio)
    }

    fun clearCurrent() {
        ALL_KEYS.forEach { storage.remove(it) }
    }

    companion object {
        private const val KEY_UNIDAD = "scj_unidad"
        private const val KEY_FECHA_HORA_SOLICITUD = "scj_fecha_hora_solicitud"
        private const val KEY_TIP_SOLICITANTE = "scj_tip_solicitante"
        private const val KEY_EMPLEO_SOLICITANTE = "scj_empleo_solicitante"
        private const val KEY_NUMERO_DILIGENCIA = "scj_numero_diligencia"
        private const val KEY_JUZGADO_NUMERO = "scj_juzgado_numero"

        private const val KEY_NOMBRE_APELLIDOS = "scj_nombre_apellidos"
        private const val KEY_NUMERO_DOCUMENTO = "scj_numero_documento"
        private const val KEY_MATRICULA = "scj_matricula"
        private const val KEY_FECHA_HORA_SINIESTRO = "scj_fecha_hora_siniestro"
        private const val KEY_CARRETERA = "scj_carretera"
        private const val KEY_PUNTO_KILOMETRICO = "scj_punto_kilometrico"
        private const val KEY_MUNICIPIO_OCURRENCIA = "scj_municipio_ocurrencia"
        private const val KEY_PARTIDO_JUDICIAL = "scj_partido_judicial"
        private const val KEY_CATALOGACION_HECHO = "scj_catalogacion_hecho"
        private const val KEY_CENTRO_SANITARIO = "scj_centro_sanitario"
        private const val KEY_FECHA_ENTREGA_OFICIO = "scj_fecha_entrega_oficio"

        private val ALL_KEYS = listOf(
            KEY_UNIDAD, KEY_FECHA_HORA_SOLICITUD, KEY_TIP_SOLICITANTE,
            KEY_EMPLEO_SOLICITANTE, KEY_NUMERO_DILIGENCIA, KEY_JUZGADO_NUMERO,
            KEY_NOMBRE_APELLIDOS, KEY_NUMERO_DOCUMENTO, KEY_MATRICULA,
            KEY_FECHA_HORA_SINIESTRO, KEY_CARRETERA, KEY_PUNTO_KILOMETRICO,
            KEY_MUNICIPIO_OCURRENCIA, KEY_PARTIDO_JUDICIAL, KEY_CATALOGACION_HECHO,
            KEY_CENTRO_SANITARIO, KEY_FECHA_ENTREGA_OFICIO
        )
    }
}
