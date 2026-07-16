package com.oscar.sincarnet.domain.model

data class SolicitudCustodiaJuzgadoData(
    val unidad: String = "",
    val fechaHoraSolicitud: String = "",
    val tipSolicitante: String = "",
    val empleoSolicitante: String = "",
    val numeroDiligencia: String = "",
    val juzgadoNumero: String = "",

    val nombreApellidos: String = "",
    val numeroDocumento: String = "",
    val matricula: String = "",
    val fechaHoraSiniestro: String = "",
    val carretera: String = "",
    val puntoKilometrico: String = "",
    val municipioOcurrencia: String = "",
    val partidoJudicial: String = "",
    val catalogacionHecho: String = "",
    val centroSanitario: String = "",
    val fechaEntregaOficio: String = ""
)
