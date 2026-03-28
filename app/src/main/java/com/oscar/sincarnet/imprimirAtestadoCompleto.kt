package com.oscar.sincarnet

import android.content.Context

fun imprimirAtestadoCompleto(
    context: Context,
    mac: String,
    sigs: PrintSignatures,
    onProgress: (index: Int, total: Int, docName: String) -> Unit = { _, _, _ -> },
    onFinished: () -> Unit = {},
    onError: (msg: String) -> Unit = {}
) {
    // Stub: implementación real pendiente
    onFinished()
}

