package com.oscar.sincarnet

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del colector de texto de manifestaciones de inmovilización.
 *
 * Usa reflexión para validar un helper interno de generación PDF sin
 * exponerlo en la API pública.
 */
class CollectInmovilizacionManifestacionesTest {

    private fun invokeCollect(value: Any?, hasSecond: Boolean, person: String): String {
        val clazz = Class.forName("com.oscar.sincarnet.AtestadoContinuousPdfGeneratorKt")
        val method = clazz.getDeclaredMethod(
            "collectOrderedTextInmovilizacionManifestaciones",
            Any::class.java,
            java.lang.Boolean.TYPE,
            String::class.java
        )
        method.isAccessible = true
        return method.invoke(null, value, hasSecond, person) as String
    }

    @Test
    fun `sustituye datosconductorhabilitado cuando existe segundo conductor`() {
        val manifestacionesJson = """[
            {
              "id": 1,
              "texto": "Que se hace cargo del vehículo un conductor habilitado.",
              "respuesta": "SI",
              "datos_conductor_habilitado": {
                "descripcion": "Datos de la persona que se hace cargo del vehículo:",
                "campo_variable": "[[datosconductorhabilitado]]",
                "condiciones": "Con permiso de conducción en vigor."
              }
            }
        ]"""
        val manifestaciones = JSONArray(manifestacionesJson)

        val res = invokeCollect(manifestaciones, true, "JUAN PEREZ (12345678A)")
        // Debe contener la línea con los datos resueltos
        assertTrue("Resultado debe contener nombre y DNI del segundo conductor: $res", res.contains("JUAN PEREZ (12345678A)"))
    }

    @Test
    fun `inserta fallback cuando no hay datos segundo conductor`() {
        val manifestacionesJson2 = """[
            {
              "id": 1,
              "texto": "Que se hace cargo del vehículo un conductor habilitado.",
              "respuesta": "SI",
              "datos_conductor_habilitado": {
                "descripcion": "Datos de la persona que se hace cargo del vehículo:",
                "campo_variable": "[[datosconductorhabilitado]]",
                "condiciones": "Con permiso de conducción en vigor."
              }
            }
        ]"""
        val manifestaciones = JSONArray(manifestacionesJson2)

        val res = invokeCollect(manifestaciones, true, "")
        // Debe contener los subguiones de fallback
        assertTrue("Resultado debe contener el fallback de subguiones: $res", res.contains("_______________"))
    }
}

