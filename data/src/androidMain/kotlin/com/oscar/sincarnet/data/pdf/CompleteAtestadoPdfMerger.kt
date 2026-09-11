package com.oscar.sincarnet.data.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import java.io.File
import java.io.FileOutputStream
import android.os.ParcelFileDescriptor

/** Combina los PDFs del atestado y de los documentos escaneados en un único PDF. */
fun mergeAtestadoPdfs(
    atestadoFile: File,
    scannedDocumentsFile: File?,
    outputFile: File,
    prefixFiles: List<File> = emptyList(),
    suffixFiles: List<File> = emptyList()
): File {
    require(atestadoFile.isFile) { "No se ha generado el PDF del atestado" }
    require(scannedDocumentsFile == null || scannedDocumentsFile.isFile) {
        "No se ha encontrado el PDF de documentos escaneados"
    }
    require(prefixFiles.all { it.isFile }) { "No se han encontrado las páginas iniciales del atestado" }
    require(suffixFiles.all { it.isFile }) { "No se han encontrado las páginas de anexos del atestado" }

    val output = PdfDocument()
    var pageNumber = 1
    (prefixFiles + listOf(atestadoFile) + suffixFiles + listOfNotNull(scannedDocumentsFile)).forEach { sourceFile ->
        ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                for (index in 0 until renderer.pageCount) {
                    renderer.openPage(index).use { sourcePage ->
                        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
                        val page = output.startPage(pageInfo)
                        val canvas = page.canvas
                        canvas.drawColor(Color.WHITE)
                        val bitmap = Bitmap.createBitmap(1190, 1684, Bitmap.Config.ARGB_8888)
                        try {
                            bitmap.eraseColor(Color.WHITE)
                            sourcePage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                            canvas.drawBitmap(bitmap, null, Rect(0, 0, 595, 842), null)
                        } finally {
                            bitmap.recycle()
                            output.finishPage(page)
                        }
                    }
                }
            }
        }
    }

    outputFile.parentFile?.mkdirs()
    FileOutputStream(outputFile).use { output.writeTo(it) }
    output.close()
    return outputFile
}
