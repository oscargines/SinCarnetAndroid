package com.oscar.sincarnet

import android.content.Context

fun imprimirAtestadoCompletoStub(
    context: Context,
    mac: String,
    sigs: PrintSignatures,
    onProgress: (index: Int, total: Int, docName: String) -> Unit = { _, _, _ -> },
    onFinished: () -> Unit = {},
    onError: (msg: String) -> Unit = {}
) {
    // Stub: implementación real pendiente (nombre cambiado para evitar conflicto con DocumentPrinter.imprimirAtestadoCompleto)
    onFinished()
}

