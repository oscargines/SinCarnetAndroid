package com.oscar.sincarnet

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Documento de citación a juicio completo cargado desde JSON.
 *
 * Estructura que representa una citación completa con secciones y elementos.
 * Se carga desde assets (`docs/citacionjuicio.json` o `docs/citacionjuiciorapido.json`)
 * y puede ser renderizado en PDF o impreso vía Bluetooth.
 *
 * **Flujo de uso**:
 * 1. [loadCitacionDocument] carga el JSON según tipoJuicio
 * 2. Se llena la estructura con secciones, items, opciones
 * 3. Se pasan al renderizador (PDF o impresora)
 * 4. Se aplican placeholders específicos del caso con [replaceCitacionPlaceholders]
 *
 * **Notas especiales**:
 * - `onlyEnterado=true`: Solo incluir sección "Enterado" (p.ej. en 03letradogratis.json)
 * - `allowSecondDriver=true`: Permitir entrada de segundo conductor habilitado
 *
 * @property titulo Título principal de la citación (p.ej. "CITACIÓN A JUICIO RÁPIDO")
 * @property cuerpoDescripcion Párrafo introductorio del documento
 * @property secciones Lista de [CitacionSeccion] que componen el cuerpo del documento
 * @property cierre Texto de cierre (firma, fecha, etc.)
 * @property enteradoTitulo Título de la sección "Enterado" (si aplica)
 * @property enteradoTexto Texto de la sección "Enterado"
 * @property onlyEnterado Si true, solo renderizar sección "Enterado" (omitir resto)
 * @property allowSecondDriver Si true, permitir especificar segundo conductor habilitado
 *
 * @see loadCitacionDocument Para cargar desde JSON
 * @see replaceCitacionPlaceholders Para completar los datos específicos del caso
 */
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

/**
 * Una sección dentro de un documento de citación.
 *
 * Las secciones son bloques de contenido que pueden contener:
 * - Título y contenido textual
 * - Lista de items (bullets)
 * - Lista de opciones alternativas
 * - Información adicional (notas, observaciones)
 *
 * Ejemplo de sección (juicio rápido):
 * ```
 * Sección "Derechos":
 *   - Título: "Derechos del acusado"
 *   - Contenido: "Usted tiene derecho a..."
 *   - Items: [
 *       "Derecho a guardar silencio",
 *       "Derecho a asistencia letrada",
 *       ...
 *     ]
 * ```
 *
 * @property titulo Encabezado de la sección (p.ej. "Derechos del acusado")
 * @property contenido Párrafo de descripción general
 * @property items Lista de puntos/bullets (generalmente con prefijo "•")
 * @property opciones Lista de alternativas para elegir (p.ej. "Designa letrado de oficio" vs "No designa")
 * @property informacionAdicional Texto adicional, notas o advertencias
 */
data class CitacionSeccion(
    val titulo: String = "",
    val contenido: String = "",
    val items: List<CitacionItem> = emptyList(),
    val opciones: List<CitacionOpcion> = emptyList(),
    val informacionAdicional: String = ""
)

/**
 * Un item/punto dentro de una lista de una [CitacionSeccion].
 *
 * Cada item representa un bullet point o elemento de lista que aparecerá
 * con prefijo "•" o similar en el documento impreso.
 *
 * @property descripcion Texto del item (p.ej. "Derecho a guardar silencio")
 */
data class CitacionItem(
    val descripcion: String = ""
)

/**
 * Una opción alternativa dentro de una [CitacionSeccion].
 *
 * Las opciones representan decisiones que el usuario puede tomar.
 * Por ejemplo, en el documento de letrado gratuito:
 * - Opción 1: "Designa letrado de oficio"
 * - Opción 2: "No designa letrado de oficio"
 *
 * En el renderizado, aparecen con casillas de selección o indicadores.
 *
 * @property descripcion Texto de la opción (p.ej. "Designa letrado de oficio")
 */
data class CitacionOpcion(
    val descripcion: String = ""
)

/**
 * Carga un documento de citación desde los assets JSON según el tipo de juicio.
 *
 * **Proceso**:
 * 1. Normaliza el tipoJuicio (minúsculas, elimina acentos y espacios)
 * 2. Decide qué archivo cargar:
 *    - Si contiene "rápido" → `citacionjuiciorapido.json`
 *    - Si no → `citacionjuicio.json`
 * 3. Abre el JSON desde `assets/docs/[archivo]`
 * 4. Parsea la estructura documento→secciones→items→opciones
 * 5. Retorna [CitacionDocument] poblado o documento vacío si falla
 *
 * **Normalización de tipoJuicio**:
 * - Elimina acentos: "Juicio Rápido" → "juicio rapido"
 * - Elimina espacios: "juicio rapido" → "juiciorapido"
 * - Convierte a minúsculas
 *
 * **Archivos soportados**:
 * - `docs/citacionjuicio.json` - Juicio ordinario
 * - `docs/citacionjuiciorapido.json` - Juicio rápido
 *
 * **Fallback a BD**: Si el JSON no tiene "secciones", retorna [CitacionDocument] vacío
 * y la BD debería proporcionar los datos dinámicamente.
 *
 * @param context Contexto de aplicación para acceso a assets
 * @param tipoJuicio Tipo de juicio ("Juicio Rápido", "juicio ordinario", etc.)
 * @return [CitacionDocument] cargado o documento vacío si error
 *
 * @see replaceCitacionPlaceholders Para completar los placeholders del documento
 */
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

        // El cuerpo principal está en "cuerpo" > "descripcion" (objeto anidado, no clave plana)
        val cuerpoDescripcion = root.optJSONObject("cuerpo")?.optString("descripcion", "")
            ?: root.optString("cuerpo_descripcion", "")   // fallback por compatibilidad

        val secciones = mutableListOf<CitacionSeccion>()
        val seccionesJson = root.optJSONArray("secciones")
        if (seccionesJson != null) {
            for (i in 0 until seccionesJson.length()) {
                val sectionJson = seccionesJson.getJSONObject(i)

                // items (lista de bullets)
                val items = mutableListOf<CitacionItem>()
                val itemsJson = sectionJson.optJSONArray("items")
                if (itemsJson != null) {
                    for (j in 0 until itemsJson.length()) {
                        val desc = itemsJson.getJSONObject(j).optString("descripcion", "")
                        if (desc.isNotBlank()) items.add(CitacionItem(desc))
                    }
                }

                // opciones (lista de opciones, p.ej. "No designa Letrado…")
                val opciones = mutableListOf<CitacionOpcion>()
                val opcionesJson = sectionJson.optJSONArray("opciones")
                if (opcionesJson != null) {
                    for (j in 0 until opcionesJson.length()) {
                        val desc = opcionesJson.getJSONObject(j).optString("descripcion", "")
                        if (desc.isNotBlank()) opciones.add(CitacionOpcion(desc))
                    }
                }

                secciones.add(
                    CitacionSeccion(
                        titulo = sectionJson.optString("titulo", ""),
                        contenido = sectionJson.optString("contenido", ""),
                        items = items,
                        opciones = opciones,
                        informacionAdicional = sectionJson.optString("informacion_adicional", "")
                    )
                )
            }
        }

        val enteradoJson = root.optJSONObject("enterado")
        val enteradoTitulo = enteradoJson?.optString("titulo", "") ?: ""
        val enteradoTexto = enteradoJson?.optString("texto", "") ?: ""

        CitacionDocument(
            titulo = root.optString("titulo", ""),
            cuerpoDescripcion = cuerpoDescripcion,
            secciones = secciones,
            cierre = root.optString("cierre", ""),
            enteradoTitulo = enteradoTitulo,
            enteradoTexto = enteradoTexto
        )
    }.getOrNull() ?: CitacionDocument()
}

/**
 * Reemplaza los placeholders [[...]] en un documento de citación con datos específicos del caso.
 *
 * **Funcionamiento**:
 * 1. Calcula la fecha/hora ajustada según `documentSequenceIndex` (suma 5min por cada documento)
 * 2. Reemplaza ~40 placeholders predefinidos
 * 3. Si no encuentra exacta coincidencia, usa Levenshtein distance para correcciones tolerantes
 * 4. Mantiene el placeholder original si no encuentra reemplazo y distancia > 2
 *
 * **Placeholders soportados** (ejemplos):
 *
 * *Ubicación y tiempo*:
 * - `[[lugar]]`, `[[lugarhechos]]`, `[[terminomunicipal]]`
 * - `[[hora]]`, `[[fechacompleta]]`, `[[horafecha]]`
 *
 * *Personas*:
 * - `[[nombrecompletoinvestigado]]`, `[[documentoidentificacion]]`
 * - `[[datosconductorydocumento]]`, `[[datosconductorhabilitado]]`
 * - `[[instructor]]`, `[[secretario]]`
 *
 * *Vehículo*:
 * - `[[tipovehiculo]]`, `[[marcavehiculo]]`, `[[modelovehiculo]]`, `[[matricula]]`
 *
 * *Juzgado*:
 * - `[[nombrejuzgado]]`, `[[direccionjuzgado]]`, `[[datosjuzgado]]`
 * - `[[horajuicio]]`, `[[fechajuicio]]`
 *
 * *Derechos (SI/NO)*:
 * - `[[right_declaracion]]`, `[[right_renuncia_letrada]]`, `[[right_letrado_particular]]`
 * - `[[right_letrado_oficio]]`, `[[right_acceso_elementos]]`, `[[right_interprete]]`
 *
 * *Manifestación (preguntas)*:
 * - `[[primerapregunta]]`, `[[segundapregunta]]`, ... `[[octavapregunta]]`
 *
 * **Tolerancia a typos**:
 * - Si no encuentra `[[fecha nacimiento]]` exactamente, busca la clave normalizada más cercana
 * - Usa Levenshtein distance: tolera hasta 2 caracteres de diferencia
 * - Ejemplos: `[[fecha_nacimiento]]` → `[[fechanacimiento]]`, `[[fechanacimiento]]`
 *
 * **Desplazamiento de hora** (documentSequenceIndex):
 * - Documento 0: sin cambios
 * - Documento 1: +5 minutos
 * - Documento 2: +10 minutos
 * - Usado para que cada documento de la secuencia tenga hora levemente diferente
 *
 * @param text Texto con placeholders a reemplazar
 * @param courtData Datos del juzgado
 * @param personData Datos de la persona investigada
 * @param ocurrenciaData Lugar y hora de los hechos
 * @param instructorTip Titulo/cargo del instructor (p.ej. "Juzgado de Instrucción nº 1")
 * @param secretaryTip Titulo/cargo del secretario
 * @param instructorUnit Unidad del instructor (p.ej. "Policía Local de Madrid")
 * @param vehicleData Datos del vehículo (opcional)
 * @param manifestacionData Respuestas a preguntas de manifestación (opcional)
 * @param inicioModalData Datos del modal de inicio (artículo, norma, etc.)
 * @param segundoConductorNombre Nombre del segundo conductor habilitado (opcional)
 * @param segundoConductorDocumento Documento del segundo conductor (opcional)
 * @param documentSequenceIndex Índice del documento en la secuencia (0-based) para desplazar hora
 * @return Texto con placeholders reemplazados
 *
 * @see loadCitacionDocument Para cargar el documento plantilla
 * @see AtestadoContinuousPdfGenerator Llamador principal en PDF continuo
 */
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
    result = result.replace("[[provinciajuzgado]]", courtData.provinciaNombre, ignoreCase = true)

    // Datos del juzgado (composición completa, incluye municipio)
    val datosJuzgado = listOf(
        courtData.sedeNombre,
        courtData.sedeDireccion,
        courtData.municipioNombre,
        courtData.sedeTelefono,
        courtData.sedeCodigoPostal
    ).map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(", ")
        .ifBlank { courtData.sedeNombre.ifBlank { courtData.municipioNombre } }
    result = result.replace("[[datosjuzgado]]", datosJuzgado, ignoreCase = true)
    result = result.replace("[[municipiojuzgado]]", courtData.municipioNombre, ignoreCase = true)

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
    /**
     * Normalizador de placeholders para corrección tolerante.
     *
     * Elimina espacios, guiones bajos, y caracteres no alfanuméricos de los nombres de placeholders.
     * Esto permite que `[[fecha nacimiento]]`, `[[fecha_nacimiento]]` y `[[fechanacimiento]]`
     * se reconozcan como equivalentes.
     *
     * @param key Nombre del placeholder (p.ej. "fecha nacimiento")
     * @param value Valor a asignar
     */
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
    putNorm("municipiojuzgado", courtData.municipioNombre)
    putNorm("provinciajuzgado", courtData.provinciaNombre)
     putNorm("horajuicio", courtData.horaJuicioRapido)
     putNorm("fechajuicio", courtData.fechaJuicioRapido)

     /**
      * Calcula distancia de Levenshtein entre dos cadenas.
      *
      * Distancia mínima de ediciones (inserciones, deleciones, sustituciones)
      * necesarias para transformar una cadena en otra.
      *
      * Usado para encontrar el placeholder más cercano cuando no hay coincidencia exacta.
      * Por ejemplo, "fechanacimeinto" (typo) coincidiría con "fechanacimiento" (distancia=1).
      *
      * Complejidad: O(|a| × |b|)
      *
      * @param a Primera cadena
      * @param b Segunda cadena
      * @return Distancia mínima de edición (0 = iguales, > 2 = muy diferentes)
      */
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

     /**
      * Reemplaza todos los placeholders con búsqueda tolerante.
      *
      * Flujo:
      * 1. Busca patrón `[[...]]` en el texto
      * 2. Normaliza el nombre del placeholder (elimina espacios, guiones)
      * 3. Si está en el mapa exacto, usa el valor
      * 4. Si no, busca la clave normalizada más cercana con Levenshtein
      * 5. Si distancia ≤ 2, usa ese valor; si no, deja el placeholder original
      *
      * Esto permite que errores tipográficos leves se corrijan automáticamente.
      */
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
