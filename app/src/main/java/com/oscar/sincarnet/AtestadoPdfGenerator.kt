package com.oscar.sincarnet

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class AtestadoPdfResult(
    val file: File,
    val createdAtMillis: Long
)

internal fun generateAtestadoSignaturesPdf(
    context: Context,
    signatures: Map<PdfSignatureSlot, PdfSignatureContent<ImageBitmap>>,
    investigatedNoSignText: String = NO_DESEA_FIRMAR_TEXT
): AtestadoPdfResult {
    val now = System.currentTimeMillis()
    val pdfDocument = PdfDocument()

    val pageWidth = 595 // A4 @ 72 dpi
    val pageHeight = 842
    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 18f
        isFakeBoldText = true
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 11f
    }
    val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    val margin = 40f
    var cursorY = 60f

    canvas.drawText("Atestado - Hoja de firmas", margin, cursorY, titlePaint)
    cursorY += 22f
    val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(now))
    canvas.drawText("Generado: $formattedDate", margin, cursorY, textPaint)
    cursorY += 28f

    val rowGap = 16f
    val boxHeight = 140f

    val slotConfig = listOf(
        PdfSignatureSlot.INSTRUCTOR to "Instructor",
        PdfSignatureSlot.SECRETARY to "Secretario",
        PdfSignatureSlot.INVESTIGATED to "Investigado",
        PdfSignatureSlot.SECOND_DRIVER to "Segundo conductor"
    )

    slotConfig.forEach { (slot, label) ->
        val top = cursorY
        val bottom = top + boxHeight
        val rect = RectF(margin, top, pageWidth - margin, bottom)

        canvas.drawText(label, rect.left + 8f, rect.top + 16f, textPaint)
        canvas.drawRect(rect, boxPaint)

        when (val content = signatures[slot]) {
            is PdfSignatureContent.Image -> {
                val bitmap = content.value.asAndroidBitmap()
                val targetTop = rect.top + 24f
                val targetBottom = rect.bottom - 10f
                val targetLeft = rect.left + 8f
                val targetRight = rect.right - 8f
                val targetWidth = targetRight - targetLeft
                val targetHeight = targetBottom - targetTop

                val srcWidth = bitmap.width.toFloat().coerceAtLeast(1f)
                val srcHeight = bitmap.height.toFloat().coerceAtLeast(1f)
                val scale = minOf(targetWidth / srcWidth, targetHeight / srcHeight)
                val drawWidth = srcWidth * scale
                val drawHeight = srcHeight * scale
                val dx = targetLeft + (targetWidth - drawWidth) / 2f
                val dy = targetTop + (targetHeight - drawHeight) / 2f
                val dst = RectF(dx, dy, dx + drawWidth, dy + drawHeight)

                canvas.drawBitmap(bitmap, null, dst, null)
            }
            is PdfSignatureContent.Text -> {
                val text = content.value.ifBlank { investigatedNoSignText }
                canvas.drawText(text, rect.left + 12f, rect.centerY(), textPaint)
            }
            null -> {
                canvas.drawText("Sin firma", rect.left + 12f, rect.centerY(), textPaint)
            }
        }

        cursorY = bottom + rowGap
    }

    pdfDocument.finishPage(page)

    val directory = File(context.filesDir, "atestados").apply { mkdirs() }
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(now))
    val file = File(directory, "atestado_firmas_$timestamp.pdf")

    FileOutputStream(file).use { output ->
        pdfDocument.writeTo(output)
    }
    pdfDocument.close()

    return AtestadoPdfResult(file = file, createdAtMillis = now)
}

