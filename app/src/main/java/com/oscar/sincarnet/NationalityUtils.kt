package com.oscar.sincarnet

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

private var isPaisesDatabasePrepared = false

/**
 * Carga la lista de nacionalidades desde una base de datos SQLite (paises.db).
 *
 * La función busca y copia la base de datos desde los assets si no existe en el
 * almacenamiento privado de la aplicación. Las nacionalidades se cargan ordenadas
 * alfabéticamente (con España siempre al inicio, si está disponible).
 *
 * La consulta ejecutada es:
 * ```sql
 * SELECT nombre FROM paises ORDER BY nombre COLLATE NOCASE ASC
 * ```
 *
 * @param context Contexto de la aplicación usado para acceder a assets y bases de datos.
 * @return Lista de nacionalidades ordenadas alfabéticamente con España al inicio.
 *
 * @throws Exception Si hay error al abrir o consultar la base de datos.
 *
 * @see withSpainFirst
 * @see ensurePaisesDatabaseAvailable
 */
internal fun loadNationalitiesFromDatabase(context: Context): List<String> {
    val dbFile = ensurePaisesDatabaseAvailable(context)
    val names = mutableListOf<String>()

    SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
        db.rawQuery("SELECT nombre FROM paises ORDER BY nombre COLLATE NOCASE ASC", null)
            .use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("nombre")
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex)?.trim().orEmpty()
                    if (name.isNotEmpty()) names += name
                }
            }
    }

    return withSpainFirst(names)
}

/**
 * Asegura que la base de datos de países (paises.db) esté disponible en el
 * almacenamiento privado de la aplicación.
 *
 * Si la base de datos no existe, la copia desde los assets. El cache `isPaisesDatabasePrepared`
 * evita copias repetidas en la misma sesión.
 *
 * @param context Contexto de la aplicación.
 * @return Archivo [File] de la base de datos paises.db preparada.
 *
 * @throws IOException Si hay error al acceder a assets o copiar el archivo.
 */
private fun ensurePaisesDatabaseAvailable(context: Context): File {
    val databaseName = "paises.db"
    val primaryAssetPath = "databases/paises.db"
    val dbFile = context.getDatabasePath(databaseName)

    if (isPaisesDatabasePrepared && dbFile.exists()) return dbFile

    dbFile.parentFile?.mkdirs()

    val inputStream = runCatching { context.assets.open(primaryAssetPath) }
        .getOrElse { context.assets.open(databaseName) }

    inputStream.use { input ->
        dbFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }

    isPaisesDatabasePrepared = true
    return dbFile
}

/**
 * Ordena una lista de nacionalidades poniendo "España" al inicio.
 *
 * La función realiza las siguientes operaciones:
 * - Elimina espacios en blanco al principio y final
 * - Filtra elementos vacíos
 * - Deduplica ignorando mayúsculas/minúsculas
 * - Busca y coloca "España" al inicio
 * - Si "España" no está en la lista original, se añade automáticamente
 *
 * @param items Lista de nacionalidades a ordenar.
 * @return Lista ordenada con "España" al inicio, seguida de las demás en orden original.
 */
internal fun withSpainFirst(items: List<String>): List<String> {
    val deduped = items
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }

    val spain = deduped.firstOrNull { it.equals("España", ignoreCase = true) }
    val rest = deduped.filterNot { it.equals("España", ignoreCase = true) }

    return if (spain != null) listOf(spain) + rest else listOf("España") + rest
}

