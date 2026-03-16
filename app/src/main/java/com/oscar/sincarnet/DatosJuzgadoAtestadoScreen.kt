package com.oscar.sincarnet

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val JUZGADO_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
private const val TIPO_JUICIO_RAPIDO = "rapido"
private const val TIPO_JUICIO_ORDINARIO = "ordinario"
private const val TIPO_JUICIO_ABREVIADO = "abreviado"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatosJuzgadoAtestadoScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onPrintClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val storage = remember(context) { JuzgadoAtestadoStorage(context) }
    val initialData = remember(isInPreview) {
        if (isInPreview) {
            JuzgadoAtestadoData(
                ccaaId = 2,
                ccaaNombre = "Asturias",
                provinciaId = 4,
                provinciaNombre = "Asturias",
                municipioNombre = "Oviedo",
                sedeId = 1,
                sedeNombre = "Juzgado de Instrucción nº 1 de Oviedo",
                sedeDireccion = "Calle Comandante Caballero, 3",
                sedeTelefono = "985000111",
                sedeCodigoPostal = "33005",
                tipoJuicio = TIPO_JUICIO_RAPIDO,
                fechaJuicioRapido = "16-03-2026",
                horaJuicioRapido = "10:30"
            )
        } else {
            storage.loadCurrent()
        }
    }

    var comunidades by remember { mutableStateOf(emptyList<JuzgadoComunidadAutonoma>()) }
    var provincias by remember { mutableStateOf(emptyList<JuzgadoProvincia>()) }
    var municipios by remember { mutableStateOf(emptyList<JuzgadoMunicipio>()) }
    var sedes by remember { mutableStateOf(emptyList<JuzgadoSede>()) }
    var selectedSede by remember { mutableStateOf<JuzgadoSede?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var ccaaExpanded by remember { mutableStateOf(false) }
    var provinciaExpanded by remember { mutableStateOf(false) }
    var municipioExpanded by remember { mutableStateOf(false) }
    var sedeExpanded by remember { mutableStateOf(false) }

    var selectedCcaaId by rememberSaveable { mutableStateOf(initialData.ccaaId) }
    var selectedCcaaName by rememberSaveable { mutableStateOf(initialData.ccaaNombre) }
    var selectedProvinciaId by rememberSaveable { mutableStateOf(initialData.provinciaId) }
    var selectedProvinciaName by rememberSaveable { mutableStateOf(initialData.provinciaNombre) }
    var selectedMunicipioName by rememberSaveable { mutableStateOf(initialData.municipioNombre) }
    var selectedSedeId by rememberSaveable { mutableStateOf(initialData.sedeId) }
    var selectedSedeName by rememberSaveable { mutableStateOf(initialData.sedeNombre) }
    var selectedSedeDireccion by rememberSaveable { mutableStateOf(initialData.sedeDireccion) }
    var selectedSedeTelefono by rememberSaveable { mutableStateOf(initialData.sedeTelefono) }
    var selectedSedeCodigoPostal by rememberSaveable { mutableStateOf(initialData.sedeCodigoPostal) }
    var trialType by rememberSaveable { mutableStateOf(initialData.tipoJuicio) }
    var quickTrialDate by rememberSaveable { mutableStateOf(initialData.fechaJuicioRapido) }
    var quickTrialTime by rememberSaveable { mutableStateOf(initialData.horaJuicioRapido) }

    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var showSaveConfirmation by rememberSaveable { mutableStateOf(false) }
    var showRequiredFieldsError by rememberSaveable { mutableStateOf(false) }
    var requiredFieldsErrorText by rememberSaveable { mutableStateOf("") }
    var showQuickTrialDatePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (isInPreview) {
            comunidades = loadPreviewJuzgadoCcaa()
            isLoading = false
            errorMessage = null
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null
        val result = runCatching {
            withContext(Dispatchers.IO) { loadJuzgadoCcaaFromDatabase(context) }
        }
        result.onSuccess {
            comunidades = it
            errorMessage = null
        }.onFailure {
            comunidades = emptyList()
            errorMessage = it.message ?: context.getString(R.string.courts_load_unknown_error)
        }
        isLoading = false
    }

    LaunchedEffect(selectedCcaaId) {
        val ccaaId = selectedCcaaId
        if (ccaaId == null) {
            provincias = emptyList()
            municipios = emptyList()
            sedes = emptyList()
            selectedSede = null
            return@LaunchedEffect
        }

        val result = runCatching {
            if (isInPreview) {
                loadPreviewJuzgadoProvincias(ccaaId)
            } else {
                withContext(Dispatchers.IO) { loadJuzgadoProvinciasFromDatabase(context, ccaaId) }
            }
        }

        result.onSuccess {
            provincias = it
            errorMessage = null
            if (selectedProvinciaId != null) {
                val provinciaMatch = it.firstOrNull { provincia -> provincia.id == selectedProvinciaId }
                if (provinciaMatch == null) {
                    selectedProvinciaId = null
                    selectedProvinciaName = ""
                    selectedMunicipioName = ""
                    selectedSedeId = null
                    selectedSedeName = ""
                    sedes = emptyList()
                    selectedSede = null
                } else {
                    selectedProvinciaName = provinciaMatch.nombre
                }
            }
        }.onFailure {
            provincias = emptyList()
            municipios = emptyList()
            sedes = emptyList()
            selectedSede = null
            errorMessage = it.message ?: context.getString(R.string.courts_load_unknown_error)
        }
    }

    LaunchedEffect(selectedProvinciaId) {
        val provinciaId = selectedProvinciaId
        if (provinciaId == null) {
            municipios = emptyList()
            sedes = emptyList()
            selectedSede = null
            return@LaunchedEffect
        }

        val result = runCatching {
            if (isInPreview) {
                loadPreviewJuzgadoMunicipios(provinciaId)
            } else {
                withContext(Dispatchers.IO) { loadJuzgadoMunicipiosFromDatabase(context, provinciaId) }
            }
        }

        result.onSuccess {
            municipios = it
            errorMessage = null
            if (selectedMunicipioName.isNotBlank()) {
                val exists = it.any { municipio -> municipio.nombre == selectedMunicipioName }
                if (!exists) {
                    selectedMunicipioName = ""
                    selectedSedeId = null
                    selectedSedeName = ""
                    selectedSede = null
                    sedes = emptyList()
                }
            }
        }.onFailure {
            municipios = emptyList()
            sedes = emptyList()
            selectedSede = null
            errorMessage = it.message ?: context.getString(R.string.courts_load_unknown_error)
        }
    }

    LaunchedEffect(selectedMunicipioName) {
        val municipio = selectedMunicipioName.takeIf { it.isNotBlank() } ?: run {
            sedes = emptyList()
            selectedSede = null
            return@LaunchedEffect
        }

        val result = runCatching {
            if (isInPreview) {
                loadPreviewJuzgadoSedes(municipio)
            } else {
                withContext(Dispatchers.IO) { loadJuzgadoSedesFromDatabase(context, municipio) }
            }
        }

        result.onSuccess {
            sedes = it
            errorMessage = null
            selectedSede = it.firstOrNull { sede -> sede.id == selectedSedeId }
            if (selectedSede == null) {
                selectedSedeId = null
                selectedSedeName = ""
                selectedSedeDireccion = ""
                selectedSedeTelefono = ""
                selectedSedeCodigoPostal = ""
            } else {
                selectedSedeName = selectedSede?.nombre.orEmpty()
                selectedSedeDireccion = selectedSede?.direccion.orEmpty()
                selectedSedeTelefono = selectedSede?.telefono.orEmpty()
                selectedSedeCodigoPostal = selectedSede?.codigoPostal.orEmpty()
            }
        }.onFailure {
            sedes = emptyList()
            selectedSede = null
            errorMessage = it.message ?: context.getString(R.string.courts_load_unknown_error)
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
                    text = stringResource(R.string.atestado_court_title),
                    style = MaterialTheme.typography.titleMedium
                )

                when {
                    isLoading -> Text(text = stringResource(R.string.courts_loading))
                    errorMessage != null -> {
                        Text(
                            text = stringResource(R.string.courts_load_error, errorMessage.orEmpty()),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    comunidades.isEmpty() -> Text(text = stringResource(R.string.courts_empty))
                    else -> {
                        CourtDropdownField(
                            label = stringResource(R.string.courts_combo_label),
                            value = selectedCcaaName,
                            options = comunidades.map { it.nombre },
                            expanded = ccaaExpanded,
                            onExpandedChange = { ccaaExpanded = !ccaaExpanded },
                            onDismissRequest = { ccaaExpanded = false }
                        ) { optionName ->
                            val selected = comunidades.firstOrNull { it.nombre == optionName } ?: return@CourtDropdownField
                            selectedCcaaId = selected.id
                            selectedCcaaName = selected.nombre
                            selectedProvinciaId = null
                            selectedProvinciaName = ""
                            selectedMunicipioName = ""
                            selectedSedeId = null
                            selectedSedeName = ""
                            selectedSedeDireccion = ""
                            selectedSedeTelefono = ""
                            selectedSedeCodigoPostal = ""
                            selectedSede = null
                            provincias = emptyList()
                            municipios = emptyList()
                            sedes = emptyList()
                            ccaaExpanded = false
                        }

                        if (selectedCcaaId != null) {
                            if (provincias.isEmpty()) {
                                Text(text = stringResource(R.string.courts_provinces_empty))
                            } else {
                                CourtDropdownField(
                                    label = stringResource(R.string.courts_province_label),
                                    value = selectedProvinciaName,
                                    options = provincias.map { it.nombre },
                                    expanded = provinciaExpanded,
                                    onExpandedChange = { provinciaExpanded = !provinciaExpanded },
                                    onDismissRequest = { provinciaExpanded = false }
                                ) { optionName ->
                                    val selected = provincias.firstOrNull { it.nombre == optionName } ?: return@CourtDropdownField
                                    selectedProvinciaId = selected.id
                                    selectedProvinciaName = selected.nombre
                                    selectedMunicipioName = ""
                                    selectedSedeId = null
                                    selectedSedeName = ""
                                    selectedSedeDireccion = ""
                                    selectedSedeTelefono = ""
                                    selectedSedeCodigoPostal = ""
                                    selectedSede = null
                                    municipios = emptyList()
                                    sedes = emptyList()
                                    provinciaExpanded = false
                                }
                            }
                        }

                        if (selectedProvinciaId != null) {
                            if (municipios.isEmpty()) {
                                Text(text = stringResource(R.string.courts_municipalities_empty))
                            } else {
                                CourtDropdownField(
                                    label = stringResource(R.string.courts_municipality_label),
                                    value = selectedMunicipioName,
                                    options = municipios.map { it.nombre },
                                    expanded = municipioExpanded,
                                    onExpandedChange = { municipioExpanded = !municipioExpanded },
                                    onDismissRequest = { municipioExpanded = false }
                                ) { optionName ->
                                    selectedMunicipioName = optionName
                                    selectedSedeId = null
                                    selectedSedeName = ""
                                    selectedSedeDireccion = ""
                                    selectedSedeTelefono = ""
                                    selectedSedeCodigoPostal = ""
                                    selectedSede = null
                                    sedes = emptyList()
                                    municipioExpanded = false
                                }
                            }
                        }

                        if (selectedMunicipioName.isNotBlank()) {
                            if (sedes.isEmpty()) {
                                Text(text = stringResource(R.string.courts_sedes_empty))
                            } else {
                                CourtDropdownField(
                                    label = stringResource(R.string.courts_sede_label),
                                    value = selectedSedeName,
                                    options = sedes.map { it.nombre },
                                    expanded = sedeExpanded,
                                    onExpandedChange = { sedeExpanded = !sedeExpanded },
                                    onDismissRequest = { sedeExpanded = false }
                                ) { optionName ->
                                    val selected = sedes.firstOrNull { it.nombre == optionName } ?: return@CourtDropdownField
                                    selectedSedeId = selected.id
                                    selectedSedeName = selected.nombre
                                    selectedSedeDireccion = selected.direccion.orEmpty()
                                    selectedSedeTelefono = selected.telefono.orEmpty()
                                    selectedSedeCodigoPostal = selected.codigoPostal.orEmpty()
                                    selectedSede = selected
                                    sedeExpanded = false
                                }
                            }
                        }
                    }
                }

                selectedSede?.let { sede ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.atestado_court_trial_section_title),
                            style = MaterialTheme.typography.titleMedium
                        )

                        OptionRadioRow(
                            text = stringResource(R.string.atestado_court_trial_rapid),
                            selected = trialType == TIPO_JUICIO_RAPIDO,
                            onSelect = { trialType = TIPO_JUICIO_RAPIDO }
                        )
                        OptionRadioRow(
                            text = stringResource(R.string.atestado_court_trial_ordinary),
                            selected = trialType == TIPO_JUICIO_ORDINARIO,
                            onSelect = {
                                trialType = TIPO_JUICIO_ORDINARIO
                                quickTrialDate = ""
                                quickTrialTime = ""
                            }
                        )
                        OptionRadioRow(
                            text = stringResource(R.string.atestado_court_trial_abbreviated),
                            selected = trialType == TIPO_JUICIO_ABREVIADO,
                            onSelect = {
                                trialType = TIPO_JUICIO_ABREVIADO
                                quickTrialDate = ""
                                quickTrialTime = ""
                            }
                        )

                        if (trialType == TIPO_JUICIO_RAPIDO) {
                            CourtDateRow(
                                label = stringResource(R.string.atestado_court_trial_date),
                                value = quickTrialDate,
                                onPickerClick = { showQuickTrialDatePicker = true }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                OutlinedTextField(
                                    value = quickTrialTime,
                                    onValueChange = {},
                                    modifier = Modifier.weight(1f),
                                    readOnly = true,
                                    singleLine = true,
                                    label = { Text(stringResource(R.string.atestado_court_trial_time)) }
                                )

                                IconButton(
                                    onClick = {
                                        val initialHour = quickTrialTime.split(":").getOrNull(0)?.toIntOrNull() ?: 9
                                        val initialMinute = quickTrialTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0
                                        TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                quickTrialTime = String.format("%02d:%02d", hour, minute)
                                            },
                                            initialHour,
                                            initialMinute,
                                            true
                                        ).show()
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .padding(top = 8.dp)
                                        .align(Alignment.Top)
                                ) {
                                    AssetImage(
                                        assetPath = "icons/clock.png",
                                        contentDescription = stringResource(R.string.atestado_court_select_time_action),
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val missing = mutableListOf<String>()
                    if (selectedCcaaId == null) missing += context.getString(R.string.courts_combo_label)
                    if (selectedProvinciaId == null) missing += context.getString(R.string.courts_province_label)
                    if (selectedMunicipioName.isBlank()) missing += context.getString(R.string.courts_municipality_label)
                    if (selectedSedeId == null) missing += context.getString(R.string.courts_sede_label)
                    if (trialType.isBlank()) missing += context.getString(R.string.atestado_court_trial_section_title)
                    if (trialType == TIPO_JUICIO_RAPIDO && quickTrialDate.isBlank()) {
                        missing += context.getString(R.string.atestado_court_trial_date)
                    }
                    if (trialType == TIPO_JUICIO_RAPIDO && quickTrialTime.isBlank()) {
                        missing += context.getString(R.string.atestado_court_trial_time)
                    }

                    if (missing.isNotEmpty()) {
                        requiredFieldsErrorText = missing.joinToString(", ")
                        showRequiredFieldsError = true
                    } else {
                        storage.saveCurrent(
                            JuzgadoAtestadoData(
                                ccaaId = selectedCcaaId,
                                ccaaNombre = selectedCcaaName,
                                provinciaId = selectedProvinciaId,
                                provinciaNombre = selectedProvinciaName,
                                municipioNombre = selectedMunicipioName,
                                sedeId = selectedSedeId,
                                sedeNombre = selectedSedeName,
                                sedeDireccion = selectedSedeDireccion,
                                sedeTelefono = selectedSedeTelefono,
                                sedeCodigoPostal = selectedSedeCodigoPostal,
                                tipoJuicio = trialType,
                                fechaJuicioRapido = quickTrialDate,
                                horaJuicioRapido = quickTrialTime
                            )
                        )
                        showSaveConfirmation = true
                    }
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = stringResource(R.string.atestado_court_save))
            }

            Button(
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(text = stringResource(R.string.atestado_court_delete))
            }
        }

        Button(
            onClick = onPrintClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Text(text = stringResource(R.string.atestado_court_print_summons))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrintClick,
                modifier = Modifier.size(44.dp)
            ) {
                AssetImage(
                    assetPath = "icons/impresora.png",
                    contentDescription = stringResource(R.string.print_icon_content_description),
                    modifier = Modifier.size(30.dp)
                )
            }
            BackIconButton(onClick = onBackClick)
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(text = stringResource(R.string.atestado_court_delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.atestado_court_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        storage.clearCurrent()
                        selectedCcaaId = null
                        selectedCcaaName = ""
                        selectedProvinciaId = null
                        selectedProvinciaName = ""
                        selectedMunicipioName = ""
                        selectedSedeId = null
                        selectedSedeName = ""
                        selectedSedeDireccion = ""
                        selectedSedeTelefono = ""
                        selectedSedeCodigoPostal = ""
                        selectedSede = null
                        trialType = ""
                        quickTrialDate = ""
                        quickTrialTime = ""
                        provincias = emptyList()
                        municipios = emptyList()
                        sedes = emptyList()
                    }
                ) {
                    Text(text = stringResource(R.string.atestado_court_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(text = stringResource(R.string.no_option))
                }
            }
        )
    }

    if (showSaveConfirmation) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmation = false },
            title = { Text(text = stringResource(R.string.atestado_court_save_confirm_title)) },
            text = { Text(text = stringResource(R.string.atestado_court_save_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { showSaveConfirmation = false }) {
                    Text(text = stringResource(R.string.accept_action))
                }
            }
        )
    }

    if (showRequiredFieldsError) {
        AlertDialog(
            onDismissRequest = { showRequiredFieldsError = false },
            title = { Text(text = stringResource(R.string.atestado_court_required_fields_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.atestado_court_required_fields_message,
                        requiredFieldsErrorText
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { showRequiredFieldsError = false }) {
                    Text(text = stringResource(R.string.accept_action))
                }
            }
        )
    }

    if (showQuickTrialDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = quickTrialDate.toJuzgadoDateMillisOrNull()
        )

        DatePickerDialog(
            onDismissRequest = { showQuickTrialDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis
                            ?.toFormattedJuzgadoDate()
                            ?.let { quickTrialDate = it }
                        showQuickTrialDatePicker = false
                    }
                ) {
                    Text(text = stringResource(R.string.person_data_birth_date_select_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickTrialDatePicker = false }) {
                    Text(text = stringResource(R.string.no_option))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourtDropdownField(
    label: String,
    value: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    onDismissRequest: () -> Unit,
    onOptionSelected: (String) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { onExpandedChange() }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option) },
                    onClick = { onOptionSelected(option) }
                )
            }
        }
    }
}

@Composable
private fun CourtDateRow(
    label: String,
    value: String,
    onPickerClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.weight(1f),
            readOnly = true,
            singleLine = true,
            label = { Text(label) }
        )

        IconButton(
            onClick = onPickerClick,
            modifier = Modifier
                .size(48.dp)
                .padding(top = 8.dp)
                .align(Alignment.Top)
        ) {
            AssetImage(
                assetPath = "icons/calendar.png",
                contentDescription = stringResource(R.string.person_data_birth_date_select_action),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun Long.toFormattedJuzgadoDate(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(JUZGADO_DATE_FORMATTER)

private fun String.toJuzgadoDateMillisOrNull(): Long? = runCatching {
    LocalDate.parse(this, JUZGADO_DATE_FORMATTER)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}.getOrNull()

@Preview(showBackground = true)
@Composable
private fun DatosJuzgadoAtestadoScreenPreview() {
    SinCarnetTheme {
        DatosJuzgadoAtestadoScreen()
    }
}

