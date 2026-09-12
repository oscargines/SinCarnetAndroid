package com.oscar.sincarnet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oscar.sincarnet.data.pdf.buildSealUnitText
import com.oscar.sincarnet.data.pdf.getInstitutionalSealBitmap
import com.oscar.sincarnet.data.pdf.isSealTextTooLong
import com.oscar.sincarnet.data.repository.ActuantesStorage
import com.oscar.sincarnet.data.repository.AtestadoInicioStorage
import com.oscar.sincarnet.data.repository.OcurrenciaDelitStorage
import com.oscar.sincarnet.data.repository.SealUnitStorage
import com.oscar.sincarnet.data.toStorage
import com.oscar.sincarnet.presentation.R

private val provinciasEspana = listOf(
    "Albacete", "Alicante", "Almería", "Asturias", "Ávila", "Badajoz", "Barcelona",
    "Bizkaia", "Burgos", "Cáceres", "Cádiz", "Cantabria", "Castellón", "Ceuta",
    "Ciudad Real", "Córdoba", "Cuenca", "Gipuzkoa", "Girona", "Granada", "Guadalajara",
    "Huelva", "Huesca", "Illes Balears", "Jaén", "La Rioja", "Las Palmas", "León",
    "Lleida", "Lugo", "Madrid", "Málaga", "Melilla", "Murcia", "Navarra", "Ourense",
    "Palencia", "Pontevedra", "Salamanca", "Santa Cruz de Tenerife", "Segovia", "Sevilla",
    "Soria", "Tarragona", "Teruel", "Toledo", "Valencia", "Valladolid", "Zamora", "Zaragoza"
)

@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GenerateCompleteAtestadoScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onDataConfirmed: (jefaturaProvincial: String, numeroBoletin: String) -> Unit = { _, _ -> },
    onAntecedentesChange: (
        tieneAntecedentes: Boolean,
        antecedentesGuardiaCivil: Boolean,
        antecedentesSenalamientosNacionales: Boolean,
        antecedentesDgt: Boolean,
        antecedentesOtrosCuerpos: Boolean,
        requisitoriasJudiciales: Boolean
    ) -> Unit = { _, _, _, _, _, _ -> },
    completeAtestadoPath: String = "",
    isGeneratingCompleteAtestado: Boolean = false,
    onSendModeChange: (enviarPorLexnet: Boolean, modoEnvio: String) -> Unit = { _, _ -> },
    onVisualizeClick: () -> Unit = {},
    onGenerateCompleteClick: (sealUnitText: String) -> Unit = {},
    onShareCompleteClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val occurrence = remember(context) {
        OcurrenciaDelitStorage(context.toStorage("ocurrencia_delit_storage")).loadCurrent()
    }
    val saved = remember(context) {
        AtestadoInicioStorage(context.toStorage("atestado_inicio_storage")).loadCurrent()
    }
    val defaultProvince = occurrence.provincia.trim().ifBlank { saved.jefaturaProvincial.trim() }

    val initialSealUnitText = remember(context) {
        val actuantes = ActuantesStorage(context.toStorage("actuantes_storage")).loadCurrent()
        SealUnitStorage(context.toStorage("seal_unit_storage")).loadSealUnitText()
            .ifBlank { buildSealUnitText(actuantes.instructorUnit) }
            .ifBlank { DEFAULT_SEAL_UNIT_TEXT }
    }

    var showInformationDialog by rememberSaveable { mutableStateOf(true) }
    var showDataDialog by rememberSaveable { mutableStateOf(false) }
    var showSealDialog by rememberSaveable { mutableStateOf(false) }
    var sealUnitText by rememberSaveable { mutableStateOf(initialSealUnitText) }
    var province by rememberSaveable { mutableStateOf(defaultProvince) }
    var bulletinNumber by rememberSaveable { mutableStateOf(saved.numeroBoletin) }
    var hasBackground by rememberSaveable { mutableStateOf(saved.tieneAntecedentes) }
    var hasGuardiaCivilBackground by rememberSaveable { mutableStateOf(saved.antecedentesGuardiaCivil) }
    var hasNationalSignalsBackground by rememberSaveable { mutableStateOf(saved.antecedentesSenalamientosNacionales) }
    var hasDgtBackground by rememberSaveable { mutableStateOf(saved.antecedentesDgt) }
    var hasOtherPoliceBackground by rememberSaveable { mutableStateOf(saved.antecedentesOtrosCuerpos) }
    var hasJudicialWarrants by rememberSaveable { mutableStateOf(saved.requisitoriasJudiciales) }
    var sendByLexnet by rememberSaveable { mutableStateOf(saved.enviarPorLexnet) }
    var otherSendMode by rememberSaveable { mutableStateOf(saved.modoEnvio) }
    var provinceExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf(false) }
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }

    fun persistAntecedentes() {
        onAntecedentesChange(
            hasBackground,
            hasGuardiaCivilBackground,
            hasNationalSignalsBackground,
            hasDgtBackground,
            hasOtherPoliceBackground,
            hasJudicialWarrants
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.generate_complete_atestado_title),
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.generate_complete_atestado_province),
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                )
                Text(province.ifBlank { "-" })
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.generate_complete_atestado_bulletin_number),
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                )
                Text(bulletinNumber.ifBlank { "-" })
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.generate_complete_atestado_has_background),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { showHelpDialog = true },
                modifier = Modifier
                    .size(40.dp)
                    .border(2.dp, Color.Red, CircleShape)
            ) {
                Text(
                    "?",
                    color = Color.Red,
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = hasBackground,
                onCheckedChange = { value ->
                    hasBackground = value
                    if (!value) {
                        hasGuardiaCivilBackground = false
                        hasNationalSignalsBackground = false
                        hasDgtBackground = false
                        hasOtherPoliceBackground = false
                        hasJudicialWarrants = false
                    }
                    persistAntecedentes()
                }
            )
        }

        if (hasBackground) {
            BackgroundSwitch(
                text = stringResource(R.string.generate_complete_atestado_background_guardia_civil),
                checked = hasGuardiaCivilBackground,
                onCheckedChange = {
                    hasGuardiaCivilBackground = it
                    persistAntecedentes()
                }
            )
            BackgroundSwitch(
                text = stringResource(R.string.generate_complete_atestado_background_signals),
                checked = hasNationalSignalsBackground,
                onCheckedChange = {
                    hasNationalSignalsBackground = it
                    persistAntecedentes()
                }
            )
            BackgroundSwitch(
                text = stringResource(R.string.generate_complete_atestado_background_dgt),
                checked = hasDgtBackground,
                onCheckedChange = {
                    hasDgtBackground = it
                    persistAntecedentes()
                }
            )
            BackgroundSwitch(
                text = stringResource(R.string.generate_complete_atestado_background_other_police),
                checked = hasOtherPoliceBackground,
                onCheckedChange = {
                    hasOtherPoliceBackground = it
                    persistAntecedentes()
                }
            )
            BackgroundSwitch(
                text = stringResource(R.string.generate_complete_atestado_background_judicial_warrants),
                checked = hasJudicialWarrants,
                onCheckedChange = {
                    hasJudicialWarrants = it
                    persistAntecedentes()
                }
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.generate_complete_atestado_send_section),
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.generate_complete_atestado_send_by_lexnet),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = sendByLexnet,
                        onCheckedChange = { value ->
                            sendByLexnet = value
                            onSendModeChange(value, otherSendMode)
                        }
                    )
                }
                if (!sendByLexnet) {
                    OutlinedTextField(
                        value = otherSendMode,
                        onValueChange = { value ->
                            otherSendMode = value
                            onSendModeChange(false, value)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.generate_complete_atestado_other_send_mode)) }
                    )
                }
            }
        }

        Button(
            onClick = { showDataDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.material3.MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF40407A),
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text(stringResource(R.string.generate_complete_atestado_edit_data))
        }

        Button(
            onClick = onVisualizeClick,
            enabled = completeAtestadoPath.isNotBlank() && !isGeneratingCompleteAtestado,
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.material3.MaterialTheme.shapes.medium,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Color(0xFF40407A),
                contentColor = Color.White
            ),
            elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text(stringResource(R.string.generate_complete_atestado_visualize))
        }
        Button(
            onClick = { showSealDialog = true },
            enabled = !isGeneratingCompleteAtestado,
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.material3.MaterialTheme.shapes.medium,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Color(0xFF40407A),
                contentColor = Color.White
            ),
            elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text(
                stringResource(
                    if (isGeneratingCompleteAtestado) {
                        R.string.generate_complete_atestado_generating
                    } else {
                        R.string.generate_complete_atestado_generate
                    }
                )
            )
        }
        Button(
            onClick = onShareCompleteClick,
            enabled = completeAtestadoPath.isNotBlank() && !isGeneratingCompleteAtestado,
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.material3.MaterialTheme.shapes.medium,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Color(0xFF40407A),
                contentColor = Color.White
            ),
            elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text(stringResource(R.string.generate_complete_atestado_share))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            BackIconButton(onClick = onBackClick)
        }
    }

    if (showInformationDialog) {
        AlertDialog(
            onDismissRequest = { showInformationDialog = false },
            title = { Text(stringResource(R.string.generate_complete_atestado_title)) },
            text = { Text(stringResource(R.string.generate_complete_atestado_information)) },
            confirmButton = {
                TextButton(onClick = {
                    showInformationDialog = false
                    showDataDialog = true
                }) {
                    Text(stringResource(R.string.continue_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showInformationDialog = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            }
        )
    }

    if (showDataDialog) {
        AlertDialog(
            onDismissRequest = { showDataDialog = false },
            title = { Text(stringResource(R.string.generate_complete_atestado_data_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = provinceExpanded,
                        onExpandedChange = { provinceExpanded = !provinceExpanded }
                    ) {
                        OutlinedTextField(
                            value = province,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            label = { Text(stringResource(R.string.generate_complete_atestado_province)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinceExpanded)
                            },
                            isError = validationError && province.isBlank()
                        )
                        ExposedDropdownMenu(
                            expanded = provinceExpanded,
                            onDismissRequest = { provinceExpanded = false }
                        ) {
                            provinciasEspana.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        province = option
                                        provinceExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = bulletinNumber,
                        onValueChange = { value ->
                            bulletinNumber = value.filter(Char::isDigit)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.generate_complete_atestado_bulletin_number)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = validationError && bulletinNumber.length < MIN_BULLETIN_DIGITS
                    )
                    if (validationError) {
                        Text(
                            stringResource(
                                if (bulletinNumber.length < MIN_BULLETIN_DIGITS) {
                                    R.string.generate_complete_atestado_bulletin_number_min_length
                                } else {
                                    R.string.generate_complete_atestado_required_data
                                }
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    validationError = province.isBlank() || bulletinNumber.length < MIN_BULLETIN_DIGITS
                    if (!validationError) {
                        showDataDialog = false
                        onDataConfirmed(province, bulletinNumber)
                    }
                }) {
                    Text(stringResource(R.string.continue_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDataDialog = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(stringResource(R.string.generate_complete_atestado_has_background)) },
            text = { Text(stringResource(R.string.generate_complete_atestado_background_help)) },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(stringResource(R.string.accept_action))
                }
            }
        )
    }

    if (showSealDialog) {
        val sealPreviewBitmap = remember(sealUnitText) {
            runCatching { getInstitutionalSealBitmap(context, sealUnitText, sizePx = 360) }.getOrNull()
        }
        val sealTextTooLong = remember(sealUnitText) { isSealTextTooLong(context, sealUnitText) }
        AlertDialog(
            onDismissRequest = { showSealDialog = false },
            title = { Text(stringResource(R.string.generate_complete_atestado_seal_dialog_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.generate_complete_atestado_seal_dialog_message),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    sealPreviewBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.atestado_acting_seal_preview),
                            modifier = Modifier.size(140.dp)
                        )
                    }
                    OutlinedTextField(
                        value = sealUnitText,
                        onValueChange = { sealUnitText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.generate_complete_atestado_seal_unit_label)) },
                        isError = sealTextTooLong,
                        supportingText = {
                            if (sealTextTooLong) {
                                Text(
                                    text = stringResource(R.string.generate_complete_atestado_seal_too_long),
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSealDialog = false
                        onGenerateCompleteClick(sealUnitText.trim())
                    },
                    enabled = sealUnitText.isNotBlank()
                ) {
                    Text(stringResource(R.string.generate_complete_atestado_generate))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSealDialog = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            }
        )
    }

    if (isGeneratingCompleteAtestado) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.generate_complete_atestado_progress_title)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.generate_complete_atestado_progress_message))
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun BackgroundSwitch(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = text, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
        if (checked) {
            Text(
                text = stringResource(R.string.generate_complete_atestado_add_annex),
                modifier = Modifier.padding(start = 8.dp),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
            )
        }
    }
}

private const val MIN_BULLETIN_DIGITS = 12
private const val DEFAULT_SEAL_UNIT_TEXT = "Dsto. Ribadesella"
