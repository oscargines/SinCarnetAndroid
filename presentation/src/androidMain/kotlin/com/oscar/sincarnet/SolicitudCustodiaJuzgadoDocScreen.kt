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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oscar.sincarnet.data.repository.SolicitudCustodiaJuzgadoStorage
import com.oscar.sincarnet.data.toStorage
import com.oscar.sincarnet.domain.model.SolicitudCustodiaJuzgadoData
import com.oscar.sincarnet.nfc.LocalNfcReaderController
import com.oscar.sincarnet.ui.theme.SinCarnetTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SCJ_DOC_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

private fun Long.toScjDocFormattedDate(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(SCJ_DOC_DATE_FORMATTER)

private fun String.toScjDocDateMillisOrNull(): Long? = runCatching {
    LocalDate.parse(this, SCJ_DOC_DATE_FORMATTER)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}.getOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudCustodiaJuzgadoDocScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onContinueClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val storage = if (isInPreview) null else SolicitudCustodiaJuzgadoStorage(context.toStorage("solicitud_custodia_juzgado_storage"))
    val initialData = storage?.loadCurrent() ?: SolicitudCustodiaJuzgadoData()

    var nombreApellidos by rememberSaveable { mutableStateOf(initialData.nombreApellidos) }
    var numeroDocumento by rememberSaveable { mutableStateOf(initialData.numeroDocumento) }
    var matricula by rememberSaveable { mutableStateOf(initialData.matricula) }
    var fechaHoraSiniestro by rememberSaveable { mutableStateOf(initialData.fechaHoraSiniestro) }
    var carretera by rememberSaveable { mutableStateOf(initialData.carretera) }
    var puntoKilometrico by rememberSaveable { mutableStateOf(initialData.puntoKilometrico) }
    var municipioOcurrencia by rememberSaveable { mutableStateOf(initialData.municipioOcurrencia) }
    var partidoJudicial by rememberSaveable { mutableStateOf(initialData.partidoJudicial) }
    var catalogacionHecho by rememberSaveable { mutableStateOf(initialData.catalogacionHecho) }
    var centroSanitario by rememberSaveable { mutableStateOf(initialData.centroSanitario) }
    var fechaEntregaOficio by rememberSaveable { mutableStateOf(initialData.fechaEntregaOficio) }

    var showSiniestroDatePicker by rememberSaveable { mutableStateOf(false) }
    var showSiniestroTimePicker by rememberSaveable { mutableStateOf(false) }
    var pendingSiniestroDate by rememberSaveable { mutableStateOf("") }

    var showEntregaDatePicker by rememberSaveable { mutableStateOf(false) }

    val nfcController = LocalNfcReaderController.current
    val nfcHelper = if (!isInPreview) {
        rememberNfcReadingHelper(
            onDataRead = { data ->
                nombreApellidos = "${data.firstName} ${data.lastName1} ${data.lastName2}".trim().uppercase()
                numeroDocumento = data.optionalData.ifBlank { data.documentNumber }.uppercase()
            },
            onEnableNfcReader = { nfcController.enableNfcReaderModeForDniRead() },
            onDisableNfcReader = { nfcController.disableNfcReaderModeForDniRead() }
        )
    } else null

    fun buildCurrentData() = SolicitudCustodiaJuzgadoData(
        unidad = initialData.unidad,
        fechaHoraSolicitud = initialData.fechaHoraSolicitud,
        tipSolicitante = initialData.tipSolicitante,
        empleoSolicitante = initialData.empleoSolicitante,
        numeroDiligencia = initialData.numeroDiligencia,
        juzgadoNumero = initialData.juzgadoNumero,
        nombreApellidos = nombreApellidos,
        numeroDocumento = numeroDocumento,
        matricula = matricula,
        fechaHoraSiniestro = fechaHoraSiniestro,
        carretera = carretera,
        puntoKilometrico = puntoKilometrico,
        municipioOcurrencia = municipioOcurrencia,
        partidoJudicial = partidoJudicial,
        catalogacionHecho = catalogacionHecho,
        centroSanitario = centroSanitario,
        fechaEntregaOficio = fechaEntregaOficio
    )

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
                    text = stringResource(R.string.scj_doc_title),
                    style = MaterialTheme.typography.titleMedium
                )

                SectionTitle(text = stringResource(R.string.scj_nombre_apellidos))
                OutlinedTextField(
                    value = nombreApellidos,
                    onValueChange = { nombreApellidos = it.uppercase() },
                    label = { Text(stringResource(R.string.scj_nombre_apellidos)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = numeroDocumento,
                        onValueChange = { numeroDocumento = it.uppercase() },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.scj_numero_documento)) }
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

                OutlinedTextField(
                    value = matricula,
                    onValueChange = { matricula = it.uppercase() },
                    label = { Text(stringResource(R.string.scj_matricula)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(text = stringResource(R.string.scj_fecha_hora_siniestro))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = fechaHoraSiniestro,
                        onValueChange = { fechaHoraSiniestro = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.scj_fecha_hora_siniestro)) },
                        placeholder = { Text("dd-MM-yyyy HH:mm") }
                    )
                    IconButton(
                        onClick = { showSiniestroDatePicker = true },
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
                SectionTitle(text = stringResource(R.string.scj_carretera))
                OutlinedTextField(
                    value = carretera,
                    onValueChange = { carretera = it.uppercase() },
                    label = { Text(stringResource(R.string.scj_carretera)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = puntoKilometrico,
                    onValueChange = { puntoKilometrico = it },
                    label = { Text(stringResource(R.string.scj_punto_kilometrico)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = municipioOcurrencia,
                    onValueChange = { municipioOcurrencia = it.uppercase() },
                    label = { Text(stringResource(R.string.scj_municipio)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = partidoJudicial,
                    onValueChange = { partidoJudicial = it.uppercase() },
                    label = { Text(stringResource(R.string.scj_partido_judicial)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(text = stringResource(R.string.scj_catalogacion_hecho))
                OutlinedTextField(
                    value = catalogacionHecho,
                    onValueChange = { catalogacionHecho = it.uppercase() },
                    label = { Text(stringResource(R.string.scj_catalogacion_hecho)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(text = stringResource(R.string.scj_centro_sanitario))
                OutlinedTextField(
                    value = centroSanitario,
                    onValueChange = { centroSanitario = it.uppercase() },
                    label = { Text(stringResource(R.string.scj_centro_sanitario)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(text = stringResource(R.string.scj_fecha_entrega_oficio))
                OutlinedTextField(
                    value = fechaEntregaOficio,
                    onValueChange = { fechaEntregaOficio = it },
                    label = { Text(stringResource(R.string.scj_fecha_entrega_oficio)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("dd-MM-yyyy") }
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
                    Text(stringResource(R.string.scj_continue))
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

    if (showSiniestroDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = fechaHoraSiniestro.take(10).toScjDocDateMillisOrNull()
        )
        DatePickerDialog(
            onDismissRequest = { showSiniestroDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.toScjDocFormattedDate()?.let {
                        pendingSiniestroDate = it
                    }
                    showSiniestroDatePicker = false
                    showSiniestroTimePicker = true
                }) { Text(stringResource(R.string.person_data_birth_date_select_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showSiniestroDatePicker = false }) { Text(stringResource(R.string.no_option)) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showSiniestroTimePicker) {
        val initialHour = fechaHoraSiniestro.takeLast(5).split(":").getOrNull(0)?.toIntOrNull() ?: 0
        val initialMinute = fechaHoraSiniestro.takeLast(5).split(":").getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(
            context,
            { _, h, m ->
                fechaHoraSiniestro = "$pendingSiniestroDate ${String.format(Locale.getDefault(), "%02d:%02d", h, m)}"
            },
            initialHour,
            initialMinute,
            true
        ).show()
        showSiniestroTimePicker = false
    }

    if (nfcHelper != null) {
        nfcHelper.NfcDialogs()
    }
}

@Preview(showBackground = true)
@Composable
private fun SolicitudCustodiaJuzgadoDocScreenPreview() {
    SinCarnetTheme {
        SolicitudCustodiaJuzgadoDocScreen()
    }
}
