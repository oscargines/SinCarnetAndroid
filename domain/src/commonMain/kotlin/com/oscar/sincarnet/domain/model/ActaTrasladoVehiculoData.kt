package com.oscar.sincarnet.domain.model

/**
 * Datos del acta de autorización de traslado de vehículo.
 *
 * Representa los campos capturados desde el flujo de documentos complementarios
 * para generar el PDF del acta.
 */
data class ActaTrasladoVehiculoData(
    val sector: String = "",
    val subsector: String = "",
    val destacamento: String = "",

    val vehiculoTipo: String = "",
    val vehiculoMarca: String = "",
    val vehiculoModelo: String = "",
    val vehiculoColor: String = "",
    val vehiculoMatricula: String = "",

    val esTitular: Boolean = true,
    val esConductor: Boolean = false,
    val nombre: String = "",
    val primerApellido: String = "",
    val segundoApellido: String = "",
    val dniNie: String = "",
    val telefono: String = "",

    val inicioAuxilioAccidente: Boolean = false,
    val inicioInfraccion: Boolean = false,
    val inicioOtroMotivo: String = "",

    val circunstanciaFactoresAtmosfericos: Boolean = false,
    val circunstanciaMalaVisibilidad: Boolean = false,
    val circunstanciaConfiguracionVia: String = "",
    val circunstanciaOtras: String = "",

    val inicioLugar: String = "",
    val inicioHora: String = "",
    val inicioFecha: String = "",

    val finLugar: String = "",
    val finHora: String = "",
    val finFecha: String = "",

    val unidadResponsable: String = "",
    val unidadTelefono: String = "",
    val agenteTip: String = "",
    val agenteUnidad: String = "",

    val consentimientoTraslado: Boolean? = null,
    val autorizaTitular: Boolean = true,
    val autorizaConductor: Boolean = false,
    val firmanteAutorizaTraslado: Boolean? = null,
    val incidenciaDuranteTraslado: Boolean? = null,
    val entregaLlaves: Boolean? = null
)
