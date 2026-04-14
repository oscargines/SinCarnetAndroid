package com.oscar.sincarnet

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Modelo de datos que agrupa las firmas capturadas en la pantalla de firma de atestados.
 *
 * Las firmas se capturan en [FirmasAtestadoScreen] como [ImageBitmap] en memoria (sin persistencia).
 * Son convertidas a formato CPCL de Zebra en [BluetoothPrinterUtils] justo antes de ser
 * enviadas a la impresora mediante [DocumentPrinter].
 *
 * Soporta múltiples escenarios:
 * - **Atestado estándar**: instructor, secretario e investigado
 * - **Atestado con inmovilización**: incluye segundo conductor
 * - **Investigado que no desea firmar**: se reemplaza la firma con un texto predefinido
 *
 * Ejemplo de uso:
 * ```kotlin
 * val sigs = PrintSignatures(
 *     instructor = instructorSignature,
 *     secretary = secretarySignature,
 *     investigated = investigatedSignature,
 *     instructorTip = "D. Juan Pérez",
 *     secretaryTip = "D. María García"
 * )
 * DocumentPrinter.imprimirAtestadoCompleto(context, mac, sigs, ...)
 * ```
 *
 * @property instructor Firma del instructor/instructor de policía como ImageBitmap, o null si no se firmó.
 * @property secretary Firma del secretario como ImageBitmap, o null si no se firmó.
 * @property investigated Firma del investigado como ImageBitmap, o null si no se firmó.
 * @property instructorTip Identificación/tip del instructor (p. ej. nombre y número de placa).
 * @property secretaryTip Identificación/tip del secretario (p. ej. nombre y número de placa).
 * @property secondDriver Firma del segundo conductor (para atestados de inmovilización), o null.
 * @property isInmovilizacion Indica si el atestado es de inmovilización de vehículo.
 * @property hasSecondDriver Indica si existe un segundo conductor que requiere firma.
 * @property investigatedNoSignText Texto que aparece en el PDF cuando el investigado no desea firmar (por defecto "NO DESEA FIRMAR").
 */
data class PrintSignatures(
    val instructor:   ImageBitmap? = null,
    val secretary:    ImageBitmap? = null,
    val investigated: ImageBitmap? = null,
    val instructorTip:  String = "",
    val secretaryTip:   String = "",
    // Nuevo: firma y flag para segundo conductor y layout especial
    val secondDriver: ImageBitmap? = null,
    val isInmovilizacion: Boolean = false,
    val hasSecondDriver: Boolean = false,
    val investigatedNoSignText: String = "NO DESEA FIRMAR"  // Texto cuando el investigado no desea firmar
)