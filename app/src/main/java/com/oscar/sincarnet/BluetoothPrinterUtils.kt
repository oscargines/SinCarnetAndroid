package com.oscar.sincarnet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.zebra.sdk.comm.BluetoothConnection
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.printer.SGD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

private const val TAG = "PrinterRW"

// ============================================================
// PAPEL — RW420 y ZQ521: 832 dots, 203 DPI
// ============================================================
private const val PAPER_W_DOTS  = 792
private const val MARGIN        = 20
private const val MARGIN_BOTTOM = 120

private val PAPER_W get() = PAPER_W_DOTS

// ============================================================
// ESCUDOS
// ============================================================
private const val IMG1_ASSET = "images/EscEspana_96x103.png"
private const val IMG2_ASSET = "images/EscGuardiaCivil_66x103.png"
private const val IMG1_W = 96;  private const val IMG1_H = 103
private const val IMG2_W = 66;  private const val IMG2_H = 103
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
private const val BODY_LINE_H    = 18
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

private val QR_X get() = (PAPER_W - QR_SIZE) / 2

// ============================================================
// FIRMAS
// Layout al final de cada documento:
//
//  ┌──────────────────┐  ┌──────────────────┐
//  │ Instructor       │  │ Secretario       │
//  │  TIP: XXXXX     │  │  TIP: XXXXX     │
//  │  [firma / vacío] │  │  [firma / vacío] │
//  └──────────────────┘  └──────────────────┘
//       ┌──────────────────────────┐
//       │ Investigado              │
//       │  [firma / "Sin firma"]   │
//       └──────────────────────────┘
//
// SIG_H:   alto de cada caja en dots
// SIG_GAP: espacio entre las dos cajas superiores
// SIG_ROW_GAP: espacio entre fila superior e inferior
// ============================================================
private const val SIG_H        = 120   // alto caja firma en dots
private const val SIG_GAP      = 14    // espacio entre instructor y secretario
private const val SIG_ROW_GAP  = 10    // espacio entre fila 1 y fila 2
private const val SIG_LABEL_H  = 20    // espacio para texto "Instructor" + TIP
private const val SIG_FONT     = 7
private const val SIG_FONT_SZ  = 0

// Ancho de cada caja de firma fila 1 (instructor + secretario)
private val SIG_W1 get() = (PAPER_W - MARGIN * 2 - SIG_GAP) / 2  // ~399
// Posiciones fila 1
private val SIG_INSTR_X  get() = MARGIN
private val SIG_SECR_X   get() = MARGIN + SIG_W1 + SIG_GAP
// Fila 2: investigado centrado al 60% del ancho
private val SIG_INV_W    get() = (PAPER_W - MARGIN * 2) * 6 / 10  // ~487
private val SIG_INV_X    get() = (PAPER_W - SIG_INV_W) / 2

// ============================================================
// EgImage
// ============================================================
private data class EgImage(val width: Int, val height: Int, val needed: Int, val hexData: String)

// ============================================================
// pngToEg — carga PNG desde assets y genera EG CPCL
// ============================================================
private fun pngToEg(context: Context, assetPath: String): EgImage {
    val bitmap = context.assets.open(assetPath).use { BitmapFactory.decodeStream(it)
        ?: throw IOException("No se pudo decodificar $assetPath") }
    val eg = bitmapToEg(bitmap)
    bitmap.recycle()
    return eg
}

// ============================================================
// bitmapToEg — convierte Bitmap Android a EG CPCL
// Umbral: luminancia < 128 → punto negro (bit=1)
// ============================================================
private fun bitmapToEg(bitmap: Bitmap): EgImage {
    val width    = bitmap.width
    val height   = bitmap.height
    val needed   = (width + 7) / 8
    val lastBits = width % 8
    val lastMask = if (lastBits == 0) 0xFF else (0xFF shl (8 - lastBits)) and 0xFF

    val sb = StringBuilder(needed * height * 2)
    for (row in 0 until height) {
        for (colByte in 0 until needed) {
            var byteVal = 0
            for (bit in 0 until 8) {
                val col = colByte * 8 + bit
                if (col < width) {
                    val pixel = bitmap.getPixel(col, row)
                    val lum = (0.299 * ((pixel shr 16) and 0xFF) +
                            0.587 * ((pixel shr  8) and 0xFF) +
                            0.114 * ( pixel         and 0xFF)).toInt()
                    if (lum < 128) byteVal = byteVal or (1 shl (7 - bit))
                }
            }
            if (colByte == needed - 1) byteVal = byteVal and lastMask
            sb.append("%02X".format(byteVal))
        }
    }
    return EgImage(width, height, needed, sb.toString())
}

// ============================================================
// imageBitmapToEg — convierte ImageBitmap (Compose) a EG
// Escala la firma al ancho/alto máximo indicado manteniendo
// la proporción, luego convierte a 1-bit.
// ============================================================
private fun imageBitmapToEg(imageBitmap: ImageBitmap, maxW: Int, maxH: Int): EgImage {
    val src = imageBitmap.asAndroidBitmap()
    val srcW = src.width.toFloat()
    val srcH = src.height.toFloat()
    val scale = minOf(maxW / srcW, maxH / srcH)
    val dstW = (srcW * scale).toInt().coerceAtLeast(1)
    val dstH = (srcH * scale).toInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(src, dstW, dstH, true)
    val eg = bitmapToEg(scaled)
    if (scaled != src) scaled.recycle()
    return eg
}

// ============================================================
// CAJAS DE CITACIÓN A JUICIO
//
// Diseño vertical centrado en el papel:
//
//          RECORDATORIO          (font 0 size 3, centrado)
//  ┌────────────────────────────────────────────────────┐
//  │    Hora y fecha de celebracion       (font 7 s0)  │
//  │       13:00 horas del dia 01/01/2025 (font 7 s1)  │
//  └────────────────────────────────────────────────────┘
//  ┌────────────────────────────────────────────────────┐
//  │    Datos del juzgado                 (font 7 s0)  │
//  │       Juzgado de Instruccion nº 1    (font 7 s1)  │
//  └────────────────────────────────────────────────────┘
//
// JUICIO_BOX_H  : alto de cada box
// JUICIO_BOX_GAP: espacio entre los dos boxes
// JUICIO_REC_H  : espacio para el texto RECORDATORIO
// JUICIO_TOTAL_H: altura total reservada en el documento
// ============================================================
private const val JUICIO_BOX_H   = 160
private const val JUICIO_BOX_GAP = 10
private const val JUICIO_REC_H   = 36

private const val JUICIO_REC_FONT = 0;  private const val JUICIO_REC_SIZE = 3
private const val JUICIO_LBL_FONT = 7;  private const val JUICIO_LBL_SIZE = 0
private const val JUICIO_VAL_FONT = 7;  private const val JUICIO_VAL_SIZE = 1

private const val JUICIO_REC_CHAR_W = 18   // font 5 size 2
private const val JUICIO_LBL_CHAR_W = 12   // font 7 size 0
private const val JUICIO_VAL_CHAR_W = 16   // font 7 size 1

private val JUICIO_TOTAL_H get() = JUICIO_REC_H + JUICIO_BOX_H + JUICIO_BOX_GAP + JUICIO_BOX_H + 16

// Pinta RECORDATORIO + dos BOX apilados. Devuelve la Y tras el bloque.
private fun StringBuilder.appendJuicioBoxes(
    fechaJuicio: String,
    horaJuicio: String,
    partidoJudicial: String, // ahora recibe partido judicial directamente
    startY: Int
): Int {
    val boxW = PAPER_W - MARGIN * 2
    var y = startY + 8

    // ── RECORDATORIO ─────────────────────────────────────────
    val recText = "RECORDATORIO"
    val recX = MARGIN + (boxW - recText.length * JUICIO_REC_CHAR_W) / 2 + 4
    append("TEXT $JUICIO_REC_FONT $JUICIO_REC_SIZE ${recX.coerceAtLeast(MARGIN)} $y $recText\r\n")
    y += JUICIO_REC_H

    // ── Box superior: Hora y fecha de celebración ────────────
    val top1 = y
    val top2 = top1 + JUICIO_BOX_H
    append("BOX $MARGIN $top1 ${MARGIN + boxW} $top2 2\r\n")

    val lbl1 = "Hora y fecha de celebracion"
    val lbl1X = MARGIN + (boxW - lbl1.length * JUICIO_LBL_CHAR_W) / 2 + 4
    append("TEXT $JUICIO_LBL_FONT $JUICIO_LBL_SIZE ${lbl1X.coerceAtLeast(MARGIN + 4)} ${top1 + 12} $lbl1\r\n")

    val val1 = "$horaJuicio horas del dia $fechaJuicio"
    val val1X = MARGIN + (boxW - val1.length * JUICIO_VAL_CHAR_W) / 2 + 4
    append("TEXT $JUICIO_VAL_FONT $JUICIO_VAL_SIZE ${val1X.coerceAtLeast(MARGIN + 4)} ${top1 + 38} $val1\r\n")

    y = top2 + JUICIO_BOX_GAP

    // ── Box inferior: Datos del juzgado ──────────────────────
    val bot1 = y
    val bot2 = bot1 + JUICIO_BOX_H
    append("BOX $MARGIN $bot1 ${MARGIN + boxW} $bot2 2\r\n")

    val lbl2 = "Partido Judicial"
    val lbl2X = MARGIN + (boxW - lbl2.length * JUICIO_LBL_CHAR_W) / 2 + 4
    append("TEXT $JUICIO_LBL_FONT $JUICIO_LBL_SIZE ${lbl2X.coerceAtLeast(MARGIN + 4)} ${bot1 + 12} $lbl2\r\n")

    // WRAP de texto para partido judicial
    val maxChars = boxW / JUICIO_VAL_CHAR_W
    // Mejor wrap: no cortar palabras
    val words = partidoJudicial.split(" ")
    val lines = mutableListOf<String>()
    var currentLine = ""
    for (word in words) {
        if (currentLine.isEmpty()) {
            currentLine = word
        } else if ((currentLine.length + 1 + word.length) <= maxChars) {
            currentLine += " " + word
        } else {
            lines.add(currentLine)
            currentLine = word
        }
    }
    if (currentLine.isNotEmpty()) lines.add(currentLine)
    lines.forEachIndexed { idx, line ->
        val val2X = MARGIN + (boxW - line.length * JUICIO_VAL_CHAR_W) / 2 + 4
        append("TEXT $JUICIO_VAL_FONT $JUICIO_VAL_SIZE ${val2X.coerceAtLeast(MARGIN + 4)} ${bot1 + 38 + idx * 32} $line\r\n")
    }

    return bot2 + 8
}


// ============================================================
// appendSignatureBlock — genera el CPCL del bloque de firmas
//
// Añade al StringBuilder:
//   - Línea separadora antes del bloque
//   - Cajas (BOX) para cada firma
//   - Etiqueta con nombre y TIP dentro de cada caja
//   - Imagen EG de la firma, o texto "Sin firma" si no hay
//
// Retorna la Y final tras el bloque.
// ============================================================
private fun StringBuilder.appendSignatureBlock(
    sigs: PrintSignatures,
    startY: Int,
    pw: Int
): Int {
    var y = startY

    // Línea separadora
    append("LINE $MARGIN $y ${pw - MARGIN} $y 2\r\n")
    y += 8

    val row1Y    = y
    val row1BotY = row1Y + SIG_H

    // ── Cajas fila 1 ─────────────────────────────────────────
    append("BOX $SIG_INSTR_X $row1Y ${SIG_INSTR_X + SIG_W1} $row1BotY 2\r\n")
    append("BOX $SIG_SECR_X $row1Y ${SIG_SECR_X + SIG_W1} $row1BotY 2\r\n")

    // ── Etiquetas fila 1 ─────────────────────────────────────
    val charW = BODY_CHAR_W
    append("TEXT $SIG_FONT $SIG_FONT_SZ ${SIG_INSTR_X + 4} ${row1Y + 14} Instructor\r\n")
    if (sigs.instructorTip.isNotEmpty())
        append("TEXT $SIG_FONT $SIG_FONT_SZ ${SIG_INSTR_X + 4} ${row1Y + 28} TIP: ${sigs.instructorTip}\r\n")

    append("TEXT $SIG_FONT $SIG_FONT_SZ ${SIG_SECR_X + 4} ${row1Y + 14} Secretario\r\n")
    if (sigs.secretaryTip.isNotEmpty())
        append("TEXT $SIG_FONT $SIG_FONT_SZ ${SIG_SECR_X + 4} ${row1Y + 28} TIP: ${sigs.secretaryTip}\r\n")

    // ── Imágenes / texto fila 1 ───────────────────────────────
    val sigImgMaxW = SIG_W1 - 2
    val sigImgMaxH = SIG_H - SIG_LABEL_H - 2

    fun appendSigContent(sig: ImageBitmap?, boxX: Int, boxY: Int, boxW: Int = sigImgMaxW, fallbackText: String = "Sin firma") {
        if (sig != null) {
            val eg = imageBitmapToEg(sig, boxW, sigImgMaxH)
            val imgX = boxX + 4 + (boxW - eg.width) / 2
            val imgY = boxY + SIG_LABEL_H + (sigImgMaxH - eg.height) / 2
            append("EG ${eg.needed} ${eg.height} $imgX $imgY ${eg.hexData}\r\n")
        } else {
            val text = fallbackText
            val textX = boxX + 4 + (boxW - text.length * charW) / 2
            val textY = boxY + SIG_H / 2 + 5
            append("TEXT $SIG_FONT $SIG_FONT_SZ $textX $textY $text\r\n")
        }
    }

    appendSigContent(sigs.instructor, SIG_INSTR_X, row1Y)
    appendSigContent(sigs.secretary,  SIG_SECR_X,  row1Y)

    // ── Fila 2: Investigado y Segundo Conductor ───────────────
    val row2Y    = row1BotY + SIG_ROW_GAP
    val row2BotY = row2Y + SIG_H

    if (sigs.isInmovilizacion && sigs.hasSecondDriver) {
        // Dos cajas: izquierda Investigado, derecha Segundo Conductor
        val boxW = (pw - 2 * MARGIN - SIG_ROW_GAP) / 2
        val invX = MARGIN
        val segX = invX + boxW + SIG_ROW_GAP

        append("BOX $invX $row2Y ${invX + boxW} $row2BotY 2\r\n")
        append("BOX $segX $row2Y ${segX + boxW} $row2BotY 2\r\n")
        append("TEXT $SIG_FONT $SIG_FONT_SZ ${invX + 4} ${row2Y + 14} Investigado\r\n")
        append("TEXT $SIG_FONT $SIG_FONT_SZ ${segX + 4} ${row2Y + 14} Segundo conductor\r\n")

        appendSigContent(sigs.investigated, invX, row2Y, boxW - 8, sigs.investigatedNoSignText)
        appendSigContent(sigs.secondDriver, segX, row2Y, boxW - 8)
    } else {
        // Layout estándar: solo Investigado centrado
        append("BOX $SIG_INV_X $row2Y ${SIG_INV_X + SIG_INV_W} $row2BotY 2\r\n")
        append("TEXT $SIG_FONT $SIG_FONT_SZ ${SIG_INV_X + 4} ${row2Y + 14} Investigado\r\n")
        val invImgMaxW = SIG_INV_W - 8
        appendSigContent(sigs.investigated, SIG_INV_X, row2Y, invImgMaxW, sigs.investigatedNoSignText)
    }

    return row2BotY + 8
}

// ============================================================
// API PÚBLICA
// ============================================================

fun printDocumentFromJson(context: Context, mac: String?, jsonAssetPath: String,
                          sigs: PrintSignatures? = null) {
    if (mac.isNullOrBlank()) { showNoMac(context); return }
    CoroutineScope(Dispatchers.IO).launch {
        try { printDocumentFromJsonSuspend(context, mac, jsonAssetPath, sigs) }
        catch (e: Exception) { showError(context, e) }
    }
}

fun printDocument(context: Context, mac: String?,
                  doc: JSONObject = JSONObject(), title: String = "",
                  qrUrl: String = "", sigs: PrintSignatures? = null) {
    if (mac.isNullOrBlank()) { showNoMac(context); return }
    CoroutineScope(Dispatchers.IO).launch {
        try { printDocumentSuspend(context, mac, doc, title, qrUrl, sigs) }
        catch (e: Exception) { showError(context, e) }
    }
}

fun printDocumentResolved(context: Context, mac: String?, title: String, body: String,
                          sigs: PrintSignatures? = null) {
    if (mac.isNullOrBlank()) { showNoMac(context); return }
    CoroutineScope(Dispatchers.IO).launch {
        try { printDocumentResolvedSuspend(context, mac, title, body, sigs) }
        catch (e: Exception) { showError(context, e) }
    }
}

fun printImageTest(context: Context, mac: String?) {
    if (mac.isNullOrBlank()) { showNoMac(context); return }
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val eg = pngToEg(context, IMG1_ASSET)
            val imgX = (PAPER_W - eg.width) / 2
            val contentH = IMG_Y + eg.height + MARGIN_BOTTOM
            val cpcl = "EG ${eg.needed} ${eg.height} $imgX $IMG_Y ${eg.hexData}\r\nPRINT\r\n"
            sendToPrinter(context, mac!!, contentH, cpcl, PAPER_W)
        } catch (e: Exception) { showError(context, e) }
    }
}

// ============================================================
// API SUSPEND
// ============================================================

suspend fun printDocumentFromJsonSuspend(context: Context, mac: String,
                                         jsonAssetPath: String,
                                         sigs: PrintSignatures? = null) {
    val json = JSONObject(context.assets.open(jsonAssetPath).bufferedReader().readText())
    val doc  = json.getJSONObject("documento")
    printDocumentSuspend(context, mac, doc,
        doc.optString("titulo", ""), doc.optString("qr_url", ""), sigs)
}

suspend fun printDocumentResolvedSuspend(
    context: Context, mac: String,
    title: String, body: String,
    sigs: PrintSignatures? = null,
    juicioData: Map<String, String>? = null,
    sharedConn: Connection? = null          // ← conexión compartida opcional
) {
    val pw         = PAPER_W
    val titleLines = wrapText(title, TITLE_CHARS)

    // Separar el body en el marcador: líneas antes y después de las cajas
    val markerLine   = DocumentPrinter.JUICIO_BOXES_MARKER
    val allBodyLines = wrapText(body, BODY_CHARS)
    val markerIdx    = allBodyLines.indexOfFirst { it.contains(markerLine) }
    val hasBoxes     = markerIdx >= 0 && juicioData != null

    val bodyBefore = if (hasBoxes) allBodyLines.subList(0, markerIdx)               else allBodyLines
    val bodyAfter  = if (hasBoxes) allBodyLines.subList(markerIdx + 1, allBodyLines.size) else emptyList()

    val juicioDatos: Triple<String, String, String>? = if (hasBoxes) juicioData!!.let { d ->
        Triple(d["fechajuicio"] ?: "", d["horajuicio"] ?: "", d["datosjuzgado"] ?: "")
    } else null

    // Calcular altura total
    val juicioH   = if (hasBoxes) JUICIO_TOTAL_H else 0
    val sigBlockH = if (sigs != null) 8 + SIG_H + SIG_ROW_GAP + SIG_H + 8 else 0
    val contentH  = (BODY_Y
            + bodyBefore.size * BODY_LINE_H
            + juicioH
            + bodyAfter.size  * BODY_LINE_H
            + sigBlockH + MARGIN_BOTTOM)

    val eg1 = pngToEg(context, IMG1_ASSET)
    val eg2 = pngToEg(context, IMG2_ASSET)

    val cpcl = buildString {
        append("EG "+eg1.needed+" "+eg1.height+" $IMG1_X $IMG_Y "+eg1.hexData+"\r\n")
        append("EG "+eg2.needed+" "+eg2.height+" $IMG2_X $IMG_Y "+eg2.hexData+"\r\n")

        if (titleLines.isNotEmpty()) {
            val totalTitleH = titleLines.size * TITLE_LINE_H
            val titleStartY = IMG_Y + (HEADER_H - totalTitleH) / 2
            append("! U1 SETBOLD $TITLE_BOLD\r\n")
            titleLines.forEachIndexed { i, line ->
                val lineX = TITLE_X1 + (TITLE_W - line.length * TITLE_CHAR_W) / 2
                append("TEXT $TITLE_FONT $TITLE_SIZE $lineX ${titleStartY + i * TITLE_LINE_H} $line\r\n")
            }
            append("! U1 SETBOLD 0\r\n")
        }

        // Líneas antes del marcador
        var curY = BODY_Y
        bodyBefore.forEach { line ->
            append("TEXT $BODY_FONT $BODY_SIZE $MARGIN $curY $line\r\n")
            curY += BODY_LINE_H
        }

        // Cajas de citación a juicio en el punto exacto del marcador
        if (hasBoxes && juicioDatos != null) {
            curY = appendJuicioBoxes(juicioDatos.first, juicioDatos.second, juicioDatos.third, curY)
            curY += 40 // Añade espacio extra después del box de juzgado
        }

        // Líneas después del marcador
        bodyAfter.forEach { line ->
            append("TEXT $BODY_FONT $BODY_SIZE $MARGIN $curY $line\r\n")
            curY += BODY_LINE_H
        }
        android.util.Log.d("Impresion", "[printDiligenciaSuspend] sigs antes de llamar: " +
                "isInmovilizacion=${sigs?.isInmovilizacion}, " +
                "hasSecondDriver=${sigs?.hasSecondDriver}, " +
                "secondDriver=${sigs?.secondDriver != null}")
        if (sigs != null) appendSignatureBlock(sigs, curY, pw)

        append("PRINT\r\n")
    }
    Log.d(TAG, "CPCL generado:\n" + cpcl)
    sendToPrinter(context, mac, contentH, cpcl, pw, sharedConn)   // ← pasa sharedConn
    Log.d(TAG, "printDocumentResolvedSuspend OK contentH=$contentH")
}

suspend fun printDocumentSuspend(context: Context, mac: String,
                                 doc: JSONObject = JSONObject(),
                                 title: String = "", qrUrl: String = "",
                                 sigs: PrintSignatures? = null) {
    android.util.Log.d("Impresion", "[printDocumentSuspend] INICIO para mac=$mac")
    val pw            = PAPER_W
    val contentBlocks = mutableListOf<String>()
    var curY          = BODY_Y

    val intro = doc.optString("introduccion", "")
    android.util.Log.d("Impresion", "[printDocumentSuspend] doc.keys=${doc.keys().asSequence().toList()}")
    if (intro.isNotEmpty()) {
        for (line in wrapText(intro, BODY_CHARS)) {
            contentBlocks.add("TEXT $BODY_FONT $BODY_SIZE $MARGIN $curY $line\r\n")
            curY += BODY_LINE_H
        }
        curY += BODY_GAP_AFTER
    }

    val articulos = doc.optJSONArray("articulos")
    android.util.Log.d("Impresion", "[printDocumentSuspend] Título recibido: '$title'")
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

    val extraSeparation = 5 * BODY_LINE_H // 5 líneas en blanco extra tras el QR
    val qrBlockH  = if (qrUrl.isNotEmpty()) QR_GAP + QR_SIZE + BODY_LINE_H + 8 + extraSeparation else 0
    val sigBlockH = if (sigs != null) 8 + SIG_H + SIG_ROW_GAP + SIG_H + 8 else 0
    val contentH  = (curY + qrBlockH + sigBlockH + MARGIN_BOTTOM)
    val titleLines = wrapText(title, TITLE_CHARS)

    val eg1 = pngToEg(context, IMG1_ASSET)
    val eg2 = pngToEg(context, IMG2_ASSET)
    android.util.Log.d("Impresion", "[printDocumentSuspend] contentBlocks.size=${contentBlocks.size}")

    val cpcl = buildString {
        append("EG ${eg1.needed} ${eg1.height} $IMG1_X $IMG_Y ${eg1.hexData}\r\n")
        append("EG ${eg2.needed} ${eg2.height} $IMG2_X $IMG_Y ${eg2.hexData}\r\n")

        if (titleLines.isNotEmpty()) {
            val totalTitleH = titleLines.size * TITLE_LINE_H
            val titleStartY = IMG_Y + (HEADER_H - totalTitleH) / 2
            append("! U1 SETBOLD $TITLE_BOLD\r\n")
            titleLines.forEachIndexed { i, line ->
                val lineX = TITLE_X1 + (TITLE_W - line.length * TITLE_CHAR_W) / 2
                append("TEXT $TITLE_FONT $TITLE_SIZE $lineX ${titleStartY + i * TITLE_LINE_H} $line\r\n")
            }
            append("! U1 SETBOLD 0\r\n")
        }

        for (block in contentBlocks) append(block)

        if (qrUrl.isNotEmpty()) {
            val qrY    = curY + QR_GAP

            // === TEXTO DINÁMICO DEL JSON ===
            val qrDescripcion = if (doc.has("qr")) {
                doc.getJSONObject("qr").optString("descripcion", "Más información")
            } else {
                "Más información"
            }

            val labelY = qrY + QR_SIZE + 8
            val labelX = QR_X + (QR_SIZE - qrDescripcion.length * BODY_CHAR_W) / 2

            append("BARCODE QR $QR_X $qrY U $QR_MODULE\r\n")
            append("MA,$qrUrl\r\n")
            append("ENDQR\r\n")
            append("TEXT $QR_LABEL_FONT $QR_LABEL_SIZE $labelX $labelY $qrDescripcion\r\n")

            // 5 líneas en blanco extra después del QR
            var sepY = labelY + BODY_LINE_H
            repeat(5) {
                append("TEXT $BODY_FONT $BODY_SIZE $MARGIN $sepY \r\n")
                sepY += BODY_LINE_H
            }
        }

        val afterContent = curY + qrBlockH
        android.util.Log.d("Impresion", "[printDiligenciaSuspend] sigs antes de llamar: " +
                "isInmovilizacion=${sigs?.isInmovilizacion}, " +
                "hasSecondDriver=${sigs?.hasSecondDriver}, " +
                "secondDriver=${sigs?.secondDriver != null}")
        if (sigs != null) appendSignatureBlock(sigs, afterContent, pw)

        append("PRINT\r\n")
    }

    android.util.Log.d("Impresion", "[printDocumentSuspend] Enviando a impresora: contentH=$contentH, curY=$curY, pw=$pw, cpcl.length=${cpcl.length}")
    try {
        sendToPrinter(context, mac, contentH, cpcl, pw)
        android.util.Log.d("Impresion", "[printDocumentSuspend] Envío a impresora OK")
    } catch (e: Exception) {
        android.util.Log.e("Impresion", "[printDocumentSuspend] ERROR al enviar a impresora: ${e.message}", e)
        throw e
    }
}

// ============================================================
// Gestión de conexión BT compartida
// Permite abrir la conexión UNA SOLA VEZ para imprimir varios
// documentos sin provocar nuevos diálogos de emparejamiento.
// ============================================================

/**
 * Abre una conexión Bluetooth y configura el lenguaje CPCL.
 * Llamar UNA VEZ antes de imprimir una serie de documentos.
 * Cerrar siempre con [closeSharedBtConnection] en un bloque finally.
 */
internal suspend fun openSharedBtConnection(mac: String): Connection =
    withContext(Dispatchers.IO) {
        val conn = BluetoothConnection(mac, 5_000, 2_000)
        conn.open()
        Log.d(TAG, "BT compartido abierto mac=$mac")
        SGD.SET("device.languages", "cpcl", conn)
        Thread.sleep(500)
        conn
    }

/** Cierra la conexión BT compartida obtenida con [openSharedBtConnection]. */
internal suspend fun closeSharedBtConnection(conn: Connection) =
    withContext(Dispatchers.IO) {
        try { conn.close() } catch (_: IOException) {}
        Log.d(TAG, "BT compartido cerrado")
    }

// ============================================================
// sendToPrinter
// Si se pasa sharedConn (conexión ya abierta) se reutiliza;
// si no, se abre y cierra una conexión propia (uso individual).
// ============================================================
private suspend fun sendToPrinter(
    context: Context, mac: String,
    contentH: Int, bodyCpcl: String, pw: Int,
    sharedConn: Connection? = null
) {
    return withContext(Dispatchers.IO) {
        if (sharedConn != null) {
            // Reutilizar la conexión compartida — no abrir ni cerrar
            sharedConn.write(
                "! 0 200 200 $contentH 1\r\nPAGE-WIDTH $pw\r\n"
                    .toByteArray(Charsets.ISO_8859_1)
            )
            sharedConn.write(bodyCpcl.toByteArray(Charsets.ISO_8859_1))
            Log.d(TAG, "Enviados ${bodyCpcl.length} bytes (conexión compartida)")
            Thread.sleep(4000)
        } else {
            // Conexión individual: abrir → enviar → cerrar
            var connection: Connection? = null
            try {
                connection = BluetoothConnection(mac, 5_000, 2_000)
                connection.open()
                Log.d(TAG, "BT abierto")
                SGD.SET("device.languages", "cpcl", connection)
                Thread.sleep(500)
                connection.write(
                    "! 0 200 200 $contentH 1\r\nPAGE-WIDTH $pw\r\n"
                        .toByteArray(Charsets.ISO_8859_1)
                )
                connection.write(bodyCpcl.toByteArray(Charsets.ISO_8859_1))
                Log.d(TAG, "Enviados ${bodyCpcl.length} bytes")
                Thread.sleep(4000)
            } finally {
                try { connection?.close() } catch (_: IOException) {}
                Log.d(TAG, "BT cerrado")
            }
        }
    }
}

private fun showNoMac(context: Context) =
    Toast.makeText(context, "No hay impresora configurada", Toast.LENGTH_SHORT).show()

private fun showError(context: Context, e: Exception) {
    Log.e(TAG, "Error: ${e.message}", e)
    CoroutineScope(Dispatchers.Main).launch {
        Toast.makeText(context, "Error al imprimir: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

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

object BluetoothPrinterUtils {
    // ============================================================
    // PAPEL — RW420 y ZQ521: 832 dots, 203 DPI
    // ============================================================
    private const val PAPER_W_DOTS  = 792
    private const val MARGIN        = 20
    private const val MARGIN_BOTTOM = 120

    private val PAPER_W get() = PAPER_W_DOTS

    // ============================================================
    // ESCUDOS
    // ============================================================
    private const val IMG1_ASSET = "images/EscEspana_96x103.png"
    private const val IMG2_ASSET = "images/EscGuardiaCivil_66x103.png"
    private const val IMG1_W = 96;  private const val IMG1_H = 103
    private const val IMG2_W = 66;  private const val IMG2_H = 103
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
    private const val BODY_LINE_H    = 18
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
    // FIRMAS
    // Layout al final de cada documento:
    //
    //  ┌──────────────────┐  ┌──────────────────┐
    //  │ Instructor       │  │ Secretario       │
    //  │  TIP: XXXXX     │  │  TIP: XXXXX     │
    //  │  [firma / vacío] │  │  [firma / vacío] │
    //  └──────────────────┘  └──────────────────┘
    //       ┌──────────────────────────┐
    //       │ Investigado              │
    //       │  [firma / "Sin firma"]   │
    //       └──────────────────────────┘
    //
    // SIG_H:   alto de cada caja en dots
    // SIG_GAP: espacio entre las dos cajas superiores
    // SIG_ROW_GAP: espacio entre fila superior e inferior
    // ============================================================
    private const val SIG_H        = 120   // alto caja firma en dots
    private const val SIG_GAP      = 14    // espacio entre instructor y secretario
    private const val SIG_ROW_GAP  = 10    // espacio entre fila 1 y fila 2
    private const val SIG_LABEL_H  = 20    // espacio para texto "Instructor" + TIP
    private const val SIG_FONT     = 7
    private const val SIG_FONT_SZ  = 0

    // Ancho de cada caja de firma fila 1 (instructor + secretario)
    private val SIG_W1 get() = (PAPER_W - MARGIN * 2 - SIG_GAP) / 2  // ~399
    // Posiciones fila 1
    private val SIG_INSTR_X  get() = MARGIN
    private val SIG_SECR_X   get() = MARGIN + SIG_W1 + SIG_GAP
    // Fila 2: investigado centrado al 60% del ancho
    private val SIG_INV_W    get() = (PAPER_W - MARGIN * 2) * 6 / 10  // ~487
    private val SIG_INV_X    get() = (PAPER_W - SIG_INV_W) / 2

    // ============================================================
    // EgImage
    // ============================================================
    private data class EgImage(val width: Int, val height: Int, val needed: Int, val hexData: String)

    // ============================================================
    // pngToEg — carga PNG desde assets y genera EG CPCL
    // ============================================================
    private fun pngToEg(context: Context, assetPath: String): EgImage {
        val bitmap = context.assets.open(assetPath).use { BitmapFactory.decodeStream(it)
            ?: throw IOException("No se pudo decodificar $assetPath") }
        val eg = bitmapToEg(bitmap)
        bitmap.recycle()
        return eg
    }

    // ============================================================
    // bitmapToEg — convierte Bitmap Android a EG CPCL
    // Umbral: luminancia < 128 → punto negro (bit=1)
    // ============================================================
    private fun bitmapToEg(bitmap: Bitmap): EgImage {
        val width    = bitmap.width
        val height   = bitmap.height
        val needed   = (width + 7) / 8
        val lastBits = width % 8
        val lastMask = if (lastBits == 0) 0xFF else (0xFF shl (8 - lastBits)) and 0xFF

        val sb = StringBuilder(needed * height * 2)
        for (row in 0 until height) {
            for (colByte in 0 until needed) {
                var byteVal = 0
                for (bit in 0 until 8) {
                    val col = colByte * 8 + bit
                    if (col < width) {
                        val pixel = bitmap.getPixel(col, row)
                        val lum = (0.299 * ((pixel shr 16) and 0xFF) +
                                0.587 * ((pixel shr  8) and 0xFF) +
                                0.114 * ( pixel         and 0xFF)).toInt()
                        if (lum < 128) byteVal = byteVal or (1 shl (7 - bit))
                    }
                }
                if (colByte == needed - 1) byteVal = byteVal and lastMask
                sb.append("%02X".format(byteVal))
            }
        }
        return EgImage(width, height, needed, sb.toString())
    }

    // ============================================================
    // imageBitmapToEg — convierte ImageBitmap (Compose) a EG
    // Escala la firma al ancho/alto máximo indicado manteniendo
    // la proporción, luego convierte a 1-bit.
    // ============================================================
    private fun imageBitmapToEg(imageBitmap: ImageBitmap, maxW: Int, maxH: Int): EgImage {
        val src = imageBitmap.asAndroidBitmap()
        val srcW = src.width.toFloat()
        val srcH = src.height.toFloat()
        val scale = minOf(maxW / srcW, maxH / srcH)
        val dstW = (srcW * scale).toInt().coerceAtLeast(1)
        val dstH = (srcH * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(src, dstW, dstH, true)
        val eg = bitmapToEg(scaled)
        if (scaled != src) scaled.recycle()
        return eg
    }

    // ============================================================
    // CAJAS DE CITACIÓN A JUICIO
    //
    // Diseño vertical centrado en el papel:
    //
    //          RECORDATORIO          (font 0 size 3, centrado)
    //  ┌────────────────────────────────────────────────────┐
    //  │    Hora y fecha de celebracion       (font 7 s0)  │
    //  │       13:00 horas del dia 01/01/2025 (font 7 s1)  │
    //  └────────────────────────────────────────────────────┘
    //  ┌────────────────────────────────────────────────────┐
    //  │    Datos del juzgado                 (font 7 s0)  │
    //  │       Juzgado de Instruccion nº 1    (font 7 s1)  │
    //  └────────────────────────────────────────────────────┘
    //
    // JUICIO_BOX_H  : alto de cada box
    // JUICIO_BOX_GAP: espacio entre los dos boxes
    // JUICIO_REC_H  : espacio para el texto RECORDATORIO
    // JUICIO_TOTAL_H: altura total reservada en el documento
    // ============================================================
    private const val JUICIO_BOX_H   = 160
    private const val JUICIO_BOX_GAP = 10
    private const val JUICIO_REC_H   = 36

    private const val JUICIO_REC_FONT = 0;  private const val JUICIO_REC_SIZE = 3
    private const val JUICIO_LBL_FONT = 7;  private const val JUICIO_LBL_SIZE = 0
    private const val JUICIO_VAL_FONT = 7;  private const val JUICIO_VAL_SIZE = 1

    private const val JUICIO_REC_CHAR_W = 18   // font 5 size 2
    private const val JUICIO_LBL_CHAR_W = 12   // font 7 size 0
    private const val JUICIO_VAL_CHAR_W = 16   // font 7 size 1

    private val JUICIO_TOTAL_H get() = JUICIO_REC_H + JUICIO_BOX_H + JUICIO_BOX_GAP + JUICIO_BOX_H + 16

    // Pinta RECORDATORIO + dos BOX apilados. Devuelve la Y tras el bloque.
    private fun StringBuilder.appendJuicioBoxes(
        fechaJuicio: String,
        horaJuicio: String,
        partidoJudicial: String, // ahora recibe partido judicial directamente
        startY: Int
    ): Int {
        val boxW = PAPER_W - MARGIN * 2
        var y = startY + 8

        // ── RECORDATORIO ─────────────────────────────────────────
        val recText = "RECORDATORIO"
        val recX = MARGIN + (boxW - recText.length * JUICIO_REC_CHAR_W) / 2 + 4
        append("TEXT $JUICIO_REC_FONT $JUICIO_REC_SIZE ${recX.coerceAtLeast(MARGIN)} $y $recText\r\n")
        y += JUICIO_REC_H

        // ── Box superior: Hora y fecha de celebración ────────────
        val top1 = y
        val top2 = top1 + JUICIO_BOX_H
        append("BOX $MARGIN $top1 ${MARGIN + boxW} $top2 2\r\n")

        val lbl1 = "Hora y fecha de celebracion"
        val lbl1X = MARGIN + (boxW - lbl1.length * JUICIO_LBL_CHAR_W) / 2 + 4
        append("TEXT $JUICIO_LBL_FONT $JUICIO_LBL_SIZE ${lbl1X.coerceAtLeast(MARGIN + 4)} ${top1 + 12} $lbl1\r\n")

        val val1 = "$horaJuicio horas del dia $fechaJuicio"
        val val1X = MARGIN + (boxW - val1.length * JUICIO_VAL_CHAR_W) / 2 + 4
        append("TEXT $JUICIO_VAL_FONT $JUICIO_VAL_SIZE ${val1X.coerceAtLeast(MARGIN + 4)} ${top1 + 38} $val1\r\n")

        y = top2 + JUICIO_BOX_GAP

        // ── Box inferior: Datos del juzgado ──────────────────────
        val bot1 = y
        val bot2 = bot1 + JUICIO_BOX_H
        append("BOX $MARGIN $bot1 ${MARGIN + boxW} $bot2 2\r\n")

        val lbl2 = "Partido Judicial"
        val lbl2X = MARGIN + (boxW - lbl2.length * JUICIO_LBL_CHAR_W) / 2 + 4
        append("TEXT $JUICIO_LBL_FONT $JUICIO_LBL_SIZE ${lbl2X.coerceAtLeast(MARGIN + 4)} ${bot1 + 12} $lbl2\r\n")

        // WRAP de texto para partido judicial
        val maxChars = boxW / JUICIO_VAL_CHAR_W
        // Mejor wrap: no cortar palabras
        val words = partidoJudicial.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        for (word in words) {
            if (currentLine.isEmpty()) {
                currentLine = word
            } else if ((currentLine.length + 1 + word.length) <= maxChars) {
                currentLine += " " + word
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        lines.forEachIndexed { idx, line ->
            val val2X = MARGIN + (boxW - line.length * JUICIO_VAL_CHAR_W) / 2 + 4
            append("TEXT $JUICIO_VAL_FONT $JUICIO_VAL_SIZE ${val2X.coerceAtLeast(MARGIN + 4)} ${bot1 + 38 + idx * 32} $line\r\n")
        }

        return bot2 + 8
    }


    // ============================================================
    // appendSignatureBlock — genera el CPCL del bloque de firmas
    //
    // Añade al StringBuilder:
    //   - Línea separadora antes del bloque
    //   - Cajas (BOX) para cada firma
    //   - Etiqueta con nombre y TIP dentro de cada caja
    //   - Imagen EG de la firma, o texto "Sin firma" si no hay
    //
    // Retorna la Y final tras el bloque.
    // ============================================================

    private fun StringBuilder.appendSignatureBlock(
        sigs: PrintSignatures,
        startY: Int,
        pw: Int
    ): Int {
        var y = startY

        append("LINE $MARGIN $y ${pw - MARGIN} $y 2\r\n")
        y += 8

        val row1Y    = y
        val row1BotY = row1Y + SIG_H

        append("BOX $SIG_INSTR_X $row1Y ${SIG_INSTR_X + SIG_W1} $row1BotY 2\r\n")
        append("BOX $SIG_SECR_X $row1Y ${SIG_SECR_X + SIG_W1} $row1BotY 2\r\n")

        val charW = BODY_CHAR_W
        append("TEXT $SIG_FONT $SIG_FONT_SZ ${SIG_INSTR_X + 4} ${row1Y + 14} Instructor\r\n")
        if (sigs.instructorTip.isNotEmpty())
            append("TEXT $SIG_FONT $SIG_FONT_SZ ${SIG_INSTR_X + 4} ${row1Y + 28} TIP: ${sigs.instructorTip}\r\n")
        append("TEXT $SIG_FONT $SIG_FONT_SZ ${SIG_SECR_X + 4} ${row1Y + 14} Secretario\r\n")
        if (sigs.secretaryTip.isNotEmpty())
            append("TEXT $SIG_FONT $SIG_FONT_SZ ${SIG_SECR_X + 4} ${row1Y + 28} TIP: ${sigs.secretaryTip}\r\n")

        val sigImgMaxW = SIG_W1 - 8
        val sigImgMaxH = SIG_H - SIG_LABEL_H - 8

        fun appendSigContent(sig: ImageBitmap?, boxX: Int, boxY: Int, boxW: Int = sigImgMaxW) {
            if (sig != null) {
                val eg = imageBitmapToEg(sig, boxW, sigImgMaxH)
                val imgX = boxX + 4 + (boxW - eg.width) / 2
                val imgY = boxY + SIG_LABEL_H + (sigImgMaxH - eg.height) / 2
                append("EG ${eg.needed} ${eg.height} $imgX $imgY ${eg.hexData}\r\n")
            } else {
                val text = "Sin firma"
                val textX = boxX + 4 + (boxW - text.length * charW) / 2
                val textY = boxY + SIG_H / 2 + 5
                append("TEXT $SIG_FONT $SIG_FONT_SZ $textX $textY $text\r\n")
            }
        }

        appendSigContent(sigs.instructor, SIG_INSTR_X, row1Y)
        appendSigContent(sigs.secretary,  SIG_SECR_X,  row1Y)

        val row2Y    = row1BotY + SIG_ROW_GAP
        val row2BotY = row2Y + SIG_H

        // ── MISMO if que en el object ─────────────────────────────
        if (sigs.isInmovilizacion && sigs.hasSecondDriver) {
            val boxW = (pw - 2 * MARGIN - SIG_ROW_GAP) / 2
            val invX = MARGIN
            val segX = invX + boxW + SIG_ROW_GAP

            append("BOX $invX $row2Y ${invX + boxW} $row2BotY 2\r\n")
            append("BOX $segX $row2Y ${segX + boxW} $row2BotY 2\r\n")
            append("TEXT $SIG_FONT $SIG_FONT_SZ ${invX + 4} ${row2Y + 14} Investigado\r\n")
            append("TEXT $SIG_FONT $SIG_FONT_SZ ${segX + 4} ${row2Y + 14} Segundo conductor\r\n")

            appendSigContent(sigs.investigated, invX, row2Y, boxW - 8)
            appendSigContent(sigs.secondDriver, segX, row2Y, boxW - 8)
        } else {
            append("BOX $SIG_INV_X $row2Y ${SIG_INV_X + SIG_INV_W} $row2BotY 2\r\n")
            append("TEXT $SIG_FONT $SIG_FONT_SZ ${SIG_INV_X + 4} ${row2Y + 14} Investigado\r\n")
            val invImgMaxW = SIG_INV_W - 8
            appendSigContent(sigs.investigated, SIG_INV_X, row2Y, invImgMaxW)
        }

        return row2BotY + 8
    }

    // ============================================================
    // API PÚBLICA
    // ============================================================

    fun printDocumentFromJson(context: Context, mac: String?, jsonAssetPath: String,
                              sigs: PrintSignatures? = null) {
        if (mac.isNullOrBlank()) { showNoMac(context); return }
        CoroutineScope(Dispatchers.IO).launch {
            try { printDocumentFromJsonSuspend(context, mac, jsonAssetPath, sigs) }
            catch (e: Exception) { showError(context, e) }
        }
    }

    fun printDocument(context: Context, mac: String?,
                      doc: JSONObject = JSONObject(), title: String = "",
                      qrUrl: String = "", sigs: PrintSignatures? = null) {
        if (mac.isNullOrBlank()) { showNoMac(context); return }
        CoroutineScope(Dispatchers.IO).launch {
            try { printDocumentSuspend(context, mac, doc, title, qrUrl, sigs) }
            catch (e: Exception) { showError(context, e) }
        }
    }

    fun printDocumentResolved(context: Context, mac: String?, title: String, body: String,
                              sigs: PrintSignatures? = null) {
        if (mac.isNullOrBlank()) { showNoMac(context); return }
        CoroutineScope(Dispatchers.IO).launch {
            try { printDocumentResolvedSuspend(context, mac, title, body, sigs) }
            catch (e: Exception) { showError(context, e) }
        }
    }

    fun printImageTest(context: Context, mac: String?) {
        if (mac.isNullOrBlank()) { showNoMac(context); return }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val eg = pngToEg(context, IMG1_ASSET)
                val imgX = (PAPER_W - eg.width) / 2
                val contentH = IMG_Y + eg.height + MARGIN_BOTTOM
                val cpcl = "EG ${eg.needed} ${eg.height} $imgX $IMG_Y ${eg.hexData}\r\nPRINT\r\n"
                sendToPrinter(context, mac!!, contentH, cpcl, PAPER_W)
            } catch (e: Exception) { showError(context, e) }
        }
    }

    // ============================================================
    // API SUSPEND
    // ============================================================

    suspend fun printDocumentFromJsonSuspend(context: Context, mac: String,
                                             jsonAssetPath: String,
                                             sigs: PrintSignatures? = null) {
        val json = JSONObject(context.assets.open(jsonAssetPath).bufferedReader().readText())
        val doc  = json.getJSONObject("documento")
        printDocumentSuspend(context, mac, doc,
            doc.optString("titulo", ""), doc.optString("qr_url", ""), sigs)
    }

    suspend fun printDocumentResolvedSuspend(
        context: Context, mac: String,
        title: String, body: String,
        sigs: PrintSignatures? = null,
        juicioData: Map<String, String>? = null,
        sharedConn: Connection? = null          // ← conexión compartida opcional
    ) {
        val pw         = PAPER_W
        val titleLines = wrapText(title, TITLE_CHARS)

        // Separar el body en el marcador: líneas antes y después de las cajas
        val markerLine   = DocumentPrinter.JUICIO_BOXES_MARKER
        val allBodyLines = wrapText(body, BODY_CHARS)
        val markerIdx    = allBodyLines.indexOfFirst { it.contains(markerLine) }
        val hasBoxes     = markerIdx >= 0 && juicioData != null

        val bodyBefore = if (hasBoxes) allBodyLines.subList(0, markerIdx)               else allBodyLines
        val bodyAfter  = if (hasBoxes) allBodyLines.subList(markerIdx + 1, allBodyLines.size) else emptyList()

        val juicioDatos: Triple<String, String, String>? = if (hasBoxes) juicioData!!.let { d ->
            Triple(d["fechajuicio"] ?: "", d["horajuicio"] ?: "", d["datosjuzgado"] ?: "")
        } else null

        // Calcular altura total
        val juicioH   = if (hasBoxes) JUICIO_TOTAL_H else 0
        val sigBlockH = if (sigs != null) 8 + SIG_H + SIG_ROW_GAP + SIG_H + 8 else 0
        val contentH  = (BODY_Y
                + bodyBefore.size * BODY_LINE_H
                + juicioH
                + bodyAfter.size  * BODY_LINE_H
                + sigBlockH + MARGIN_BOTTOM)

        val eg1 = pngToEg(context, IMG1_ASSET)
        val eg2 = pngToEg(context, IMG2_ASSET)

        val cpcl = buildString {
            append("EG "+eg1.needed+" "+eg1.height+" $IMG1_X $IMG_Y "+eg1.hexData+"\r\n")
            append("EG "+eg2.needed+" "+eg2.height+" $IMG2_X $IMG_Y "+eg2.hexData+"\r\n")

            if (titleLines.isNotEmpty()) {
                val totalTitleH = titleLines.size * TITLE_LINE_H
                val titleStartY = IMG_Y + (HEADER_H - totalTitleH) / 2
                append("! U1 SETBOLD $TITLE_BOLD\r\n")
                titleLines.forEachIndexed { i, line ->
                    val lineX = TITLE_X1 + (TITLE_W - line.length * TITLE_CHAR_W) / 2
                    append("TEXT $TITLE_FONT $TITLE_SIZE $lineX ${titleStartY + i * TITLE_LINE_H} $line\r\n")
                }
                append("! U1 SETBOLD 0\r\n")
            }

            // Líneas antes del marcador
            var curY = BODY_Y
            bodyBefore.forEach { line ->
                append("TEXT $BODY_FONT $BODY_SIZE $MARGIN $curY $line\r\n")
                curY += BODY_LINE_H
            }

            // Cajas de citación a juicio en el punto exacto del marcador
            if (hasBoxes && juicioDatos != null) {
                curY = appendJuicioBoxes(juicioDatos.first, juicioDatos.second, juicioDatos.third, curY)
                curY += 40 // Añade espacio extra después del box de juzgado
            }

            // Líneas después del marcador
            bodyAfter.forEach { line ->
                append("TEXT $BODY_FONT $BODY_SIZE $MARGIN $curY $line\r\n")
                curY += BODY_LINE_H
            }
            android.util.Log.d("Impresion", "[printDiligenciaSuspend] sigs antes de llamar: " +
                    "isInmovilizacion=${sigs?.isInmovilizacion}, " +
                    "hasSecondDriver=${sigs?.hasSecondDriver}, " +
                    "secondDriver=${sigs?.secondDriver != null}")
            if (sigs != null) appendSignatureBlock(sigs, curY, pw)

            append("PRINT\r\n")
        }
        Log.d(TAG, "CPCL generado:\n" + cpcl)
        sendToPrinter(context, mac, contentH, cpcl, pw, sharedConn)   // ← pasa sharedConn
        Log.d(TAG, "printDocumentResolvedSuspend OK contentH=$contentH")
    }

    suspend fun printDocumentSuspend(context: Context, mac: String,
                                     doc: JSONObject = JSONObject(),
                                     title: String = "", qrUrl: String = "",
                                     sigs: PrintSignatures? = null) {
        android.util.Log.d("Impresion", "[printDocumentSuspend] INICIO para mac=$mac")
        val pw            = PAPER_W
        val contentBlocks = mutableListOf<String>()
        var curY          = BODY_Y

        val intro = doc.optString("introduccion", "")
        android.util.Log.d("Impresion", "[printDocumentSuspend] doc.keys=${doc.keys().asSequence().toList()}")
        if (intro.isNotEmpty()) {
            for (line in wrapText(intro, BODY_CHARS)) {
                contentBlocks.add("TEXT $BODY_FONT $BODY_SIZE $MARGIN $curY $line\r\n")
                curY += BODY_LINE_H
            }
            curY += BODY_GAP_AFTER
        }

        val articulos = doc.optJSONArray("articulos")
        android.util.Log.d("Impresion", "[printDocumentSuspend] Título recibido: '$title'")
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

        val extraSeparation = 5 * BODY_LINE_H // 5 líneas en blanco extra tras el QR
        val qrBlockH  = if (qrUrl.isNotEmpty()) QR_GAP + QR_SIZE + BODY_LINE_H + 8 + extraSeparation else 0
        val sigBlockH = if (sigs != null) 8 + SIG_H + SIG_ROW_GAP + SIG_H + 8 else 0
        val contentH  = (curY + qrBlockH + sigBlockH + MARGIN_BOTTOM)
        val titleLines = wrapText(title, TITLE_CHARS)

        val eg1 = pngToEg(context, IMG1_ASSET)
        val eg2 = pngToEg(context, IMG2_ASSET)
        android.util.Log.d("Impresion", "[printDocumentSuspend] contentBlocks.size=${contentBlocks.size}")

        val cpcl = buildString {
            append("EG ${eg1.needed} ${eg1.height} $IMG1_X $IMG_Y ${eg1.hexData}\r\n")
            append("EG ${eg2.needed} ${eg2.height} $IMG2_X $IMG_Y ${eg2.hexData}\r\n")

            if (titleLines.isNotEmpty()) {
                val totalTitleH = titleLines.size * TITLE_LINE_H
                val titleStartY = IMG_Y + (HEADER_H - totalTitleH) / 2
                append("! U1 SETBOLD $TITLE_BOLD\r\n")
                titleLines.forEachIndexed { i, line ->
                    val lineX = TITLE_X1 + (TITLE_W - line.length * TITLE_CHAR_W) / 2
                    append("TEXT $TITLE_FONT $TITLE_SIZE $lineX ${titleStartY + i * TITLE_LINE_H} $line\r\n")
                }
                append("! U1 SETBOLD 0\r\n")
            }

            for (block in contentBlocks) append(block)

            if (qrUrl.isNotEmpty()) {
                val qrY    = curY + QR_GAP

                // === TEXTO DINÁMICO DEL JSON ===
                val qrDescripcion = if (doc.has("qr")) {
                    doc.getJSONObject("qr").optString("descripcion", "Más información")
                } else {
                    "Más información"
                }

                val labelY = qrY + QR_SIZE + 8
                val labelX = QR_X + (QR_SIZE - qrDescripcion.length * BODY_CHAR_W) / 2

                append("BARCODE QR $QR_X $qrY U $QR_MODULE\r\n")
                append("MA,$qrUrl\r\n")
                append("ENDQR\r\n")
                append("TEXT $QR_LABEL_FONT $QR_LABEL_SIZE $labelX $labelY $qrDescripcion\r\n")

                // 5 líneas en blanco extra después del QR
                var sepY = labelY + BODY_LINE_H
                repeat(5) {
                    append("TEXT $BODY_FONT $BODY_SIZE $MARGIN $sepY \r\n")
                    sepY += BODY_LINE_H
                }
            }

            val afterContent = curY + qrBlockH
            android.util.Log.d("Impresion", "[printDiligenciaSuspend] sigs antes de llamar: " +
                    "isInmovilizacion=${sigs?.isInmovilizacion}, " +
                    "hasSecondDriver=${sigs?.hasSecondDriver}, " +
                    "secondDriver=${sigs?.secondDriver != null}")
            if (sigs != null) appendSignatureBlock(sigs, afterContent, pw)

            append("PRINT\r\n")
        }

        android.util.Log.d("Impresion", "[printDocumentSuspend] Enviando a impresora: contentH=$contentH, curY=$curY, pw=$pw, cpcl.length=${cpcl.length}")
        try {
            sendToPrinter(context, mac, contentH, cpcl, pw)
            android.util.Log.d("Impresion", "[printDocumentSuspend] Envío a impresora OK")
        } catch (e: Exception) {
            android.util.Log.e("Impresion", "[printDocumentSuspend] ERROR al enviar a impresora: ${e.message}", e)
            throw e
        }
    }

    // ============================================================
    // sendToPrinter
    // ============================================================
    private suspend fun sendToPrinter(
        context: Context, mac: String,
        contentH: Int, bodyCpcl: String, pw: Int,
        sharedConn: Connection? = null
    ) {
        return withContext(Dispatchers.IO) {
            if (sharedConn != null) {
                // Reutilizar la conexión compartida — no abrir ni cerrar
                sharedConn.write(
                    "! 0 200 200 $contentH 1\r\nPAGE-WIDTH $pw\r\n"
                        .toByteArray(Charsets.ISO_8859_1)
                )
                sharedConn.write(bodyCpcl.toByteArray(Charsets.ISO_8859_1))
                Log.d(TAG, "Enviados ${bodyCpcl.length} bytes (conexión compartida)")
                Thread.sleep(4000)
            } else {
                // Conexión individual: abrir → enviar → cerrar
                var connection: Connection? = null
                try {
                    connection = BluetoothConnection(mac, 5_000, 2_000)
                    connection.open()
                    Log.d(TAG, "BT abierto")
                    SGD.SET("device.languages", "cpcl", connection)
                    Thread.sleep(500)
                    connection.write(
                        "! 0 200 200 $contentH 1\r\nPAGE-WIDTH $pw\r\n"
                            .toByteArray(Charsets.ISO_8859_1)
                    )
                    connection.write(bodyCpcl.toByteArray(Charsets.ISO_8859_1))
                    Log.d(TAG, "Enviados ${bodyCpcl.length} bytes")
                    Thread.sleep(4000)
                } finally {
                    try { connection?.close() } catch (_: IOException) {}
                    Log.d(TAG, "BT cerrado")
                }
            }
        }
    }

    private fun showNoMac(context: Context) =
        Toast.makeText(context, "No hay impresora configurada", Toast.LENGTH_SHORT).show()

    private fun showError(context: Context, e: Exception) {
        Log.e(TAG, "Error: ${e.message}", e)
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, "Error al imprimir: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

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
}

