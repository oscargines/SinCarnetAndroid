package com.oscar.sincarnet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oscar.sincarnet.ui.theme.SinCarnetTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val NATIONALITY_FALLBACK = listOf("España")
private val PERSON_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatosPersonaInvestigadaScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    nationality: String = "España",
    onNationalityChange: (String) -> Unit = {},
    sex: String = "Desconocido",
    onSexChange: (String) -> Unit = {},
    firstName: String = "",
    onFirstNameChange: (String) -> Unit = {},
    lastName1: String = "",
    onLastName1Change: (String) -> Unit = {},
    lastName2: String = "",
    onLastName2Change: (String) -> Unit = {},
    address: String = "",
    onAddressChange: (String) -> Unit = {},
    birthDate: String = "",
    onBirthDateChange: (String) -> Unit = {},
    birthPlace: String = "",
    onBirthPlaceChange: (String) -> Unit = {},
    fatherName: String = "",
    onFatherNameChange: (String) -> Unit = {},
    motherName: String = "",
    onMotherNameChange: (String) -> Unit = {},
    phone: String = "",
    onPhoneChange: (String) -> Unit = {},
    email: String = "",
    onEmailChange: (String) -> Unit = {},
    onSaveClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onRightsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    var nationalityOptions by remember { mutableStateOf(NATIONALITY_FALLBACK) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var showSaveConfirmation by rememberSaveable { mutableStateOf(false) }
    var showRequiredFieldsError by rememberSaveable { mutableStateOf(false) }
    var requiredFieldsErrorText by rememberSaveable { mutableStateOf("") }
    var showBirthDatePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (isInPreview) {
            nationalityOptions = NATIONALITY_FALLBACK
            return@LaunchedEffect
        }

        val result = runCatching {
            withContext(Dispatchers.IO) { loadNationalitiesFromDatabase(context) }
        }

        nationalityOptions = result.getOrElse { NATIONALITY_FALLBACK }
    }

    val sexOptions = listOf(
        stringResource(R.string.person_data_sex_male),
        stringResource(R.string.person_data_sex_female),
        stringResource(R.string.person_data_sex_unknown)
    )
    val nationalityLabel = stringResource(R.string.person_data_nationality)
    val sexLabel = stringResource(R.string.person_data_sex)
    val nameLabel = stringResource(R.string.person_data_name)
    val lastName1Label = stringResource(R.string.person_data_last_name_1)
    val lastName2Label = stringResource(R.string.person_data_last_name_2)
    val addressLabel = stringResource(R.string.person_data_address)
    val birthDateLabel = stringResource(R.string.person_data_birth_date)
    val birthPlaceLabel = stringResource(R.string.person_data_birth_place)
    val fatherNameLabel = stringResource(R.string.person_data_father_name)
    val motherNameLabel = stringResource(R.string.person_data_mother_name)
    val phoneLabel = stringResource(R.string.person_data_phone)
    val emailLabel = stringResource(R.string.person_data_email)

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
                    text = stringResource(R.string.person_data_title),
                    style = MaterialTheme.typography.titleMedium
                )

                DropdownField(
                    label = stringResource(R.string.person_data_nationality),
                    value = nationality,
                    options = nationalityOptions,
                    onValueSelected = onNationalityChange
                )

                DropdownField(
                    label = stringResource(R.string.person_data_sex),
                    value = sex,
                    options = sexOptions,
                    onValueSelected = onSexChange
                )

                OutlinedTextField(
                    value = firstName,
                    onValueChange = { onFirstNameChange(it.uppercase()) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.person_data_name)) }
                )

                OutlinedTextField(
                    value = lastName1,
                    onValueChange = { onLastName1Change(it.uppercase()) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.person_data_last_name_1)) }
                )

                OutlinedTextField(
                    value = lastName2,
                    onValueChange = { onLastName2Change(it.uppercase()) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.person_data_last_name_2)) }
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = onAddressChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.person_data_address)) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = {},
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        singleLine = true,
                        label = { Text(stringResource(R.string.person_data_birth_date)) },
                        supportingText = { Text(stringResource(R.string.person_data_birth_date_hint)) }
                    )

                    IconButton(
                        onClick = { showBirthDatePicker = true },
                        modifier = Modifier
                            .size(65.dp)
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

                OutlinedTextField(
                    value = birthPlace,
                    onValueChange = onBirthPlaceChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.person_data_birth_place)) }
                )

                OutlinedTextField(
                    value = fatherName,
                    onValueChange = onFatherNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.person_data_father_name)) }
                )

                OutlinedTextField(
                    value = motherName,
                    onValueChange = onMotherNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.person_data_mother_name)) }
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    label = { Text(stringResource(R.string.person_data_phone)) }
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    label = { Text(stringResource(R.string.person_data_email)) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val requiredFields = listOf(
                        nationalityLabel to nationality,
                        sexLabel to sex,
                        nameLabel to firstName,
                        lastName1Label to lastName1,
                        lastName2Label to lastName2,
                        addressLabel to address,
                        birthDateLabel to birthDate,
                        birthPlaceLabel to birthPlace,
                        fatherNameLabel to fatherName,
                        motherNameLabel to motherName,
                        phoneLabel to phone,
                        emailLabel to email
                    )

                    val missingFieldNames = requiredFields
                        .filter { it.second.trim().isEmpty() }
                        .map { it.first }

                    if (missingFieldNames.isNotEmpty()) {
                        requiredFieldsErrorText = missingFieldNames.joinToString(", ")
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
                Text(text = stringResource(R.string.person_data_save))
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
                Text(text = stringResource(R.string.person_data_delete))
            }

            Button(
                onClick = onRightsClick,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = stringResource(R.string.person_data_rights))
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

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(text = stringResource(R.string.person_data_delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.person_data_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteClick()
                    }
                ) {
                    Text(text = stringResource(R.string.person_data_delete))
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
            title = { Text(text = stringResource(R.string.person_data_save_confirm_title)) },
            text = { Text(text = stringResource(R.string.person_data_save_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { showSaveConfirmation = false }) {
                    Text(text = stringResource(R.string.close_action))
                }
            }
        )
    }

    if (showRequiredFieldsError) {
        AlertDialog(
            onDismissRequest = { showRequiredFieldsError = false },
            title = { Text(text = stringResource(R.string.person_data_required_fields_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.person_data_required_fields_message,
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

    if (showBirthDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = birthDate.toDateMillisOrNull()
        )

        DatePickerDialog(
            onDismissRequest = { showBirthDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis
                            ?.toFormattedBirthDate()
                            ?.let(onBirthDateChange)
                        showBirthDatePicker = false
                    }
                ) {
                    Text(text = stringResource(R.string.person_data_birth_date_select_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBirthDatePicker = false }) {
                    Text(text = stringResource(R.string.no_option))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}


private fun Long.toFormattedBirthDate(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(PERSON_DATE_FORMATTER)

private fun String.toDateMillisOrNull(): Long? = runCatching {
    LocalDate.parse(this, PERSON_DATE_FORMATTER)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}.getOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
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
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )

        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
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

@Preview(showBackground = true)
@Composable
private fun DatosPersonaInvestigadaScreenPreview() {
    SinCarnetTheme {
        DatosPersonaInvestigadaScreen()
    }
}

