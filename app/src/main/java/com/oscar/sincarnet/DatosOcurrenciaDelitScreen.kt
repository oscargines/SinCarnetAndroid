package com.oscar.sincarnet

import android.app.TimePickerDialog
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oscar.sincarnet.ui.theme.SinCarnetTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val OCURRENCIA_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd-MM-yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatosOcurrenciaDelitScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onPrintClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val storage = OcurrenciaDelitStorage(context)
    val initialData = storage.loadCurrent()

    var carretera by rememberSaveable { mutableStateOf(initialData.carretera) }
    var pk by rememberSaveable { mutableStateOf(initialData.pk) }
    var localidad by rememberSaveable { mutableStateOf(initialData.localidad) }
    var terminoMunicipal by rememberSaveable { mutableStateOf(initialData.terminoMunicipal) }
    var fecha by rememberSaveable { mutableStateOf(initialData.fecha) }
    var hora by rememberSaveable { mutableStateOf(initialData.hora) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.ocurrencia_delit_title),
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = carretera,
                    onValueChange = { carretera = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.ocurrencia_delit_carretera)) }
                )

                OutlinedTextField(
                    value = pk,
                    onValueChange = { pk = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.ocurrencia_delit_pk)) }
                )

                OutlinedTextField(
                    value = localidad,
                    onValueChange = { localidad = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.ocurrencia_delit_localidad)) }
                )

                OutlinedTextField(
                    value = terminoMunicipal,
                    onValueChange = { terminoMunicipal = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.ocurrencia_delit_termino_municipal)) }
                )

                // Fecha — mismo patrón que DatosJuzgadoAtestadoScreen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = fecha,
                        onValueChange = {},
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        singleLine = true,
                        label = { Text(stringResource(R.string.ocurrencia_delit_fecha)) }
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
                            contentDescription = stringResource(R.string.ocurrencia_delit_select_date),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Hora — mismo patrón que DatosJuzgadoAtestadoScreen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = hora,
                        onValueChange = {},
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        singleLine = true,
                        label = { Text(stringResource(R.string.ocurrencia_delit_hora)) }
                    )
                    IconButton(
                        onClick = {
                            val initialHour = hora.split(":").getOrNull(0)?.toIntOrNull() ?: 0
                            val initialMinute = hora.split(":").getOrNull(1)?.toIntOrNull() ?: 0
                            TimePickerDialog(
                                context,
                                { _, h, m -> hora = String.format("%02d:%02d", h, m) },
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
                            contentDescription = stringResource(R.string.ocurrencia_delit_select_time),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        storage.saveCurrent(
                            OcurrenciaDelitData(
                                carretera = carretera,
                                pk = pk,
                                localidad = localidad,
                                terminoMunicipal = terminoMunicipal,
                                fecha = fecha,
                                hora = hora
                            )
                        )
                        onBackClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.ocurrencia_delit_save))
                }

                Button(
                    onClick = {
                        storage.clearCurrent()
                        carretera = ""
                        pk = ""
                        localidad = ""
                        terminoMunicipal = ""
                        fecha = ""
                        hora = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.ocurrencia_delit_delete))
                }
            }
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

    // Material3 DatePickerDialog — mismo patrón que DatosJuzgadoAtestadoScreen
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = fecha.toOcurrenciaDateMillisOrNull()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis
                            ?.toFormattedOcurrenciaDate()
                            ?.let { fecha = it }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.person_data_birth_date_select_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.no_option))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun Long.toFormattedOcurrenciaDate(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(OCURRENCIA_DATE_FORMATTER)

private fun String.toOcurrenciaDateMillisOrNull(): Long? = runCatching {
    LocalDate.parse(this, OCURRENCIA_DATE_FORMATTER)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}.getOrNull()

@Preview(showBackground = true)
@Composable
private fun DatosOcurrenciaDelitScreenPreview() {
    SinCarnetTheme {
        DatosOcurrenciaDelitScreen()
    }
}
