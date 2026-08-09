package com.oscar.sincarnet.data.datasource

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.oscar.sincarnet.domain.model.JuzgadoComunidadAutonoma
import com.oscar.sincarnet.domain.model.JuzgadoMunicipio
import com.oscar.sincarnet.domain.model.JuzgadoProvincia
import com.oscar.sincarnet.domain.model.JuzgadoSede
import java.io.File

private var isAestadoJuzgadosDatabasePrepared = false
private const val LOG_TAG_JUZGADOS_DS = "JuzgadosDataSource"

/**
 * Carga todas las comunidades autónomas desde la base de datos juzgados.db.
 *
 * @param context Contexto de la aplicación.
 * @return Lista de comunidades autónomas ordenadas por ID.
 * @throws Exception Si hay error al acceder a la base de datos.
 */
fun loadJuzgadoCcaaFromDatabase(context: Context): List<JuzgadoComunidadAutonoma> {
    val dbFile = ensureJuzgadosDatabaseAvailable(context)
    val result = mutableListOf<JuzgadoComunidadAutonoma>()

    SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
        db.rawQuery("SELECT idCCAA, Nombre FROM CCAA ORDER BY idCCAA ASC", null).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow("idCCAA")
            val nameIndex = cursor.getColumnIndexOrThrow("Nombre")
            while (cursor.moveToNext()) {
                result += JuzgadoComunidadAutonoma(
                    id = cursor.getInt(idIndex),
                    nombre = cursor.getString(nameIndex)
                )
            }
        }
    }

    return result
}

/**
 * Carga las provincias asociadas a una comunidad autónoma específica.
 *
 * @param context Contexto de la aplicación.
 * @param ccaaId Identificador de la comunidad autónoma.
 * @return Lista de provincias ordenadas alfabéticamente.
 * @throws Exception Si hay error al acceder a la base de datos.
 */
fun loadJuzgadoProvinciasFromDatabase(
    context: Context,
    ccaaId: Int
): List<JuzgadoProvincia> {
    val dbFile = ensureJuzgadosDatabaseAvailable(context)
    val result = mutableListOf<JuzgadoProvincia>()

    SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
        db.rawQuery(
            """
            SELECT 
                p.idProvincia,
                p.Provincia
            FROM PROVINCIAS p
            WHERE p.idCCAA = ?
            ORDER BY p.Provincia
            """.trimIndent(),
            arrayOf(ccaaId.toString())
        ).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow("idProvincia")
            val nameIndex = cursor.getColumnIndexOrThrow("Provincia")
            while (cursor.moveToNext()) {
                result += JuzgadoProvincia(
                    id = cursor.getInt(idIndex),
                    nombre = cursor.getString(nameIndex)
                )
            }
        }
    }

    return result
}

/**
 * Carga los municipios con sedes de juzgado en una provincia específica.
 *
 * @param context Contexto de la aplicación.
 * @param provinciaId Identificador de la provincia.
 * @return Lista de municipios únicos ordenados alfabéticamente.
 * @throws Exception Si hay error al acceder a la base de datos.
 */
fun loadJuzgadoMunicipiosFromDatabase(
    context: Context,
    provinciaId: Int
): List<JuzgadoMunicipio> {
    val dbFile = ensureJuzgadosDatabaseAvailable(context)
    val result = mutableListOf<JuzgadoMunicipio>()

    SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
        db.rawQuery(
            """
            SELECT DISTINCT
                m.idMunicipio,
                m.Municipio
            FROM MUNICIPIOS m
            INNER JOIN SEDES s ON s.municipio = m.Municipio
            WHERE m.idProvincia = ?
            ORDER BY m.Municipio ASC
            """.trimIndent(),
            arrayOf(provinciaId.toString())
        ).use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("Municipio")
            while (cursor.moveToNext()) {
                result += JuzgadoMunicipio(nombre = cursor.getString(nameIndex))
            }
        }
    }

    return result
}

/**
 * Carga todas las sedes de juzgado en un municipio específico.
 *
 * @param context Contexto de la aplicación.
 * @param municipio Nombre del municipio.
 * @return Lista de sedes con información completa.
 * @throws Exception Si hay error al acceder a la base de datos.
 */
fun loadJuzgadoSedesFromDatabase(
    context: Context,
    municipio: String
): List<JuzgadoSede> {
    val dbFile = ensureJuzgadosDatabaseAvailable(context)
    val result = mutableListOf<JuzgadoSede>()

    SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
        db.rawQuery(
            """
            SELECT 
                s.id_juzgado,
                s.nombre,
                s.direccion,
                s.telefono,
                s.codigo_postal
            FROM SEDES s
            WHERE s.municipio = ?
            ORDER BY s.nombre
            """.trimIndent(),
            arrayOf(municipio)
        ).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow("id_juzgado")
            val nameIndex = cursor.getColumnIndexOrThrow("nombre")
            val addressIndex = cursor.getColumnIndexOrThrow("direccion")
            val phoneIndex = cursor.getColumnIndexOrThrow("telefono")
            val postalCodeIndex = cursor.getColumnIndexOrThrow("codigo_postal")
            while (cursor.moveToNext()) {
                result += JuzgadoSede(
                    id = cursor.getInt(idIndex),
                    nombre = cursor.getString(nameIndex),
                    direccion = cursor.getString(addressIndex),
                    telefono = cursor.getString(phoneIndex),
                    codigoPostal = cursor.getString(postalCodeIndex)
                )
            }
        }
    }

    return result
}

/**
 * Proporciona una lista de vista previa de comunidades autónomas para Compose Preview.
 *
 * @return Lista de 3 CCAAs de ejemplo (Andalucía, Asturias, Madrid).
 */
fun loadPreviewJuzgadoCcaa(): List<JuzgadoComunidadAutonoma> = listOf(
    JuzgadoComunidadAutonoma(1, "Andalucia"),
    JuzgadoComunidadAutonoma(2, "Asturias"),
    JuzgadoComunidadAutonoma(3, "Madrid")
)

/**
 * Proporciona provincias de vista previa para una CCAA específica.
 *
 * @param ccaaId Identificador de la CCAA.
 * @return Lista de provincias de ejemplo para esa CCAA.
 */
fun loadPreviewJuzgadoProvincias(ccaaId: Int): List<JuzgadoProvincia> = when (ccaaId) {
    1 -> listOf(
        JuzgadoProvincia(1, "Almeria"),
        JuzgadoProvincia(2, "Cadiz"),
        JuzgadoProvincia(3, "Sevilla")
    )

    2 -> listOf(JuzgadoProvincia(4, "Asturias"))
    3 -> listOf(JuzgadoProvincia(5, "Madrid"))
    else -> emptyList()
}

/**
 * Proporciona municipios de vista previa para una provincia específica.
 *
 * @param provinciaId Identificador de la provincia.
 * @return Lista de municipios de ejemplo para esa provincia.
 */
fun loadPreviewJuzgadoMunicipios(provinciaId: Int): List<JuzgadoMunicipio> = when (provinciaId) {
    1 -> listOf(JuzgadoMunicipio("Almeria"), JuzgadoMunicipio("El Ejido"))
    4 -> listOf(JuzgadoMunicipio("Cangas de Onis"), JuzgadoMunicipio("Oviedo"))
    5 -> listOf(JuzgadoMunicipio("Madrid"))
    else -> emptyList()
}

/**
 * Proporciona sedes de vista previa para un municipio específico.
 *
 * @param municipio Nombre del municipio.
 * @return Lista de sedes de ejemplo para ese municipio.
 */
fun loadPreviewJuzgadoSedes(municipio: String): List<JuzgadoSede> = when (municipio) {
    "Oviedo" -> listOf(
        JuzgadoSede(
            id = 1,
            nombre = "Sección de Instrucción del Tribunal de Instancia nº 1 de Oviedo",
            direccion = "Calle Comandante Caballero, 3",
            telefono = "985000111",
            codigoPostal = "33005"
        )
    )

    "Madrid" -> listOf(
        JuzgadoSede(
            id = 2,
            nombre = "Sección de Instrucción del Tribunal de Instancia de Guardia de Madrid",
            direccion = "Plaza de Castilla, s/n",
            telefono = "914000333",
            codigoPostal = "28046"
        )
    )

    else -> emptyList()
}

private fun ensureJuzgadosDatabaseAvailable(context: Context): File {
    val databaseName = "juzgados.db"
    val primaryAssetPath = "databases/juzgados.db"
    val dbFile = context.getDatabasePath(databaseName)

    if (isAestadoJuzgadosDatabasePrepared && dbFile.exists()) return dbFile

    dbFile.parentFile?.mkdirs()

    val inputStream = runCatching { context.assets.open(primaryAssetPath) }
        .getOrElse { context.assets.open(databaseName) }

    inputStream.use { input ->
        dbFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }

    repairJuzgadosCcaaMappingIfNeeded(dbFile)
    isAestadoJuzgadosDatabasePrepared = true
    return dbFile
}

private fun repairJuzgadosCcaaMappingIfNeeded(dbFile: File) {
    SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
        val alreadyCorrect = db.rawQuery(
            "SELECT COUNT(*) FROM PROVINCIAS WHERE idCCAA = 5 AND idProvincia IN (2,13,16,19,45)",
            null
        ).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0) == 5
        }

        if (alreadyCorrect) {
            Log.d(LOG_TAG_JUZGADOS_DS, "Mapa idCCAA/provincias ya correcto.")
            return
        }

        db.beginTransaction()
        try {
            db.execSQL(
                """
                UPDATE PROVINCIAS
                SET idCCAA = CASE
                    WHEN idProvincia IN (4,11,14,18,21,23,29,41) THEN 1
                    WHEN idProvincia IN (22,44,50) THEN 2
                    WHEN idProvincia = 33 THEN 3
                    WHEN idProvincia = 39 THEN 4
                    WHEN idProvincia IN (2,13,16,19,45) THEN 5
                    WHEN idProvincia IN (5,9,24,34,37,40,42,47,49) THEN 6
                    WHEN idProvincia IN (8,17,25,43) THEN 7
                    WHEN idProvincia IN (3,12,46) THEN 8
                    WHEN idProvincia IN (6,10) THEN 9
                    WHEN idProvincia IN (15,27,32,36) THEN 10
                    WHEN idProvincia = 7 THEN 11
                    WHEN idProvincia IN (35,38) THEN 12
                    WHEN idProvincia = 26 THEN 13
                    WHEN idProvincia = 28 THEN 14
                    WHEN idProvincia = 30 THEN 15
                    WHEN idProvincia = 31 THEN 16
                    WHEN idProvincia IN (1,20,48) THEN 17
                    WHEN idProvincia = 51 THEN 18
                    WHEN idProvincia = 52 THEN 19
                    ELSE idCCAA
                END
                """.trimIndent()
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}