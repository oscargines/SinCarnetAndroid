package com.oscar.sincarnet

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

private var isPaisesDatabasePrepared = false

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

internal fun withSpainFirst(items: List<String>): List<String> {
    val deduped = items
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }

    val spain = deduped.firstOrNull { it.equals("España", ignoreCase = true) }
    val rest = deduped.filterNot { it.equals("España", ignoreCase = true) }

    return if (spain != null) listOf(spain) + rest else listOf("España") + rest
}

