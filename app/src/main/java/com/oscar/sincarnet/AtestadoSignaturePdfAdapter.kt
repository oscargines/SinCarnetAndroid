package com.oscar.sincarnet

/**
 * Constantes para los identificadores de firmantes en los diccionarios de firmas.
 * Se utilizan como claves en [Map] para asociar firmas con sus signatarios.
 */
internal const val SIGNER_INSTRUCTOR = "instructor"
internal const val SIGNER_SECRETARY = "secretary"
internal const val SIGNER_INVESTIGATED = "investigated"
internal const val SIGNER_SECOND_DRIVER = "second_driver"
internal const val NO_DESEA_FIRMAR_TEXT = "NO DESEA FIRMAR"

/**
 * Enumeración que representa las posiciones/espacios de firma en el PDF del atestado.
 *
 * Cada valor corresponde a un área específica del documento donde se inserta una firma,
 * ya sea como imagen de firma manuscrita o como texto (cuando el firmante se niega).
 */
internal enum class PdfSignatureSlot {
    INSTRUCTOR,
    SECRETARY,
    INVESTIGATED,
    SECOND_DRIVER
}

/**
 * Interfaz sellada que representa el contenido de una firma en un PDF.
 *
 * Puede ser una imagen (firma manuscrita capturada en pantalla) o texto
 * (cuando el investigado no desea firmar, se inserta un texto predefinido).
 *
 * @param T Tipo genérico para las imágenes de firma.
 */
internal sealed interface PdfSignatureContent<out T> {
    /**
     * Firma capturada como imagen.
     * @param value Imagen de la firma (típicamente ImageBitmap o ByteArray).
     */
    data class Image<T>(val value: T) : PdfSignatureContent<T>

    /**
     * Firma representada como texto (p. ej. "NO DESEA FIRMAR").
     * @param value Texto que aparecerá en el PDF en lugar de una firma manuscrita.
     */
    data class Text(val value: String) : PdfSignatureContent<Nothing>
}

/**
 * Estructura que agrupa las firmas capturadas de todos los signatarios en un atestado.
 *
 * Se utiliza como paso intermedio antes de convertir las firmas a formato compatible
 * con PDF, permitiendo un manejo más estructurado de las firmas individuales.
 *
 * @param T Tipo genérico de las firmas (típicamente ImageBitmap).
 * @property instructor Firma del instructor, o null si no se capturó.
 * @property secretary Firma del secretario, o null si no se capturó.
 * @property investigated Firma del investigado, o null si no se capturó.
 * @property secondDriver Firma del segundo conductor (si existe), o null.
 */
internal data class CapturedSignatures<T>(
    val instructor: T?,
    val secretary: T?,
    val investigated: T?,
    val secondDriver: T?
)

/**
 * Convierte [CapturedSignatures] a un mapa asociando cada firma con su correspondiente
 * [PdfSignatureSlot]. Solo incluye las firmas que existen (no null).
 *
 * @return Diccionario de [PdfSignatureSlot] a imagen de firma, sin entradas null.
 */
internal fun <T> CapturedSignatures<T>.toPdfSlotMap(): Map<PdfSignatureSlot, T> {
    val result = mutableMapOf<PdfSignatureSlot, T>()

    instructor?.let { result[PdfSignatureSlot.INSTRUCTOR] = it }
    secretary?.let { result[PdfSignatureSlot.SECRETARY] = it }
    investigated?.let { result[PdfSignatureSlot.INVESTIGATED] = it }
    secondDriver?.let { result[PdfSignatureSlot.SECOND_DRIVER] = it }

    return result
}

/**
 * Mapea un diccionario de firmas (por nombre de firmante) a un diccionario indexado por
 * [PdfSignatureSlot], facilitando su inserción en el PDF.
 *
 * @param signaturesBySigner Diccionario con claves como [SIGNER_INSTRUCTOR], [SIGNER_SECRETARY], etc.
 * @param T Tipo genérico de la firma (típicamente ImageBitmap).
 * @return Diccionario [PdfSignatureSlot] → firma, ready para PDFDocument.
 */
internal fun <T> mapSignaturesForPdf(signaturesBySigner: Map<String, T>): Map<PdfSignatureSlot, T> {
    val captured = CapturedSignatures(
        instructor = signaturesBySigner[SIGNER_INSTRUCTOR],
        secretary = signaturesBySigner[SIGNER_SECRETARY],
        investigated = signaturesBySigner[SIGNER_INVESTIGATED],
        secondDriver = signaturesBySigner[SIGNER_SECOND_DRIVER]
    )

    return captured.toPdfSlotMap()
}

/**
 * Versión extendida de [mapSignaturesForPdf] que maneja el caso especial
 * donde el investigado no desea firmar, reemplazando su firma con un texto.
 *
 * @param signaturesBySigner Diccionario de firmas por nombre de firmante.
 * @param investigatedWantsToSign Si false, se reemplaza la firma del investigado con texto.
 * @param investigatedNoSignText Texto a insertar si [investigatedWantsToSign] es false (por defecto "NO DESEA FIRMAR").
 * @param T Tipo genérico de la firma.
 * @return Diccionario [PdfSignatureSlot] → [PdfSignatureContent], permitiendo mezclar imágenes y texto.
 */
internal fun <T> mapSignaturesForPdf(
    signaturesBySigner: Map<String, T>,
    investigatedWantsToSign: Boolean,
    investigatedNoSignText: String = NO_DESEA_FIRMAR_TEXT
): Map<PdfSignatureSlot, PdfSignatureContent<T>> {
    val result = mutableMapOf<PdfSignatureSlot, PdfSignatureContent<T>>()

    signaturesBySigner[SIGNER_INSTRUCTOR]?.let {
        result[PdfSignatureSlot.INSTRUCTOR] = PdfSignatureContent.Image(it)
    }
    signaturesBySigner[SIGNER_SECRETARY]?.let {
        result[PdfSignatureSlot.SECRETARY] = PdfSignatureContent.Image(it)
    }

    if (investigatedWantsToSign) {
        signaturesBySigner[SIGNER_INVESTIGATED]?.let {
            result[PdfSignatureSlot.INVESTIGATED] = PdfSignatureContent.Image(it)
        }
    } else {
        result[PdfSignatureSlot.INVESTIGATED] = PdfSignatureContent.Text(investigatedNoSignText)
    }

    signaturesBySigner[SIGNER_SECOND_DRIVER]?.let {
        result[PdfSignatureSlot.SECOND_DRIVER] = PdfSignatureContent.Image(it)
    }

    return result
}

