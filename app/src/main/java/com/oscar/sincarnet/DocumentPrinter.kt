package com.oscar.sincarnet

import android.content.Context
import com.zebra.sdk.comm.Connection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ═══════════════════════════════════════════════════════════════════════════════
// DOCUMENTPRINTER - ORQUESTADOR CENTRAL DE IMPRESIÓN
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Orquestador central para la impresión de todas las diligencias legales de SinCarnet.
 *
 * **Responsabilidades Principales**:
 * 1. **Lectura de datos**: Obtiene datos desde Storage managers (OcurrenciaDelitStorage, etc.)
 * 2. **Construcción de placeholders**: Mapea datos reales a [[clave]] para plantillas JSON
 * 3. **Carga de plantillas**: Lee documentos JSON desde assets/docs/
 * 4. **Renderizado de documentos**: Resuelve placeholders y renderiza con BluetoothPrinterUtils
 * 5. **Gestión de Bluetooth**: Coordina con SDK de Zebra para impresión
 *
 * **Flujo General**:
 * ```
 * Usuario toca "Imprimir" → imprimirDerechos/Citacion/Manifestacion()
 *   → Lee datos de Storage
 *   → Construye mapa de placeholders
 *   → Carga JSON de plantilla
 *   → printDiligencia() → resuelve placeholders
 *   → BluetoothPrinterUtils.printDocumentSuspend() → envía a Zebra
 * ```
 *
 * **Diligencias Soportadas**:
 * - `imprimirDerechos()` → docs/02derechos.json (Información de derechos)
 * - `imprimirCitacionJuicioRapido()` → docs/citacionjuiciorapido.json
 * - `imprimirCitacionJuicio()` → docs/citacionjuicio.json
 * - `imprimirManifestacion()` → docs/04manifestacion.json
 *
 * **Características Especiales**:
 * - **QR dinámico**: Algunos documentos incluyen códigos QR
 * - **Placeholders tolerantes**: Sistema robusto de resolución de [[clave]]
 * - **JSON anidado**: Soporta estructuras complejas (articulos, subapartados, opciones)
 * - **Conexión compartida**: Reutiliza conexión Bluetooth en flujos de múltiples documentos
 * - **Manejo de errores**: Reintentos automáticos y mensajes descriptivos
 *
 * **Patrones de Uso**:
 * ```kotlin
 * // Desde pantalla de impresión (botón individual)
 * DocumentPrinter.imprimirDerechos(context, mac)
 *
 * // Desde flujo de atestado (múltiples documentos)
 * DocumentPrinter.imprimirAtestadoCompleto(context, mac, sigs, ...)
 * ```
 *
 * **Dependencias**:
 * - BluetoothPrinterUtils → Conversión a CPCL y envío
 * - Storage managers → Lectura de datos persistentes
 * - SDK Zebra → Protocolo Bluetooth de impresión
 *
 * @see BluetoothPrinterUtils Para detalles de conversión CPCL
 * @see OcurrenciaDelitStorage Para datos de delito
 * @see PersonaInvestigadaStorage Para datos del investigado
 * @see ManifestacionStorage Para respuestas de manifestación
 */
object DocumentPrinter {
    // ─────────────────────────────────────────────────────────────────────────
    // CONSTANTES DE CONFIGURACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Delay entre impresiones de documentos cuando se imprimen múltiples.
     * Permite que la impresora procese completamente cada documento antes del siguiente.
     */
    private const val DELAY_BETWEEN_DOCS_MS: Long = 5000L

    /**
     * Tiempo de espera final antes de cerrar la conexión Bluetooth.
     * Asegura que el último documento se imprima completamente antes de desconectar.
     */
    private const val FINAL_DRAIN_BEFORE_CLOSE_MS: Long = 15000L

    private const val BATCH_LOG_TAG = "AtestadoPrintBatch"

    /**
     * Mensaje de validación para manifestación obligatoria.
     * Se muestra si el usuario intenta imprimir sin completar renuncia letrada y desea declarar.
     */
    internal const val MANIFESTACION_REQUIRED_MSG =
        "Debe completar la manifestación (renuncia letrada y desea declarar) antes de imprimir"

    /**
     * Marcador especial que BluetoothPrinterUtils detecta en el body.
     * Cuando está presente, dibuja las cajas CPCL de citación a juicio en ese punto exacto del documento.
     *
     * Se inserta automáticamente en buildDiligenciaBody() cuando hay datos de juicio.
     */
    const val JUICIO_BOXES_MARKER = "\u0000JUICIO_BOXES\u0000"

    // ─────────────────────────────────────────────────────────────────────────
    // 02derechos.json - DILIGENCIA DE INVESTIGACIÓN E INFORMACIÓN DE DERECHOS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Imprime la diligencia de investigación e información de derechos.
     *
     * Documento legal que informa al investigado de sus derechos constitucionales
     * (derecho a guardar silencio, asistencia letrada, etc.)
     *
     * **Datos leídos desde**:
     * - OcurrenciaDelitStorage: lugar, fecha, hora del hecho
     * - JuzgadoAtestadoStorage: juzgado competente
     * - ActuantesStorage: instructor y secretario
     * - PersonaInvestigadaStorage: datos del investigado
     *
     * **Placeholders construidos**:
     * - lugar, hora, fecha, instructor, secretario
     * - nombrecompletoinvestigado, documentoidentificacion
     * - lugarfechahoralecturaderechos, lugarfechahoracomisióndelito
     *
     * @param context Contexto para acceso a Storage y Bluetooth
     * @param mac Dirección MAC de la impresora Bluetooth (null = no imprime)
     *
     * @see OcurrenciaDelitStorage
     * @see PersonaInvestigadaStorage
     */
    fun imprimirDerechos(context: Context, mac: String?) {
        val ocurrencia = OcurrenciaDelitStorage(context).loadCurrent()
        val juzgado = JuzgadoAtestadoStorage(context).loadCurrent()
        val actuantes = ActuantesStorage(context).loadCurrent()
        val investigado = PersonaInvestigadaStorage(context).loadCurrent()
        val now = LocalDateTime.now()

        val lugar = buildLugar(ocurrencia)
        val lugarFechaHoraLectura = "$lugar, ${ocurrencia.fecha} a las ${ocurrencia.hora} horas"
        val lugarFechaHoraDelito =
            "${ocurrencia.localidad}, ${ocurrencia.fecha} a las ${ocurrencia.hora} horas"

        val placeholders = mapOf(
            "lugar" to lugar,
            "terminomunicipal" to ocurrencia.terminoMunicipal,
            "partidojudicial" to juzgado.municipioNombre,
            "hora" to ocurrencia.hora,
            "fechacompleta" to ocurrencia.fecha,
            "instructor" to actuantes.instructorTip,
            "secretario" to actuantes.secretaryTip,
            "nombrecompletoinvestigado" to buildNombreCompleto(investigado),
            "documentoidentificacion" to investigado.documentIdentification,
            "fechanacimiento" to investigado.birthDate,
            "lugarnacimiento" to investigado.birthPlace,
            "nombrepadre" to investigado.fatherName,
            "nombremadre" to investigado.motherName,
            "domicilio" to investigado.address,
            "telefono" to investigado.phone,
            "correoelectronico" to investigado.email,
            "lugarfechahoralecturaderechos" to lugarFechaHoraLectura,
            "lugarfechahoracomisióndelito" to lugarFechaHoraDelito,
            "nombreletrado" to ""   // se rellena si hay letrado privado
        )

        printDiligencia(context, mac, "docs/02derechos.json", placeholders, investigado)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // citacionjuiciorapido.json - CITACIÓN PARA JUICIO RÁPIDO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Imprime la citación para juicio rápido.
     *
     * Documento que cita al investigado para comparecer en juicio en procedimiento rápido
     * (máximo 6 meses). Se usa cuando se espera sentencia condena inmediata.
     *
     * **Datos leídos desde**:
     * - JuzgadoAtestadoStorage: hora y fecha del juicio rápido
     * - Otros: igual que imprimirDerechos()
     *
     * **Placeholders adicionales**:
     * - horajuicio, fechajuicio, datosjuzgado
     *
     * @param context Contexto para acceso a Storage y Bluetooth
     * @param mac Dirección MAC de la impresora (null = no imprime)
     *
     * @see JuzgadoAtestadoStorage.isJuicioRapido
     */
    fun imprimirCitacionJuicioRapido(context: Context, mac: String?) {
        android.util.Log.d("CITACION_PRINT", "imprimirCitacionJuicioRapido: usando plantilla docs/citacionjuiciorapido.json")
        val ocurrencia = OcurrenciaDelitStorage(context).loadCurrent()
        val juzgado = JuzgadoAtestadoStorage(context).loadCurrent()
        val actuantes = ActuantesStorage(context).loadCurrent()
        val investigado = PersonaInvestigadaStorage(context).loadCurrent()

        val placeholders = mapOf(
            "lugar" to buildLugar(ocurrencia),
            "terminomunicipal" to ocurrencia.terminoMunicipal,
            "partidojudicial" to juzgado.municipioNombre,
            "hora" to ocurrencia.hora,
            "fechacompleta" to ocurrencia.fecha,
            "instructor" to actuantes.instructorTip,
            "secretario" to actuantes.secretaryTip,
            "nombrecompletoinvestigado" to buildNombreCompleto(investigado),
            "documentoidentificacion" to investigado.documentIdentification,
            "unidadinferior" to actuantes.instructorUnit,
            "horajuicio" to juzgado.horaJuicioRapido,
            "fechajuicio" to juzgado.fechaJuicioRapido,
            "datosjuzgado" to buildDatosJuzgado(juzgado),
            "provinciajuzgado" to juzgado.provinciaNombre
        )

        printDiligencia(context, mac, "docs/citacionjuiciorapido.json", placeholders, investigado)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // citacionjuicio.json - CITACIÓN PARA JUICIO ORDINARIO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Imprime la citación para juicio ordinario.
     *
     * Documento que cita al investigado para comparecer en juicio en procedimiento ordinario
     * (sin límite temporal). Se usa para casos más complejos que requieren mayor investigación.
     *
     * **Diferencia con juicio rápido**: No incluye hora y fecha del juicio (se determina después)
     *
     * @param context Contexto para acceso a Storage y Bluetooth
     * @param mac Dirección MAC de la impresora (null = no imprime)
     *
     * @see imprimirCitacionJuicioRapido Para citación de juicio rápido
     */
    fun imprimirCitacionJuicio(context: Context, mac: String?) {
        android.util.Log.d("CITACION_PRINT", "imprimirCitacionJuicio: usando plantilla docs/citacionjuicio.json")
        val ocurrencia = OcurrenciaDelitStorage(context).loadCurrent()
        val juzgado = JuzgadoAtestadoStorage(context).loadCurrent()
        val actuantes = ActuantesStorage(context).loadCurrent()
        val investigado = PersonaInvestigadaStorage(context).loadCurrent()

        val placeholders = mapOf(
            "lugar" to buildLugar(ocurrencia),
            "terminomunicipal" to ocurrencia.terminoMunicipal,
            "partidojudicial" to juzgado.municipioNombre,
            "hora" to ocurrencia.hora,
            "fechacompleta" to ocurrencia.fecha,
            "instructor" to actuantes.instructorTip,
            "secretario" to actuantes.secretaryTip,
            "nombrecompletoinvestigado" to buildNombreCompleto(investigado),
            "documentoidentificacion" to investigado.documentIdentification,
            "unidadinferior" to actuantes.instructorUnit,
            "datosjuzgado" to buildDatosJuzgado(juzgado),
            "provinciajuzgado" to juzgado.provinciaNombre
        )

        printDiligencia(context, mac, "docs/citacionjuicio.json", placeholders, investigado)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 04manifestacion.json - MANIFESTACIÓN DEL INVESTIGADO NO DETENIDO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Imprime la manifestación del investigado no detenido.
     *
     * Documento legal que registra:
     * - Si renuncia a asistencia letrada
     * - Si desea declarar
     * - Respuestas a preguntas de la Guardia Civil (8 preguntas)
     *
     * **Validaciones**:
     * - Requiere que renunciaAsistenciaLetrada y deseaDeclarar estén completas
     * - Si alguno es null, se imprime "NO" como valor por defecto
     *
     * **Datos leídos desde**:
     * - ManifestacionStorage: respuestas SI/NO y respuestas a preguntas
     * - OcurrenciaDelitStorage, JuzgadoAtestadoStorage, etc.
     *
     * @param context Contexto para acceso a Storage y Bluetooth
     * @param mac Dirección MAC de la impresora (null = no imprime)
     *
     * @throws IllegalStateException Si manifestación no está completa (pero imprime con valores por defecto)
     *
     * @see ManifestacionStorage
     * @see validateManifestacionForPrint
     */
    fun imprimirManifestacion(context: Context, mac: String?) {
        val ocurrencia = OcurrenciaDelitStorage(context).loadCurrent()
        val juzgado = JuzgadoAtestadoStorage(context).loadCurrent()
        val investigado = PersonaInvestigadaStorage(context).loadCurrent()
        val vehiculo = VehiculoStorage(context).loadCurrent()
        val manifestacion = ManifestacionStorage(context).loadCurrent()
        validateManifestacionForPrint(manifestacion)
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm 'horas del día' dd/MM/yyyy")
        val horaFechaAhora = now.format(formatter)

        val respuestas = manifestacion.respuestasPreguntas

        // === Placeholders con el texto final "SI" / "NO" ===
        val renunciaStr = manifestacionSiNo(
            manifestacion.renunciaAsistenciaLetrada,
            "renunciaAsistenciaLetrada"
        )
        val deseaStr = manifestacionSiNo(
            manifestacion.deseaDeclarar,
            "deseaDeclarar"
        )

        val placeholders = mapOf(
            "horafechamanifestacion" to horaFechaAhora,
            "terminomunicipal" to ocurrencia.terminoMunicipal,
            "partidojudicial" to juzgado.municipioNombre,
            "nombrecompletoinvestigado" to buildNombreCompleto(investigado),
            "documentoidentificacion" to investigado.documentIdentification,
            "matricula" to vehiculo.plate,
            "otrosdocumentos" to (investigado.otrosDocumentos ?: ""),
            "primerapregunta" to (respuestas[1] ?: ""),
            "segundapregunta" to (respuestas[2] ?: ""),
            "tercerapregunta" to (respuestas[3] ?: ""),
            "cuartapregunta" to (respuestas[4] ?: ""),
            "quintapregunta" to (respuestas[5] ?: ""),
            "sextapregunta" to (respuestas[6] ?: ""),
            "septimapregunta" to (respuestas[7] ?: ""),
            "octavapregunta" to (respuestas[8] ?: ""),
            "segundafechahora" to horaFechaAhora,
            "renunciaAsistenciaLetrada" to renunciaStr,
            "deseaDeclarar" to deseaStr,
            // Claves alternativas para mantener compatibilidad entre flujos
            "respuesta_manifestacion_1" to renunciaStr,
            "respuesta_manifestacion_2" to deseaStr
        )

        printDiligencia(context, mac, "docs/04manifestacion.json", placeholders, investigado)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS DE CONSTRUCCIÓN DE STRINGS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Construye el nombre completo del investigado.
     *
     * Combina nombre, primer apellido y segundo apellido, filtrando valores vacíos.
     *
     * Ejemplo: "Juan García López"
     *
     * @param p Datos de persona investigada
     * @return Nombre completo con espacios entre componentes
     */
    private fun buildNombreCompleto(p: PersonaInvestigadaData): String =
        listOf(p.firstName, p.lastName1, p.lastName2)
            .filter { it.isNotBlank() }
            .joinToString(" ")

    /**
     * Construye la ubicación del hecho combinando carretera, PK y localidad.
     *
     * Ejemplo: "N-I, PK 23.5, Alcalá de Henares"
     *
     * @param o Datos de ocurrencia del delito
     * @return Ubicación formateada
     */
    private fun buildLugar(o: OcurrenciaDelitData): String {
        val parts = listOf(o.carretera, o.pk, o.localidad).filter { it.isNotBlank() }
        return parts.joinToString(", ")
    }

    /**
     * Construye datos completos del juzgado para el documento.
     *
     * Combina nombre de la sede, dirección y municipio.
     *
     * @param j Datos del juzgado
     * @return Datos formateados
     */
    private fun buildDatosJuzgado(j: JuzgadoAtestadoData): String {
        val parts = listOf(j.sedeNombre, j.sedeDireccion, j.municipioNombre)
            .filter { it.isNotBlank() }
        return parts.joinToString(", ")
    }

    /**
     * Convierte un valor booleano nullable a "SI" o "NO" para impresión.
     *
     * - null → "NO" (con advertencia en log)
     * - true → "SI"
     * - false → "NO"
     *
     * @param value Valor booleano de manifestación
     * @param fieldName Nombre del campo (para logging)
     * @return "SI" o "NO"
     */
    private fun manifestacionSiNo(value: Boolean?, fieldName: String): String = when (value) {
        true -> "SI"
        false -> "NO"
        null -> {
            android.util.Log.w(
                "Impresion",
                "[Manifestacion] '$fieldName' es null. Se imprimirá 'NO' para evitar el literal '[SI/NO]'"
            )
            "NO"
        }
    }

    /**
     * Valida que los campos críticos de manifestación estén completos.
     *
     * Si renunciaAsistenciaLetrada o deseaDeclarar son null, registra advertencia
     * pero permite continuar (se usan valores por defecto "NO").
     *
     * @param data Datos de manifestación a validar
     *
     * @see manifestacionSiNo
     */
    private fun validateManifestacionForPrint(data: ManifestacionData) {
        if (data.renunciaAsistenciaLetrada == null || data.deseaDeclarar == null) {
            android.util.Log.w(
                "Impresion",
                "[validateManifestacionForPrint] Manifestación incompleta " +
                        "(renunciaAsistenciaLetrada=${data.renunciaAsistenciaLetrada}, " +
                        "deseaDeclarar=${data.deseaDeclarar}). " +
                        "Se imprimirá con valores por defecto 'NO'."
            )
        }
    }

    /**
     * Busca una opción en un JSONArray por su ID.
     *
     * Útil para encontrar subopciones dinámicamente en manifiestos y documentos.
     *
     * @param array Array de opciones (puede ser null)
     * @param id ID a buscar (se convierte a string para comparación)
     * @return JSONObject con la opción encontrada, o null
     */
    private fun findJsonOptionById(array: JSONArray?, id: Any): JSONObject? {
        if (array == null) return null
        val target = id.toString()
        for (i in 0 until array.length()) {
            val option = array.optJSONObject(i) ?: continue
            if (option.opt("id")?.toString() == target) return option
        }
        return null
    }

    /**
     * Construye el body del documento "inicio" para atestados complejos.
     *
     * Maneja:
     * - Secciones de cuerpo y vehículo
     * - Selección de motivo (Siniestro, Control, Infracción)
     * - Constancia de carencia de autorización
     * - Vicisitudes especiales (pérdida de vigencia, condena, etc.)
     *
     * @param doc JSONObject del documento
     * @param inicioData Datos iniciales/modales del atestado
     * @param ocurrencia Datos de ocurrencia
     * @param juzgado Datos del juzgado
     * @param actuantes Datos de actuantes
     * @param investigado Datos del investigado
     * @param vehiculo Datos del vehículo
     * @return Body completo con todas las secciones resueltas
     */
    private fun buildInicioBody(
        doc: JSONObject,
        inicioData: AtestadoInicioModalData,
        ocurrencia: OcurrenciaDelitData,
        juzgado: JuzgadoAtestadoData,
        actuantes: ActuantesData,
        investigado: PersonaInvestigadaData,
        vehiculo: VehiculoData
    ): String {
        fun resolveInicio(text: String): String = replaceCitacionPlaceholders(
            text = text,
            courtData = juzgado,
            personData = investigado,
            ocurrenciaData = ocurrencia,
            instructorTip = actuantes.instructorTip,
            secretaryTip = actuantes.secretaryTip,
            instructorUnit = actuantes.instructorUnit,
            vehicleData = vehiculo,
            inicioModalData = inicioData
        )

        val sb = StringBuilder()
        fun line(text: String) {
            val resolved = resolveInicio(text).trim()
            if (resolved.isNotBlank()) sb.appendLine(resolved)
        }
        fun blank() {
            if (sb.isNotEmpty()) sb.appendLine()
        }

        doc.optJSONObject("cuerpo")?.optString("descripcion", "")?.let {
            line(it)
            blank()
        }

        doc.optJSONObject("vehiculo")?.optString("descripcion", "")?.let {
            line(it)
            blank()
        }

        doc.optJSONObject("motivo_identificacion")?.let { motivo ->
            line(motivo.optString("descripcion", ""))
            val motivoId = when (inicioData.motivo) {
                "Siniestro Vial" -> 1
                "Control preventivo" -> 2
                "Cometer infracción" -> 3
                else -> null
            }
            motivoId?.let { id ->
                val motivoSeleccionado = findJsonOptionById(motivo.optJSONArray("opciones"), id)
                    ?.optString("texto", "")
                    .orEmpty()
                line(motivoSeleccionado)
            }
            blank()
        }

        doc.optJSONObject("constatacion_carencia_autorizacion")?.let { constatacion ->
            line(constatacion.optString("descripcion", ""))
            val situaciones: JSONArray? = constatacion.optJSONArray("situaciones")
            if (inicioData.dgtNoRecord) {
                line(findJsonOptionById(situaciones, 1)?.optString("texto", "").orEmpty())
            }
            if (inicioData.internationalNoRecord) {
                line(findJsonOptionById(situaciones, 2)?.optString("texto", "").orEmpty())
            }
            if (inicioData.existsRecord) {
                val situacion3 = findJsonOptionById(situaciones, 3)
                line(situacion3?.optString("texto", "").orEmpty())
                val subOptionId = when (inicioData.vicisitudesOption) {
                    "No ha obtenido nunca" -> "3a"
                    "Pérdida de vigencia por pérdida de puntos" -> "3b"
                    "Condena firme en vigor" -> "3c"
                    "No consta realización y superación de cursos" -> "3d"
                    else -> null
                }
                subOptionId?.let { id ->
                    val subtexto = findJsonOptionById(situacion3?.optJSONArray("subopciones"), id)
                        ?.optString("texto", "")
                        .orEmpty()
                    line(subtexto)
                }
            }
            blank()
        }

        when {
            doc.has("cierre") && doc.get("cierre") is String -> line(doc.getString("cierre"))
            doc.has("cierre") && doc.get("cierre") is JSONObject ->
                line(doc.getJSONObject("cierre").optString("texto", ""))
        }

        return sb.toString().trimEnd()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MOTOR DE RENDERIZADO DE DILIGENCIAS (VERSIÓN SUSPEND)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Renderiza e imprime una diligencia de forma asincrónica.
     *
     * Proceso:
     * 1. Lee el JSON de plantilla desde assets
     * 2. Resuelve todos los [[placeholder]] con datos reales
     * 3. Construye el body completo de la diligencia
     * 4. Envía a BluetoothPrinterUtils para conversión CPCL
     * 5. Imprime en la impresora Zebra
     *
     * **Manejo de QR**: Si el documento incluye sección "qr", se usa printDocumentSuspend()
     * que renderiza el código. Si no, se usa printDocumentResolvedSuspend().
     *
     * @param context Contexto para Storage y Bluetooth
     * @param mac Dirección MAC de la impresora
     * @param jsonAssetPath Ruta en assets del JSON de plantilla (p. ej. "docs/02derechos.json")
     * @param placeholders Mapa de [[clave]] → valor para reemplazar en la plantilla
     * @param investigado Datos del investigado (para construcción de body)
     * @param sigs Firmas capturadas (opcional, para atestados)
     * @param sharedConn Conexión compartida Bluetooth (opcional, reutiliza de flujo anterior)
     *
     * @throws Exception Si hay error al leer JSON, renderizar o imprimir
     *
     * @see BluetoothPrinterUtils.printDocumentSuspend
     * @see printDocumentResolvedSuspend
     */
    private suspend fun printDiligenciaSuspend(
        context: Context,
        mac: String,
        jsonAssetPath: String,
        placeholders: Map<String, String>,
        investigado: PersonaInvestigadaData,
        sigs: PrintSignatures? = null,
        sharedConn: Connection? = null          // ← conexión compartida opcional
    ) {
        android.util.Log.d(
            "Impresion",
            "[printDiligenciaSuspend] INICIO jsonAssetPath=$jsonAssetPath"
        )
        val raw = context.assets.open(jsonAssetPath).bufferedReader().readText()
        android.util.Log.d("Impresion", "[printDiligenciaSuspend] JSON leído (${raw.length} chars)")
        val json = JSONObject(raw)
        val doc = json.getJSONObject("documento")
        val tituloJson = doc.optString("titulo", "")
        val title = when {
            jsonAssetPath.contains("derechos") || jsonAssetPath.contains("inicio") ->
                "DILIGENCIA INVESTIGACIÓN E INFORMACIÓN DERECHOS A INVESTIGADO NO DETENIDO"
            else -> resolve(tituloJson, placeholders)
        }
        android.util.Log.d("Impresion", "[printDiligenciaSuspend] Título: '$title'")
        val body = buildDiligenciaBody(doc, placeholders, investigado)
        android.util.Log.d(
            "Impresion",
            "[printDiligenciaSuspend] Body generado (${body.length} chars)"
        )
        var qrDato: String? = null
        if (doc.has("qr")) {
            val qrObj = doc.getJSONObject("qr")
            qrDato = qrObj.opt("dato")?.toString()?.takeIf { it.isNotBlank() }
        }
        android.util.Log.d("Impresion", "[printDiligenciaSuspend] qrDato=$qrDato")
        try {
            if (!qrDato.isNullOrBlank()) {
                // Documentos con QR: usan su propia conexión (no afecta al flujo de atestado)
                android.util.Log.d(
                    "Impresion",
                    "[printDiligenciaSuspend] Llamando a BluetoothPrinterUtils.printDocumentSuspend..."
                )
                BluetoothPrinterUtils.printDocumentSuspend(context, mac, doc, title, qrDato, sigs)
            } else {
                android.util.Log.d(
                    "Impresion",
                    "[printDiligenciaSuspend] Llamando a printDocumentResolvedSuspend..."
                )
                android.util.Log.d("Impresion", "[printDiligenciaSuspend] sigs antes de llamar: " +
                        "isInmovilizacion=${sigs?.isInmovilizacion}, " +
                        "hasSecondDriver=${sigs?.hasSecondDriver}, " +
                        "secondDriver=${sigs?.secondDriver != null}")
                printDocumentResolvedSuspend(
                    context,
                    mac,
                    title,
                    body,
                    sigs,
                    juicioData = placeholders,
                    sharedConn = sharedConn         // ← propagar conexión compartida
                )
            }
            android.util.Log.d("Impresion", "[printDiligenciaSuspend] Impresión finalizada OK")
        } catch (e: Exception) {
            android.util.Log.e("Impresion", "[printDiligenciaSuspend] ERROR: ${e.message}", e)
            throw e
        }
    }

    /**
     * Versión NO asincrónica para impresión desde botones individuales.
     *
     * Crea una coroutine en IO dispatcher para no bloquear la UI.
     * Muestra Toast con error si la impresión falla.
     *
     * @param context Contexto
     * @param mac Dirección MAC de impresora (null = no hace nada)
     * @param jsonAssetPath Ruta de plantilla
     * @param placeholders Mapa de placeholders
     * @param investigado Datos investigado
     *
     * @see printDiligenciaSuspend Para versión asincrónica
     */
    private fun printDiligencia(
        context: Context,
        mac: String?,
        jsonAssetPath: String,
        placeholders: Map<String, String>,
        investigado: PersonaInvestigadaData
    ) {
        if (mac.isNullOrBlank()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                printDiligenciaSuspend(context, mac, jsonAssetPath, placeholders, investigado)
            } catch (e: Exception) {
                android.util.Log.e("DocumentPrinter", "Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context, "Error al imprimir: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Resuelve todos los [[placeholder]] en un texto usando un mapa.
     *
     * Búsqueda simple case-sensitive. Útil para reemplazar placeholders en títulos
     * sin necesidad de lógica compleja.
     *
     * @param text Texto con placeholders [[clave]]
     * @param placeholders Mapa de [[clave]] → valor
     * @return Texto con placeholders resueltos
     */
    private fun resolve(text: String, placeholders: Map<String, String>): String {
        var result = text
        for ((key, value) in placeholders) {
            result = result.replace("[[$key]]", value)
        }
        return result
    }

    /**
     * Construye el body completo de una diligencia.
     *
     * Recorre todas las secciones del documento JSON:
     * - cuerpo, articulos, derechos_articulo_520
     * - manifestacion_investigado, opciones_investigado
     * - hechos_investigacion, etc.
     *
     * Resuelve placeholders en cada sección y respeta la estructura.
     *
     * @param doc JSONObject de "documento" del JSON
     * @param ph Mapa de placeholders
     * @param investigado Datos del investigado (para manifestación)
     * @return Body completo de la diligencia
     *
     * @see resolve
     */
    private fun buildDiligenciaBody(
        doc: JSONObject,
        ph: Map<String, String>,
        investigado: PersonaInvestigadaData
    ): String {
        val sb = StringBuilder()

        fun r(text: String) = resolve(text, ph)
        fun line(text: String) {
            sb.appendLine(r(text))
        }

        fun blank() {
            sb.appendLine()
        }

        // cuerpo principal
        val tieneJuicio =
            ph.containsKey("fechajuicio") && ph.containsKey("horajuicio") && ph.containsKey("datosjuzgado")
        doc.optJSONObject("cuerpo")?.let { cuerpo ->
            line(cuerpo.optString("descripcion", ""))
            blank()
            // Si hay datos de juicio, insertar marcador para que BluetoothPrinterUtils
            // pinte las cajas CPCL (BOX) en este punto exacto del documento.
            if (tieneJuicio) sb.appendLine(JUICIO_BOXES_MARKER)
        }

        // --- Añadir soporte para 'articulos' (usado en 03letradogratis) ---
        doc.optJSONArray("articulos")?.let { articulos ->
            for (i in 0 until articulos.length()) {
                val art = articulos.getJSONObject(i)
                val artId = art.optString("id", "")
                val artTitulo = art.optString("titulo", "")
                val artDesc = art.optString("descripcion", "")
                if (artId.isNotBlank() || artTitulo.isNotBlank()) {
                    line("${artId}${if (artTitulo.isNotBlank()) ". $artTitulo" else ""}")
                }
                if (artDesc.isNotBlank()) {
                    line(artDesc)
                }
                // Apartados
                art.optJSONArray("apartados")?.let { apartados ->
                    for (j in 0 until apartados.length()) {
                        val ap = apartados.getJSONObject(j)
                        val apId = ap.optString("id", "")
                        val apTexto = ap.optString("texto", "")
                        val apNota = ap.optString("nota", "")
                        if (apId.isNotBlank() || apTexto.isNotBlank()) {
                            line("  ${apId}${if (apTexto.isNotBlank()) ". $apTexto" else ""}")
                        }
                        if (apNota.isNotBlank()) {
                            line("    Nota: $apNota")
                        }
                        // Subapartados
                        ap.optJSONArray("subapartados")?.let { subs ->
                            for (k in 0 until subs.length()) {
                                val sub = subs.getJSONObject(k)
                                val subId = sub.optString("id", "")
                                val subTexto = sub.optString("texto", "")
                                if (subId.isNotBlank() || subTexto.isNotBlank()) {
                                    line("    ${subId}${if (subTexto.isNotBlank()) ". $subTexto" else ""}")
                                }
                            }
                        }
                    }
                }
                blank()
            }
        }

        // momento_informacion_derechos
        doc.optJSONObject("momento_informacion_derechos")?.let { sec ->
            line(sec.optString("descripcion", ""))
            sec.optJSONArray("opciones")?.let { opts ->
                for (i in 0 until opts.length()) {
                    val opt = opts.getJSONObject(i)
                    line("  ${opt.optInt("id")}. ${opt.optString("texto", "")}")
                }
            }
            blank()
        }

        // hechos_investigacion
        doc.optJSONObject("hechos_investigacion")?.let { sec ->
            line(sec.optString("descripcion", ""))
            sec.optJSONArray("puntos")?.let { puntos ->
                for (i in 0 until puntos.length()) {
                    val p = puntos.getJSONObject(i)
                    val id = p.optString("id", "")
                    val tit = p.optString("titulo", "")
                    val texto = when {
                        p.has("campo_variable") -> r(p.getString("campo_variable"))
                        p.has("texto") -> r(p.getString("texto"))
                        else -> ""
                    }
                    line("  $id. $tit: $texto")
                }
            }
            blank()
        }

        // derechos_articulo_520
        doc.optJSONObject("derechos_articulo_520")?.let { sec ->
            line(sec.optString("descripcion", ""))
            sec.optJSONArray("derechos")?.let { derechos ->
                for (i in 0 until derechos.length()) {
                    val d = derechos.getJSONObject(i)
                    line("  ${d.optString("id")}) ${r(d.optString("texto", ""))}")
                }
            }
            blank()
        }

        // manifestacion_investigado
        doc.optJSONObject("manifestacion_investigado")?.let { sec ->
            line(sec.optString("descripcion", ""))
            sec.optJSONArray("opciones")?.let { opts ->
                // Obtener respuestas reales del investigado
                val respuestas = mapOf(
                    1 to investigado.rightToRemainSilentInformed,
                    2 to investigado.waivesLegalAssistance,
                    3 to investigado.requestsPrivateLawyer,
                    4 to investigado.requestsDutyLawyer,
                    5 to investigado.accessesEssentialProceedings,
                    6 to investigado.needsInterpreter
                )

                fun toSiNo(value: Boolean?): String = when (value) {
                    true -> "SI"
                    false -> "NO"
                    null -> ""
                }
                for (i in 0 until opts.length()) {
                    val opt = opts.getJSONObject(i)
                    val extra =
                        if (opt.has("campo_variable")) " ${r(opt.getString("campo_variable"))}" else ""
                    val nota = if (opt.has("nota")) " (${opt.getString("nota")})" else ""
                    val id = opt.optInt("id")
                    val respuesta = toSiNo(respuestas[id])
                    line("  $id. ${opt.optString("texto", "")}$extra$nota  [$respuesta]")
                }
            }
            blank()
        }

        // opciones_investigado (manifestacion)
        doc.optJSONObject("opciones_investigado")?.let { sec ->
            line(sec.optString("descripcion", ""))
            sec.optJSONArray("opciones")?.let { opts ->
                for (i in 0 until opts.length()) {
                    val opt = opts.getJSONObject(i)
                    val id = opt.optInt("id")
                    val respuestaById = when (id) {
                        1 -> ph["renunciaAsistenciaLetrada"]
                            ?: ph["respuesta_manifestacion_1"]
                        2 -> ph["deseaDeclarar"]
                            ?: ph["respuesta_manifestacion_2"]
                        else -> null
                    }
                    val respuestaByIndex = when (i) {
                        0 -> ph["renunciaAsistenciaLetrada"] ?: ph["respuesta_manifestacion_1"]
                        1 -> ph["deseaDeclarar"] ?: ph["respuesta_manifestacion_2"]
                        else -> null
                    }
                    val respuesta = (respuestaById ?: respuestaByIndex ?: opt.optString("respuesta", "NO"))
                        .ifBlank { "NO" }
                    line("  $id. ${opt.optString("texto", "")}")
                    line("     Respuesta: [$respuesta]")
                }
            }
            blank()
        }

        // documentacion (manifestacion)
        doc.optJSONObject("documentacion")?.let { sec ->
            line(sec.optString("descripcion", ""))
            sec.optJSONArray("anexos")?.let { anexos ->
                for (i in 0 until anexos.length()) {
                    val a = anexos.getJSONObject(i)
                    line("  ${a.optString("id")}: ${r(a.optString("texto", ""))}")
                }
            }
            blank()
        }

        // preguntas (manifestacion)
        doc.optJSONArray("preguntas")?.let { preguntas ->
            for (i in 0 until preguntas.length()) {
                val p = preguntas.getJSONObject(i)
                val id = p.optInt("id")
                val preg = p.optString("pregunta", "")
                val resp = r(p.optString("campo_variable", ""))
                line("$id. $preg")
                line("   R: $resp")
                blank()
            }
        }

        // elementos_esenciales_impugnacion
        doc.optJSONObject("elementos_esenciales_impugnacion")?.let { sec ->
            line(sec.optString("descripcion", ""))
            line(r(sec.optString("texto", "")))
            blank()
        }

        // secciones (citaciones)
        doc.optJSONArray("secciones")?.let { secciones ->
            for (i in 0 until secciones.length()) {
                val sec = secciones.getJSONObject(i)
                line(sec.optString("titulo", ""))
                if (sec.has("contenido")) line(r(sec.getString("contenido")))
                sec.optJSONArray("opciones")?.let { opts ->
                    for (j in 0 until opts.length()) {
                        val opt = opts.getJSONObject(j)
                        line(
                            "  ${opt.optString("afectado", "")}: ${
                                r(
                                    opt.optString(
                                        "descripcion",
                                        ""
                                    )
                                )
                            }"
                        )
                    }
                }
                if (sec.has("informacion_adicional")) line(r(sec.getString("informacion_adicional")))
                sec.optJSONArray("items")?.let { items ->
                    for (j in 0 until items.length()) {
                        val item = items.getJSONObject(j)
                        line(
                            "  ${item.optString("afectado", "")}: ${
                                r(
                                    item.optString(
                                        "descripcion",
                                        ""
                                    )
                                )
                            }"
                        )
                    }
                }
                blank()
            }
        }
        // ==================== NUEVAS SECCIONES PARA 05inmovilizacion.json ====================

        // 1. Normativa aplicable
        doc.optJSONObject("normativa_aplicable")?.let { sec ->
            line(sec.optString("descripcion", ""))
            blank()
            sec.optJSONArray("normas")?.let { normas ->
                for (i in 0 until normas.length()) {
                    val n = normas.getJSONObject(i)
                    line("${n.optInt("id")}. ${n.optString("referencia", "")}")
                    val texto = n.optString("texto", "")
                    texto.split("\n").forEach { line(it.trim()) }
                    blank()
                }
            }
        }

        // 2. Introducción + Manifestaciones (con lógica del segundo conductor)
        doc.optString("introduccion_manifestaciones").let { intro ->
            if (intro.isNotBlank()) {
                line(intro)
                blank()
            }
        }

        doc.optJSONArray("manifestaciones")?.let { manifests ->
            for (i in 0 until manifests.length()) {
                val m = manifests.getJSONObject(i)
                val id = m.optInt("id")
                val texto = m.optString("texto", "")

                val respuesta = when (id) {
                    1 -> ph["respuesta_manifestacion_1"] ?: m.optString("respuesta", "SI/NO")
                    2 -> ph["respuesta_manifestacion_2"] ?: m.optString("respuesta", "SI/NO")
                    else -> m.optString("respuesta", "SI/NO")
                }

                line("$id. $texto  [$respuesta]")

                // === DATOS DEL SEGUNDO CONDUCTOR (solo si respondió SI) ===
                if (id == 1 && respuesta == "SI") {
                    val sub = m.optJSONObject("datos_conductor_habilitado")
                    if (sub != null) {
                        val desc = sub.optString("descripcion", "")
                        if (desc.isNotBlank()) line("   $desc")

                        val campoVar = sub.optString("campo_variable", "")
                        if (campoVar.isNotBlank()) {
                            line("   ${r(campoVar)}")   // ← aquí se dibuja el nombre + DNI
                        }

                        val condiciones = sub.optString("condiciones", "")
                        if (condiciones.isNotBlank()) {
                            condiciones.split("\n").forEach { line("   $it") }
                        }
                    }
                }

                // Información adicional para la opción 2
                if (id == 2) {
                    val info = m.optString("informacion_levantamiento", "")
                    if (info.isNotBlank()) {
                        info.split("\n").forEach { line(it.trim()) }
                    }
                }

                blank()
            }
        }

        // 3. Responsabilidades penales
        doc.optJSONObject("responsabilidades_penales")?.let { sec ->
            line(sec.optString("descripcion", ""))
            blank()
            sec.optJSONArray("articulos")?.let { arts ->
                for (i in 0 until arts.length()) {
                    val a = arts.getJSONObject(i)
                    line(a.optString("referencia", ""))
                    val txt = a.optString("texto", "")
                    txt.split("\n").forEach { line(it.trim()) }
                    blank()
                }
            }
        }

        // cierre
        when {
            doc.has("cierre") && doc.get("cierre") is String ->
                line(r(doc.getString("cierre")))

            doc.has("cierre") && doc.get("cierre") is JSONObject ->
                line(r(doc.getJSONObject("cierre").optString("texto", "")))
        }

        // enterado
        doc.optJSONObject("enterado")?.let { ent ->
            blank()
            line(ent.optString("titulo", ""))
            line(r(ent.optString("texto", "")))
        }

        return sb.toString().trimEnd()
    }

    // ============================================================
    // Versiones suspend — bloquean realmente hasta que el trabajo
    // se envía a la impresora. Usadas por imprimirAtestadoCompleto
    // para garantizar impresión secuencial sin conexiones solapadas.
    // ============================================================
    suspend fun imprimirInicioSuspend(
        context: Context,
        mac: String,
        sigs: PrintSignatures? = null,
        sharedConn: Connection? = null
    ) {
        val ocurrencia = OcurrenciaDelitStorage(context).loadCurrent()
        val juzgado = JuzgadoAtestadoStorage(context).loadCurrent()
        val actuantes = ActuantesStorage(context).loadCurrent()
        val investigado = PersonaInvestigadaStorage(context).loadCurrent()
        val vehiculo = VehiculoStorage(context).loadCurrent()
        val inicioData = AtestadoInicioStorage(context).loadCurrent()
        val raw = context.assets.open("docs/01inicio.json").bufferedReader().readText()
        val doc = JSONObject(raw).getJSONObject("documento")
        val title = replaceCitacionPlaceholders(
            text = doc.optString("titulo", ""),
            courtData = juzgado,
            personData = investigado,
            ocurrenciaData = ocurrencia,
            instructorTip = actuantes.instructorTip,
            secretaryTip = actuantes.secretaryTip,
            instructorUnit = actuantes.instructorUnit,
            vehicleData = vehiculo,
            inicioModalData = inicioData
        )
        val body = buildInicioBody(doc, inicioData, ocurrencia, juzgado, actuantes, investigado, vehiculo)
        printDocumentResolvedSuspend(context, mac, title, body, sigs, sharedConn = sharedConn)
    }

    suspend fun imprimirDerechosSuspend(
        context: android.content.Context,
        mac: String,
        sigs: PrintSignatures? = null,
        sharedConn: Connection? = null      // ← añadido
    ) {
        val ocurrencia = OcurrenciaDelitStorage(context).loadCurrent()
        val juzgado = JuzgadoAtestadoStorage(context).loadCurrent()
        val actuantes = ActuantesStorage(context).loadCurrent()
        val investigado = PersonaInvestigadaStorage(context).loadCurrent()
        val lugar = buildLugar(ocurrencia)
        val placeholders = mapOf(
            "lugar" to lugar,
            "terminomunicipal" to ocurrencia.terminoMunicipal,
            "partidojudicial" to juzgado.municipioNombre,
            "hora" to ocurrencia.hora,
            "fechacompleta" to ocurrencia.fecha,
            "instructor" to actuantes.instructorTip,
            "secretario" to actuantes.secretaryTip,
            "nombrecompletoinvestigado" to buildNombreCompleto(investigado),
            "documentoidentificacion" to investigado.documentIdentification,
            "fechanacimiento" to investigado.birthDate,
            "lugarnacimiento" to investigado.birthPlace,
            "nombrepadre" to investigado.fatherName,
            "nombremadre" to investigado.motherName,
            "domicilio" to investigado.address,
            "telefono" to investigado.phone,
            "correoelectronico" to investigado.email,
            "lugarfechahoralecturaderechos" to "$lugar, ${ocurrencia.fecha} a las ${ocurrencia.hora} horas",
            "lugarfechahoracomisióndelito" to "${ocurrencia.localidad}, ${ocurrencia.fecha} a las ${ocurrencia.hora} horas",
            "nombreletrado" to ""
        )
        printDiligenciaSuspend(
            context,
            mac,
            "docs/02derechos.json",
            placeholders,
            investigado,
            sigs,
            sharedConn
        )
    }

    suspend fun imprimirCitacionJuicioRapidoSuspend(
        context: android.content.Context,
        mac: String,
        sigs: PrintSignatures? = null,
        sharedConn: Connection? = null      // ← añadido
    ) {
        val ocurrencia = OcurrenciaDelitStorage(context).loadCurrent()
        val juzgado = JuzgadoAtestadoStorage(context).loadCurrent()
        val actuantes = ActuantesStorage(context).loadCurrent()
        val investigado = PersonaInvestigadaStorage(context).loadCurrent()
        val placeholders = mapOf(
            "lugar" to buildLugar(ocurrencia),
            "terminomunicipal" to ocurrencia.terminoMunicipal,
            "partidojudicial" to juzgado.municipioNombre,
            "hora" to ocurrencia.hora,
            "fechacompleta" to ocurrencia.fecha,
            "instructor" to actuantes.instructorTip,
            "secretario" to actuantes.secretaryTip,
            "nombrecompletoinvestigado" to buildNombreCompleto(investigado),
            "documentoidentificacion" to investigado.documentIdentification,
            "unidadinferior" to actuantes.instructorUnit,
            "horajuicio" to juzgado.horaJuicioRapido,
            "fechajuicio" to juzgado.fechaJuicioRapido,
            "datosjuzgado" to buildDatosJuzgado(juzgado),
            "provinciajuzgado" to juzgado.provinciaNombre
        )
        printDiligenciaSuspend(
            context,
            mac,
            "docs/citacionjuiciorapido.json",
            placeholders,
            investigado,
            sigs,
            sharedConn
        )
    }

    suspend fun imprimirCitacionJuicioSuspend(
        context: android.content.Context,
        mac: String,
        sigs: PrintSignatures? = null,
        sharedConn: Connection? = null      // ← añadido
    ) {
        val ocurrencia = OcurrenciaDelitStorage(context).loadCurrent()
        val juzgado = JuzgadoAtestadoStorage(context).loadCurrent()
        val actuantes = ActuantesStorage(context).loadCurrent()
        val investigado = PersonaInvestigadaStorage(context).loadCurrent()
        val placeholders = mapOf(
            "lugar" to buildLugar(ocurrencia),
            "terminomunicipal" to ocurrencia.terminoMunicipal,
            "partidojudicial" to juzgado.municipioNombre,
            "hora" to ocurrencia.hora,
            "fechacompleta" to ocurrencia.fecha,
            "instructor" to actuantes.instructorTip,
            "secretario" to actuantes.secretaryTip,
            "nombrecompletoinvestigado" to buildNombreCompleto(investigado),
            "documentoidentificacion" to investigado.documentIdentification,
            "unidadinferior" to actuantes.instructorUnit,
            "datosjuzgado" to buildDatosJuzgado(juzgado),
            "provinciajuzgado" to juzgado.provinciaNombre
        )
        printDiligenciaSuspend(
            context,
            mac,
            "docs/citacionjuicio.json",
            placeholders,
            investigado,
            sigs,
            sharedConn
        )
    }

    suspend fun imprimirManifestacionSuspend(
        context: android.content.Context,
        mac: String,
        sigs: PrintSignatures? = null,
        sharedConn: Connection? = null      // ← añadido
    ) {
        val ocurrencia = OcurrenciaDelitStorage(context).loadCurrent()
        val juzgado = JuzgadoAtestadoStorage(context).loadCurrent()
        val investigado = PersonaInvestigadaStorage(context).loadCurrent()
        val vehiculo = VehiculoStorage(context).loadCurrent()
        val manifestacion = ManifestacionStorage(context).loadCurrent()
        validateManifestacionForPrint(manifestacion)
        val formatter =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm 'horas del día' dd/MM/yyyy")
        val horaFecha = java.time.LocalDateTime.now().format(formatter)

        val respuestas = manifestacion.respuestasPreguntas
        val renunciaStr = manifestacionSiNo(
            manifestacion.renunciaAsistenciaLetrada,
            "renunciaAsistenciaLetrada"
        )
        val deseaStr = manifestacionSiNo(
            manifestacion.deseaDeclarar,
            "deseaDeclarar"
        )
        val placeholders = mapOf(
            "horafechamanifestacion" to horaFecha,
            "terminomunicipal" to ocurrencia.terminoMunicipal,
            "partidojudicial" to juzgado.municipioNombre,
            "nombrecompletoinvestigado" to buildNombreCompleto(investigado),
            "documentoidentificacion" to investigado.documentIdentification,
            "matricula" to vehiculo.plate,
            "otrosdocumentos" to (investigado.otrosDocumentos ?: ""),
            "primerapregunta" to (respuestas[1] ?: ""),
            "segundapregunta" to (respuestas[2] ?: ""),
            "tercerapregunta" to (respuestas[3] ?: ""),
            "cuartapregunta" to (respuestas[4] ?: ""),
            "quintapregunta" to (respuestas[5] ?: ""),
            "sextapregunta" to (respuestas[6] ?: ""),
            "septimapregunta" to (respuestas[7] ?: ""),
            "octavapregunta" to (respuestas[8] ?: ""),
            "segundafechahora" to horaFecha,
            "renunciaAsistenciaLetrada" to renunciaStr,
            "deseaDeclarar" to deseaStr,
            "respuesta_manifestacion_1" to renunciaStr,
            "respuesta_manifestacion_2" to deseaStr
        )
        printDiligenciaSuspend(
            context, mac, "docs/04manifestacion.json", placeholders, investigado, sigs, sharedConn
        )
    }

    suspend fun imprimirLetradoGratisSuspend(
        context: Context,
        mac: String,
        sigs: PrintSignatures? = null,
        sharedConn: Connection? = null      // ← añadido
    ) {
        val placeholders = emptyMap<String, String>()
        printDiligenciaSuspend(
            context, mac, "docs/03letradogratis.json",
            placeholders, PersonaInvestigadaData(), null, sharedConn
        )
    }

    suspend fun imprimirInmovilizacionSuspend(
        context: Context,
        mac: String,
        sigs: PrintSignatures? = null,
        sharedConn: Connection? = null      // ← añadido
    ) {
        android.util.Log.d("Impresion", "[imprimirInmovilizacionSuspend] INICIO")
        val ocurrencia = OcurrenciaDelitStorage(context).loadCurrent()
        val juzgado = JuzgadoAtestadoStorage(context).loadCurrent()
        val actuantes = ActuantesStorage(context).loadCurrent()
        val investigado = PersonaInvestigadaStorage(context).loadCurrent()
        val vehiculo = VehiculoStorage(context).loadCurrent()
        val prefs = context.getSharedPreferences("segundo_conductor", Context.MODE_PRIVATE)
        val existeSegundoConductor = prefs.getBoolean("existe", false)
        val nombreSegundo = prefs.getString("nombre", "")?.trim() ?: ""
        val docSegundo = prefs.getString("documento", "")?.trim() ?: ""
        val datosSegundoCompleto =
            if (nombreSegundo.isNotBlank() && docSegundo.isNotBlank()) "$nombreSegundo ($docSegundo)" else nombreSegundo
        val conductorPrincipal = listOf(
            investigado.firstName,
            investigado.lastName1,
            investigado.lastName2
        ).filter { it.isNotBlank() }.joinToString("  ")
        val datosConductorYDoc =
            if (conductorPrincipal.isNotBlank() && investigado.documentIdentification.isNotBlank()) "$conductorPrincipal (${investigado.documentIdentification})" else conductorPrincipal
        val now = java.time.LocalDateTime.now()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        val horafecha = now.format(formatter)
        val hasSecond = sigs?.hasSecondDriver == true
        val datosSegundoParaActa = if (hasSecond) datosSegundoCompleto else ""
        val placeholders = mutableMapOf(
            "lugarhechos" to (ocurrencia.localidad.ifBlank { ocurrencia.terminoMunicipal }),
            "horafecha" to horafecha,
            "marca" to vehiculo.brand,
            "modelo" to vehiculo.model,
            "matricula" to vehiculo.plate,
            "datosconductorydocumento" to datosConductorYDoc,
            "datosconductorhabilitado" to datosSegundoParaActa,
            "personasehacecargo" to datosSegundoParaActa,
            "lugar" to (ocurrencia.localidad.ifBlank { ocurrencia.terminoMunicipal }),
            "terminomunicipal" to ocurrencia.terminoMunicipal,
            "partidojudicial" to juzgado.municipioNombre,
            "hora" to ocurrencia.hora,
            "fechacompleta" to ocurrencia.fecha,
            "instructor" to actuantes.instructorTip,
            "secretario" to actuantes.secretaryTip,
            "nombrecompletoinvestigado" to conductorPrincipal,
            "documentoidentificacion" to investigado.documentIdentification
        )
        // --- Lógica de manifestaciones ---
        val respuesta1 = if (hasSecond) "SI" else "NO"
        val respuesta2 = if (hasSecond) "NO" else "SI"
        placeholders["respuesta_manifestacion_1"] = respuesta1
        placeholders["respuesta_manifestacion_2"] = respuesta2

        android.util.Log.d(
            "Impresion",
            "[imprimirInmovilizacionSuspend] hasSecondUI=$hasSecond, " +
                "prefExiste=$existeSegundoConductor, datosSegundo='${datosSegundoParaActa}'"
        )

        val firmasInmovilizacion = PrintSignatures(
            instructor = sigs?.instructor,
            instructorTip = sigs?.instructorTip ?: actuantes.instructorTip,
            secretary = sigs?.secretary,
            secretaryTip = sigs?.secretaryTip ?: actuantes.secretaryTip,
            investigated = sigs?.investigated,
            secondDriver = if (hasSecond) sigs?.secondDriver else null,
            isInmovilizacion = true,
            hasSecondDriver = hasSecond
        )
        printDiligenciaSuspend(
            context, mac, "docs/05inmovilizacion.json",
            placeholders, investigado, firmasInmovilizacion, sharedConn
        )
        android.util.Log.d("Impresion", "[imprimirInmovilizacionSuspend] finalizado")
    }

    // ============================================================
    // imprimirAtestadoCompleto
    // Abre UNA SOLA conexión BT → imprime todos los documentos →
    // cierra la conexión. Así el emparejamiento sólo ocurre una vez.
    // ============================================================
    fun imprimirAtestadoCompleto(
        context: Context,
        mac: String,
        sigs: PrintSignatures? = null,
        onProgress: (index: Int, total: Int, docName: String) -> Unit,
        onFinished: () -> Unit,
        onError: (String) -> Unit
    ) {
        val juzgado = JuzgadoAtestadoStorage(context).loadCurrent()
        val esJuicioRapido = juzgado.isJuicioRapido()

        // Cada trabajo recibe la conexión compartida ya abierta
        data class DocJob(val name: String, val print: suspend (Connection) -> Unit)

        val docs = buildList<DocJob> {
            // 1. Diligencia de inicio
            add(DocJob("Diligencia de conocimiento e inicio") { conn ->
                DocumentPrinter.imprimirInicioSuspend(context, mac, sigs, conn)
            })
            // 2. Diligencia de derechos
            add(DocJob("Diligencia de derechos del investigado") { conn ->
                DocumentPrinter.imprimirDerechosSuspend(context, mac, sigs, conn)
            })
            // 3. Letrado gratuito
            add(DocJob("Información asistencia jurídica gratuita") { conn ->
                DocumentPrinter.imprimirLetradoGratisSuspend(context, mac, sigs, conn)
            })
            // 4. Manifestación
            add(DocJob("Manifestación del investigado") { conn ->
                DocumentPrinter.imprimirManifestacionSuspend(context, mac, sigs, conn)
            })
            // 5. Citación
            add(DocJob(if (esJuicioRapido) "Citacion a juicio rápido" else "Citacion a juicio") { conn ->
                if (esJuicioRapido) DocumentPrinter.imprimirCitacionJuicioRapidoSuspend(context, mac, sigs, conn)
                else DocumentPrinter.imprimirCitacionJuicioSuspend(context, mac, sigs, conn)
            })
            // 6. Acta de inmovilización
            add(DocJob("Acta de inmovilización") { conn ->
                DocumentPrinter.imprimirInmovilizacionSuspend(context, mac, sigs, conn)
            })
        }

        CoroutineScope(Dispatchers.IO).launch {
            var sharedConn: Connection? = null
            try {
                android.util.Log.i(BATCH_LOG_TAG, "Inicio lote impresión atestado: totalDocs=${docs.size}, mac=$mac")
                // ── 1. Descubrimiento / Emparejamiento + Conexión BT (UNA SOLA VEZ) ──
                withContext(Dispatchers.Main) {
                    onProgress(0, docs.size, "Conectando con la impresora…")
                }
                val connectStartedAt = System.currentTimeMillis()
                sharedConn = openSharedBtConnection(mac)
                android.util.Log.i(
                    BATCH_LOG_TAG,
                    "Conexión BT lista en ${System.currentTimeMillis() - connectStartedAt} ms"
                )

                // ── 2-6. Preparación, Conversión, Envío de cada documento ────────────
                for ((i, doc) in docs.withIndex()) {
                    val jobIndex = i + 1
                    val startedAt = System.currentTimeMillis()
                    android.util.Log.i(
                        BATCH_LOG_TAG,
                        "[$jobIndex/${docs.size}] INICIO doc='${doc.name}'"
                    )
                    withContext(Dispatchers.Main) {
                        onProgress(jobIndex, docs.size, doc.name)
                    }
                    doc.print(sharedConn)
                    android.util.Log.i(
                        BATCH_LOG_TAG,
                        "[$jobIndex/${docs.size}] FIN doc='${doc.name}' en ${System.currentTimeMillis() - startedAt} ms"
                    )
                    if (i < docs.size - 1) {
                        android.util.Log.d(
                            BATCH_LOG_TAG,
                            "[$jobIndex/${docs.size}] Espera entre documentos: ${DELAY_BETWEEN_DOCS_MS} ms"
                        )
                        kotlinx.coroutines.delay(DELAY_BETWEEN_DOCS_MS)
                    }
                }
                android.util.Log.i(BATCH_LOG_TAG, "Lote impresión atestado finalizado correctamente")
                withContext(Dispatchers.Main) { onFinished() }
            } catch (e: Exception) {
                android.util.Log.e(BATCH_LOG_TAG, "Error en lote impresión atestado: ${e.message}", e)
                withContext(Dispatchers.Main) { onError(e.message ?: "Error desconocido") }
            } finally {
                // ── 7. Desconexión (siempre, aunque haya error) ──────────────────────
                sharedConn?.let {
                    android.util.Log.i(
                        BATCH_LOG_TAG,
                        "Espera final antes de cerrar BT: ${FINAL_DRAIN_BEFORE_CLOSE_MS} ms"
                    )
                    kotlinx.coroutines.delay(FINAL_DRAIN_BEFORE_CLOSE_MS)
                    closeSharedBtConnection(it)
                }
                android.util.Log.i(BATCH_LOG_TAG, "Conexión BT de lote cerrada")
            }
        }
    }
}
