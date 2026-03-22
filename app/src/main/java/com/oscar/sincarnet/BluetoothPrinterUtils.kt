package com.oscar.sincarnet

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.zebra.sdk.comm.BluetoothConnection
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.graphics.ZebraImageFactory
import com.zebra.sdk.printer.SGD
import com.zebra.sdk.printer.ZebraPrinter
import com.zebra.sdk.printer.ZebraPrinterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException

private const val TAG = "PrinterRW"

// ============================================================
// PAPEL
// ============================================================
private const val PAPER_W_MM    = 99f
private const val PRINTER_DPI   = 200
private const val MARGIN        = 10
private const val MARGIN_BOTTOM = 120

private val PAPER_W get() = (PAPER_W_MM / 25.4f * PRINTER_DPI).toInt()

// ============================================================
// IMÁGENES
// ============================================================
private const val IMG1_W = 95;  private const val IMG1_H = 102
private const val IMG2_W = 65;  private const val IMG2_H = 101
private const val IMG_Y  = MARGIN

private val IMG1_X   get() = MARGIN
private val IMG2_X   get() = PAPER_W - MARGIN - IMG2_W
private val HEADER_H get() = maxOf(IMG1_H, IMG2_H)

// ============================================================
// TÍTULO  — Font 7 size 3
// ============================================================
private const val TITLE_FONT   = 7
private const val TITLE_SIZE   = 3
private const val TITLE_LINE_H = 34
private const val TITLE_CHAR_W = 12
private const val TITLE_BOLD   = 2

private val TITLE_X1    get() = IMG1_X + IMG1_W + 7
private val TITLE_X2    get() = IMG2_X - 15
private val TITLE_W     get() = TITLE_X2 - TITLE_X1
private val TITLE_CHARS get() = TITLE_W / TITLE_CHAR_W

// ============================================================
// CUERPO  — Font 7 size 0
// ============================================================
private const val BODY_FONT      = 7
private const val BODY_SIZE      = 0
private const val BODY_LINE_H    = 22
private const val BODY_CHAR_W    = 12
private const val HEADER_BOLD    = 2
private const val BODY_GAP_AFTER = 5

private val BODY_Y     get() = IMG_Y + HEADER_H + 20
private val BODY_CHARS get() = (PAPER_W - MARGIN * 2) / BODY_CHAR_W

// ============================================================
// QR
// ============================================================
private const val QR_MODULE     = 3
private const val QR_SIZE       = 140
private const val QR_GAP        = 6
private const val QR_LABEL_FONT = 5
private const val QR_LABEL_SIZE = 0

private val QR_X get() = PAPER_W - MARGIN - QR_SIZE

// ============================================================
// API PÚBLICA — para uso desde botones individuales
// Lanza la corutina internamente y retorna inmediatamente.
// ============================================================

fun printDocumentFromJson(context: Context, mac: String?, jsonAssetPath: String) {
    if (mac.isNullOrBlank()) {
        Toast.makeText(context, "No hay impresora configurada", Toast.LENGTH_SHORT).show()
        return
    }
    CoroutineScope(Dispatchers.IO).launch {
        try {
            printDocumentFromJsonSuspend(context, mac, jsonAssetPath)
        } catch (e: Exception) {
            showError(context, e)
        }
    }
}

fun printDocument(
    context: Context,
    mac: String?,
    doc: JSONObject = JSONObject(),
    title: String   = "",
    qrUrl: String   = ""
) {
    if (mac.isNullOrBlank()) {
        Toast.makeText(context, "No hay impresora configurada", Toast.LENGTH_SHORT).show()
        return
    }
    CoroutineScope(Dispatchers.IO).launch {
        try {
            printDocumentSuspend(context, mac, doc, title, qrUrl)
        } catch (e: Exception) {
            showError(context, e)
        }
    }
}

fun printDocumentResolved(context: Context, mac: String?, title: String, body: String) {
    if (mac.isNullOrBlank()) {
        Toast.makeText(context, "No hay impresora configurada", Toast.LENGTH_SHORT).show()
        return
    }
    CoroutineScope(Dispatchers.IO).launch {
        try {
            printDocumentResolvedSuspend(context, mac, title, body)
        } catch (e: Exception) {
            showError(context, e)
        }
    }
}

// ============================================================
// API SUSPEND — para uso desde flujos secuenciales
// (DocumentPrinter / imprimirAtestadoCompleto)
// Bloquea hasta que el trabajo se envía completamente.
// La conexión se abre y cierra dentro de cada llamada.
// ============================================================

suspend fun printDocumentFromJsonSuspend(
    context: Context,
    mac: String,
    jsonAssetPath: String
) {
    val json  = JSONObject(context.assets.open(jsonAssetPath).bufferedReader().readText())
    val doc   = json.getJSONObject("documento")
    val title = doc.optString("titulo", "")
    val qrUrl = doc.optString("qr_url", "")
    printDocumentSuspend(context, mac, doc, title, qrUrl)
}

suspend fun printDocumentResolvedSuspend(
    context: Context,
    mac: String,
    title: String,
    body: String
) {
    val pw         = PAPER_W
    val titleLines = wrapText(title, TITLE_CHARS)
    val bodyLines  = wrapText(body,  BODY_CHARS)
    val contentH   = BODY_Y + bodyLines.size * BODY_LINE_H + MARGIN_BOTTOM

    val cpcl = buildString {
        if (titleLines.isNotEmpty()) {
            val totalTitleH = titleLines.size * TITLE_LINE_H
            val titleStartY = IMG_Y + (HEADER_H - totalTitleH) / 2
            append("! U1 SETBOLD $TITLE_BOLD\r\n")
            titleLines.forEachIndexed { i, line ->
                val lineW = line.length * TITLE_CHAR_W
                val lineX = TITLE_X1 + (TITLE_W - lineW) / 2
                val lineY = titleStartY + i * TITLE_LINE_H
                append("TEXT $TITLE_FONT $TITLE_SIZE $lineX $lineY $line\r\n")
            }
            append("! U1 SETBOLD 0\r\n")
        }
        bodyLines.forEachIndexed { i, line ->
            append("TEXT $BODY_FONT $BODY_SIZE $MARGIN ${BODY_Y + i * BODY_LINE_H} $line\r\n")
        }
        append("PRINT\r\n")
    }

    sendToprinter(context, mac, contentH, cpcl, pw)
    Log.d(TAG, "printDocumentResolvedSuspend OK — contentH=$contentH bodyLines=${bodyLines.size}")
}

suspend fun printDocumentSuspend(
    context: Context,
    mac: String,
    doc: JSONObject = JSONObject(),
    title: String   = "",
    qrUrl: String   = ""
) {
    val pw           = PAPER_W
    val contentBlocks = mutableListOf<String>()
    var curY         = BODY_Y

    // Introducción
    val intro = doc.optString("introduccion", "")
    if (intro.isNotEmpty()) {
        for (line in wrapText(intro, BODY_CHARS)) {
            contentBlocks.add("TEXT $BODY_FONT $BODY_SIZE $MARGIN $curY $line\r\n")
            curY += BODY_LINE_H
        }
        curY += BODY_GAP_AFTER
    }

    // Artículos
    val articulos = doc.optJSONArray("articulos")
    if (articulos != null) {
        for (i in 0 until articulos.length()) {
            val art   = articulos.getJSONObject(i)
            val artId = art.optString("id", "")
            val artTt = art.optString("titulo", "")

            if (artId.isNotEmpty() || artTt.isNotEmpty()) {
                curY += BODY_GAP_AFTER
                val header = if (artTt.isNotEmpty()) "$artId. $artTt" else artId
                contentBlocks.add("! U1 SETBOLD $HEADER_BOLD\r\n")
                for (line in wrapText(header, BODY_CHARS)) {
                    contentBlocks.add("TEXT $BODY_FONT $BODY_SIZE $MARGIN $curY $line\r\n")
                    curY += BODY_LINE_H
                }
                contentBlocks.add("! U1 SETBOLD 0\r\n")
                curY += 3
            }

            val desc = art.optString("descripcion", "")
            if (desc.isNotEmpty()) {
                for (line in wrapText(desc, BODY_CHARS)) {
                    contentBlocks.add("TEXT $BODY_FONT $BODY_SIZE $MARGIN $curY $line\r\n")
                    curY += BODY_LINE_H
                }
                curY += 3
            }

            val aps = art.optJSONArray("apartados")
            if (aps != null) {
                for (j in 0 until aps.length()) {
                    val ap    = aps.getJSONObject(j)
                    val id    = ap.optString("id", "")
                    val texto = when {
                        ap.has("texto") -> "$id) ${ap.getString("texto")}"
                        ap.has("nota")  -> "$id) [${ap.getString("nota")}]"
                        else -> ""
                    }
                    if (texto.isNotEmpty()) {
                        for (line in wrapText(texto, BODY_CHARS)) {
                            contentBlocks.add("TEXT $BODY_FONT $BODY_SIZE $MARGIN $curY $line\r\n")
                            curY += BODY_LINE_H
                        }
                    }
                }
            }

            val puntos = art.optJSONArray("puntos")
            if (puntos != null) {
                for (j in 0 until puntos.length()) {
                    for (line in wrapText(puntos.getString(j), BODY_CHARS)) {
                        contentBlocks.add("TEXT $BODY_FONT $BODY_SIZE $MARGIN $curY $line\r\n")
                        curY += BODY_LINE_H
                    }
                }
            }

            curY += BODY_GAP_AFTER
        }
    }

    val qrBlockH = if (qrUrl.isNotEmpty()) QR_GAP + QR_SIZE + BODY_LINE_H + 8 else 0
    val contentH = curY + qrBlockH + MARGIN_BOTTOM
    val titleLines = wrapText(title, TITLE_CHARS)

    val cpcl = buildString {
        if (titleLines.isNotEmpty()) {
            val totalTitleH = titleLines.size * TITLE_LINE_H
            val titleStartY = IMG_Y + (HEADER_H - totalTitleH) / 2
            append("! U1 SETBOLD $TITLE_BOLD\r\n")
            titleLines.forEachIndexed { i, line ->
                val lineW = line.length * TITLE_CHAR_W
                val lineX = TITLE_X1 + (TITLE_W - lineW) / 2
                val lineY = titleStartY + i * TITLE_LINE_H
                append("TEXT $TITLE_FONT $TITLE_SIZE $lineX $lineY $line\r\n")
            }
            append("! U1 SETBOLD 0\r\n")
        }

        for (block in contentBlocks) append(block)

        if (qrUrl.isNotEmpty()) {
            val qrY    = curY + QR_GAP
            val labelY = qrY + QR_SIZE + 8
            val label  = "Más información"
            val labelX = QR_X + (QR_SIZE - label.length * BODY_CHAR_W) / 2
            append("BARCODE QR $QR_X $qrY U $QR_MODULE\r\n")
            append("MA,$qrUrl\r\n")
            append("ENDQR\r\n")
            append("TEXT $QR_LABEL_FONT $QR_LABEL_SIZE $labelX $labelY $label\r\n")
        }

        append("PRINT\r\n")
    }

    sendToprinter(context, mac, contentH, cpcl, pw)
    Log.d(TAG, "printDocumentSuspend OK — contentH=$contentH curY=$curY")
}

// ============================================================
// sendToPrinter — núcleo de conexión, compartido por todas
// las funciones suspend. Abre la conexión, envía los datos
// y la cierra. Una sola conexión por trabajo.
// ============================================================
private suspend fun sendToprinter(
    context: Context,
    mac: String,
    contentH: Int,
    bodyCpcl: String,
    pw: Int
) {
    var connection: Connection? = null
    try {
        connection = BluetoothConnection(mac, 5000, 2000)
        connection.open()
        Log.d(TAG, "BT abierto")

        SGD.SET("device.languages", "cpcl", connection)
        Thread.sleep(500)

        val printer: ZebraPrinter = ZebraPrinterFactory.getInstance(connection)

        // Cabecera CPCL
        connection.write(
            "! 0 200 200 $contentH 1\r\nPAGE-WIDTH $pw\r\n"
                .toByteArray(Charsets.ISO_8859_1)
        )

        // Imágenes
        printer.printImage(
            ZebraImageFactory.getImage(context.assets.open("images/EscEspana.png")),
            IMG1_X, IMG_Y, IMG1_W, IMG1_H, true
        )
        printer.printImage(
            ZebraImageFactory.getImage(context.assets.open("images/EscGuardiaCivil.png")),
            IMG2_X, IMG_Y, IMG2_W, IMG2_H, true
        )

        // Cuerpo + PRINT
        connection.write(bodyCpcl.toByteArray(Charsets.ISO_8859_1))
        Log.d(TAG, "Enviados ${bodyCpcl.length} bytes")

    } finally {
        try { connection?.close() } catch (_: IOException) {}
        Log.d(TAG, "BT cerrado")
    }
}

// ============================================================
// showError — muestra Toast en Main thread
// ============================================================
private fun showError(context: Context, e: Exception) {
    Log.e(TAG, "Error: ${e.message}", e)
    CoroutineScope(Dispatchers.Main).launch {
        Toast.makeText(context, "Error al imprimir: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// ============================================================
// Word wrap
// ============================================================
private fun wrapText(text: String, maxChars: Int): List<String> {
    if (text.isBlank() || maxChars <= 0) return emptyList()
    val result = mutableListOf<String>()
    text.split("\n").forEach { paragraph ->
        if (paragraph.isBlank()) { result.add(""); return@forEach }
        var line = ""
        for (word in paragraph.trim().split(" ")) {
            val test = if (line.isEmpty()) word else "$line $word"
            if (test.length <= maxChars) line = test
            else {
                if (line.isNotEmpty()) result.add(line)
                line = if (word.length > maxChars) {
                    result.add(word.substring(0, maxChars)); word.substring(maxChars)
                } else word
            }
        }
        if (line.isNotEmpty()) result.add(line)
    }
    return result
}