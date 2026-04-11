package com.oscar.sincarnet

import android.content.Context
import android.util.Log
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
    val enteradoTexto: String = "",
    val onlyEnterado: Boolean = false,
    val allowSecondDriver: Boolean = false
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
    // Normalizar tipoJuicio: pasar a minúsculas, eliminar acentos y espacios redundantes
    val tipoNormalized = java.text.Normalizer.normalize(tipoJuicio ?: "", java.text.Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase(Locale.getDefault())
        .trim()

    val fileName = if (tipoNormalized.contains("rapido")) {
        "citacionjuiciorapido.json"
    } else {
        "citacionjuicio.json"
    }

    Log.d("CITACION", "loadCitacionDocument: tipoJuicio='$tipoJuicio' -> normalized='$tipoNormalized' -> fileName=$fileName")

    return runCatching {
        val inputStream = context.assets.open("docs/$fileName")
        val content = InputStreamReader(inputStream).use { it.readText() }
        val root = JSONObject(content).getJSONObject("documento")
        Log.d("CITACION", "loaded documento titulo='${root.optString("titulo", "")} for file=$fileName")

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
    inicioModalData: AtestadoInicioModalData = AtestadoInicioModalData(),
    segundoConductorNombre: String? = null,
    segundoConductorDocumento: String? = null,
    documentSequenceIndex: Int = 0
): String {
    var result = text

    val isoLocale = Locale("es", "ES")
    val inputDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    val inputTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val shiftedDateTime = runCatching {
        val baseDate = LocalDate.parse(ocurrenciaData.fecha, inputDateFormatter)
        val baseTime = LocalTime.parse(ocurrenciaData.hora, inputTimeFormatter)
        LocalDateTime.of(baseDate, baseTime).plusMinutes((documentSequenceIndex * 5).toLong())
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
    val datosConductorYDoc = if (nombreCompleto.isNotBlank() && personData.documentIdentification.isNotBlank())
        "$nombreCompleto (${personData.documentIdentification})"
    else nombreCompleto
    result = result.replace("[[nombrecompletoinvestigado]]", nombreCompleto, ignoreCase = true)
    result = result.replace("[[documentoidentificacion]]", personData.documentIdentification, ignoreCase = true)
    result = result.replace("[[datosconductorydocumento]]", datosConductorYDoc, ignoreCase = true)
    // Para el conductor habilitado (segundo conductor), si existe
    val nombreCompletoHabilitado = segundoConductorNombre?.takeIf { it.isNotBlank() } ?: ""
    val docHabilitado = segundoConductorDocumento?.takeIf { it.isNotBlank() } ?: ""
    val datosConductorHabilitado = if (nombreCompletoHabilitado.isNotBlank() && docHabilitado.isNotBlank())
        "$nombreCompletoHabilitado ($docHabilitado)"
    else nombreCompletoHabilitado
    result = result.replace("[[datosconductorhabilitado]]", datosConductorHabilitado, ignoreCase = true)

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
    val lugarComisionDelito = buildList {
        val carreteraBase = ocurrenciaData.carretera.trim()
        if (carreteraBase.isNotBlank()) add(carreteraBase)
        val pkBase = ocurrenciaData.pk.trim()
        if (pkBase.isNotBlank()) add("PK $pkBase")
    }.joinToString(", ").ifBlank { ocurrenciaData.localidad.ifBlank { ocurrenciaData.terminoMunicipal } }

    // Este campo debe reflejar exactamente la comisión del delito almacenada en OcurrenciaDelitStorage.
    val fechaHoraComisionDelito = listOf(ocurrenciaData.hora, ocurrenciaData.fecha)
        .filter { it.isNotBlank() }
        .joinToString(" del día ")
    result = result.replace("[[lugarhechos]]", ocurrenciaData.localidad, ignoreCase = true)
    result = result.replace("[[horafecha]]", fechaHoraLecturaDerechos, ignoreCase = true)
    result = result.replace("[[marca]]", vehicleData.brand, ignoreCase = true)
    result = result.replace("[[modelo]]", vehicleData.model, ignoreCase = true)
    result = result.replace("[[lugarfechahoralecturaderechos]]", "$fechaHoraLecturaDerechos en $lugarComisionDelito", ignoreCase = true)
    result = result.replace("[[lugarfechahoracomisióndelito]]", "$fechaHoraComisionDelito en $lugarComisionDelito", ignoreCase = true)
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

    // --------------------------------------------------
    // Pase final tolerante: resuelve placeholders mal escritos
    // Ejemplos corregidos: [[fecha nacimiento]] -> [[fechanacimiento]], [[fecha_nacimiento]] -> [[fechanacimiento]]
    // Normalizamos el nombre del placeholder (eliminando espacios, guiones bajos y no alfanuméricos)
    // y lo mapeamos a los valores ya calculados.
    val normalizedMap = mutableMapOf<String, String>()
    fun putNorm(key: String, value: String?) {
        val norm = key.replace("[^A-Za-z0-9]".toRegex(), "").lowercase()
        normalizedMap[norm] = value.orEmpty()
    }

    // Datos básicos y persona
    putNorm("lugar", lugarFormateado)
    putNorm("terminomunicipal", ocurrenciaData.terminoMunicipal)
    putNorm("partidojudicial", courtData.municipioNombre)
    putNorm("hora", shiftedHour)
    putNorm("fechacompleta", shiftedDatePretty)
    putNorm("instructor", instructorTip)
    putNorm("secretario", secretaryTip)
    putNorm("unidadinferior", instructorUnit)
    putNorm("nombrecompletoinvestigado", nombreCompleto)
    putNorm("documentoidentificacion", personData.documentIdentification)
    putNorm("datosconductorydocumento", datosConductorYDoc)
    putNorm("datosconductorhabilitado", datosConductorHabilitado)
    putNorm("tipovehiculo", vehicleData.vehicleType)
    putNorm("marcavehiculo", vehicleData.brand)
    putNorm("modelovehiculo", vehicleData.model)
    putNorm("matricula", vehicleData.plate)
    putNorm("tipopermisoconducir", vehicleData.clasePermiso)

    // Persona investigada campos comunes
    putNorm("fechanacimiento", personData.birthDate)
    putNorm("lugarnacimiento", personData.birthPlace)
    putNorm("nombrepadre", personData.fatherName)
    putNorm("nombremadre", personData.motherName)
    putNorm("domicilio", personData.address)
    putNorm("telefono", personData.phone)
    putNorm("correoelectronico", personData.email)

    // Otros
    putNorm("lugarfechahoralecturaderechos", "$fechaHoraLecturaDerechos en $lugarComisionDelito")
    putNorm("lugarfechahoracomisióndelito", "$fechaHoraComisionDelito en $lugarComisionDelito")
    putNorm("datosjuzgado", datosJuzgado)
    putNorm("horajuicio", courtData.horaJuicioRapido)
    putNorm("fechajuicio", courtData.fechaJuicioRapido)

    // Levenshtein para tolerar typos leves en el nombre del placeholder
    fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[a.length][b.length]
    }

    val placeholderRegex = Regex("\\[\\[\\s*([^\\]]+?)\\s*\\]\\]")
    result = placeholderRegex.replace(result) { m ->
        val raw = m.groupValues[1]
        val normalized = raw.replace("[^A-Za-z0-9]".toRegex(), "").lowercase()
        var replacement = normalizedMap[normalized]
        if (replacement.isNullOrEmpty()) {
            // buscar la clave normalizada más cercana
            var bestKey: String? = null
            var bestDist = Int.MAX_VALUE
            for (k in normalizedMap.keys) {
                val d = levenshtein(normalized, k)
                if (d < bestDist) {
                    bestDist = d
                    bestKey = k
                }
            }
            if (bestKey != null && bestDist <= 2) {
                replacement = normalizedMap[bestKey]
            }
        }
        if (replacement != null && replacement.isNotEmpty()) replacement else m.value
    }

    return result
}
