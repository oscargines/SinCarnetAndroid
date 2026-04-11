package com.oscar.sincarnet

import org.junit.Assert.assertTrue
import org.junit.Test

class ReplacePlaceholdersTolerantTest {

    @Test
    fun `replaceCitacionPlaceholders corrige placeholders con espacios y typos`() {
        val template = """
            En [[lugar]], término municipal de [[terminomunicipal]] y Partido Judicial de [[partidojudicial]], siendo las [[hora]] horas del día [[fechacompleta]], siendo el Instructor de las presentes el Guardia Civil con TIP número [[instructor]], y actuando como Secretario el Guardia Civil con TIP numero [[secretario]], mediante la presente, se hace constar que se procede a la identificación de D./Dña. [[nombrecompletoinvestigado]] con documento nacional de identidad número [[documentoidentificacion]], identidad acreditada documentalmente, nacido/a el [[fecha nacimiento]], en la localidad de [[luga rnacimiento]], hijo de [[nombrepadre]] y de [[nombremadre]], con domicilio en [[domicilio]] con teléfono móvil [[telefono]] y correo electrónico [[correoeIectronico ]].
        """.trimIndent()

        val courtData = JuzgadoAtestadoData(
            municipioNombre = "Llanes",
            sedeNombre = "Sede",
            sedeDireccion = "Dir",
            sedeTelefono = "900000000",
            sedeCodigoPostal = "33001"
        )

        val personData = PersonaInvestigadaData(
            firstName = "PEPE",
            lastName1 = "PEREZ",
            lastName2 = "PEREZ",
            documentIdentification = "12345678A",
            birthDate = "01-01-1980",
            birthPlace = "Ribadesella",
            fatherName = "JUAN",
            motherName = "MARIA",
            address = "Calle Falsa 1",
            phone = "600000000",
            email = "pepe@example.com"
        )

        val ocurrenciaData = OcurrenciaDelitData(
            carretera = "N-630",
            pk = "14",
            localidad = "Ribadesella",
            terminoMunicipal = "Ribadesella",
            fecha = "17-03-2026",
            hora = "07:55"
        )

        val result = replaceCitacionPlaceholders(
            text = template,
            courtData = courtData,
            personData = personData,
            ocurrenciaData = ocurrenciaData,
            instructorTip = "R13968H",
            secretaryTip = "A12345B",
            instructorUnit = "Puesto",
        )

        // Comprobamos partes individuales para ayudar a depuración si algo falla
        assertTrue("Result no contiene fecha nacimiento: $result", result.contains("01-01-1980"))
        assertTrue("Result no contiene lugar nacimiento: $result", result.contains("Ribadesella"))
        assertTrue("Result no contiene nombre completo: $result", result.contains("PEPE PEREZ PEREZ"))
        assertTrue("Result no contiene telefono: $result", result.contains("600000000"))
        assertTrue("Result no contiene email: $result", result.contains("pepe@example.com"))
    }
}

