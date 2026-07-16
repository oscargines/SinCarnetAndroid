package com.oscar.sincarnet.domain.model

/**
 * Datos del oficio de solicitud de custodia y conservación de muestras de sangre
 * obtenidas con fines terapéuticos en un Centro Sanitario.
 *
 * @property unidad Unidad actuante del solicitante
 * @property centroDestinatario Centro sanitario destinatario
 * @property fechaHoraSolicitud Fecha y hora de generación del oficio
 * @property tipSolicitante TIP del solicitante (formato: X99999X)
 * @property empleoSolicitante Empleo/rango del solicitante
 * @property nombreApellidos Nombre y apellidos del conductor (obtenido vía NFC)
 * @property numeroDocumento Número de DNI/NIE del conductor (obtenido vía NFC)
 * @property fechaHoraSiniestro Fecha y hora del siniestro vial
 * @property carretera Carretera donde ocurrió el siniestro
 * @property puntoKilometrico Punto kilométrico del siniestro
 * @property municipioOcurrencia Término municipal de ocurrencia
 * @property partidoJudicial Partido judicial de ocurrencia
 * @property catalogacionHecho Catalogación del hecho (descripción multilinea)
 * @property juzgadoCompetente Juzgado competente al que se solicita
 */
data class OficioCustodiaSangreData(
    val unidad: String = "",
    val centroDestinatario: String = "",
    val fechaHoraSolicitud: String = "",
    val tipSolicitante: String = "",
    val empleoSolicitante: String = "",

    val nombreApellidos: String = "",
    val numeroDocumento: String = "",
    val fechaHoraSiniestro: String = "",
    val carretera: String = "",
    val puntoKilometrico: String = "",
    val municipioOcurrencia: String = "",
    val partidoJudicial: String = "",
    val catalogacionHecho: String = "",
    val juzgadoCompetente: String = "",
    val numeroDiligencia: String = ""
)
