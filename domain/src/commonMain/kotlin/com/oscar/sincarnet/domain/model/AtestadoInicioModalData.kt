package com.oscar.sincarnet.domain.model

data class AtestadoInicioModalData(
    val motivo: String = "",
    val norma: String = "",
    val articulo: String = "",
    val dgtNoRecord: Boolean = false,
    val internationalNoRecord: Boolean = false,
    val existsRecord: Boolean = false,
    val vicisitudesOption: String = "",
    val jefaturaProvincial: String = "",
    val numeroBoletin: String = "",
    val tieneAntecedentes: Boolean = false,
    val antecedentesGuardiaCivil: Boolean = false,
    val antecedentesSenalamientosNacionales: Boolean = false,
    val antecedentesDgt: Boolean = false,
    val antecedentesOtrosCuerpos: Boolean = false,
    val requisitoriasJudiciales: Boolean = false,
    val enviarPorLexnet: Boolean = true,
    val modoEnvio: String = "",
    val atestadoPdfPath: String = "",
    val documentosEscaneadosPdfPath: String = "",
    val atestadoCompletoPdfPath: String = "",
    val tiempoPrivacion: String = "",
    val juzgadoDecreta: String = ""
)
