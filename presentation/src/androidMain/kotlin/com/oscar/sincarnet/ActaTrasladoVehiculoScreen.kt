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
import com.oscar.sincarnet.data.repository.ActaTrasladoVehiculoStorage
import com.oscar.sincarnet.data.toStorage
import com.oscar.sincarnet.domain.model.ActaTrasladoVehiculoData
import com.oscar.sincarnet.ui.theme.SinCarnetTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TRASLADO_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

private fun Long.toTrasladoFormattedDate(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(TRASLADO_DATE_FORMATTER)

private fun String.toTrasladoDateMillisOrNull(): Long? = runCatching {
    LocalDate.parse(this, TRASLADO_DATE_FORMATTER)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}.getOrNull()

/** Applies dd-MM-yyyy mask while typing (digits only, dashes auto-inserted). */
private fun applyDateMask(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(8)
    return buildString {
        digits.forEachIndexed { i, c ->
            if (i == 2 || i == 4) append('-')
            append(c)
        }
    }
}

/** Applies HH:mm mask while typing (digits only, colon auto-inserted). */
private fun applyTimeMask(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(4)
    return buildString {
        digits.forEachIndexed { i, c ->
            if (i == 2) append(':')
            append(c)
        }
    }
}

private enum class InicioActuacionOption { AUXILIO_ACCIDENTE, INFRACCION, OTRO_MOTIVO }
private enum class CircunstanciaTrasladoOption { FACTORES_ATMOSFERICOS, MALA_VISIBILIDAD, CONFIGURACION_VIA, OTRAS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActaTrasladoVehiculoScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onContinueClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val storage = if (isInPreview) null else ActaTrasladoVehiculoStorage(context.toStorage("acta_traslado_vehiculo_storage"))
    val initialData = storage?.loadCurrent() ?: ActaTrasladoVehiculoData()

    var sector by rememberSaveable { mutableStateOf(initialData.sector) }
    var subsector by rememberSaveable { mutableStateOf(initialData.subsector) }
    var destacamento by rememberSaveable { mutableStateOf(initialData.destacamento) }

    var vehiculoTipo by rememberSaveable { mutableStateOf(initialData.vehiculoTipo) }
    var vehiculoMarca by rememberSaveable { mutableStateOf(initialData.vehiculoMarca) }
    var vehiculoModelo by rememberSaveable { mutableStateOf(initialData.vehiculoModelo) }
    var vehiculoColor by rememberSaveable { mutableStateOf(initialData.vehiculoColor) }
    var vehiculoMatricula by rememberSaveable { mutableStateOf(initialData.vehiculoMatricula) }

    var esTitular by rememberSaveable { mutableStateOf(initialData.esTitular) }
    var esConductor by rememberSaveable { mutableStateOf(initialData.esConductor) }
    var nombre by rememberSaveable { mutableStateOf(initialData.nombre) }
    var primerApellido by rememberSaveable { mutableStateOf(initialData.primerApellido) }
    var segundoApellido by rememberSaveable { mutableStateOf(initialData.segundoApellido) }
    var dniNie by rememberSaveable { mutableStateOf(initialData.dniNie) }
    var telefono by rememberSaveable { mutableStateOf(initialData.telefono) }

    var inicioActuacionSeleccionada by rememberSaveable {
        mutableStateOf(
            when {
                initialData.inicioAuxilioAccidente -> InicioActuacionOption.AUXILIO_ACCIDENTE
                initialData.inicioInfraccion -> InicioActuacionOption.INFRACCION
                initialData.inicioOtroMotivo.isNotBlank() -> InicioActuacionOption.OTRO_MOTIVO
                else -> null
            }
        )
    }
    var inicioOtroMotivo by rememberSaveable { mutableStateOf(initialData.inicioOtroMotivo) }

    var circunstanciaSeleccionada by rememberSaveable {
        mutableStateOf(
            when {
                initialData.circunstanciaFactoresAtmosfericos -> CircunstanciaTrasladoOption.FACTORES_ATMOSFERICOS
                initialData.circunstanciaMalaVisibilidad -> CircunstanciaTrasladoOption.MALA_VISIBILIDAD
                initialData.circunstanciaConfiguracionVia.isNotBlank() -> CircunstanciaTrasladoOption.CONFIGURACION_VIA
                initialData.circunstanciaOtras.isNotBlank() -> CircunstanciaTrasladoOption.OTRAS
                else -> null
            }
        )
    }
    var circunstanciaConfiguracionVia by rememberSaveable { mutableStateOf(initialData.circunstanciaConfiguracionVia) }
    var circunstanciaOtras by rememberSaveable { mutableStateOf(initialData.circunstanciaOtras) }

    var inicioLugar by rememberSaveable { mutableStateOf(initialData.inicioLugar) }
    var inicioHora by rememberSaveable { mutableStateOf(initialData.inicioHora) }
    var inicioFecha by rememberSaveable { mutableStateOf(initialData.inicioFecha) }
    var finLugar by rememberSaveable { mutableStateOf(initialData.finLugar) }
    var finHora by rememberSaveable { mutableStateOf(initialData.finHora) }
    var finFecha by rememberSaveable { mutableStateOf(initialData.finFecha) }
    var unidadResponsable by rememberSaveable { mutableStateOf(initialData.unidadResponsable) }
    var unidadTelefono by rememberSaveable { mutableStateOf(initialData.unidadTelefono) }
    var agenteTip by rememberSaveable { mutableStateOf(initialData.agenteTip) }
    var agenteUnidad by rememberSaveable { mutableStateOf(initialData.agenteUnidad) }

    var showInicioDatePicker by rememberSaveable { mutableStateOf(false) }
    var showFinDatePicker by rememberSaveable { mutableStateOf(false) }

    fun buildCurrentData() = ActaTrasladoVehiculoData(
        sector = sector,
        subsector = subsector,
        destacamento = destacamento,
        vehiculoTipo = vehiculoTipo,
        vehiculoMarca = vehiculoMarca,
        vehiculoModelo = vehiculoModelo,
        vehiculoColor = vehiculoColor,
        vehiculoMatricula = vehiculoMatricula,
        esTitular = esTitular,
        esConductor = esConductor,
        nombre = nombre,
        primerApellido = primerApellido,
        segundoApellido = segundoApellido,
        dniNie = dniNie,
        telefono = telefono,
        inicioAuxilioAccidente = inicioActuacionSeleccionada == InicioActuacionOption.AUXILIO_ACCIDENTE,
        inicioInfraccion = inicioActuacionSeleccionada == InicioActuacionOption.INFRACCION,
        inicioOtroMotivo = if (inicioActuacionSeleccionada == InicioActuacionOption.OTRO_MOTIVO) inicioOtroMotivo else "",
        circunstanciaFactoresAtmosfericos = circunstanciaSeleccionada == CircunstanciaTrasladoOption.FACTORES_ATMOSFERICOS,
        circunstanciaMalaVisibilidad = circunstanciaSeleccionada == CircunstanciaTrasladoOption.MALA_VISIBILIDAD,
        circunstanciaConfiguracionVia = if (circunstanciaSeleccionada == CircunstanciaTrasladoOption.CONFIGURACION_VIA) circunstanciaConfiguracionVia else "",
        circunstanciaOtras = if (circunstanciaSeleccionada == CircunstanciaTrasladoOption.OTRAS) circunstanciaOtras else "",
        inicioLugar = inicioLugar,
        inicioHora = inicioHora,
        inicioFecha = inicioFecha,
        finLugar = finLugar,
        finHora = finHora,
        finFecha = finFecha,
        unidadResponsable = unidadResponsable,
        unidadTelefono = unidadTelefono,
        agenteTip = agenteTip,
        agenteUnidad = agenteUnidad
    )

    fun resetForm() {
        sector = ""
        subsector = ""
        destacamento = ""
        vehiculoTipo = ""
        vehiculoMarca = ""
        vehiculoModelo = ""
        vehiculoColor = ""
        vehiculoMatricula = ""
        esTitular = true
        esConductor = false
        nombre = ""
        primerApellido = ""
        segundoApellido = ""
        dniNie = ""
        telefono = ""
        inicioActuacionSeleccionada = null
        inicioOtroMotivo = ""
        circunstanciaSeleccionada = null
        circunstanciaConfiguracionVia = ""
        circunstanciaOtras = ""
        inicioLugar = ""
        inicioHora = ""
        inicioFecha = ""
        finLugar = ""
        finHora = ""
        finFecha = ""
        unidadResponsable = ""
        unidadTelefono = ""
        agenteTip = ""
        agenteUnidad = ""
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
                    text = stringResource(R.string.acta_traslado_title),
                    style = MaterialTheme.typography.titleMedium
                )

                SectionTitle(text = stringResource(R.string.acta_traslado_unidad_title))
                OutlinedTextField(value = sector, onValueChange = { sector = it.uppercase() }, label = { Text(stringResource(R.string.acta_traslado_sector)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = subsector, onValueChange = { subsector = it.uppercase() }, label = { Text(stringResource(R.string.acta_traslado_subsector)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = destacamento, onValueChange = { destacamento = it.uppercase() }, label = { Text(stringResource(R.string.acta_traslado_destacamento)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(text = stringResource(R.string.acta_traslado_vehiculo_title))
                OutlinedTextField(value = vehiculoTipo, onValueChange = { vehiculoTipo = it.uppercase() }, label = { Text(stringResource(R.string.acta_traslado_vehiculo_tipo)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = vehiculoMarca, onValueChange = { vehiculoMarca = it.uppercase() }, label = { Text(stringResource(R.string.acta_traslado_vehiculo_marca)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = vehiculoModelo, onValueChange = { vehiculoModelo = it.uppercase() }, label = { Text(stringResource(R.string.acta_traslado_vehiculo_modelo)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = vehiculoColor, onValueChange = { vehiculoColor = it.uppercase() }, label = { Text(stringResource(R.string.acta_traslado_vehiculo_color)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = vehiculoMatricula, onValueChange = { vehiculoMatricula = it.uppercase() }, label = { Text(stringResource(R.string.acta_traslado_vehiculo_matricula)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(text = stringResource(R.string.acta_traslado_persona_title))
                CheckboxOptionRow(text = stringResource(R.string.acta_traslado_persona_titular), checked = esTitular) { checked ->
                    esTitular = checked
                    if (checked) esConductor = false
                }
                CheckboxOptionRow(text = stringResource(R.string.acta_traslado_persona_conductor), checked = esConductor) { checked ->
                    esConductor = checked
                    if (checked) esTitular = false
                }
                OutlinedTextField(value = nombre, onValueChange = { nombre = it.uppercase() }, label = { Text(stringResource(R.string.acta_traslado_nombre)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = primerApellido, onValueChange = { primerApellido = it.uppercase() }, label = { Text(stringResource(R.string.acta_traslado_primer_apellido)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = segundoApellido, onValueChange = { segundoApellido = it.uppercase() }, label = { Text(stringResource(R.string.acta_traslado_segundo_apellido)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = dniNie, onValueChange = { dniNie = it.uppercase() }, label = { Text(stringResource(R.string.acta_traslado_dni_nie)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text(stringResource(R.string.acta_traslado_telefono)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(text = stringResource(R.string.acta_traslado_inicio_actuacion_title))
                OptionRadioRow(
                    text = stringResource(R.string.acta_traslado_inicio_auxilio),
                    selected = inicioActuacionSeleccionada == InicioActuacionOption.AUXILIO_ACCIDENTE,
                    onSelect = {
                        inicioActuacionSeleccionada = InicioActuacionOption.AUXILIO_ACCIDENTE
                        inicioOtroMotivo = ""
                    }
                )
                OptionRadioRow(
                    text = stringResource(R.string.acta_traslado_inicio_infraccion),
                    selected = inicioActuacionSeleccionada == InicioActuacionOption.INFRACCION,
                    onSelect = {
                        inicioActuacionSeleccionada = InicioActuacionOption.INFRACCION
                        inicioOtroMotivo = ""
                    }
                )
                OptionRadioRow(
                    text = stringResource(R.string.acta_traslado_inicio_otro_motivo_option),
                    selected = inicioActuacionSeleccionada == InicioActuacionOption.OTRO_MOTIVO,
                    onSelect = { inicioActuacionSeleccionada = InicioActuacionOption.OTRO_MOTIVO }
                )
                if (inicioActuacionSeleccionada == InicioActuacionOption.OTRO_MOTIVO) {
                    OutlinedTextField(
                        value = inicioOtroMotivo,
                        onValueChange = { inicioOtroMotivo = it },
                        label = { Text(stringResource(R.string.acta_traslado_inicio_otro_motivo)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(text = stringResource(R.string.acta_traslado_circunstancias_title))
                OptionRadioRow(
                    text = stringResource(R.string.acta_traslado_circ_atmosfericos),
                    selected = circunstanciaSeleccionada == CircunstanciaTrasladoOption.FACTORES_ATMOSFERICOS,
                    onSelect = {
                        circunstanciaSeleccionada = CircunstanciaTrasladoOption.FACTORES_ATMOSFERICOS
                        circunstanciaConfiguracionVia = ""
                        circunstanciaOtras = ""
                    }
                )
                OptionRadioRow(
                    text = stringResource(R.string.acta_traslado_circ_visibilidad),
                    selected = circunstanciaSeleccionada == CircunstanciaTrasladoOption.MALA_VISIBILIDAD,
                    onSelect = {
                        circunstanciaSeleccionada = CircunstanciaTrasladoOption.MALA_VISIBILIDAD
                        circunstanciaConfiguracionVia = ""
                        circunstanciaOtras = ""
                    }
                )
                OptionRadioRow(
                    text = stringResource(R.string.acta_traslado_circ_configuracion_via_option),
                    selected = circunstanciaSeleccionada == CircunstanciaTrasladoOption.CONFIGURACION_VIA,
                    onSelect = {
                        circunstanciaSeleccionada = CircunstanciaTrasladoOption.CONFIGURACION_VIA
                        circunstanciaOtras = ""
                    }
                )
                if (circunstanciaSeleccionada == CircunstanciaTrasladoOption.CONFIGURACION_VIA) {
                    OutlinedTextField(
                        value = circunstanciaConfiguracionVia,
                        onValueChange = { circunstanciaConfiguracionVia = it },
                        label = { Text(stringResource(R.string.acta_traslado_circ_configuracion_via)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OptionRadioRow(
                    text = stringResource(R.string.acta_traslado_circ_otras_option),
                    selected = circunstanciaSeleccionada == CircunstanciaTrasladoOption.OTRAS,
                    onSelect = {
                        circunstanciaSeleccionada = CircunstanciaTrasladoOption.OTRAS
                        circunstanciaConfiguracionVia = ""
                    }
                )
                if (circunstanciaSeleccionada == CircunstanciaTrasladoOption.OTRAS) {
                    OutlinedTextField(
                        value = circunstanciaOtras,
                        onValueChange = { circunstanciaOtras = it },
                        label = { Text(stringResource(R.string.acta_traslado_circ_otras)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(text = stringResource(R.string.acta_traslado_detalle_traslado_title))
                OutlinedTextField(value = inicioLugar, onValueChange = { inicioLugar = it.uppercase() }, label = { Text(stringResource(R.string.acta_traslado_inicio_lugar)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    OutlinedTextField(
                        value = inicioHora,
                        onValueChange = { inicioHora = applyTimeMask(it) },
                        label = { Text(stringResource(R.string.acta_traslado_inicio_hora)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("HH:mm") }
                    )
                    IconButton(
                        onClick = {
                            val parts = inicioHora.split(":")
                            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
                            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                            TimePickerDialog(context, { _, hour, minute ->
                                inicioHora = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                            }, h, m, true).show()
                        },
                        modifier = Modifier.size(48.dp).padding(top = 8.dp).align(Alignment.Top)
                    ) {
                        AssetImage(assetPath = "icons/clock.png", contentDescription = stringResource(R.string.acta_traslado_select_time), modifier = Modifier.fillMaxSize())
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    OutlinedTextField(
                        value = inicioFecha,
                        onValueChange = { inicioFecha = applyDateMask(it) },
                        label = { Text(stringResource(R.string.acta_traslado_inicio_fecha)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("dd-MM-yyyy") }
                    )
                    IconButton(
                        onClick = { showInicioDatePicker = true },
                        modifier = Modifier.size(48.dp).padding(top = 8.dp).align(Alignment.Top)
                    ) {
                        AssetImage(assetPath = "icons/calendar.png", contentDescription = stringResource(R.string.acta_traslado_select_date), modifier = Modifier.fillMaxSize())
                    }
                }
                OutlinedTextField(value = finLugar, onValueChange = { finLugar = it.uppercase() }, label = { Text(stringResource(R.string.acta_traslado_fin_lugar)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    OutlinedTextField(
                        value = finHora,
                        onValueChange = { finHora = applyTimeMask(it) },
                        label = { Text(stringResource(R.string.acta_traslado_fin_hora)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("HH:mm") }
                    )
                    IconButton(
                        onClick = {
                            val parts = finHora.split(":")
                            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
                            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                            TimePickerDialog(context, { _, hour, minute ->
                                finHora = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                            }, h, m, true).show()
                        },
                        modifier = Modifier.size(48.dp).padding(top = 8.dp).align(Alignment.Top)
                    ) {
                        AssetImage(assetPath = "icons/clock.png", contentDescription = stringResource(R.string.acta_traslado_select_time), modifier = Modifier.fillMaxSize())
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    OutlinedTextField(
                        value = finFecha,
                        onValueChange = { finFecha = applyDateMask(it) },
                        label = { Text(stringResource(R.string.acta_traslado_fin_fecha)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("dd-MM-yyyy") }
                    )
                    IconButton(
                        onClick = { showFinDatePicker = true },
                        modifier = Modifier.size(48.dp).padding(top = 8.dp).align(Alignment.Top)
                    ) {
                        AssetImage(assetPath = "icons/calendar.png", contentDescription = stringResource(R.string.acta_traslado_select_date), modifier = Modifier.fillMaxSize())
                    }
                }
                OutlinedTextField(value = unidadResponsable, onValueChange = { unidadResponsable = it.uppercase() }, label = { Text(stringResource(R.string.acta_traslado_unidad_responsable)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = unidadTelefono, onValueChange = { unidadTelefono = it }, label = { Text(stringResource(R.string.acta_traslado_unidad_telefono)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = agenteTip, onValueChange = { agenteTip = it.uppercase() }, label = { Text(stringResource(R.string.acta_traslado_agente_tip)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = agenteUnidad, onValueChange = { agenteUnidad = it.uppercase() }, label = { Text(stringResource(R.string.acta_traslado_agente_unidad)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        storage?.saveCurrent(buildCurrentData())
                        onContinueClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White)
                ) {
                    Text(stringResource(R.string.acta_traslado_continue))
                }

                Button(
                    onClick = {
                        resetForm()
                        storage?.clearCurrent()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020), contentColor = Color.White)
                ) {
                    Text(stringResource(R.string.acta_traslado_clear))
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

    if (showInicioDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = inicioFecha.toTrasladoDateMillisOrNull()
        )
        DatePickerDialog(
            onDismissRequest = { showInicioDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.toTrasladoFormattedDate()?.let { inicioFecha = it }
                    showInicioDatePicker = false
                }) { Text(stringResource(R.string.person_data_birth_date_select_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showInicioDatePicker = false }) { Text(stringResource(R.string.no_option)) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showFinDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = finFecha.toTrasladoDateMillisOrNull()
        )
        DatePickerDialog(
            onDismissRequest = { showFinDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.toTrasladoFormattedDate()?.let { finFecha = it }
                    showFinDatePicker = false
                }) { Text(stringResource(R.string.person_data_birth_date_select_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showFinDatePicker = false }) { Text(stringResource(R.string.no_option)) }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

@Preview(showBackground = true)
@Composable
private fun ActaTrasladoVehiculoScreenPreview() {
    SinCarnetTheme {
        ActaTrasladoVehiculoScreen()
    }
}
