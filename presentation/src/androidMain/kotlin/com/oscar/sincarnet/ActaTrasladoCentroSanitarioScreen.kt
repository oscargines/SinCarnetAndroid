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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.oscar.sincarnet.data.repository.ActaTrasladoCentroSanitarioStorage
import com.oscar.sincarnet.data.toStorage
import com.oscar.sincarnet.domain.model.ActaTrasladoCentroSanitarioData
import com.oscar.sincarnet.nfc.LocalNfcReaderController
import com.oscar.sincarnet.ui.theme.SinCarnetTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CS_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

private fun Long.toCsFormattedDate(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(CS_DATE_FORMATTER)

private fun String.toCsDateMillisOrNull(): Long? = runCatching {
    LocalDate.parse(this, CS_DATE_FORMATTER)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActaTrasladoCentroSanitarioScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onContinueClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val storage = if (isInPreview) null else ActaTrasladoCentroSanitarioStorage(context.toStorage("centro_sanitario_storage"))
    val initialData = storage?.loadCurrent() ?: ActaTrasladoCentroSanitarioData()

    var subsector by rememberSaveable { mutableStateOf(initialData.subsector) }
    var unidadInterviniente by rememberSaveable { mutableStateOf(initialData.unidadInterviniente) }
    var numeroExpediente by rememberSaveable { mutableStateOf(initialData.numeroExpediente) }

    var lugarIntervencion by rememberSaveable { mutableStateOf(initialData.lugarIntervencion) }
    var motivo by rememberSaveable { mutableStateOf(initialData.motivo) }
    var fechaHora by rememberSaveable { mutableStateOf(initialData.fechaHora) }

    var nombreApellidos by rememberSaveable { mutableStateOf(initialData.nombreApellidos) }
    var dniNiePasaporte by rememberSaveable { mutableStateOf(initialData.dniNiePasaporte) }

    var facultativoColegiado by rememberSaveable { mutableStateOf(initialData.facultativoColegiado) }

    var pruebaSangre by rememberSaveable { mutableStateOf(initialData.pruebaSangre) }
    var pruebaOtroTipo by rememberSaveable { mutableStateOf(initialData.pruebaOtroTipo) }
    var pruebaOtroDetalle by rememberSaveable { mutableStateOf(initialData.pruebaOtroDetalle) }

    var centroSanitario by rememberSaveable { mutableStateOf(initialData.centroSanitario) }
    var extraccionColegiado by rememberSaveable { mutableStateOf(initialData.extraccionColegiado) }
    var fechaHoraExtraccion by rememberSaveable { mutableStateOf(initialData.fechaHoraExtraccion) }

    var precintosSeguridad by rememberSaveable { mutableStateOf(initialData.precintosSeguridad) }
    var datoMuestra by rememberSaveable { mutableStateOf(initialData.datoMuestra) }

    var juzgadoOrganismo by rememberSaveable { mutableStateOf(initialData.juzgadoOrganismo) }
    var otrasObservaciones by rememberSaveable { mutableStateOf(initialData.otrasObservaciones) }
    var numTip by rememberSaveable { mutableStateOf(initialData.numTip) }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var pendingDate by rememberSaveable { mutableStateOf("") }
    var showExtraccionDatePicker by rememberSaveable { mutableStateOf(false) }
    var showExtraccionTimePicker by rememberSaveable { mutableStateOf(false) }
    var pendingExtraccionDate by rememberSaveable { mutableStateOf("") }

    val nfcController = LocalNfcReaderController.current
    val nfcHelper = if (!isInPreview) {
        rememberNfcReadingHelper(
            onDataRead = { data ->
                nombreApellidos = "${data.firstName} ${data.lastName1} ${data.lastName2}".trim().uppercase()
                dniNiePasaporte = data.optionalData.ifBlank { data.documentNumber }.uppercase()
            },
            onEnableNfcReader = { nfcController.enableNfcReaderModeForDniRead() },
            onDisableNfcReader = { nfcController.disableNfcReaderModeForDniRead() }
        )
    } else null

    fun buildCurrentData() = ActaTrasladoCentroSanitarioData(
        subsector = subsector,
        unidadInterviniente = unidadInterviniente,
        numeroExpediente = numeroExpediente,
        lugarIntervencion = lugarIntervencion,
        motivo = motivo,
        fechaHora = fechaHora,
        nombreApellidos = nombreApellidos,
        dniNiePasaporte = dniNiePasaporte,
        facultativoColegiado = facultativoColegiado,
        pruebaSangre = pruebaSangre,
        pruebaOtroTipo = pruebaOtroTipo,
        pruebaOtroDetalle = pruebaOtroDetalle,
        centroSanitario = centroSanitario,
        extraccionColegiado = extraccionColegiado,
        fechaHoraExtraccion = fechaHoraExtraccion,
        precintosSeguridad = precintosSeguridad,
        datoMuestra = datoMuestra,
        juzgadoOrganismo = juzgadoOrganismo,
        otrasObservaciones = otrasObservaciones,
        numTip = numTip
    )

    fun resetForm() {
        subsector = ""
        unidadInterviniente = ""
        numeroExpediente = ""
        lugarIntervencion = ""
        motivo = ""
        fechaHora = ""
        nombreApellidos = ""
        dniNiePasaporte = ""
        facultativoColegiado = ""
        pruebaSangre = false
        pruebaOtroTipo = false
        pruebaOtroDetalle = ""
        centroSanitario = ""
        extraccionColegiado = ""
        fechaHoraExtraccion = ""
        precintosSeguridad = "PRECINTOS DE SEGURIDAD"
        datoMuestra = ""
        juzgadoOrganismo = ""
        otrasObservaciones = ""
        numTip = ""
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
                    text = stringResource(R.string.centro_sanitario_title),
                    style = MaterialTheme.typography.titleMedium
                )

                SectionTitle(text = stringResource(R.string.centro_sanitario_cabecera_title))
                OutlinedTextField(
                    value = subsector,
                    onValueChange = { subsector = it.uppercase() },
                    label = { Text(stringResource(R.string.centro_sanitario_subsector)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = unidadInterviniente,
                    onValueChange = { unidadInterviniente = it.uppercase() },
                    label = { Text(stringResource(R.string.centro_sanitario_unidad_interviniente)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = numeroExpediente,
                    onValueChange = { numeroExpediente = it },
                    label = { Text(stringResource(R.string.centro_sanitario_numero_expediente)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(text = stringResource(R.string.centro_sanitario_lugar_title))
                OutlinedTextField(
                    value = lugarIntervencion,
                    onValueChange = { lugarIntervencion = it.uppercase() },
                    label = { Text(stringResource(R.string.centro_sanitario_lugar)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it.uppercase() },
                    label = { Text(stringResource(R.string.centro_sanitario_motivo)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = fechaHora,
                        onValueChange = { fechaHora = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.centro_sanitario_fecha_hora)) },
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
                SectionTitle(text = stringResource(R.string.centro_sanitario_persona_title))
                OutlinedTextField(
                    value = nombreApellidos,
                    onValueChange = { nombreApellidos = it.uppercase() },
                    label = { Text(stringResource(R.string.centro_sanitario_nombre_apellidos)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = dniNiePasaporte,
                        onValueChange = { dniNiePasaporte = it.uppercase() },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.centro_sanitario_dni_nie)) }
                    )
                    if (nfcHelper != null) {
                        IconButton(
                            onClick = { nfcHelper.startCanDialog() },
                            modifier = Modifier
                                .size(48.dp)
                                .padding(top = 8.dp)
                                .align(Alignment.Top)
                        ) {
                            AssetImage(
                                assetPath = "icons/rfid.png",
                                contentDescription = stringResource(R.string.can_button_content_description),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(text = stringResource(R.string.centro_sanitario_facultativo_title))
                OutlinedTextField(
                    value = facultativoColegiado,
                    onValueChange = { facultativoColegiado = it },
                    label = { Text(stringResource(R.string.centro_sanitario_colegiado)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(text = stringResource(R.string.centro_sanitario_pruebas_title))
                CheckboxOptionRow(
                    text = stringResource(R.string.centro_sanitario_prueba_sangre),
                    checked = pruebaSangre,
                    onCheckedChange = { pruebaSangre = it }
                )
                CheckboxOptionRow(
                    text = stringResource(R.string.centro_sanitario_prueba_otro),
                    checked = pruebaOtroTipo,
                    onCheckedChange = { pruebaOtroTipo = it }
                )
                if (pruebaOtroTipo) {
                    OutlinedTextField(
                        value = pruebaOtroDetalle,
                        onValueChange = { pruebaOtroDetalle = it },
                        label = { Text(stringResource(R.string.centro_sanitario_prueba_otro_detalle)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(text = stringResource(R.string.centro_sanitario_centro_title))
                OutlinedTextField(
                    value = centroSanitario,
                    onValueChange = { centroSanitario = it.uppercase() },
                    label = { Text(stringResource(R.string.centro_sanitario_centro_sanitario)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = extraccionColegiado,
                    onValueChange = { extraccionColegiado = it },
                    label = { Text(stringResource(R.string.centro_sanitario_extraccion_colegiado)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = fechaHoraExtraccion,
                        onValueChange = { fechaHoraExtraccion = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.centro_sanitario_fecha_hora_extraccion)) },
                        placeholder = { Text("dd-MM-yyyy HH:mm") }
                    )
                    IconButton(
                        onClick = { showExtraccionDatePicker = true },
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
                SectionTitle(text = stringResource(R.string.centro_sanitario_muestras_title))
                OutlinedTextField(
                    value = precintosSeguridad,
                    onValueChange = { precintosSeguridad = it },
                    label = { Text(stringResource(R.string.centro_sanitario_precintos)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = datoMuestra,
                    onValueChange = { datoMuestra = it },
                    label = { Text(stringResource(R.string.centro_sanitario_dato_muestra)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(text = stringResource(R.string.centro_sanitario_juzgado_title))
                OutlinedTextField(
                    value = juzgadoOrganismo,
                    onValueChange = { juzgadoOrganismo = it.uppercase() },
                    label = { Text(stringResource(R.string.centro_sanitario_juzgado)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(text = stringResource(R.string.centro_sanitario_otras_observaciones))
                OutlinedTextField(
                    value = otrasObservaciones,
                    onValueChange = { otrasObservaciones = it },
                    label = { Text(stringResource(R.string.centro_sanitario_otras_observaciones)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = numTip,
                    onValueChange = { numTip = csNormalizeTipInput(it) },
                    label = { Text(stringResource(R.string.centro_sanitario_num_tip)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    isError = numTip.isNotEmpty() && !csIsTipValid(numTip),
                    supportingText = {
                        if (numTip.isNotEmpty() && !csIsTipValid(numTip)) {
                            Text(text = stringResource(R.string.atestado_acting_tip_format_hint))
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        storage?.saveCurrent(buildCurrentData())
                        onContinueClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White)
                ) {
                    Text(stringResource(R.string.centro_sanitario_continue))
                }

                Button(
                    onClick = {
                        resetForm()
                        storage?.clearCurrent()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020), contentColor = Color.White)
                ) {
                    Text(stringResource(R.string.centro_sanitario_clear))
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
            initialSelectedDateMillis = fechaHora.take(10).toCsDateMillisOrNull()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.toCsFormattedDate()?.let {
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
        val initialHour = fechaHora.takeLast(5).split(":").getOrNull(0)?.toIntOrNull() ?: 0
        val initialMinute = fechaHora.takeLast(5).split(":").getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(
            context,
            { _, h, m ->
                fechaHora = "$pendingDate ${String.format(Locale.getDefault(), "%02d:%02d", h, m)}"
            },
            initialHour,
            initialMinute,
            true
        ).show()
        showTimePicker = false
    }

    if (showExtraccionDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = fechaHoraExtraccion.take(10).toCsDateMillisOrNull()
        )
        DatePickerDialog(
            onDismissRequest = { showExtraccionDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.toCsFormattedDate()?.let {
                        pendingExtraccionDate = it
                    }
                    showExtraccionDatePicker = false
                    showExtraccionTimePicker = true
                }) { Text(stringResource(R.string.person_data_birth_date_select_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showExtraccionDatePicker = false }) { Text(stringResource(R.string.no_option)) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showExtraccionTimePicker) {
        val initialHour = fechaHoraExtraccion.takeLast(5).split(":").getOrNull(0)?.toIntOrNull() ?: 0
        val initialMinute = fechaHoraExtraccion.takeLast(5).split(":").getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(
            context,
            { _, h, m ->
                fechaHoraExtraccion = "$pendingExtraccionDate ${String.format(Locale.getDefault(), "%02d:%02d", h, m)}"
            },
            initialHour,
            initialMinute,
            true
        ).show()
        showExtraccionTimePicker = false
    }

    if (nfcHelper != null) {
        nfcHelper.NfcDialogs()
    }
}

private const val CS_TIP_MAX_LENGTH = 7
private val CS_TIP_REGEX = Regex("^[A-Z][0-9]{5}[A-Z]$")

private fun csNormalizeTipInput(input: String): String {
    val upper = input.uppercase()
    val result = StringBuilder(CS_TIP_MAX_LENGTH)
    for (char in upper) {
        if (!char.isLetterOrDigit()) continue
        when (result.length) {
            0 -> if (char.isLetter()) result.append(char)
            in 1..5 -> if (char.isDigit()) result.append(char)
            6 -> if (char.isLetter()) result.append(char)
        }
        if (result.length == CS_TIP_MAX_LENGTH) break
    }
    return result.toString()
}

private fun csIsTipValid(tip: String): Boolean = CS_TIP_REGEX.matches(tip)

@Preview(showBackground = true)
@Composable
private fun ActaTrasladoCentroSanitarioScreenPreview() {
    SinCarnetTheme {
        ActaTrasladoCentroSanitarioScreen()
    }
}
