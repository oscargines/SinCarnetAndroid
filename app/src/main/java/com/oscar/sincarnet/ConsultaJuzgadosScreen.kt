package com.oscar.sincarnet

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oscar.sincarnet.ui.theme.SinCarnetTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private var isJuzgadosDatabasePrepared = false
private const val LOG_TAG_JUZGADOS = "ConsultaJuzgados"

/** Comunidad autónoma disponible en la BD de juzgados. */
private data class ComunidadAutonoma(
    val id: Int,
    val nombre: String
)

/** Provincia disponible para la CCAA seleccionada. */
private data class Provincia(
    val id: Int,
    val nombre: String
)

/** Municipio judicial disponible para la provincia seleccionada. */
private data class MunicipioJudicial(
    val nombre: String
)

/** Sede judicial con datos de contacto y localización. */
private data class SedeJudicial(
    val id: Int,
    val nombre: String,
    val direccion: String?,
    val telefono: String?,
    val codigoPostal: String?
)

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Pantalla de consulta jerárquica de juzgados (CCAA → provincia → municipio → sede).
 *
 * Carga catálogo desde SQLite y permite visualizar datos de contacto de la sede.
 *
 * @param modifier Modificador raíz
 * @param onBackClick Vuelve a la pantalla anterior
 */
@Composable
fun ConsultaJuzgadosScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    var comunidades by remember { mutableStateOf(emptyList<ComunidadAutonoma>()) }
    var provincias by remember { mutableStateOf(emptyList<Provincia>()) }
    var municipios by remember { mutableStateOf(emptyList<MunicipioJudicial>()) }
    var sedes by remember { mutableStateOf(emptyList<SedeJudicial>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var ccaaExpanded by remember { mutableStateOf(false) }
    var provinciaExpanded by remember { mutableStateOf(false) }
    var municipioExpanded by remember { mutableStateOf(false) }
    var sedeExpanded by remember { mutableStateOf(false) }
    var selectedCcaaId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedCcaaName by rememberSaveable { mutableStateOf("") }
    var selectedProvinciaId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedProvinciaName by rememberSaveable { mutableStateOf("") }
    var selectedMunicipioName by rememberSaveable { mutableStateOf("") }
    var selectedSedeName by rememberSaveable { mutableStateOf("") }
    var selectedSede by remember { mutableStateOf<SedeJudicial?>(null) }

    LaunchedEffect(Unit) {
        if (isInPreview) {
            comunidades = listOf(
                ComunidadAutonoma(1, "Andalucia"),
                ComunidadAutonoma(2, "Asturias"),
                ComunidadAutonoma(3, "Madrid")
            )
            isLoading = false
            errorMessage = null
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null

        val result = runCatching {
            withContext(Dispatchers.IO) { loadCcaaFromDatabase(context) }
        }

        result.onSuccess {
            comunidades = it
            Log.d(
                LOG_TAG_JUZGADOS,
                "CCAA cargadas=${it.size}; ids=${it.joinToString(prefix = "[", postfix = "]") { ccaa -> ccaa.id.toString() }}"
            )
        }.onFailure {
            Log.e(LOG_TAG_JUZGADOS, "Error cargando CCAA", it)
            errorMessage = it.message ?: "Error desconocido"
        }

        isLoading = false
    }

    LaunchedEffect(selectedCcaaId) {
        val ccaaId = selectedCcaaId ?: return@LaunchedEffect
        Log.d(LOG_TAG_JUZGADOS, "Trigger carga provincias para idCCAA=$ccaaId")

        val result = runCatching {
            if (isInPreview) {
                loadPreviewProvincias(ccaaId)
            } else {
                withContext(Dispatchers.IO) { loadProvinciasFromDatabase(context, ccaaId) }
            }
        }

        result.onSuccess {
            provincias = it
            Log.d(
                LOG_TAG_JUZGADOS,
                "Provincias cargadas para idCCAA=$ccaaId -> total=${it.size}; muestras=${it.take(5).joinToString { p -> "${p.id}:${p.nombre}" }}"
            )
            errorMessage = null
        }.onFailure {
            Log.e(LOG_TAG_JUZGADOS, "Error cargando provincias para idCCAA=$ccaaId", it)
            provincias = emptyList()
            municipios = emptyList()
            sedes = emptyList()
            selectedSedeName = ""
            selectedSede = null
            errorMessage = it.message ?: "Error desconocido"
        }
    }

    LaunchedEffect(selectedProvinciaId) {
        val provinciaId = selectedProvinciaId ?: return@LaunchedEffect

        val result = runCatching {
            if (isInPreview) {
                loadPreviewMunicipios(provinciaId)
            } else {
                withContext(Dispatchers.IO) {
                    loadMunicipiosConJuzgadoFromDatabase(context, provinciaId)
                }
            }
        }

        result.onSuccess {
            municipios = it
            errorMessage = null
        }.onFailure {
            municipios = emptyList()
            sedes = emptyList()
            selectedSedeName = ""
            selectedSede = null
            errorMessage = it.message ?: "Error desconocido"
        }
    }

    LaunchedEffect(selectedMunicipioName) {
        val municipio = selectedMunicipioName.takeIf { it.isNotBlank() } ?: return@LaunchedEffect

        val result = runCatching {
            if (isInPreview) {
                loadPreviewSedes(municipio)
            } else {
                withContext(Dispatchers.IO) { loadSedesFromDatabase(context, municipio) }
            }
        }

        result.onSuccess {
            sedes = it
            errorMessage = null
        }.onFailure {
            sedes = emptyList()
            selectedSedeName = ""
            selectedSede = null
            errorMessage = it.message ?: "Error desconocido"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.courts_screen_title),
                    style = MaterialTheme.typography.titleMedium
                )

                when {
                    isLoading -> {
                        Text(text = stringResource(R.string.courts_loading))
                    }

                    errorMessage != null -> {
                        Text(
                            text = stringResource(R.string.courts_load_error, errorMessage.orEmpty()),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    comunidades.isEmpty() -> {
                        Text(text = stringResource(R.string.courts_empty))
                    }

                    else -> {
                        ExposedDropdownMenuBox(
                            expanded = ccaaExpanded,
                            onExpandedChange = { ccaaExpanded = !ccaaExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedCcaaName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(text = stringResource(R.string.courts_combo_label)) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = ccaaExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )

                            DropdownMenu(
                                expanded = ccaaExpanded,
                                onDismissRequest = { ccaaExpanded = false }
                            ) {
                                comunidades.forEach { ccaa ->
                                    DropdownMenuItem(
                                        text = { Text(text = ccaa.nombre) },
                                        onClick = {
                                            Log.d(
                                                LOG_TAG_JUZGADOS,
                                                "CCAA seleccionada id=${ccaa.id}, nombre=${ccaa.nombre}. Reseteando provincias/municipios/sedes"
                                            )
                                            selectedCcaaId = ccaa.id
                                            selectedCcaaName = ccaa.nombre
                                            selectedProvinciaId = null
                                            selectedProvinciaName = ""
                                            selectedMunicipioName = ""
                                            selectedSedeName = ""
                                            provincias = emptyList()
                                            municipios = emptyList()
                                            sedes = emptyList()
                                            selectedSede = null
                                            ccaaExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (selectedCcaaId != null) {
                            if (provincias.isEmpty()) {
                                Text(text = stringResource(R.string.courts_provinces_empty))
                            } else {
                                ExposedDropdownMenuBox(
                                    expanded = provinciaExpanded,
                                    onExpandedChange = { provinciaExpanded = !provinciaExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = selectedProvinciaName,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(text = stringResource(R.string.courts_province_label)) },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinciaExpanded)
                                        },
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth()
                                    )

                                    DropdownMenu(
                                        expanded = provinciaExpanded,
                                        onDismissRequest = { provinciaExpanded = false }
                                    ) {
                                        provincias.forEach { provincia ->
                                            DropdownMenuItem(
                                                text = { Text(text = provincia.nombre) },
                                                onClick = {
                                                    selectedProvinciaId = provincia.id
                                                    selectedProvinciaName = provincia.nombre
                                                    selectedMunicipioName = ""
                                                    selectedSedeName = ""
                                                    municipios = emptyList()
                                                    sedes = emptyList()
                                                    selectedSede = null
                                                    provinciaExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (selectedProvinciaId != null) {
                            if (municipios.isEmpty()) {
                                Text(text = stringResource(R.string.courts_municipalities_empty))
                            } else {
                                ExposedDropdownMenuBox(
                                    expanded = municipioExpanded,
                                    onExpandedChange = { municipioExpanded = !municipioExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = selectedMunicipioName,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(text = stringResource(R.string.courts_municipality_label)) },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = municipioExpanded)
                                        },
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth()
                                    )

                                    DropdownMenu(
                                        expanded = municipioExpanded,
                                        onDismissRequest = { municipioExpanded = false }
                                    ) {
                                        municipios.forEach { municipio ->
                                            DropdownMenuItem(
                                                text = { Text(text = municipio.nombre) },
                                                onClick = {
                                                    selectedMunicipioName = municipio.nombre
                                                    selectedSedeName = ""
                                                    sedes = emptyList()
                                                    selectedSede = null
                                                    municipioExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (selectedMunicipioName.isNotBlank()) {
                            if (sedes.isEmpty()) {
                                Text(text = stringResource(R.string.courts_sedes_empty))
                            } else {
                                ExposedDropdownMenuBox(
                                    expanded = sedeExpanded,
                                    onExpandedChange = { sedeExpanded = !sedeExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = selectedSedeName,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(text = stringResource(R.string.courts_sede_label)) },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = sedeExpanded)
                                        },
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth()
                                    )

                                    DropdownMenu(
                                        expanded = sedeExpanded,
                                        onDismissRequest = { sedeExpanded = false }
                                    ) {
                                        sedes.forEach { sede ->
                                            DropdownMenuItem(
                                                text = { Text(text = sede.nombre) },
                                                onClick = {
                                                    selectedSedeName = sede.nombre
                                                    selectedSede = sede
                                                    sedeExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        selectedSede?.let { sede ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.courts_selected_details_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(text = stringResource(R.string.courts_detail_id, sede.id.toString()))
                    Text(
                        text = stringResource(
                            R.string.courts_detail_name,
                            sede.nombre.ifBlank { stringResource(R.string.courts_not_available) }
                        )
                    )
                    Text(
                        text = stringResource(
                            R.string.courts_detail_address,
                            sede.direccion?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.courts_not_available)
                        )
                    )
                    Text(
                        text = stringResource(
                            R.string.courts_detail_phone,
                            sede.telefono?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.courts_not_available)
                        )
                    )
                    Text(
                        text = stringResource(
                            R.string.courts_detail_postal_code,
                            sede.codigoPostal?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.courts_not_available)
                        )
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackIconButton(onClick = onBackClick)
        }
    }
}

private fun loadCcaaFromDatabase(context: Context): List<ComunidadAutonoma> {
    val dbFile = ensureDatabaseAvailable(context)
    Log.d(LOG_TAG_JUZGADOS, "Leyendo CCAA desde DB=${dbFile.absolutePath}")

    val ccaa = mutableListOf<ComunidadAutonoma>()
    SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
        db.rawQuery("SELECT idCCAA, Nombre FROM CCAA ORDER BY idCCAA ASC", null).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow("idCCAA")
            val nameIndex = cursor.getColumnIndexOrThrow("Nombre")
            while (cursor.moveToNext()) {
                ccaa.add(
                    ComunidadAutonoma(
                        id = cursor.getInt(idIndex),
                        nombre = cursor.getString(nameIndex)
                    )
                )
            }
        }
    }
    return ccaa
}

private fun loadProvinciasFromDatabase(context: Context, ccaaId: Int): List<Provincia> {
    val dbFile = ensureDatabaseAvailable(context)
    Log.d(LOG_TAG_JUZGADOS, "Consulta provincias con idCCAA=$ccaaId en DB=${dbFile.absolutePath}")
    val provincias = mutableListOf<Provincia>()

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
                provincias.add(
                    Provincia(
                        id = cursor.getInt(idIndex),
                        nombre = cursor.getString(nameIndex)
                    )
                )
            }
        }
    }

    Log.d(
        LOG_TAG_JUZGADOS,
        "Resultado SQL provincias idCCAA=$ccaaId -> total=${provincias.size}; muestras=${provincias.take(5).joinToString { p -> "${p.id}:${p.nombre}" }}"
    )

    return provincias
}

private fun loadMunicipiosConJuzgadoFromDatabase(
    context: Context,
    provinciaId: Int
): List<MunicipioJudicial> {
    val dbFile = ensureDatabaseAvailable(context)
    val municipios = mutableListOf<MunicipioJudicial>()

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
                municipios.add(MunicipioJudicial(nombre = cursor.getString(nameIndex)))
            }
        }
    }

    return municipios
}

private fun loadSedesFromDatabase(
    context: Context,
    municipio: String
): List<SedeJudicial> {
    val dbFile = ensureDatabaseAvailable(context)
    val sedes = mutableListOf<SedeJudicial>()

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
                sedes.add(
                    SedeJudicial(
                        id = cursor.getInt(idIndex),
                        nombre = cursor.getString(nameIndex),
                        direccion = cursor.getString(addressIndex),
                        telefono = cursor.getString(phoneIndex),
                        codigoPostal = cursor.getString(postalCodeIndex)
                    )
                )
            }
        }
    }

    return sedes
}

private fun loadPreviewProvincias(ccaaId: Int): List<Provincia> = when (ccaaId) {
    1 -> listOf(
        Provincia(1, "Almeria"),
        Provincia(2, "Cadiz"),
        Provincia(3, "Sevilla")
    )

    2 -> listOf(
        Provincia(4, "Asturias")
    )

    3 -> listOf(
        Provincia(5, "Madrid")
    )

    else -> emptyList()
}

private fun loadPreviewMunicipios(provinciaId: Int): List<MunicipioJudicial> = when (provinciaId) {
    1 -> listOf(
        MunicipioJudicial("Almeria"),
        MunicipioJudicial("El Ejido"),
        MunicipioJudicial("Roquetas de Mar")
    )

    2 -> listOf(
        MunicipioJudicial("Algeciras"),
        MunicipioJudicial("Cadiz"),
        MunicipioJudicial("Jerez de la Frontera")
    )

    3 -> listOf(
        MunicipioJudicial("Sevilla")
    )

    4 -> listOf(
        MunicipioJudicial("Cangas de Onis"),
        MunicipioJudicial("Gijon"),
        MunicipioJudicial("Oviedo")
    )

    5 -> listOf(
        MunicipioJudicial("Alcala de Henares"),
        MunicipioJudicial("Madrid")
    )

    else -> emptyList()
}

private fun loadPreviewSedes(municipio: String): List<SedeJudicial> = when (municipio) {
    "Oviedo" -> listOf(
        SedeJudicial(
            id = 1,
            nombre = "Juzgado de Instrucción nº 1 de Oviedo",
            direccion = "Calle Comandante Caballero, 3",
            telefono = "985000111",
            codigoPostal = "33005"
        ),
        SedeJudicial(
            id = 2,
            nombre = "Juzgado de lo Penal nº 2 de Oviedo",
            direccion = "Calle Concepción Arenal, 1",
            telefono = "985000222",
            codigoPostal = "33005"
        )
    )

    "Madrid" -> listOf(
        SedeJudicial(
            id = 3,
            nombre = "Juzgado de Guardia de Madrid",
            direccion = "Plaza de Castilla, s/n",
            telefono = "914000333",
            codigoPostal = "28046"
        )
    )

    "Almeria" -> listOf(
        SedeJudicial(
            id = 4,
            nombre = "Juzgado de Primera Instancia nº 1 de Almeria",
            direccion = "Carretera de Ronda, 120",
            telefono = "950000444",
            codigoPostal = "04005"
        )
    )

    else -> emptyList()
}

private fun ensureDatabaseAvailable(context: Context): File {
    val databaseName = "juzgados.db"
    val primaryAssetPath = "databases/juzgados.db"
    val dbFile = context.getDatabasePath(databaseName)

    if (isJuzgadosDatabasePrepared && dbFile.exists()) return dbFile

    dbFile.parentFile?.mkdirs()

    val inputStream = runCatching { context.assets.open(primaryAssetPath) }
        .getOrElse { context.assets.open(databaseName) }

    inputStream.use { input ->
        dbFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }

    // Normalize bad idCCAA mappings found in some legacy datasets.
    repairCcaaMappingIfNeeded(dbFile)

    isJuzgadosDatabasePrepared = true

    return dbFile
}

private fun repairCcaaMappingIfNeeded(dbFile: File) {
    SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
        val alreadyCorrect = db.rawQuery(
            "SELECT COUNT(*) FROM PROVINCIAS WHERE idCCAA = 5 AND idProvincia IN (2,13,16,19,45)",
            null
        ).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0) == 5
        }

        if (alreadyCorrect) {
            Log.d(LOG_TAG_JUZGADOS, "Mapa idCCAA/provincias ya correcto. No se aplica reparación.")
            return
        }

        Log.w(LOG_TAG_JUZGADOS, "Detectado mapa idCCAA desalineado. Aplicando reparación de PROVINCIAS...")
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
            Log.i(LOG_TAG_JUZGADOS, "Reparación de idCCAA en PROVINCIAS aplicada correctamente.")
        } finally {
            db.endTransaction()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConsultaJuzgadosScreenPreview() {
    SinCarnetTheme {
        ConsultaJuzgadosScreen()
    }
}

