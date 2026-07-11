package com.oscar.sincarnet.domain.model

/**
 * Datos del acta de requerimiento para la realización de pruebas de alcohol o drogas.
 *
 * Se almacena de forma persistente en la memoria del dispositivo a través
 * de la capa de datos.
 */
data class ComplementarioAlcoholData(
    val fecha: String = "",
    val hora: String = "",
    val lugar: String = "",
    val terminoMunicipal: String = "",
    val partidoJudicial: String = "",
    val cuerpoSolicitante: String = "",
    val otroCuerpo: String = "",
    val agenteSolicitanteId: String = "",
    
    // Pruebas realizadas (checkboxes independientes)
    val pruebasAlcohol: Boolean = false,
    val pruebasDrogas: Boolean = false,
    
    // Caso (tabla tasas máximas permitidas)
    val caso: String = "",  // "ConductorVehiculos", "UsuarioAccidente", "Mercancias", "Novato", "Otros"
    
    // Unidad que presta el apoyo
    val tipoUnidad: String = "",  // "Sector", "Subsector", "Destacamento"
    val unidadNombre: String = "",  // Nombre/identificador de la unidad
    
    // Diligencias/expediente
    val diligenciasExpediente: String = "",
    
    // Motivos de requerimiento (expandidos)
    val motivoSiniestroVial: Boolean = false,
    val motivoSignosEvidentes: Boolean = false,
    val motivoRequerimientoJudicial: Boolean = false,
    val motivoInfraccionTrafico: Boolean = false,
    val motivoControlPreventivo: Boolean = false,
    val motivoOtros: Boolean = false,
    val motivoOtrosDetalle: String = "",
    
    // Persona sometida
    val personaNombre: String = "",
    val personaDni: String = "",
    val personaFechaExpedicion: String = "",
    val personaFechaNacimiento: String = "",
    val personaDomicilio: String = "",
    val personaTelefono: String = "",
    
    // En calidad de
    val enCalidadDe: String = "",  // "Conductor", "OtrosUsuarios", "Otros"
    val enCalidadOtros: String = "",
    
    // Vehículo
    val vehiculoMarca: String = "",
    val vehiculoModelo: String = "",
    val vehiculoMatricula: String = "",
    
    // Operador
    val operadorTip: String = "",
    val operadorUnidad: String = "",
    val tipoConductor: String = "",
    
    // Etilómetro evidencial
    val etilometroMarca: String = "",
    val etilometroModelo: String = "",
    val etilometroNumeroSerie: String = "",
    val etilometroHoraVerificacion: String = "",
    val etilometroAgente: String = "",
    
    // Lector de drogas
    val lectorDrogasMarca: String = "",
    val lectorDrogasModelo: String = "",
    val lectorDrogasNumeroSerie: String = "",
    val lectorDrogasHoraVerificacion: String = "",
    val lectorDrogasAgente: String = "",
    
    // Resultados alcohol
    val primeraPruebaHora: String = "",
    val primeraPruebaResultado: String = "",
    val segundaPruebaHora: String = "",
    val segundaPruebaResultado: String = "",
    val pruebaContrasteAlcohol: String = "",
    
    // Resultados drogas
    val pruebaDrogasIndiciaría: Boolean = false,
    val pruebaDrogasResultado: String = "",  // "Negativa", "Positiva"
    val pruebaDrogasTipo: String = "",  // "THC", "OPI", "COC", "AMP", "MAMP"
    val pruebaDrogasContraste: String = ""
)
