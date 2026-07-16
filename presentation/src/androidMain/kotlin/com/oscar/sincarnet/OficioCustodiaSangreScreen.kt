package com.oscar.sincarnet

import android.app.TimePickerDialog
import com.oscar.sincarnet.presentation.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.oscar.sincarnet.data.repository.OficioCustodiaSangreStorage
import com.oscar.sincarnet.data.toStorage
import com.oscar.sincarnet.domain.model.OficioCustodiaSangreData
import com.oscar.sincarnet.ui.theme.SinCarnetTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val OCS_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

private fun Long.toOcsFormattedDate(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(OCS_DATE_FORMATTER)

private fun String.toOcsDateMillisOrNull(): Long? = runCatching {
    LocalDate.parse(this, OCS_DATE_FORMATTER)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}.getOrNull()

private fun applyDateMask(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(8)
    return buildString {
        digits.forEachIndexed { i, c ->
            if (i == 2 || i == 4) append('-')
            append(c)
        }
    }
}

private fun applyTimeMask(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(4)
    return buildString {
        digits.forEachIndexed { i, c ->
            if (i == 2) append(':')
            append(c)
        }
    }
}

private val EMPLEOS_GC = listOf(
    "Guardia Civil",
    "Guardia de 1\u00aa",
    "Cabo",
    "Cabo 1\u00ba",
    "Cabo Mayor",
    "Sargento",
    "Sargento 1\u00ba",
    "Brigada",
    "Subteniente",
    "Suboficial Mayor",
    "Teniente",
    "Capit\u00e1n",
    "Comandante",
    "Teniente Coronel",
    "Coronel"
)

private const val OCS_TIP_MAX_LENGTH = 7
private val OCS_TIP_REGEX = Regex("^[A-Z][0-9]{5}[A-Z]$")

private fun ocsNormalizeTipInput(input: String): String {
    val upper = input.uppercase()
    val result = StringBuilder(OCS_TIP_MAX_LENGTH)
    for (char in upper) {
        if (!char.isLetterOrDigit()) continue
        when (result.length) {
            0 -> if (char.isLetter()) result.append(char)
            in 1..5 -> if (char.isDigit()) result.append(char)
            6 -> if (char.isLetter()) result.append(char)
        }
        if (result.length == OCS_TIP_MAX_LENGTH) break
    }
    return result.toString()
}

private fun ocsIsTipValid(tip: String): Boolean = OCS_TIP_REGEX.matches(tip)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OficioCustodiaSangreScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onContinueClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val storage = if (isInPreview) null else OficioCustodiaSangreStorage(context.toStorage("oficio_custodia_sangre_storage"))
    val initialData = storage?.loadCurrent() ?: OficioCustodiaSangreData()

    var unidad by rememberSaveable { mutableStateOf(initialData.unidad) }
    var centroDestinatario by rememberSaveable { mutableStateOf(initialData.centroDestinatario) }
    var fechaHoraSolicitud by rememberSaveable { mutableStateOf(initialData.fechaHoraSolicitud) }
    var tipSolicitante by rememberSaveable { mutableStateOf(initialData.tipSolicitante) }
    var empleoSolicitante by rememberSaveable { mutableStateOf(initialData.empleoSolicitante) }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var pendingDate by rememberSaveable { mutableStateOf("") }
    var empleoExpanded by rememberSaveable { mutableStateOf(false) }

    fun buildCurrentData() = OficioCustodiaSangreData(
        unidad = unidad,
        centroDestinatario = centroDestinatario,
        fechaHoraSolicitud = fechaHoraSolicitud,
        tipSolicitante = tipSolicitante,
        empleoSolicitante = empleoSolicitante,
        nombreApellidos = initialData.nombreApellidos,
        numeroDocumento = initialData.numeroDocumento,
        fechaHoraSiniestro = initialData.fechaHoraSiniestro,
        carretera = initialData.carretera,
        puntoKilometrico = initialData.puntoKilometrico,
        municipioOcurrencia = initialData.municipioOcurrencia,
        partidoJudicial = initialData.partidoJudicial,
        catalogacionHecho = initialData.catalogacionHecho,
        juzgadoCompetente = initialData.juzgadoCompetente,
        numeroDiligencia = initialData.numeroDiligencia
    )

    fun resetForm() {
        unidad = ""
        centroDestinatario = ""
        fechaHoraSolicitud = ""
        tipSolicitante = ""
        empleoSolicitante = ""
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.oficio_custodia_sangre_title),
                    style = MaterialTheme.typography.titleMedium
                )

                SectionTitle(text = stringResource(R.string.ocs_unidad))
                OutlinedTextField(
                    value = unidad,
                    onValueChange = { unidad = it.uppercase() },
                    label = { Text(stringResource(R.string.ocs_unidad)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                SectionTitle(text = stringResource(R.string.ocs_centro_destinatario))
                OutlinedTextField(
                    value = centroDestinatario,
                    onValueChange = { centroDestinatario = it.uppercase() },
                    label = { Text(stringResource(R.string.ocs_centro_destinatario)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(text = stringResource(R.string.ocs_fecha_hora_solicitud))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = fechaHoraSolicitud,
                        onValueChange = { fechaHoraSolicitud = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.ocs_fecha_hora_solicitud)) },
                        placeholder = { Text("dd-MM-yyyy HH:mm") }
                    )
                    IconButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .size(48.dp)
                            .padding(top = 8.dp)
                            .align(Alignment.Top)
                    ) {
                        AssetImage(
                            assetPath = "icons/calendar.png",
                            contentDescription = stringResource(R.string.acta_traslado_select_date),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(text = stringResource(R.string.ocs_tip_solicitante))
                OutlinedTextField(
                    value = tipSolicitante,
                    onValueChange = { tipSolicitante = ocsNormalizeTipInput(it) },
                    label = { Text(stringResource(R.string.ocs_tip_solicitante)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    isError = tipSolicitante.isNotEmpty() && !ocsIsTipValid(tipSolicitante),
                    supportingText = {
                        if (tipSolicitante.isNotEmpty() && !ocsIsTipValid(tipSolicitante)) {
                            Text(text = stringResource(R.string.atestado_acting_tip_format_hint))
                        }
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(text = stringResource(R.string.ocs_empleo))
                ExposedDropdownMenuBox(
                    expanded = empleoExpanded,
                    onExpandedChange = { empleoExpanded = it }
                ) {
                    OutlinedTextField(
                        value = empleoSolicitante,
                        onValueChange = { empleoSolicitante = it.uppercase() },
                        label = { Text(stringResource(R.string.ocs_empleo)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = empleoExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = empleoExpanded,
                        onDismissRequest = { empleoExpanded = false }
                    ) {
                        EMPLEOS_GC.forEach { empleo ->
                            DropdownMenuItem(
                                text = { Text(empleo) },
                                onClick = {
                                    empleoSolicitante = empleo
                                    empleoExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        storage?.saveCurrent(buildCurrentData())
                        onContinueClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White)
                ) {
                    Text(stringResource(R.string.ocs_continue))
                }

                Button(
                    onClick = {
                        resetForm()
                        storage?.clearCurrent()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020), contentColor = Color.White)
                ) {
                    Text(stringResource(R.string.ocs_clear))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackIconButton(onClick = onBackClick)
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = fechaHoraSolicitud.take(10).toOcsDateMillisOrNull()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.toOcsFormattedDate()?.let {
                        pendingDate = it
                    }
                    showDatePicker = false
                    showTimePicker = true
                }) { Text(stringResource(R.string.person_data_birth_date_select_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.no_option)) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val initialHour = fechaHoraSolicitud.takeLast(5).split(":").getOrNull(0)?.toIntOrNull() ?: 0
        val initialMinute = fechaHoraSolicitud.takeLast(5).split(":").getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(
            context,
            { _, h, m ->
                fechaHoraSolicitud = "$pendingDate ${String.format(Locale.getDefault(), "%02d:%02d", h, m)}"
            },
            initialHour,
            initialMinute,
            true
        ).show()
        showTimePicker = false
    }
}

@Preview(showBackground = true)
@Composable
private fun OficioCustodiaSangreScreenPreview() {
    SinCarnetTheme {
        OficioCustodiaSangreScreen()
    }
}
