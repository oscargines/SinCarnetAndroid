package com.oscar.sincarnet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del mapeo de firmas de UI a slots de renderizado PDF.
 */
class AtestadoSignaturePdfAdapterTest {

    @Test
    fun `mapSignaturesForPdf maps all signer roles to pdf slots`() {
        val signaturesBySigner = mapOf(
            SIGNER_INSTRUCTOR to "firma_instructor",
            SIGNER_SECRETARY to "firma_secretario",
            SIGNER_INVESTIGATED to "firma_investigado",
            SIGNER_SECOND_DRIVER to "firma_segundo_conductor"
        )

        val mapped = mapSignaturesForPdf(signaturesBySigner)

        assertEquals("firma_instructor", mapped[PdfSignatureSlot.INSTRUCTOR])
        assertEquals("firma_secretario", mapped[PdfSignatureSlot.SECRETARY])
        assertEquals("firma_investigado", mapped[PdfSignatureSlot.INVESTIGATED])
        assertEquals("firma_segundo_conductor", mapped[PdfSignatureSlot.SECOND_DRIVER])
    }

    @Test
    fun `mapSignaturesForPdf excludes empty optional slots`() {
        val signaturesBySigner = mapOf(
            SIGNER_INSTRUCTOR to "firma_instructor",
            SIGNER_SECRETARY to "firma_secretario"
        )

        val mapped = mapSignaturesForPdf(signaturesBySigner)

        assertEquals(2, mapped.size)
        assertFalse(mapped.containsKey(PdfSignatureSlot.INVESTIGATED))
        assertFalse(mapped.containsKey(PdfSignatureSlot.SECOND_DRIVER))
    }

    @Test
    fun `mapSignaturesForPdf uses no-sign text when investigated does not want to sign`() {
        val signaturesBySigner = mapOf(
            SIGNER_INSTRUCTOR to "firma_instructor",
            SIGNER_SECRETARY to "firma_secretario",
            SIGNER_INVESTIGATED to "firma_investigado"
        )

        val mapped = mapSignaturesForPdf(
            signaturesBySigner = signaturesBySigner,
            investigatedWantsToSign = false
        )

        assertEquals(
            PdfSignatureContent.Text(NO_DESEA_FIRMAR_TEXT),
            mapped[PdfSignatureSlot.INVESTIGATED]
        )
    }

    @Test
    fun `mapSignaturesForPdf keeps investigated image when wants to sign`() {
        val signaturesBySigner = mapOf(
            SIGNER_INSTRUCTOR to "firma_instructor",
            SIGNER_SECRETARY to "firma_secretario",
            SIGNER_INVESTIGATED to "firma_investigado"
        )

        val mapped = mapSignaturesForPdf(
            signaturesBySigner = signaturesBySigner,
            investigatedWantsToSign = true
        )

        assertEquals(
            PdfSignatureContent.Image("firma_investigado"),
            mapped[PdfSignatureSlot.INVESTIGATED]
        )
        assertTrue(mapped[PdfSignatureSlot.INVESTIGATED] is PdfSignatureContent.Image)
    }
}

