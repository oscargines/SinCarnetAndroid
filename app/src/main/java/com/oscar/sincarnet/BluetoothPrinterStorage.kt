package com.oscar.sincarnet

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

internal data class SavedBluetoothPrinter(
    val id: Int,
    val nombre: String,
    val mac: String
)

internal class BluetoothPrinterStorage(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSavedPrinters(): List<SavedBluetoothPrinter> {
        val dbFile = ensureBluetoothPrintersDatabaseAvailable(context)
        val printers = mutableListOf<SavedBluetoothPrinter>()

        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery(
                "SELECT id, nombre, mac FROM dispositivos ORDER BY nombre COLLATE NOCASE ASC",
                null
            ).use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow("id")
                val nameIndex = cursor.getColumnIndexOrThrow("nombre")
                val macIndex = cursor.getColumnIndexOrThrow("mac")
                while (cursor.moveToNext()) {
                    printers += SavedBluetoothPrinter(
                        id = cursor.getInt(idIndex),
                        nombre = cursor.getString(nameIndex).orEmpty(),
                        mac = cursor.getString(macIndex).orEmpty()
                    )
                }
            }
        }

        return printers
    }

    fun savePrinter(nombre: String, mac: String): SavedBluetoothPrinter {
        val dbFile = ensureBluetoothPrintersDatabaseAvailable(context)
        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.beginTransaction()
            try {
                val existingId = db.rawQuery(
                    "SELECT id FROM dispositivos WHERE mac = ? LIMIT 1",
                    arrayOf(mac)
                ).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getInt(0) else null
                }

                if (existingId != null) {
                    db.execSQL(
                        "UPDATE dispositivos SET nombre = ?, mac = ? WHERE id = ?",
                        arrayOf<Any>(nombre, mac, existingId)
                    )
                } else {
                    db.execSQL(
                        "INSERT INTO dispositivos(nombre, mac) VALUES(?, ?)",
                        arrayOf(nombre, mac)
                    )
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

        setDefaultPrinterMac(mac)
        return loadSavedPrinters().first { it.mac == mac }
    }

    fun getDefaultPrinter(): SavedBluetoothPrinter? {
        val defaultMac = prefs.getString(KEY_DEFAULT_PRINTER_MAC, null) ?: return null
        return loadSavedPrinters().firstOrNull { it.mac == defaultMac }
    }

    fun setDefaultPrinterMac(mac: String) {
        prefs.edit().putString(KEY_DEFAULT_PRINTER_MAC, mac).apply()
    }

    private companion object {
        const val PREFS_NAME = "bluetooth_printer_storage"
        const val KEY_DEFAULT_PRINTER_MAC = "default_printer_mac"
    }
}

private var isBluetoothPrintersDbPrepared = false

private fun ensureBluetoothPrintersDatabaseAvailable(context: Context): File {
    val databaseName = "dispositivos.db"
    val primaryAssetPath = "databases/dispositivos.db"
    val dbFile = context.getDatabasePath(databaseName)

    if (isBluetoothPrintersDbPrepared && dbFile.exists()) return dbFile

    dbFile.parentFile?.mkdirs()

    if (!dbFile.exists()) {
        val copied = runCatching {
            context.assets.open(primaryAssetPath).use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }.isSuccess

        if (!copied) {
            SQLiteDatabase.openOrCreateDatabase(dbFile, null).close()
        }
    }

    ensureBluetoothPrintersSchema(dbFile)
    isBluetoothPrintersDbPrepared = true
    return dbFile
}

private fun ensureBluetoothPrintersSchema(dbFile: File) {
    SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS dispositivos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                mac TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

