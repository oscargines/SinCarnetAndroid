package com.oscar.sincarnet.domain.model

/**
 * Datos del acta de realización de pruebas de detección de alcohol y/o
 * presencia de drogas mediante reconocimiento médico o análisis clínicos
 * por razones justificadas (Anexo XIV).
 *
 * Representa los campos capturados desde el flujo de documentos complementarios
 * para generar el PDF del acta de traslado a Centro Sanitario.
 */
data class ActaTrasladoCentroSanitarioData(
    val subsector: String = "",
    val unidadInterviniente: String = "",
    val numeroExpediente: String = "",

    val lugarIntervencion: String = "",
    val motivo: String = "",
    val fechaHora: String = "",

    val nombreApellidos: String = "",
    val dniNiePasaporte: String = "",

    val facultativoColegiado: String = "",

    val pruebaSangre: Boolean = false,
    val pruebaOtroTipo: Boolean = false,
    val pruebaOtroDetalle: String = "",

    val centroSanitario: String = "",
    val extraccionColegiado: String = "",
    val fechaHoraExtraccion: String = "",

    val precintosSeguridad: String = "PRECINTOS DE SEGURIDAD",
    val datoMuestra: String = "",

    val juzgadoOrganismo: String = "",

    val otrasObservaciones: String = "",

    val numTip: String = ""
)
