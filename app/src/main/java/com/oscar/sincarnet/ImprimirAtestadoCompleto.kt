package com.oscar.sincarnet

import android.content.Context

/**
 * Función stub para impresión de atestado completo.
 *
 * Esta es una función reservada para futura implementación de impresión.
 * La funcionalidad de impresión actual se encuentra en [DocumentPrinter.imprimirAtestadoCompleto].
 *
 * @param context Contexto de la aplicación.
 * @param mac Dirección MAC de la impresora Bluetooth.
 * @param sigs Estructura con las firmas capturadas.
 * @param onProgress Callback invocado durante la impresión (index, total, docName).
 * @param onFinished Callback invocado cuando la impresión finaliza.
 * @param onError Callback invocado si ocurre un error durante la impresión.
 *
 * @see PrintSignatures
 * @see DocumentPrinter
 */
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

