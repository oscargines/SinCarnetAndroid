package com.oscar.sincarnet

import androidx.compose.ui.graphics.ImageBitmap

// ============================================================
// PrintSignatures
//
// Agrupa las firmas capturadas en FirmasAtestadoScreen para
// pasarlas al motor de impresión.
//
// Las firmas son ImageBitmap en memoria (no se persisten).
// Se convierten a EG CPCL en BluetoothPrinterUtils justo
// antes de enviarlas a la impresora.
//
// Uso desde TomaDatosAtestadoScreen:
//   val sigs = PrintSignatures(
//       instructor  = instructorSignature,
//       secretary   = secretarySignature,
//       investigated = investigatedSignature
//   )
//   imprimirAtestadoCompleto(context, mac, sigs, ...)
// ============================================================
data class PrintSignatures(
    val instructor:   ImageBitmap? = null,
    val secretary:    ImageBitmap? = null,
    val investigated: ImageBitmap? = null,
    val instructorTip:  String = "",
    val secretaryTip:   String = "",
    // Nuevo: firma y flag para segundo conductor y layout especial
    val secondDriver: ImageBitmap? = null,
    val isInmovilizacion: Boolean = false,
    val hasSecondDriver: Boolean = false
)