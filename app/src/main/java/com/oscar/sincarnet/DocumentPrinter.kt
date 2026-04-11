package com.oscar.sincarnet

import android.content.Context
import com.oscar.sincarnet.BluetoothPrinterUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.full.callSuspend
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ============================================================
// DocumentPrinter
//
// Centraliza la preparación e impresión de todas las diligencias.
// Cada función:
//   1. Lee los datos de los Storage necesarios
//   2. Construye el mapa de placeholders [[clave]] → valor real
//   3. Lee el JSON de assets/docs/
//   4. Llama a printDiligencia() que resuelve los placeholders
//      y renderiza el documento con el layout estándar de BluetoothPrinterUtils
//
// Uso (desde el botón de impresión de cada pantalla):
//   DocumentPrinter.imprimirDerechos(context, mac)
//   DocumentPrinter.imprimirCitacionJuicioRapido(context, mac)
//   DocumentPrinter.imprimirCitacionJuicio(context, mac)
//   DocumentPrinter.imprimirManifestacion(context, mac)
// ============================================================
object DocumentPrinter {
    private const val DELAY_BETWEEN_DOCS_MS: Long = 3000L

    // Marcador especial que BluetoothPrinterUtils detecta en el body para
    // pintar las cajas CPCL de citación a juicio en ese punto exacto.
    const val JUICIO_BOXES_MARKER = "\u0000JUICIO_BOXES\u0000"

    // ----------------------------------------------------------
    // 02derechos.json
    // Diligencia de investigación e información de derechos
    // ----------------------------------------------------------
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

    // ----------------------------------------------------------
    // citacionjuiciorapido.json
    // Diligencia de citación para juicio rápido
    // ----------------------------------------------------------
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
            "datosjuzgado" to buildDatosJuzgado(juzgado)
        )

        printDiligencia(context, mac, "docs/citacionjuiciorapido.json", placeholders, investigado)
    }

    // ----------------------------------------------------------
    // citacionjuicio.json
    // Diligencia de citación para juicio ordinario
    // ----------------------------------------------------------
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
            "datosjuzgado" to buildDatosJuzgado(juzgado)
        )

        printDiligencia(context, mac, "docs/citacionjuicio.json", placeholders, investigado)
    }

    // ----------------------------------------------------------
// 04manifestacion.json
// Diligencia de manifestación del investigado no detenido
// ----------------------------------------------------------
    fun imprimirManifestacion(context: Context, mac: String?) {
        val ocurrencia = OcurrenciaDelitStorage(context).loadCurrent()
        val juzgado = JuzgadoAtestadoStorage(context).loadCurrent()
        val investigado = PersonaInvestigadaStorage(context).loadCurrent()
        val vehiculo = VehiculoStorage(context).loadCurrent()
        val manifestacion = ManifestacionStorage(context).loadCurrent()
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm 'horas del día' dd/MM/yyyy")
        val horaFechaAhora = now.format(formatter)

        val respuestas = manifestacion.respuestasPreguntas

        // === CAMBIO: placeholders con el texto final "SI" / "NO" ===
        val renunciaStr = when (manifestacion.renunciaAsistenciaLetrada) {
            true -> "SI"
            false -> "NO"
            null -> "SI/NO"
        }
        val deseaStr = when (manifestacion.deseaDeclarar) {
            true -> "SI"
            false -> "NO"
            null -> "SI/NO"
        }

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
            "deseaDeclarar" to deseaStr
        )

        printDiligencia(context, mac, "docs/04manifestacion.json", placeholders, investigado)
    }

    // ----------------------------------------------------------
    // Helpers de construcción de strings
    // ----------------------------------------------------------

    private fun buildNombreCompleto(p: PersonaInvestigadaData): String =
        listOf(p.firstName, p.lastName1, p.lastName2)
            .filter { it.isNotBlank() }
            .joinToString(" ")

    private fun buildLugar(o: OcurrenciaDelitData): String {
        val parts = listOf(o.carretera, o.pk, o.localidad).filter { it.isNotBlank() }
        return parts.joinToString(", ")
    }

    private fun buildDatosJuzgado(j: JuzgadoAtestadoData): String {
        val parts = listOf(j.sedeNombre, j.sedeDireccion, j.municipioNombre)
            .filter { it.isNotBlank() }
        return parts.joinToString(", ")
    }

    // ----------------------------------------------------------
    // Motor de renderizado de diligencias
    //
    // Lee el JSON, construye el body resolviendo los placeholders,
    // y llama a printDocument() de BluetoothPrinterUtils.
    //
    // El body se construye recorriendo las secciones conocidas
    // del JSON en el orden en que aparecen.
    // ----------------------------------------------------------
    // Versión suspend: bloquea hasta que el trabajo llega a la impresora.
    // Usada tanto por las funciones públicas (lanzadas en corutina propia)
    // como por el flujo secuencial de imprimirAtestadoCompleto.
    private suspend fun printDiligenciaSuspend(
        context: Context,
        mac: String,
        jsonAssetPath: String,
        placeholders: Map<String, String>,
        investigado: PersonaInvestigadaData,
        sigs: PrintSignatures? = null
    ) {
        android.util.Log.d(
            "Impresion",
            "[printDiligenciaSuspend] INICIO jsonAssetPath=$jsonAssetPath"
        )
        val raw = context.assets.open(jsonAssetPath).bufferedReader().readText()
        android.util.Log.d("Impresion", "[printDiligenciaSuspend] JSON leído (${raw.length} chars)")
        val json = JSONObject(raw)
        val doc = json.getJSONObject("documento")
        // === AQUÍ SE FUERZA EL TÍTULO EXACTO ===
        // Si es la diligencia de derechos (02derechos.json o cualquier JSON que contenga "derechos" o "inicio")
        // se ignora lo que venga en el JSON y se usa exactamente el título que pediste.
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
        // --- Extraer QR si existe ---
        var qrDato: String? = null
        if (doc.has("qr")) {
            val qrObj = doc.getJSONObject("qr")
            qrDato = qrObj.optString("dato", null)
        }
        android.util.Log.d("Impresion", "[printDiligenciaSuspend] qrDato=$qrDato")
        try {
            if (!qrDato.isNullOrBlank()) {
                android.util.Log.d(
                    "Impresion",
                    "[printDiligenciaSuspend] Llamando a BluetoothPrinterUtils.printDocument..."
                )
                BluetoothPrinterUtils.printDocument(context, mac, doc, title, qrDato, sigs)
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
                    juicioData = placeholders
                )
            }
            android.util.Log.d("Impresion", "[printDiligenciaSuspend] Impresión finalizada OK")
        } catch (e: Exception) {
            android.util.Log.e("Impresion", "[printDiligenciaSuspend] ERROR: ${e.message}", e)
            throw e
        }
    }

    // Versión no-suspend para llamadas individuales desde botones
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

    // Resuelve todos los [[placeholder]] en un string
    private fun resolve(text: String, placeholders: Map<String, String>): String {
        var result = text
        for ((key, value) in placeholders) {
            result = result.replace("[[$key]]", value)
        }
        return result
    }

    // Construye el body completo de la diligencia recorriendo todas las secciones
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
                    val respuesta = when (id) {
                        1 -> ph["renunciaAsistenciaLetrada"] ?: "SI/NO"
                        2 -> ph["deseaDeclarar"] ?: "SI/NO"
                        else -> opt.optString("respuesta", "SI/NO")
                    }
                    // Primero el texto de la opción (puede hacer wrap)
                    line("  $id. ${opt.optString("texto", "")}")
                    // Luego la respuesta en línea propia, siempre visible
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
    suspend fun imprimirDerechosSuspend(
        context: android.content.Context,
        mac: String,
        sigs: PrintSignatures? = null
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
            sigs
        )
    }

    suspend fun imprimirCitacionJuicioRapidoSuspend(
        context: android.content.Context,
        mac: String,
        sigs: PrintSignatures? = null
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
            "datosjuzgado" to buildDatosJuzgado(juzgado)
        )
        printDiligenciaSuspend(
            context,
            mac,
            "docs/citacionjuiciorapido.json",
            placeholders,
            investigado,
            sigs
        )
    }

    suspend fun imprimirCitacionJuicioSuspend(
        context: android.content.Context,
        mac: String,
        sigs: PrintSignatures? = null
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
            "datosjuzgado" to buildDatosJuzgado(juzgado)
        )
        printDiligenciaSuspend(
            context,
            mac,
            "docs/citacionjuicio.json",
            placeholders,
            investigado,
            sigs
        )
    }

    suspend fun imprimirManifestacionSuspend(
        context: android.content.Context,
        mac: String,
        sigs: PrintSignatures? = null
    ) {
        val ocurrencia = OcurrenciaDelitStorage(context).loadCurrent()
        val juzgado = JuzgadoAtestadoStorage(context).loadCurrent()
        val investigado = PersonaInvestigadaStorage(context).loadCurrent()
        val vehiculo = VehiculoStorage(context).loadCurrent()
        val manifestacion = ManifestacionStorage(context).loadCurrent()
        val formatter =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm 'horas del día' dd/MM/yyyy")
        val horaFecha = java.time.LocalDateTime.now().format(formatter)
        val respuestas = manifestacion.respuestasPreguntas
        val renunciaStr = when (manifestacion.renunciaAsistenciaLetrada) {
            true -> "SI"
            false -> "NO"
            null -> "SI/NO"
        }
        val deseaStr = when (manifestacion.deseaDeclarar) {
            true -> "SI"
            false -> "NO"
            null -> "SI/NO"
        }
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
            "deseaDeclarar" to deseaStr
        )
        printDiligenciaSuspend(
            context,
            mac,
            "docs/04manifestacion.json",
            placeholders,
            investigado,
            sigs
        )
    }

    // ----------------------------------------------------------
    // 03letradogratis.json
    // Información sobre el derecho de asistencia jurídica gratuita
    // ----------------------------------------------------------
    suspend fun imprimirLetradoGratisSuspend(
        context: Context,
        mac: String,
        sigs: PrintSignatures? = null
    ) {
        val placeholders = emptyMap<String, String>()
        // No imprimir firmas en 03letradogratis
        printDiligenciaSuspend(
            context,
            mac,
            "docs/03letradogratis.json",
            placeholders,
            PersonaInvestigadaData(),
            null
        )
    }

    // ============================================================
// 05inmovilizacion.json
// Acta de inmovilización
// ============================================================
    suspend fun imprimirInmovilizacionSuspend(
        context: Context,
        mac: String,
        sigs: PrintSignatures? = null
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
        android.util.Log.d(
            "Impresion",
            "[imprimirInmovilizacionSuspend] existeSegundoConductor=$existeSegundoConductor, datosSegundoCompleto='$datosSegundoCompleto'"
        )
        val conductorPrincipal = listOf(
            investigado.firstName,
            investigado.lastName1,
            investigado.lastName2
        ).filter { it.isNotBlank() }.joinToString("  ")
        val datosConductorYDoc =
            if (conductorPrincipal.isNotBlank() && investigado.documentIdentification.isNotBlank()) "$conductorPrincipal (${investigado.documentIdentification})" else conductorPrincipal
        android.util.Log.d(
            "Impresion",
            "[imprimirInmovilizacionSuspend] datosConductorYDoc='$datosConductorYDoc'"
        )
        val now = java.time.LocalDateTime.now()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        val horafecha = now.format(formatter)
        android.util.Log.d("Impresion", "[imprimirInmovilizacionSuspend] horafecha='$horafecha'")
        val placeholders = mutableMapOf(
            "lugarhechos" to (ocurrencia.localidad.ifBlank { ocurrencia.terminoMunicipal }),
            "horafecha" to horafecha,
            "marca" to vehiculo.brand,
            "modelo" to vehiculo.model,
            "matricula" to vehiculo.plate,
            "datosconductorydocumento" to datosConductorYDoc,
            "datosconductorhabilitado" to datosSegundoCompleto,
            "personasehacecargo" to datosSegundoCompleto,
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
        var respuesta1 = "NO"
        var respuesta2 = "SI"
        if (existeSegundoConductor && datosSegundoCompleto.isNotBlank()) {
            respuesta1 = "SI"
            respuesta2 = "NO"
        }
        placeholders["respuesta_manifestacion_1"] = respuesta1
        placeholders["respuesta_manifestacion_2"] = respuesta2
        android.util.Log.d(
            "Impresion",
            "[imprimirInmovilizacionSuspend] placeholders=$placeholders"
        )

        // === CORRECCIÓN: Crear PrintSignatures con flags correctas ===
        val hasSecond = existeSegundoConductor && datosSegundoCompleto.isNotBlank()
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
        android.util.Log.d(
            "Impresion",
            "[DEBUG] firmasInmovilizacion => isInmovilizacion=${firmasInmovilizacion.isInmovilizacion}, " +
                    "hasSecondDriver=${firmasInmovilizacion.hasSecondDriver}, " +
                    "secondDriver=${firmasInmovilizacion.secondDriver != null}"
        )
        // ===========================================================

        printDiligenciaSuspend(
            context,
            mac,
            "docs/05inmovilizacion.json",
            placeholders,
            investigado,
            firmasInmovilizacion
        )
        android.util.Log.d("Impresion", "[imprimirInmovilizacionSuspend] finalizado")
    }

    // ============================================================
// Estado del progreso de impresión (compartido)
// ============================================================


    // ============================================================
// imprimirAtestadoCompleto (compartido)
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

        data class DocJob(val name: String, val print: suspend () -> Unit)

        val docs = buildList<DocJob> {
            // 1. 01inicio
            add(DocJob("Diligencia de inicio") {
                // Se asume que existe la función imprimirInicioSuspend
                if (DocumentPrinter::class.members.any { it.name == "imprimirInicioSuspend" }) {
                    val method =
                        DocumentPrinter::class.members.first { it.name == "imprimirInicioSuspend" }
                    method.callSuspend(DocumentPrinter, context, mac, sigs)
                }
            })
            // 2. 02derechos
            add(DocJob("Diligencia de derechos del investigado") {
                DocumentPrinter.imprimirDerechosSuspend(context, mac, sigs)
            })
            // 3. 03letradogratis
            add(DocJob("Información asistencia jurídica gratuita") {
                if (DocumentPrinter::class.members.any { it.name == "imprimirLetradoGratisSuspend" }) {
                    val method =
                        DocumentPrinter::class.members.first { it.name == "imprimirLetradoGratisSuspend" }
                    method.callSuspend(DocumentPrinter, context, mac, sigs)
                }
            })
            // 4. 04manifestacion
            add(DocJob("Manifestación del investigado") {
                DocumentPrinter.imprimirManifestacionSuspend(context, mac, sigs)
            })
            // 5. Citación (la que corresponda)
            add(DocJob(if (esJuicioRapido) "Citacion a juicio rápido" else "Citacion a juicio") {
                if (esJuicioRapido) DocumentPrinter.imprimirCitacionJuicioRapidoSuspend(
                    context,
                    mac,
                    sigs
                )
                else DocumentPrinter.imprimirCitacionJuicioSuspend(context, mac, sigs)
            })
            // 6. 05inmovilizacion
            add(DocJob("Acta de inmovilización") {
                val method =
                    DocumentPrinter::class.members.first { it.name == "imprimirInmovilizacionSuspend" }
                method.callSuspend(DocumentPrinter, context, mac, sigs)
            })
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                for ((i, doc) in docs.withIndex()) {
                    withContext(Dispatchers.Main) {
                        onProgress(i + 1, docs.size, doc.name)
                    }
                    doc.print()
                    kotlinx.coroutines.delay(3000L)
                }
                withContext(Dispatchers.Main) { onFinished() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.message ?: "Error desconocido") }
            }
        }
    }
}

