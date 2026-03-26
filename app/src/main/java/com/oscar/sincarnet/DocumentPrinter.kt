package com.oscar.sincarnet

import android.content.Context
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

    // ----------------------------------------------------------
    // 02derechos.json
    // Diligencia de investigación e información de derechos
    // ----------------------------------------------------------
    fun imprimirDerechos(context: Context, mac: String?) {
        val ocurrencia  = OcurrenciaDelitStorage(context).loadCurrent()
        val juzgado     = JuzgadoAtestadoStorage(context).loadCurrent()
        val actuantes   = ActuantesStorage(context).loadCurrent()
        val investigado = PersonaInvestigadaStorage(context).loadCurrent()
        val now         = LocalDateTime.now()

        val lugar = buildLugar(ocurrencia)
        val lugarFechaHoraLectura = "$lugar, ${ocurrencia.fecha} a las ${ocurrencia.hora} horas"
        val lugarFechaHoraDelito  = "${ocurrencia.localidad}, ${ocurrencia.fecha} a las ${ocurrencia.hora} horas"

        val placeholders = mapOf(
            "lugar"                       to lugar,
            "terminomunicipal"            to ocurrencia.terminoMunicipal,
            "partidojudicial"             to juzgado.municipioNombre,
            "hora"                        to ocurrencia.hora,
            "fechacompleta"               to ocurrencia.fecha,
            "instructor"                  to actuantes.instructorTip,
            "secretario"                  to actuantes.secretaryTip,
            "nombrecompletoinvestigado"   to buildNombreCompleto(investigado),
            "documentoidentificacion"     to investigado.documentIdentification,
            "fechanacimiento"             to investigado.birthDate,
            "lugarnacimiento"             to investigado.birthPlace,
            "nombrepadre"                 to investigado.fatherName,
            "nombremadre"                 to investigado.motherName,
            "domicilio"                   to investigado.address,
            "telefono"                    to investigado.phone,
            "correoelectronico"           to investigado.email,
            "lugarfechahoralecturaderechos" to lugarFechaHoraLectura,
            "lugarfechahoracomisióndelito"  to lugarFechaHoraDelito,
            "nombreletrado"               to ""   // se rellena si hay letrado privado
        )

        printDiligencia(context, mac, "docs/02derechos.json", placeholders)
    }

    // ----------------------------------------------------------
    // citacionjuiciorapido.json
    // Diligencia de citación para juicio rápido
    // ----------------------------------------------------------
    fun imprimirCitacionJuicioRapido(context: Context, mac: String?) {
        val ocurrencia  = OcurrenciaDelitStorage(context).loadCurrent()
        val juzgado     = JuzgadoAtestadoStorage(context).loadCurrent()
        val actuantes   = ActuantesStorage(context).loadCurrent()
        val investigado = PersonaInvestigadaStorage(context).loadCurrent()

        val placeholders = mapOf(
            "lugar"                     to buildLugar(ocurrencia),
            "terminomunicipal"          to ocurrencia.terminoMunicipal,
            "partidojudicial"           to juzgado.municipioNombre,
            "hora"                      to ocurrencia.hora,
            "fechacompleta"             to ocurrencia.fecha,
            "instructor"                to actuantes.instructorTip,
            "secretario"                to actuantes.secretaryTip,
            "nombrecompletoinvestigado" to buildNombreCompleto(investigado),
            "documentoidentificacion"   to investigado.documentIdentification,
            "unidadinferior"            to actuantes.instructorUnit,
            "horajuicio"                to juzgado.horaJuicioRapido,
            "fechajuicio"               to juzgado.fechaJuicioRapido,
            "datosjuzgado"              to buildDatosJuzgado(juzgado)
        )

        printDiligencia(context, mac, "docs/citacionjuiciorapido.json", placeholders)
    }

    // ----------------------------------------------------------
    // citacionjuicio.json
    // Diligencia de citación para juicio ordinario
    // ----------------------------------------------------------
    fun imprimirCitacionJuicio(context: Context, mac: String?) {
        val ocurrencia  = OcurrenciaDelitStorage(context).loadCurrent()
        val juzgado     = JuzgadoAtestadoStorage(context).loadCurrent()
        val actuantes   = ActuantesStorage(context).loadCurrent()
        val investigado = PersonaInvestigadaStorage(context).loadCurrent()

        val placeholders = mapOf(
            "lugar"                     to buildLugar(ocurrencia),
            "terminomunicipal"          to ocurrencia.terminoMunicipal,
            "partidojudicial"           to juzgado.municipioNombre,
            "hora"                      to ocurrencia.hora,
            "fechacompleta"             to ocurrencia.fecha,
            "instructor"                to actuantes.instructorTip,
            "secretario"                to actuantes.secretaryTip,
            "nombrecompletoinvestigado" to buildNombreCompleto(investigado),
            "documentoidentificacion"   to investigado.documentIdentification,
            "unidadinferior"            to actuantes.instructorUnit,
            "datosjuzgado"              to buildDatosJuzgado(juzgado)
        )

        printDiligencia(context, mac, "docs/citacionjuicio.json", placeholders)
    }

    // ----------------------------------------------------------
    // 04manifestacion.json
    // Diligencia de manifestación del investigado no detenido
    // ----------------------------------------------------------
    fun imprimirManifestacion(context: Context, mac: String?) {
        val ocurrencia     = OcurrenciaDelitStorage(context).loadCurrent()
        val juzgado        = JuzgadoAtestadoStorage(context).loadCurrent()
        val investigado    = PersonaInvestigadaStorage(context).loadCurrent()
        val vehiculo       = VehiculoStorage(context).loadCurrent()
        val manifestacion  = ManifestacionStorage(context).loadCurrent()
        val now            = LocalDateTime.now()
        val formatter      = DateTimeFormatter.ofPattern("HH:mm 'horas del día' dd/MM/yyyy")
        val horaFechaAhora = now.format(formatter)

        val respuestas = manifestacion.respuestasPreguntas

        val placeholders = mapOf(
            "horafechamanifestacion"    to horaFechaAhora,
            "terminomunicipal"          to ocurrencia.terminoMunicipal,
            "partidojudicial"           to juzgado.municipioNombre,
            "nombrecompletoinvestigado" to buildNombreCompleto(investigado),
            "documentoidentificacion"   to investigado.documentIdentification,
            "matricula"                 to vehiculo.plate,
            "otrosdocumentos"           to (investigado.otrosDocumentos ?: ""),
            "primerapregunta"           to (respuestas[1] ?: ""),
            "segundapregunta"           to (respuestas[2] ?: ""),
            "tercerapregunta"           to (respuestas[3] ?: ""),
            "cuartapregunta"            to (respuestas[4] ?: ""),
            "quintapregunta"            to (respuestas[5] ?: ""),
            "sextapregunta"             to (respuestas[6] ?: ""),
            "septimapregunta"           to (respuestas[7] ?: ""),
            "octavapregunta"            to (respuestas[8] ?: ""),
            "segundafechahora"          to horaFechaAhora
        )

        printDiligencia(context, mac, "docs/04manifestacion.json", placeholders)
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
        sigs: PrintSignatures? = null
    ) {
        val raw   = context.assets.open(jsonAssetPath).bufferedReader().readText()
        val json  = JSONObject(raw)
        val doc   = json.getJSONObject("documento")
        val title = resolve(doc.optString("titulo", ""), placeholders)
        val body  = buildDiligenciaBody(doc, placeholders)
        printDocumentResolvedSuspend(context, mac, title, body, sigs)
    }

    // Versión no-suspend para llamadas individuales desde botones
    private fun printDiligencia(
        context: Context,
        mac: String?,
        jsonAssetPath: String,
        placeholders: Map<String, String>
    ) {
        if (mac.isNullOrBlank()) return
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                printDiligenciaSuspend(context, mac, jsonAssetPath, placeholders)
            } catch (e: Exception) {
                android.util.Log.e("DocumentPrinter", "Error: ${e.message}", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
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
    private fun buildDiligenciaBody(doc: JSONObject, ph: Map<String, String>): String {
        val sb = StringBuilder()

        fun r(text: String) = resolve(text, ph)
        fun line(text: String) { sb.appendLine(r(text)) }
        fun blank() { sb.appendLine() }

        // cuerpo principal
        doc.optJSONObject("cuerpo")?.let { cuerpo ->
            line(cuerpo.optString("descripcion", ""))
            blank()
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
                    val id    = p.optString("id", "")
                    val tit   = p.optString("titulo", "")
                    val texto = when {
                        p.has("campo_variable") -> r(p.getString("campo_variable"))
                        p.has("texto")          -> r(p.getString("texto"))
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
                for (i in 0 until opts.length()) {
                    val opt = opts.getJSONObject(i)
                    val extra = if (opt.has("campo_variable")) " ${r(opt.getString("campo_variable"))}" else ""
                    val nota  = if (opt.has("nota")) " (${opt.getString("nota")})" else ""
                    line("  ${opt.optInt("id")}. ${opt.optString("texto", "")}$extra$nota  [${opt.optString("respuesta", "SI/NO")}]")
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
                    line("  ${opt.optInt("id")}. ${opt.optString("texto", "")}  [${opt.optString("respuesta", "SI/NO")}]")
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
                val id  = p.optInt("id")
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
                        line("  ${opt.optString("afectado", "")}: ${r(opt.optString("descripcion", ""))}")
                    }
                }
                if (sec.has("informacion_adicional")) line(r(sec.getString("informacion_adicional")))
                sec.optJSONArray("items")?.let { items ->
                    for (j in 0 until items.length()) {
                        val item = items.getJSONObject(j)
                        line("  ${item.optString("afectado", "")}: ${r(item.optString("descripcion", ""))}")
                    }
                }
                blank()
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
    suspend fun imprimirDerechosSuspend(context: android.content.Context, mac: String, sigs: PrintSignatures? = null) {
        val ocurrencia  = OcurrenciaDelitStorage(context).loadCurrent()
        val juzgado     = JuzgadoAtestadoStorage(context).loadCurrent()
        val actuantes   = ActuantesStorage(context).loadCurrent()
        val investigado = PersonaInvestigadaStorage(context).loadCurrent()
        val lugar = buildLugar(ocurrencia)
        val placeholders = mapOf(
            "lugar"                        to lugar,
            "terminomunicipal"             to ocurrencia.terminoMunicipal,
            "partidojudicial"              to juzgado.municipioNombre,
            "hora"                         to ocurrencia.hora,
            "fechacompleta"                to ocurrencia.fecha,
            "instructor"                   to actuantes.instructorTip,
            "secretario"                   to actuantes.secretaryTip,
            "nombrecompletoinvestigado"    to buildNombreCompleto(investigado),
            "documentoidentificacion"      to investigado.documentIdentification,
            "fechanacimiento"              to investigado.birthDate,
            "lugarnacimiento"              to investigado.birthPlace,
            "nombrepadre"                  to investigado.fatherName,
            "nombremadre"                  to investigado.motherName,
            "domicilio"                    to investigado.address,
            "telefono"                     to investigado.phone,
            "correoelectronico"            to investigado.email,
            "lugarfechahoralecturaderechos" to "$lugar, ${ocurrencia.fecha} a las ${ocurrencia.hora} horas",
            "lugarfechahoracomisióndelito"  to "${ocurrencia.localidad}, ${ocurrencia.fecha} a las ${ocurrencia.hora} horas",
            "nombreletrado"                to ""
        )
        printDiligenciaSuspend(context, mac, "docs/02derechos.json", placeholders, sigs)
    }

    suspend fun imprimirCitacionJuicioRapidoSuspend(context: android.content.Context, mac: String, sigs: PrintSignatures? = null) {
        val ocurrencia  = OcurrenciaDelitStorage(context).loadCurrent()
        val juzgado     = JuzgadoAtestadoStorage(context).loadCurrent()
        val actuantes   = ActuantesStorage(context).loadCurrent()
        val investigado = PersonaInvestigadaStorage(context).loadCurrent()
        val placeholders = mapOf(
            "lugar"                     to buildLugar(ocurrencia),
            "terminomunicipal"          to ocurrencia.terminoMunicipal,
            "partidojudicial"           to juzgado.municipioNombre,
            "hora"                      to ocurrencia.hora,
            "fechacompleta"             to ocurrencia.fecha,
            "instructor"                to actuantes.instructorTip,
            "secretario"                to actuantes.secretaryTip,
            "nombrecompletoinvestigado" to buildNombreCompleto(investigado),
            "documentoidentificacion"   to investigado.documentIdentification,
            "unidadinferior"            to actuantes.instructorUnit,
            "horajuicio"                to juzgado.horaJuicioRapido,
            "fechajuicio"               to juzgado.fechaJuicioRapido,
            "datosjuzgado"              to buildDatosJuzgado(juzgado)
        )
        printDiligenciaSuspend(context, mac, "docs/citacionjuiciorapido.json", placeholders, sigs)
    }

    suspend fun imprimirCitacionJuicioSuspend(context: android.content.Context, mac: String, sigs: PrintSignatures? = null) {
        val ocurrencia  = OcurrenciaDelitStorage(context).loadCurrent()
        val juzgado     = JuzgadoAtestadoStorage(context).loadCurrent()
        val actuantes   = ActuantesStorage(context).loadCurrent()
        val investigado = PersonaInvestigadaStorage(context).loadCurrent()
        val placeholders = mapOf(
            "lugar"                     to buildLugar(ocurrencia),
            "terminomunicipal"          to ocurrencia.terminoMunicipal,
            "partidojudicial"           to juzgado.municipioNombre,
            "hora"                      to ocurrencia.hora,
            "fechacompleta"             to ocurrencia.fecha,
            "instructor"                to actuantes.instructorTip,
            "secretario"                to actuantes.secretaryTip,
            "nombrecompletoinvestigado" to buildNombreCompleto(investigado),
            "documentoidentificacion"   to investigado.documentIdentification,
            "unidadinferior"            to actuantes.instructorUnit,
            "datosjuzgado"              to buildDatosJuzgado(juzgado)
        )
        printDiligenciaSuspend(context, mac, "docs/citacionjuicio.json", placeholders, sigs)
    }

    suspend fun imprimirManifestacionSuspend(context: android.content.Context, mac: String, sigs: PrintSignatures? = null) {
        val ocurrencia    = OcurrenciaDelitStorage(context).loadCurrent()
        val juzgado       = JuzgadoAtestadoStorage(context).loadCurrent()
        val investigado   = PersonaInvestigadaStorage(context).loadCurrent()
        val vehiculo      = VehiculoStorage(context).loadCurrent()
        val manifestacion = ManifestacionStorage(context).loadCurrent()
        val formatter     = java.time.format.DateTimeFormatter.ofPattern("HH:mm 'horas del día' dd/MM/yyyy")
        val horaFecha     = java.time.LocalDateTime.now().format(formatter)
        val respuestas    = manifestacion.respuestasPreguntas
        val placeholders  = mapOf(
            "horafechamanifestacion"    to horaFecha,
            "terminomunicipal"          to ocurrencia.terminoMunicipal,
            "partidojudicial"           to juzgado.municipioNombre,
            "nombrecompletoinvestigado" to buildNombreCompleto(investigado),
            "documentoidentificacion"   to investigado.documentIdentification,
            "matricula"                 to vehiculo.plate,
            "otrosdocumentos"           to (investigado.otrosDocumentos ?: ""),
            "primerapregunta"           to (respuestas[1] ?: ""),
            "segundapregunta"           to (respuestas[2] ?: ""),
            "tercerapregunta"           to (respuestas[3] ?: ""),
            "cuartapregunta"            to (respuestas[4] ?: ""),
            "quintapregunta"            to (respuestas[5] ?: ""),
            "sextapregunta"             to (respuestas[6] ?: ""),
            "septimapregunta"           to (respuestas[7] ?: ""),
            "octavapregunta"            to (respuestas[8] ?: ""),
            "segundafechahora"          to horaFecha
        )
        printDiligenciaSuspend(context, mac, "docs/04manifestacion.json", placeholders, sigs)
    }

    // ----------------------------------------------------------
    // 03letradogratis.json
    // Información sobre el derecho de asistencia jurídica gratuita
    // ----------------------------------------------------------
    suspend fun imprimirLetradoGratisSuspend(context: Context, mac: String, sigs: PrintSignatures? = null) {
        val placeholders = emptyMap<String, String>()
        // No imprimir firmas en 03letradogratis
        printDiligenciaSuspend(context, mac, "docs/03letradogratis.json", placeholders, null)
    }
}

// ============================================================
// Estado del progreso de impresión (compartido)
// ============================================================
data class PrintProgress(
    val isVisible: Boolean = false,
    val currentDoc: String = "",
    val currentIndex: Int = 0,
    val totalDocs: Int = 0,
    val isError: Boolean = false,
    val errorMessage: String = ""
)

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
    val juzgado     = JuzgadoAtestadoStorage(context).loadCurrent()
    val esJuicioRapido = juzgado.tipoJuicio.contains("rápido", ignoreCase = true) ||
            juzgado.tipoJuicio.contains("rapido", ignoreCase = true)

    data class DocJob(val name: String, val print: suspend () -> Unit)

    val docs = buildList<DocJob> {
        add(DocJob("Diligencia de derechos del investigado") {
            DocumentPrinter.imprimirDerechosSuspend(context, mac, sigs)
        })
        add(DocJob("Información asistencia jurídica gratuita") {
            // 03letradogratis.json: se asume que existe función imprimirLetradoGratisSuspend
            if (DocumentPrinter::class.members.any { it.name == "imprimirLetradoGratisSuspend" }) {
                val method = DocumentPrinter::class.members.first { it.name == "imprimirLetradoGratisSuspend" }
                method.callSuspend(DocumentPrinter, context, mac, sigs)
            }
        })
        add(DocJob("Manifestación del investigado") {
            DocumentPrinter.imprimirManifestacionSuspend(context, mac, sigs)
        })
        add(DocJob(if (esJuicioRapido) "Citacion a juicio rápido" else "Citacion a juicio") {
            if (esJuicioRapido) DocumentPrinter.imprimirCitacionJuicioRapidoSuspend(context, mac, sigs)
            else DocumentPrinter.imprimirCitacionJuicioSuspend(context, mac, sigs)
        })
        // Añadir 05inmovilizacion si existe la función
        if (DocumentPrinter::class.members.any { it.name == "imprimirInmovilizacionSuspend" }) {
            add(DocJob("Acta de inmovilización") {
                val method = DocumentPrinter::class.members.first { it.name == "imprimirInmovilizacionSuspend" }
                method.callSuspend(DocumentPrinter, context, mac, sigs)
            })
        }
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
