package com.oscar.sincarnet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CitacionDocumentLoaderTest {

    @Test
    fun `replaceCitacionPlaceholders compone datosjuzgado con nombre direccion telefono y cp`() {
        val template = "Juzgado: [[datosjuzgado]]"
        val courtData = JuzgadoAtestadoData(
            sedeNombre = "Juzgado de Instruccion n 2",
            sedeDireccion = "Calle Mayor 10",
            sedeTelefono = "985000111",
            sedeCodigoPostal = "33001"
        )

        val result = replaceCitacionPlaceholders(
            text = template,
            courtData = courtData,
            personData = PersonaInvestigadaData(),
            ocurrenciaData = OcurrenciaDelitData(),
            instructorTip = "TIP1",
            secretaryTip = "TIP2",
            instructorUnit = "Puesto Principal"
        )

        assertEquals(
            "Juzgado: Juzgado de Instruccion n 2, Calle Mayor 10, 985000111, 33001",
            result
        )
    }

    @Test
    fun `replaceCitacionPlaceholders no deja placeholders individuales de juzgado sin resolver`() {
        val template = "[[nombrejuzgado]] - [[direccionjuzgado]] - [[telefonojuzgado]] - [[codigopostaljuzgado]]"
        val courtData = JuzgadoAtestadoData(
            sedeNombre = "Juzgado de Instruccion n 5",
            sedeDireccion = "Avenida del Juzgado 1",
            sedeTelefono = "910000000",
            sedeCodigoPostal = "28001"
        )

        val result = replaceCitacionPlaceholders(
            text = template,
            courtData = courtData,
            personData = PersonaInvestigadaData(),
            ocurrenciaData = OcurrenciaDelitData(),
            instructorTip = "TIP1",
            secretaryTip = "TIP2",
            instructorUnit = "Puesto Principal"
        )

        assertEquals(
            "Juzgado de Instruccion n 5 - Avenida del Juzgado 1 - 910000000 - 28001",
            result
        )
        assertFalse(result.contains("[["))
    }
}

