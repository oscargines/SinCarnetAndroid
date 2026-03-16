package com.oscar.sincarnet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.oscar.sincarnet.ui.theme.SinCarnetTheme

private val EMPLEOS_GC = listOf(
    "Guardia Civil",
    "Guardia de 1ª",
    "Cabo",
    "Cabo 1º",
    "Cabo Mayor",
    "Sargento",
    "Sargento 1º",
    "Brigada",
    "Subteniente",
    "Suboficial Mayor",
    "Teniente",
    "Capitán",
    "Comandante",
    "Teniente Coronel",
    "Coronel"
)

private const val TIP_MAX_LENGTH = 7
private val TIP_REGEX = Regex("^[A-Z][0-9]{5}[A-Z]$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatosActuantesScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    instructorEmployment: String = "",
    onInstructorEmploymentChange: (String) -> Unit = {},
    instructorTip: String = "",
    onInstructorTipChange: (String) -> Unit = {},
    instructorUnit: String = "",
    onInstructorUnitChange: (String) -> Unit = {},
    secretaryEmployment: String = "",
    onSecretaryEmploymentChange: (String) -> Unit = {},
    secretaryTip: String = "",
    onSecretaryTipChange: (String) -> Unit = {},
    secretaryUnit: String = "",
    onSecretaryUnitChange: (String) -> Unit = {},
    sameUnit: Boolean = false,
    onSameUnitChange: (Boolean) -> Unit = {},
    tipHistory: List<String> = emptyList(),
    unitHistory: List<String> = emptyList(),
    onSaveClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onRecoverClick: () -> Unit = {},
    canRecover: Boolean = false,
    statusMessage: String? = null
) {
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

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
                    text = stringResource(R.string.atestado_acting_title),
                    style = MaterialTheme.typography.titleMedium
                )

                OfficerCard(
                    title = stringResource(R.string.atestado_acting_instructor_title),
                    employment = instructorEmployment,
                    onEmploymentChange = onInstructorEmploymentChange,
                                    tip = instructorTip,
                                    onTipChange = onInstructorTipChange,
                                    unit = instructorUnit,
                                    onUnitChange = onInstructorUnitChange,
                                    unitEnabled = true,
                                    tipHistory = tipHistory,
                                    unitHistory = unitHistory,
                                    footer = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = sameUnit, onCheckedChange = onSameUnitChange)
                            Text(text = stringResource(R.string.atestado_acting_same_unit))
                        }
                    }
                )

                OfficerCard(
                    title = stringResource(R.string.atestado_acting_secretary_title),
                    employment = secretaryEmployment,
                    onEmploymentChange = onSecretaryEmploymentChange,
                                    tip = secretaryTip,
                                    onTipChange = onSecretaryTipChange,
                                    unit = secretaryUnit,
                                    onUnitChange = onSecretaryUnitChange,
                                    unitEnabled = !sameUnit,
                                    tipHistory = tipHistory,
                                    unitHistory = unitHistory
                )

                if (!statusMessage.isNullOrBlank()) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSaveClick,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = stringResource(R.string.atestado_acting_save))
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
                Text(text = stringResource(R.string.atestado_acting_delete))
            }

            Button(
                onClick = onRecoverClick,
                enabled = canRecover,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = stringResource(R.string.atestado_acting_recover))
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
            title = { Text(text = stringResource(R.string.atestado_acting_delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.atestado_acting_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteClick()
                    }
                ) {
                    Text(text = stringResource(R.string.atestado_acting_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(text = stringResource(R.string.no_option))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfficerCard(
    title: String,
    employment: String,
    onEmploymentChange: (String) -> Unit,
    tip: String,
    onTipChange: (String) -> Unit,
    unit: String,
    onUnitChange: (String) -> Unit,
    unitEnabled: Boolean,
    tipHistory: List<String>,
    unitHistory: List<String>,
    footer: @Composable (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = employment,
                    onValueChange = {},
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    readOnly = true,
                    label = { Text(text = stringResource(R.string.atestado_acting_employment)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )

                androidx.compose.material3.DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    EMPLEOS_GC.forEach { empleo ->
                        DropdownMenuItem(
                            text = { Text(text = empleo) },
                            onClick = {
                                onEmploymentChange(empleo)
                                expanded = false
                            }
                        )
                    }
                }
            }

                    var tipDropdownExpanded by remember { mutableStateOf(false) }
                    val filteredTipSuggestions = tipHistory.filter { it.startsWith(tip, ignoreCase = true) && it != tip }
                    ExposedDropdownMenuBox(
                        expanded = tipDropdownExpanded,
                        onExpandedChange = { tipDropdownExpanded = !tipDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = tip,
                            onValueChange = {
                                onTipChange(normalizeTipInput(it))
                                tipDropdownExpanded = true
                            },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                            isError = tip.isNotEmpty() && !isTipValid(tip),
                            label = { Text(text = stringResource(R.string.atestado_acting_tip)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tipDropdownExpanded) },
                            supportingText = {
                                if (tip.isNotEmpty() && !isTipValid(tip)) {
                                    Text(text = stringResource(R.string.atestado_acting_tip_format_hint))
                                }
                            }
                        )
                        androidx.compose.material3.DropdownMenu(
                            expanded = tipDropdownExpanded && filteredTipSuggestions.isNotEmpty(),
                            onDismissRequest = { tipDropdownExpanded = false }
                        ) {
                            filteredTipSuggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(text = suggestion) },
                                    onClick = {
                                        onTipChange(suggestion)
                                        tipDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    var unitDropdownExpanded by remember { mutableStateOf(false) }
                    val filteredUnitSuggestions = unitHistory.filter { it.startsWith(unit, ignoreCase = true) && it != unit }
                    ExposedDropdownMenuBox(
                        expanded = unitDropdownExpanded,
                        onExpandedChange = { unitDropdownExpanded = !unitDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {
                                onUnitChange(it)
                                unitDropdownExpanded = true
                            },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true,
                            enabled = unitEnabled,
                            label = { Text(text = stringResource(R.string.atestado_acting_unit)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) }
                        )
                        androidx.compose.material3.DropdownMenu(
                            expanded = unitDropdownExpanded && filteredUnitSuggestions.isNotEmpty(),
                            onDismissRequest = { unitDropdownExpanded = false }
                        ) {
                            filteredUnitSuggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(text = suggestion) },
                                    onClick = {
                                        onUnitChange(suggestion)
                                        unitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

            footer?.invoke()
        }
    }
}

private fun normalizeTipInput(input: String): String {
    val upper = input.uppercase()
    val result = StringBuilder(TIP_MAX_LENGTH)

    for (char in upper) {
        if (!char.isLetterOrDigit()) continue

        when (result.length) {
            0 -> if (char.isLetter()) result.append(char)
            in 1..5 -> if (char.isDigit()) result.append(char)
            6 -> if (char.isLetter()) result.append(char)
        }

        if (result.length == TIP_MAX_LENGTH) break
    }

    return result.toString()
}

private fun isTipValid(tip: String): Boolean = TIP_REGEX.matches(tip)

@Preview(showBackground = true)
@Composable
private fun DatosActuantesScreenPreview() {
    SinCarnetTheme {
        DatosActuantesScreen()
    }
}

