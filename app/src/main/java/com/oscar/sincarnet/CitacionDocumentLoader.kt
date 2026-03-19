package com.oscar.sincarnet

import android.content.Context
import org.json.JSONObject
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class CitacionDocument(
    val titulo: String = "",
    val cuerpoDescripcion: String = "",
    val secciones: List<CitacionSeccion> = emptyList(),
    val cierre: String = "",
    val enteradoTitulo: String = "",
    val enteradoTexto: String = ""
)

internal data class CitacionSeccion(
    val titulo: String = "",
    val contenido: String = "",
    val opciones: List<CitacionOpcion> = emptyList(),
    val informacionAdicional: String = "",
    val items: List<CitacionItem> = emptyList()
)

internal data class CitacionOpcion(
    val afectado: String = "",
    val descripcion: String = ""
)

internal data class CitacionItem(
    val afectado: String = "",
    val descripcion: String = ""
)

internal data class AtestadoInicioModalData(
    val motivo: String = "",
    val norma: String = "",
    val articulo: String = "",
    val dgtNoRecord: Boolean = false,
    val internationalNoRecord: Boolean = false,
    val existsRecord: Boolean = false,
    val vicisitudesOption: String = "",
    val jefaturaProvincial: String = "",
    val tiempoPrivacion: String = "",
    val juzgadoDecreta: String = ""
)

internal fun loadCitacionDocument(
    context: Context,
    tipoJuicio: String
): CitacionDocument {
    val fileName = when (tipoJuicio) {
        "rapido" -> "citacionjuiciorapido.json"
        else -> "citacionjuicio.json"
    }
    
    return runCatching {
        val inputStream = context.assets.open("docs/$fileName")
        val content = InputStreamReader(inputStream).use { it.readText() }
        val jsonRoot = JSONObject(content)
        val documento = jsonRoot.getJSONObject("documento")
        
        val titulo = documento.optString("titulo", "")
        val cuerpo = documento.getJSONObject("cuerpo")
        val cuerpoDescripcion = cuerpo.optString("descripcion", "")
        
        val seccionesArray = documento.getJSONArray("secciones")
        val secciones = mutableListOf<CitacionSeccion>()
        
        for (i in 0 until seccionesArray.length()) {
            val secObj = seccionesArray.getJSONObject(i)
            val secTitulo = secObj.optString("titulo", "")
            val secContenido = secObj.optString("contenido", "")
            val secInfoAdicional = secObj.optString("informacion_adicional", "")
            
            val opciones = mutableListOf<CitacionOpcion>()
            if (secObj.has("opciones")) {
                val opcionesArray = secObj.getJSONArray("opciones")
                for (j in 0 until opcionesArray.length()) {
                    val optObj = opcionesArray.getJSONObject(j)
                    opciones.add(
                        CitacionOpcion(
                            afectado = optObj.optString("afectado", ""),
                            descripcion = optObj.optString("descripcion", "")
                        )
                    )
                }
            }
            
            val items = mutableListOf<CitacionItem>()
            if (secObj.has("items")) {
                val itemsArray = secObj.getJSONArray("items")
                for (j in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(j)
                    items.add(
                        CitacionItem(
                            afectado = itemObj.optString("afectado", ""),
                            descripcion = itemObj.optString("descripcion", "")
                        )
                    )
                }
            }
            
            secciones.add(
                CitacionSeccion(
                    titulo = secTitulo,
                    contenido = secContenido,
                    opciones = opciones,
                    informacionAdicional = secInfoAdicional,
                    items = items
                )
            )
        }
        
        val cierre = documento.optString("cierre", "")
        val enterado = documento.getJSONObject("enterado")
        val enteradoTitulo = enterado.optString("titulo", "")
        val enteradoTexto = enterado.optString("texto", "")
        
        CitacionDocument(
            titulo = titulo,
            cuerpoDescripcion = cuerpoDescripcion,
            secciones = secciones,
            cierre = cierre,
            enteradoTitulo = enteradoTitulo,
            enteradoTexto = enteradoTexto
        )
    }.getOrNull() ?: CitacionDocument()
}

internal fun replaceCitacionPlaceholders(
    text: String,
    courtData: JuzgadoAtestadoData,
    personData: PersonaInvestigadaData,
    ocurrenciaData: OcurrenciaDelitData,
    instructorTip: String,
    secretaryTip: String,
    instructorUnit: String,
    vehicleData: VehiculoData = VehiculoData(),
    manifestacionData: ManifestacionData? = null,
    inicioModalData: AtestadoInicioModalData = AtestadoInicioModalData()
): String {
    var result = text

    val isoLocale = Locale("es", "ES")
    val inputDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    val inputTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val shiftedDateTime = runCatching {
        val baseDate = LocalDate.parse(ocurrenciaData.fecha, inputDateFormatter)
        val baseTime = LocalTime.parse(ocurrenciaData.hora, inputTimeFormatter)
        LocalDateTime.of(baseDate, baseTime).plusMinutes(10)
    }.getOrNull()

    val shiftedHour = shiftedDateTime?.toLocalTime()?.format(inputTimeFormatter)
        ?: ocurrenciaData.hora
    val shiftedDateRaw = shiftedDateTime?.toLocalDate()?.format(inputDateFormatter)
        ?: ocurrenciaData.fecha
    val shiftedDatePretty = runCatching {
        val parsed = shiftedDateTime?.toLocalDate() ?: LocalDate.parse(ocurrenciaData.fecha, inputDateFormatter)
        parsed.format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", isoLocale))
    }.getOrElse { shiftedDateRaw }

    val lugarFormateado = buildList {
        val carretera = ocurrenciaData.carretera.trim()
        if (carretera.isNotBlank()) add(carretera.uppercase(isoLocale))
        val pk = ocurrenciaData.pk.trim()
        if (pk.isNotBlank()) add("PK $pk")
    }.joinToString(", ").ifBlank { ocurrenciaData.localidad }

    // Lugar y fecha de la diligencia (desde OcurrenciaDelitStorage)
    result = result.replace("[[lugar]]", lugarFormateado)
    result = result.replace("[[terminomunicipal]]", ocurrenciaData.terminoMunicipal)
    result = result.replace("[[hora]]", shiftedHour)
    result = result.replace("[[fechacompleta]]", shiftedDatePretty)

    // Partido judicial = municipio del juzgado
    result = result.replace("[[partidojudicial]]", courtData.municipioNombre)

    // Datos de actuantes
    result = result.replace("[[instructor]]", instructorTip)
    result = result.replace("[[secretario]]", secretaryTip)
    result = result.replace("[[unidadinferior]]", instructorUnit)

    // Datos del investigado
    val nombreCompleto = listOf(personData.firstName, personData.lastName1, personData.lastName2)
        .filter { it.isNotBlank() }.joinToString(" ")
    result = result.replace("[[nombrecompletoinvestigado]]", nombreCompleto)
    result = result.replace("[[documentoidentificacion]]", personData.documentIdentification)
    result = result.replace("[[fechanacimiento]]", personData.birthDate)
    result = result.replace("[[lugarnacimiento]]", personData.birthPlace)
    result = result.replace("[[nombrepadre]]", personData.fatherName)
    result = result.replace("[[nombremadre]]", personData.motherName)
    result = result.replace("[[domicilio]]", personData.address)
    result = result.replace("[[telefono]]", personData.phone)
    result = result.replace("[[correoelectronico]]", personData.email)

    // Datos del vehículo
    result = result.replace("[[tipovehiculo]]", vehicleData.vehicleType)
    result = result.replace("[[marcavehiculo]]", vehicleData.brand)
    result = result.replace("[[modelovehiculo]]", vehicleData.model)
    result = result.replace("[[matricula]]", vehicleData.plate)
    result = result.replace("[[tipopermisoconducir]]", vehicleData.clasePermiso)

    // Placeholders de otras diligencias
    val fechaHoraLecturaDerechos = listOf(shiftedHour, shiftedDateRaw)
        .filter { it.isNotBlank() }
        .joinToString(" del día ")
    val fechaHoraComisionDelito = listOf(ocurrenciaData.hora, ocurrenciaData.fecha)
        .filter { it.isNotBlank() }
        .joinToString(" del día ")
    result = result.replace("[[lugarhechos]]", ocurrenciaData.localidad)
    result = result.replace("[[horafecha]]", fechaHoraLecturaDerechos)
    result = result.replace("[[datosconductorydocumento]]", nombreCompleto)
    result = result.replace("[[marca]]", vehicleData.brand)
    result = result.replace("[[modelo]]", vehicleData.model)
    result = result.replace("[[lugarfechahoralecturaderechos]]", "$fechaHoraLecturaDerechos en ${ocurrenciaData.localidad}")
    result = result.replace("[[lugarfechahoracomisióndelito]]", "$fechaHoraComisionDelito en ${ocurrenciaData.terminoMunicipal}")
    result = result.replace("[[nombreletrado]]", "")
    result = result.replace("[[articulo]]", inicioModalData.articulo)
    result = result.replace("[[norma]]", inicioModalData.norma)
    result = result.replace("[[jefaturatrafico]]", inicioModalData.jefaturaProvincial)
    result = result.replace("[[tiempoperdidavigenciajudicial]]", inicioModalData.tiempoPrivacion)
    result = result.replace("[[juzgadoquecondena]]", inicioModalData.juzgadoDecreta.ifBlank { courtData.sedeNombre })

    // Manifestación
    result = result.replace("[[horafechamanifestacion]]", fechaHoraLecturaDerechos)
    result = result.replace("[[segundafechahora]]", fechaHoraLecturaDerechos)
    val respuestasManifestacion = manifestacionData?.respuestasPreguntas.orEmpty()
    result = result.replace("[[primerapregunta]]", respuestasManifestacion[1].orEmpty())
    result = result.replace("[[segundapregunta]]", respuestasManifestacion[2].orEmpty())
    result = result.replace("[[tercerapregunta]]", respuestasManifestacion[3].orEmpty())
    result = result.replace("[[cuartapregunta]]", respuestasManifestacion[4].orEmpty())
    result = result.replace("[[quintapregunta]]", respuestasManifestacion[5].orEmpty())
    result = result.replace("[[sextapregunta]]", respuestasManifestacion[6].orEmpty())
    result = result.replace("[[septimapregunta]]", respuestasManifestacion[7].orEmpty())
    result = result.replace("[[octavapregunta]]", respuestasManifestacion[8].orEmpty())

    // Datos del juzgado (campos individuales)
    result = result.replace("[[nombrejuzgado]]", courtData.sedeNombre)
    result = result.replace("[[direccionjuzgado]]", courtData.sedeDireccion)
    result = result.replace("[[telefonojuzgado]]", courtData.sedeTelefono)
    result = result.replace("[[codigopostaljuzgado]]", courtData.sedeCodigoPostal)

    // Datos del juzgado (composición completa)
    val datosJuzgado = listOf(
        courtData.sedeNombre,
        courtData.sedeDireccion,
        courtData.sedeTelefono,
        courtData.sedeCodigoPostal
    ).map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(", ")
        .ifBlank { courtData.sedeNombre.ifBlank { courtData.municipioNombre } }
    result = result.replace("[[datosjuzgado]]", datosJuzgado)

    // Juicio rápido
    result = result.replace("[[horajuicio]]", courtData.horaJuicioRapido)
    result = result.replace("[[fechajuicio]]", courtData.fechaJuicioRapido)

    // Respuestas SI/NO de lectura de derechos (02derechos -> manifestacion_investigado)
    fun toSiNo(value: Boolean?): String = when (value) {
        true -> "SI"
        false -> "NO"
        null -> ""
    }
    result = result.replace("[[right_declaracion]]", toSiNo(personData.rightToRemainSilentInformed))
    result = result.replace("[[right_renuncia_letrada]]", toSiNo(personData.waivesLegalAssistance))
    result = result.replace("[[right_letrado_particular]]", toSiNo(personData.requestsPrivateLawyer))
    result = result.replace("[[right_letrado_oficio]]", toSiNo(personData.requestsDutyLawyer))
    result = result.replace("[[right_acceso_elementos]]", toSiNo(personData.accessesEssentialProceedings))
    result = result.replace("[[right_interprete]]", toSiNo(personData.needsInterpreter))

    return result
}
