package com.oscar.sincarnet

internal const val SIGNER_INSTRUCTOR = "instructor"
internal const val SIGNER_SECRETARY = "secretary"
internal const val SIGNER_INVESTIGATED = "investigated"
internal const val SIGNER_SECOND_DRIVER = "second_driver"
internal const val NO_DESEA_FIRMAR_TEXT = "NO DESEA FIRMAR"

internal enum class PdfSignatureSlot {
    INSTRUCTOR,
    SECRETARY,
    INVESTIGATED,
    SECOND_DRIVER
}

internal sealed interface PdfSignatureContent<out T> {
    data class Image<T>(val value: T) : PdfSignatureContent<T>
    data class Text(val value: String) : PdfSignatureContent<Nothing>
}

internal data class CapturedSignatures<T>(
    val instructor: T?,
    val secretary: T?,
    val investigated: T?,
    val secondDriver: T?
)

internal fun <T> CapturedSignatures<T>.toPdfSlotMap(): Map<PdfSignatureSlot, T> {
    val result = mutableMapOf<PdfSignatureSlot, T>()

    instructor?.let { result[PdfSignatureSlot.INSTRUCTOR] = it }
    secretary?.let { result[PdfSignatureSlot.SECRETARY] = it }
    investigated?.let { result[PdfSignatureSlot.INVESTIGATED] = it }
    secondDriver?.let { result[PdfSignatureSlot.SECOND_DRIVER] = it }

    return result
}

internal fun <T> mapSignaturesForPdf(signaturesBySigner: Map<String, T>): Map<PdfSignatureSlot, T> {
    val captured = CapturedSignatures(
        instructor = signaturesBySigner[SIGNER_INSTRUCTOR],
        secretary = signaturesBySigner[SIGNER_SECRETARY],
        investigated = signaturesBySigner[SIGNER_INVESTIGATED],
        secondDriver = signaturesBySigner[SIGNER_SECOND_DRIVER]
    )

    return captured.toPdfSlotMap()
}

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

