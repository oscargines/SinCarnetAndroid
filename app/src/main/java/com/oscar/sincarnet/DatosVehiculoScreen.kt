package com.oscar.sincarnet

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.oscar.sincarnet.ui.theme.SinCarnetTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val VEHICLE_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatosVehiculoScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    brand: String = "",
    onBrandChange: (String) -> Unit = {},
    model: String = "",
    onModelChange: (String) -> Unit = {},
    plate: String = "",
    onPlateChange: (String) -> Unit = {},
    registrationDate: String = "",
    onRegistrationDateChange: (String) -> Unit = {},
    nationality: String = "España",
    onNationalityChange: (String) -> Unit = {},
    itvDate: String = "",
    onItvDateChange: (String) -> Unit = {},
    insurer: String = "",
    onInsurerChange: (String) -> Unit = {},
    vehicleType: String = "",
    onVehicleTypeChange: (String) -> Unit = {},
    clasePermiso: String = "",
    onClasePermisoChange: (String) -> Unit = {},
    ownerIsOther: Boolean = false,
    onOwnerIsOtherChange: (Boolean) -> Unit = {},
    ownerName: String = "",
    onOwnerNameChange: (String) -> Unit = {},
    ownerLastNames: String = "",
    onOwnerLastNamesChange: (String) -> Unit = {},
    ownerDni: String = "",
    onOwnerDniChange: (String) -> Unit = {},
    ownerAddress: String = "",
    onOwnerAddressChange: (String) -> Unit = {},
    ownerPhone: String = "",
    onOwnerPhoneChange: (String) -> Unit = {},
    onSaveClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    val isInPreview = LocalInspectionMode.current

    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var showSaveConfirmation by rememberSaveable { mutableStateOf(false) }
    var showRequiredFieldsError by rememberSaveable { mutableStateOf(false) }
    var requiredFieldsErrorText by rememberSaveable { mutableStateOf("") }
    var showRegistrationDatePicker by rememberSaveable { mutableStateOf(false) }
    var showItvDatePicker by rememberSaveable { mutableStateOf(false) }

    val vehicleTypeOptions = listOf(
        stringResource(R.string.vehicle_type_car),
        stringResource(R.string.vehicle_type_motorcycle),
        stringResource(R.string.vehicle_type_moped),
        stringResource(R.string.vehicle_type_van),
        stringResource(R.string.vehicle_type_truck),
        stringResource(R.string.vehicle_type_bus),
        stringResource(R.string.vehicle_type_trailer),
        stringResource(R.string.vehicle_type_semi),
        stringResource(R.string.vehicle_type_tractor),
        stringResource(R.string.vehicle_type_quadricycle),
        stringResource(R.string.vehicle_type_special),
        stringResource(R.string.vehicle_type_other)
    )

    val clasePermisoOptions = listOf(
        "Lic. Ciclo.", "AM", "A1", "A2", "A", "B1", "B", "C1", "C", "D1", "D",
        "B+E", "C1+E", "C+E", "D1+E", "D+E"
    )

    val brandLabel = stringResource(R.string.vehicle_brand)
    val modelLabel = stringResource(R.string.vehicle_model)
    val plateLabel = stringResource(R.string.vehicle_plate)
    val regDateLabel = stringResource(R.string.vehicle_registration_date)
    val nationalityLabel = stringResource(R.string.vehicle_nationality)
    val vehicleTypeLabel = stringResource(R.string.vehicle_type)
    val clasePermisoLabel = stringResource(R.string.vehicle_required_license)
    val ownerNameLabel = stringResource(R.string.vehicle_owner_name)
    val ownerLastNamesLabel = stringResource(R.string.vehicle_owner_last_names)
    val ownerDniLabel = stringResource(R.string.vehicle_owner_dni)
    val ownerAddressLabel = stringResource(R.string.vehicle_owner_address)
    val ownerPhoneLabel = stringResource(R.string.vehicle_owner_phone)

    // Nacionalidades reutilizando mismo datasource que persona investigada
    var nationalityOptions by androidx.compose.runtime.remember { mutableStateOf(listOf("España")) }
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!isInPreview) {
            val result = runCatching {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    loadNationalitiesFromDatabase(context)
                }
            }
            nationalityOptions = result.getOrElse { listOf("España") }
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.vehicle_data_title),
                    style = MaterialTheme.typography.titleMedium
                )

                // Marca
                OutlinedTextField(
                    value = brand,
                    onValueChange = { onBrandChange(it.uppercase()) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.vehicle_brand)) }
                )

                // Modelo
                OutlinedTextField(
                    value = model,
                    onValueChange = { onModelChange(it.uppercase()) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.vehicle_model)) }
                )

                // Matrícula
                OutlinedTextField(
                    value = plate,
                    onValueChange = { onPlateChange(it.uppercase()) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.vehicle_plate)) }
                )

                // Fecha de matriculación
                VehicleDateRow(
                    label = stringResource(R.string.vehicle_registration_date),
                    value = registrationDate,
                    onPickerClick = { showRegistrationDatePicker = true }
                )

                // Nacionalidad del vehículo
                VehicleDropdownField(
                    label = stringResource(R.string.vehicle_nationality),
                    value = nationality,
                    options = nationalityOptions,
                    onValueSelected = onNationalityChange
                )

                // Tipo de vehículo
                VehicleDropdownField(
                    label = stringResource(R.string.vehicle_type),
                    value = vehicleType,
                    options = vehicleTypeOptions,
                    onValueSelected = onVehicleTypeChange
                )

                // Permiso necesario conducción
                VehicleDropdownField(
                    label = stringResource(R.string.vehicle_required_license),
                    value = clasePermiso,
                    options = clasePermisoOptions,
                    onValueSelected = onClasePermisoChange
                )

                // Fecha de ITV (opcional)
                VehicleDateRow(
                    label = stringResource(R.string.vehicle_itv_date),
                    value = itvDate,
                    onPickerClick = { showItvDatePicker = true },
                    isOptional = true
                )

                // Aseguradora (opcional)
                OutlinedTextField(
                    value = insurer,
                    onValueChange = onInsurerChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.vehicle_insurer)) },
                    supportingText = { Text(stringResource(R.string.vehicle_optional)) }
                )

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))

                // Propietario distinto al investigado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = ownerIsOther,
                        onCheckedChange = onOwnerIsOtherChange
                    )
                    Text(
                        text = stringResource(R.string.vehicle_owner_is_other),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (ownerIsOther) {
                    Text(
                        text = stringResource(R.string.vehicle_owner_section_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { onOwnerNameChange(it.uppercase()) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.vehicle_owner_name)) }
                    )

                    OutlinedTextField(
                        value = ownerLastNames,
                        onValueChange = { onOwnerLastNamesChange(it.uppercase()) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.vehicle_owner_last_names)) }
                    )

                    OutlinedTextField(
                        value = ownerDni,
                        onValueChange = { onOwnerDniChange(it.uppercase()) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.vehicle_owner_dni)) }
                    )

                    OutlinedTextField(
                        value = ownerAddress,
                        onValueChange = onOwnerAddressChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.vehicle_owner_address)) }
                    )

                    OutlinedTextField(
                        value = ownerPhone,
                        onValueChange = onOwnerPhoneChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        label = { Text(stringResource(R.string.vehicle_owner_phone)) }
                    )
                }
            }
        }

        // Botones de acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val requiredFields = mutableListOf(
                        brandLabel to brand,
                        modelLabel to model,
                        plateLabel to plate,
                        regDateLabel to registrationDate,
                        nationalityLabel to nationality,
                        vehicleTypeLabel to vehicleType,
                        clasePermisoLabel to clasePermiso
                    )

                    if (ownerIsOther) {
                        requiredFields += listOf(
                            ownerNameLabel to ownerName,
                            ownerLastNamesLabel to ownerLastNames,
                            ownerDniLabel to ownerDni,
                            ownerAddressLabel to ownerAddress,
                            ownerPhoneLabel to ownerPhone
                        )
                    }

                    val missing = requiredFields
                        .filter { it.second.trim().isEmpty() }
                        .map { it.first }

                    if (missing.isNotEmpty()) {
                        requiredFieldsErrorText = missing.joinToString(", ")
                        showRequiredFieldsError = true
                    } else {
                        onSaveClick()
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
                Text(text = stringResource(R.string.vehicle_save))
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
                Text(text = stringResource(R.string.vehicle_delete))
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

    // Diálogo: confirmación borrado
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.vehicle_delete_confirm_title)) },
            text = { Text(stringResource(R.string.vehicle_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onDeleteClick()
                }) { Text(stringResource(R.string.vehicle_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.no_option))
                }
            }
        )
    }

    // Diálogo: datos guardados
    if (showSaveConfirmation) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmation = false },
            title = { Text(stringResource(R.string.vehicle_save_confirm_title)) },
            text = { Text(stringResource(R.string.vehicle_save_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { showSaveConfirmation = false }) {
                    Text(stringResource(R.string.close_action))
                }
            }
        )
    }

    // Diálogo: campos obligatorios
    if (showRequiredFieldsError) {
        AlertDialog(
            onDismissRequest = { showRequiredFieldsError = false },
            title = { Text(stringResource(R.string.vehicle_required_fields_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.vehicle_required_fields_message,
                        requiredFieldsErrorText
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { showRequiredFieldsError = false }) {
                    Text(stringResource(R.string.accept_action))
                }
            }
        )
    }

    // DatePicker: fecha de matriculación
    if (showRegistrationDatePicker) {
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = registrationDate.toVehicleDateMillisOrNull()
        )
        DatePickerDialog(
            onDismissRequest = { showRegistrationDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.toFormattedVehicleDate()?.let(onRegistrationDateChange)
                    showRegistrationDatePicker = false
                }) { Text(stringResource(R.string.vehicle_date_select)) }
            },
            dismissButton = {
                TextButton(onClick = { showRegistrationDatePicker = false }) {
                    Text(stringResource(R.string.no_option))
                }
            }
        ) { DatePicker(state = dpState) }
    }

    // DatePicker: fecha de ITV
    if (showItvDatePicker) {
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = itvDate.toVehicleDateMillisOrNull()
        )
        DatePickerDialog(
            onDismissRequest = { showItvDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.toFormattedVehicleDate()?.let(onItvDateChange)
                    showItvDatePicker = false
                }) { Text(stringResource(R.string.vehicle_date_select)) }
            },
            dismissButton = {
                TextButton(onClick = { showItvDatePicker = false }) {
                    Text(stringResource(R.string.no_option))
                }
            }
        ) { DatePicker(state = dpState) }
    }
}

@Composable
private fun VehicleDateRow(
    label: String,
    value: String,
    onPickerClick: () -> Unit,
    isOptional: Boolean = false
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
            label = { Text(label) },
            supportingText = if (isOptional) {
                { Text(stringResource(R.string.vehicle_optional)) }
            } else null
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
                contentDescription = stringResource(R.string.vehicle_date_select),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleDropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueSelected: (String) -> Unit
) {
    var expanded by androidx.compose.runtime.remember { mutableStateOf(false) }

    androidx.compose.material3.ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
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
            trailingIcon = {
                androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun Long.toFormattedVehicleDate(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(VEHICLE_DATE_FORMATTER)

private fun String.toVehicleDateMillisOrNull(): Long? = runCatching {
    LocalDate.parse(this, VEHICLE_DATE_FORMATTER)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}.getOrNull()

@Preview(showBackground = true)
@Composable
private fun DatosVehiculoScreenPreview() {
    SinCarnetTheme {
        DatosVehiculoScreen()
    }
}

