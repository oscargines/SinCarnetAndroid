package com.oscar.sincarnet

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CitacionDocument(
    val titulo: String = "",
    val cuerpoDescripcion: String = "",
    val secciones: List<CitacionSeccion> = emptyList(),
    val cierre: String = "",
    val enteradoTitulo: String = "",
    val enteradoTexto: String = ""
)

data class CitacionSeccion(
    val titulo: String = "",
    val contenido: String = "",
    val items: List<CitacionItem> = emptyList(),
    val opciones: List<CitacionOpcion> = emptyList(),
    val informacionAdicional: String = ""
)

data class CitacionItem(
    val descripcion: String = ""
)

data class CitacionOpcion(
    val descripcion: String = ""
)

fun loadCitacionDocument(context: Context, tipoJuicio: String): CitacionDocument {
    val fileName = when (tipoJuicio.lowercase(Locale.getDefault())) {
        "rapido", "juicio rápido" -> "citacionjuiciorapido.json"
        else -> "citacionjuicio.json"
    }

    return runCatching {
        val inputStream = context.assets.open("docs/$fileName")
        val content = InputStreamReader(inputStream).use { it.readText() }
        val root = JSONObject(content).getJSONObject("documento")

        val secciones = mutableListOf<CitacionSeccion>()
        val seccionesJson = root.optJSONArray("secciones")
        if (seccionesJson != null) {
            for (i in 0 until seccionesJson.length()) {
                val sectionJson = seccionesJson.getJSONObject(i)
                val items = mutableListOf<CitacionItem>()
                val itemsJson = sectionJson.optJSONArray("items")
                if (itemsJson != null) {
                    for (j in 0 until itemsJson.length()) {
                        items.add(CitacionItem(itemsJson.getJSONObject(j).getString("descripcion")))
                    }
                }

                secciones.add(
                    CitacionSeccion(
                        titulo = sectionJson.optString("titulo", ""),
                        contenido = sectionJson.optString("contenido", ""),
                        items = items
                    )
                )
            }
        }

        val enteradoJson = root.optJSONObject("enterado")
        val enteradoTitulo = enteradoJson?.optString("titulo", "") ?: ""
        val enteradoTexto = enteradoJson?.optString("texto", "") ?: ""

        CitacionDocument(
            titulo = root.optString("titulo", ""),
            cuerpoDescripcion = root.optString("cuerpo_descripcion", ""),
            secciones = secciones,
            cierre = root.optString("cierre", ""),
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
        LocalDateTime.of(baseDate, baseTime).minusMinutes(5)
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
    result = result.replace("[[lugar]]", lugarFormateado, ignoreCase = true)
    result = result.replace("[[terminomunicipal]]", ocurrenciaData.terminoMunicipal, ignoreCase = true)
    result = result.replace("[[hora]]", shiftedHour, ignoreCase = true)
    result = result.replace("[[fechacompleta]]", shiftedDatePretty, ignoreCase = true)

    // Partido judicial = municipio del juzgado
    result = result.replace("[[partidojudicial]]", courtData.municipioNombre, ignoreCase = true)

    // Datos de actuantes
    result = result.replace("[[instructor]]", instructorTip, ignoreCase = true)
    result = result.replace("[[secretario]]", secretaryTip, ignoreCase = true)
    result = result.replace("[[unidadinferior]]", instructorUnit, ignoreCase = true)

    // Datos del investigado
    val nombreCompleto = listOf(personData.firstName, personData.lastName1, personData.lastName2)
        .filter { it.isNotBlank() }.joinToString(" ")
    result = result.replace("[[nombrecompletoinvestigado]]", nombreCompleto, ignoreCase = true)
    result = result.replace("[[documentoidentificacion]]", personData.documentIdentification, ignoreCase = true)
    result = result.replace("[[fechanacimiento]]", personData.birthDate, ignoreCase = true)
    result = result.replace("[[lugarnacimiento]]", personData.birthPlace, ignoreCase = true)
    result = result.replace("[[nombrepadre]]", personData.fatherName, ignoreCase = true)
    result = result.replace("[[nombremadre]]", personData.motherName, ignoreCase = true)
    result = result.replace("[[domicilio]]", personData.address, ignoreCase = true)
    result = result.replace("[[telefono]]", personData.phone, ignoreCase = true)
    result = result.replace("[[correoelectronico]]", personData.email, ignoreCase = true)

    // Datos del vehículo
    result = result.replace("[[tipovehiculo]]", vehicleData.vehicleType, ignoreCase = true)
    result = result.replace("[[marcavehiculo]]", vehicleData.brand, ignoreCase = true)
    result = result.replace("[[modelovehiculo]]", vehicleData.model, ignoreCase = true)
    result = result.replace("[[matricula]]", vehicleData.plate, ignoreCase = true)
    result = result.replace("[[tipopermisoconducir]]", vehicleData.clasePermiso, ignoreCase = true)

    // Placeholders de otras diligencias
    val fechaHoraLecturaDerechos = listOf(shiftedHour, shiftedDateRaw)
        .filter { it.isNotBlank() }
        .joinToString(" del día ")
    val fechaHoraComisionDelito = listOf(ocurrenciaData.hora, ocurrenciaData.fecha)
        .filter { it.isNotBlank() }
        .joinToString(" del día ")
    result = result.replace("[[lugarhechos]]", ocurrenciaData.localidad, ignoreCase = true)
    result = result.replace("[[horafecha]]", fechaHoraLecturaDerechos, ignoreCase = true)
    result = result.replace("[[datosconductorydocumento]]", nombreCompleto, ignoreCase = true)
    result = result.replace("[[marca]]", vehicleData.brand, ignoreCase = true)
    result = result.replace("[[modelo]]", vehicleData.model, ignoreCase = true)
    result = result.replace("[[lugarfechahoralecturaderechos]]", "$fechaHoraLecturaDerechos en ${ocurrenciaData.localidad}", ignoreCase = true)
    result = result.replace("[[lugarfechahoracomisióndelito]]", "$fechaHoraComisionDelito en ${ocurrenciaData.terminoMunicipal}", ignoreCase = true)
    result = result.replace("[[nombreletrado]]", "", ignoreCase = true)
    result = result.replace("[[articulo]]", inicioModalData.articulo, ignoreCase = true)
    result = result.replace("[[norma]]", inicioModalData.norma, ignoreCase = true)
    result = result.replace("[[jefaturatrafico]]", inicioModalData.jefaturaProvincial, ignoreCase = true)
    result = result.replace("[[tiempoperdidavigenciajudicial]]", inicioModalData.tiempoPrivacion, ignoreCase = true)
    result = result.replace("[[juzgadoquecondena]]", inicioModalData.juzgadoDecreta.ifBlank { courtData.sedeNombre }, ignoreCase = true)

    // Manifestación
    result = result.replace("[[horafechamanifestacion]]", fechaHoraLecturaDerechos, ignoreCase = true)
    result = result.replace("[[segundafechahora]]", fechaHoraLecturaDerechos, ignoreCase = true)
    val respuestasManifestacion = manifestacionData?.respuestasPreguntas.orEmpty()
    result = result.replace("[[primerapregunta]]", respuestasManifestacion[1].orEmpty(), ignoreCase = true)
    result = result.replace("[[segundapregunta]]", respuestasManifestacion[2].orEmpty(), ignoreCase = true)
    result = result.replace("[[tercerapregunta]]", respuestasManifestacion[3].orEmpty(), ignoreCase = true)
    result = result.replace("[[cuartapregunta]]", respuestasManifestacion[4].orEmpty(), ignoreCase = true)
    result = result.replace("[[quintapregunta]]", respuestasManifestacion[5].orEmpty(), ignoreCase = true)
    result = result.replace("[[sextapregunta]]", respuestasManifestacion[6].orEmpty(), ignoreCase = true)
    result = result.replace("[[septimapregunta]]", respuestasManifestacion[7].orEmpty(), ignoreCase = true)
    result = result.replace("[[octavapregunta]]", respuestasManifestacion[8].orEmpty(), ignoreCase = true)

    // Datos del juzgado (campos individuales)
    result = result.replace("[[nombrejuzgado]]", courtData.sedeNombre, ignoreCase = true)
    result = result.replace("[[direccionjuzgado]]", courtData.sedeDireccion, ignoreCase = true)
    result = result.replace("[[telefonojuzgado]]", courtData.sedeTelefono, ignoreCase = true)
    result = result.replace("[[codigopostaljuzgado]]", courtData.sedeCodigoPostal, ignoreCase = true)

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
    result = result.replace("[[datosjuzgado]]", datosJuzgado, ignoreCase = true)

    // Juicio rápido
    result = result.replace("[[horajuicio]]", courtData.horaJuicioRapido, ignoreCase = true)
    result = result.replace("[[fechajuicio]]", courtData.fechaJuicioRapido, ignoreCase = true)

    // Respuestas SI/NO de lectura de derechos (02derechos -> manifestacion_investigado)
    fun toSiNo(value: Boolean?): String = when (value) {
        true -> "SI"
        false -> "NO"
        null -> ""
    }
    result = result.replace("[[right_declaracion]]", toSiNo(personData.rightToRemainSilentInformed), ignoreCase = true)
    result = result.replace("[[right_renuncia_letrada]]", toSiNo(personData.waivesLegalAssistance), ignoreCase = true)
    result = result.replace("[[right_letrado_particular]]", toSiNo(personData.requestsPrivateLawyer), ignoreCase = true)
    result = result.replace("[[right_letrado_oficio]]", toSiNo(personData.requestsDutyLawyer), ignoreCase = true)
    result = result.replace("[[right_acceso_elementos]]", toSiNo(personData.accessesEssentialProceedings), ignoreCase = true)
    result = result.replace("[[right_interprete]]", toSiNo(personData.needsInterpreter), ignoreCase = true)

    return result
}
