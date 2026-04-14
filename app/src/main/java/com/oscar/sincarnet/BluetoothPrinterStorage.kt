package com.oscar.sincarnet

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * Datos de una impresora Bluetooth guardada en la aplicación.
 *
 * Cada impresora se identifica por su dirección MAC (Media Access Control),
 * que es el identificador único del dispositivo Bluetooth.
 *
 * @property id ID único en la base de datos (PRIMARY KEY AUTOINCREMENT)
 * @property nombre Nombre descriptivo asignado por el usuario (p.ej. "Impresora 1º Turno")
 * @property mac Dirección MAC del dispositivo (p.ej. "00:1A:7D:DA:71:13")
 */
internal data class SavedBluetoothPrinter(
    val id: Int,
    val nombre: String,
    val mac: String
)

/**
 * Gestor de persistencia de impresoras Bluetooth.
 *
 * Responsabilidades:
 * 1. **Lectura de impresoras**: Carga lista de impresoras guardadas desde SQLite
 * 2. **Guardado/Actualización**: Persiste nuevas impresoras o actualiza existentes
 * 3. **Impresora por defecto**: Mantiene referencia a la impresora seleccionada por el usuario
 * 4. **Validación**: Verifica que la MAC sea válida antes de guardar
 *
 * **Almacenamiento**:
 * - Tabla SQLite: `dispositivos` (id, nombre, mac)
 * - SharedPreferences: Clave de impresora por defecto
 * - Base de datos: Copiada desde assets (`databases/dispositivos.db`) si es primera vez
 *
 * **Flujo típico**:
 * 1. Usuario abre BluetoothPrinterScreen
 * 2. Se carga lista con [loadSavedPrinters]
 * 3. Usuario selecciona impresora y presiona "Usar esta"
 * 4. Se llama [setDefaultPrinterMac] para guardar
 * 5. Siguientes veces, [getDefaultPrinter] la carga automáticamente
 *
 * @property context Contexto de aplicación (para acceso a SharedPreferences y databases)
 *
 * @see SavedBluetoothPrinter Estructura de datos de impresora
 * @see DocumentPrinter.imprimirAtestadoCompleto Usuario principal de esta clase
 */
internal class BluetoothPrinterStorage(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Carga todas las impresoras guardadas desde la base de datos.
     *
     * Proceso:
     * 1. Garantiza que la BD existe (copia desde assets si es necesario)
     * 2. Abre la BD en modo lectura
     * 3. Ejecuta query: `SELECT id, nombre, mac FROM dispositivos ORDER BY nombre COLLATE NOCASE ASC`
     * 4. Mapea cada fila a [SavedBluetoothPrinter]
     * 5. Retorna lista ordenada alfabéticamente (sin distinción mayúsculas/minúsculas)
     *
     * @return Lista de [SavedBluetoothPrinter] (vacía si no hay impresoras)
     * @throws Exception Si ocurre error al abrir/leer la BD
     */
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

     /**
      * Guarda una nueva impresora o actualiza una existente.
      *
      * Lógica:
      * 1. Si la MAC ya existe en la BD, actualiza nombre y MAC (en caso de cambios)
      * 2. Si es nueva MAC, inserta registro con nombre y MAC
      * 3. Establece automáticamente como impresora por defecto
      * 4. Retorna el objeto [SavedBluetoothPrinter] guardado (incluye ID generado)
      *
      * Transaccionalidad:
      * - Usa transacción SQLite para garantizar consistencia
      * - Si algo falla, rollback automático
      *
      * Validación:
      * - MAC no debe estar vacía/nula
      * - Nombre puede estar vacío (se permite)
      *
      * @param nombre Nombre descriptivo de la impresora (p.ej. "Impresora Juzgado")
      * @param mac Dirección MAC (p.ej. "00:1A:7D:DA:71:13")
      * @return [SavedBluetoothPrinter] con datos guardados (incluye ID auto-generado)
      * @throws Exception Si ocurre error en la transacción
      *
      * @see setDefaultPrinterMac Se llama automáticamente
      */
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

     /**
      * Obtiene la impresora configurada como por defecto.
      *
      * Flujo:
      * 1. Lee la MAC desde SharedPreferences
      * 2. Si no hay MAC guardada, retorna null
      * 3. Busca la impresora con esa MAC en la BD
      * 4. Si la impresora ya no existe, limpia la referencia
      * 5. Retorna [SavedBluetoothPrinter] o null
      *
      * **Caso de uso**: Cuando la app abre, intenta usar la última impresora seleccionada.
      *
      * @return [SavedBluetoothPrinter] si existe, null si no hay por defecto o fue eliminada
      */
     fun getDefaultPrinter(): SavedBluetoothPrinter? {
         val defaultMac = prefs.getString(KEY_DEFAULT_PRINTER_MAC, null) ?: return null
         val printer = loadSavedPrinters().firstOrNull { it.mac == defaultMac }
         if (printer == null) {
             clearDefaultPrinter()
         }
         return printer
     }

     /**
      * Establece una impresora como la por defecto.
      *
      * Requisitos:
      * - La MAC no debe estar vacía/nula
      * - La MAC debe corresponder a una impresora ya guardada
      * 
      * Si la MAC está vacía, se limpia la impresora por defecto.
      * Si la MAC no existe, se limpia la impresora por defecto.
      *
      * @param mac Dirección MAC de la impresora a establecer como defecto
      *
      * @see getDefaultPrinter Para obtener la impresora por defecto
      * @see clearDefaultPrinter Para limpiar la selección
      */
     fun setDefaultPrinterMac(mac: String) {
        if (mac.isBlank()) {
            clearDefaultPrinter()
            return
        }

        val exists = loadSavedPrinters().any { it.mac == mac }
        if (exists) {
            prefs.edit().putString(KEY_DEFAULT_PRINTER_MAC, mac).apply()
        } else {
            clearDefaultPrinter()
        }
    }

     /**
      * Elimina la referencia de impresora por defecto de SharedPreferences.
      *
      * Llamado cuando:
      * - Se establece MAC vacía/nula
      * - La impresora por defecto ya no existe en la BD
      * - Usuario presiona "Olvidar esta impresora"
      */
     private fun clearDefaultPrinter() {
         prefs.edit().remove(KEY_DEFAULT_PRINTER_MAC).apply()
     }

     private companion object {
         const val PREFS_NAME = "bluetooth_printer_storage"
         const val KEY_DEFAULT_PRINTER_MAC = "default_printer_mac"
     }
 }

/**
 * Flag global para evitar reiniciar la BD múltiples veces.
 *
 * Se establece en true después de la primera inicialización exitosa.
 * Mejora rendimiento evitando validaciones repetidas.
 */
 private var isBluetoothPrintersDbPrepared = false

/**
 * Garantiza que la base de datos de impresoras Bluetooth está disponible.
 *
 * Flujo:
 * 1. Si ya está preparada y el archivo existe, retorna directamente
 * 2. Si no existe, la copia desde assets (`databases/dispositivos.db`)
 * 3. Si la copia falla, crea una BD vacía
 * 4. Valida y crea el esquema (tabla `dispositivos` si no existe)
 * 5. Marca como preparada y retorna la ruta del archivo
 *
 * **Assets**:
 * - Ruta: `app/src/main/assets/databases/dispositivos.db`
 * - Contiene lista inicial de impresoras y juzgados (si aplica)
 *
 * @param context Contexto para acceso a assets y databases
 * @return [File] apuntando a la BD (`context.getDatabasePath("dispositivos.db")`)
 */
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

 /**
  * Valida y crea el esquema de la base de datos de impresoras.
  *
  * Crea la tabla `dispositivos` si no existe:
  * ```sql
  * CREATE TABLE IF NOT EXISTS dispositivos (
  *     id INTEGER PRIMARY KEY AUTOINCREMENT,
  *     nombre TEXT NOT NULL,
  *     mac TEXT NOT NULL
  * )
  * ```
  *
  * Se ejecuta siempre para garantizar que la tabla está lista,
  * aunque el `IF NOT EXISTS` previene errores si ya existe.
  *
  * @param dbFile [File] apuntando a la BD `dispositivos.db`
  */
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

