@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.oscar.sincarnet

import com.oscar.sincarnet.presentation.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oscar.sincarnet.ui.theme.SinCarnetTheme

/** Catálogo de empleos de Guardia Civil para selección guiada. */
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
/**
 * Pantalla de datos de actuantes (instructor y secretario).
 *
 * Permite editar empleo, TIP y unidad de ambos actuantes, reutilizar historial
 * de entradas frecuentes y recuperar borrado reciente.
 *
 * @param modifier Modificador raíz
 * @param onBackClick Vuelve a la pantalla anterior
 * @param onSaveClick Guarda datos de actuantes
 * @param onDeleteClick Borra datos actuales conservando backup recuperable
 * @param onRecoverClick Recupera el último backup disponible
 */
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

        AdaptiveActionRow(
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
        AccessibleAlertDialog(
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
    var tipValue by remember { mutableStateOf(TextFieldValue(tip)) }
    var unitValue by remember { mutableStateOf(TextFieldValue(unit)) }

    LaunchedEffect(tip) {
        if (tipValue.text != tip) {
            tipValue = TextFieldValue(tip, selection = TextRange(tip.length))
        }
    }
    LaunchedEffect(unit) {
        if (unitValue.text != unit) {
            unitValue = TextFieldValue(unit, selection = TextRange(unit.length))
        }
    }

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
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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

            val clearFieldDescription = stringResource(R.string.atestado_acting_clear_field)

            HistoryDropdownTextField(
                value = tipValue,
                onValueChange = { newValue ->
                    tipValue = newValue
                    onTipChange(newValue.text)
                },
                label = stringResource(R.string.atestado_acting_tip),
                history = tipHistory,
                clearContentDescription = clearFieldDescription,
                showHistory = false,
                sanitize = ::sanitizeTipInput,
                isError = tipValue.text.isNotEmpty() && !isTipValid(tipValue.text),
                supportingText = if (tipValue.text.isNotEmpty() && !isTipValid(tipValue.text)) {
                    { Text(text = stringResource(R.string.atestado_acting_tip_format_hint)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
            )

            HistoryDropdownTextField(
                value = unitValue,
                onValueChange = { newValue ->
                    unitValue = newValue
                    onUnitChange(newValue.text)
                },
                label = stringResource(R.string.atestado_acting_unit),
                history = unitHistory,
                clearContentDescription = clearFieldDescription,
                enabled = unitEnabled
            )

            footer?.invoke()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDropdownTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    history: List<String>,
    clearContentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    sanitize: ((String) -> String)? = null,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    showHistory: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val suggestions = if (showHistory) {
        history.filter { it.startsWith(value.text, ignoreCase = true) && it != value.text }
    } else {
        emptyList()
    }
    val menuVisible = showHistory && expanded && suggestions.isNotEmpty()

    val clearButton: @Composable () -> Unit = {
        if (enabled && value.text.isNotEmpty()) {
            IconButton(
                onClick = {
                    currentOnValueChange(TextFieldValue(""))
                    expanded = false
                },
                modifier = Modifier
                    .size(40.dp)
                    .semantics { this.contentDescription = clearContentDescription }
            ) {
                Text(
                    text = "×",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (!showHistory) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                val sanitized = sanitize?.invoke(newValue.text) ?: newValue.text
                val adjusted = if (sanitized == newValue.text) {
                    newValue
                } else {
                    TextFieldValue(sanitized, selection = TextRange(sanitized.length))
                }
                currentOnValueChange(adjusted)
            },
            modifier = modifier
                .fillMaxWidth()
                .selectAllOnDoubleTap {
                    val current = currentValue
                    if (current.text.isNotEmpty()) {
                        currentOnValueChange(
                            current.copy(selection = TextRange(0, current.text.length))
                        )
                    }
                },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            isError = isError,
            label = { Text(text = label) },
            trailingIcon = clearButton,
            supportingText = supportingText
        )
        return
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                val sanitized = sanitize?.invoke(newValue.text) ?: newValue.text
                val adjusted = if (sanitized == newValue.text) {
                    newValue
                } else {
                    TextFieldValue(sanitized, selection = TextRange(sanitized.length))
                }
                currentOnValueChange(adjusted)
                expanded = adjusted.text.isNotEmpty()
            },
            modifier = modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .fillMaxWidth()
                .selectAllOnDoubleTap {
                    val current = currentValue
                    if (current.text.isNotEmpty()) {
                        currentOnValueChange(
                            current.copy(selection = TextRange(0, current.text.length))
                        )
                    }
                },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            isError = isError,
            label = { Text(text = label) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    clearButton()
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuVisible)
                }
            },
            supportingText = supportingText
        )

        androidx.compose.material3.DropdownMenu(
            expanded = menuVisible,
            onDismissRequest = { expanded = false }
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(text = suggestion) },
                    onClick = {
                        currentOnValueChange(
                            TextFieldValue(suggestion, selection = TextRange(suggestion.length))
                        )
                        expanded = false
                    }
                )
            }
        }
    }
}

private const val DOUBLE_TAP_TIMEOUT_MS = 400L

private fun Modifier.selectAllOnDoubleTap(onDoubleTap: () -> Unit): Modifier =
    pointerInput(Unit) {
        awaitPointerEventScope {
            var lastTapUpUptime = 0L
            var pendingSelectAll = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val now = event.changes.firstOrNull()?.uptimeMillis ?: continue
                when (event.type) {
                    PointerEventType.Press -> {
                        if (lastTapUpUptime != 0L && now - lastTapUpUptime <= DOUBLE_TAP_TIMEOUT_MS) {
                            pendingSelectAll = true
                            lastTapUpUptime = 0L
                        }
                    }
                    PointerEventType.Release -> {
                        lastTapUpUptime = now
                        if (pendingSelectAll) {
                            pendingSelectAll = false
                            onDoubleTap()
                        }
                    }
                }
            }
        }
    }

internal fun sanitizeTipInput(input: String): String =
    input.uppercase()
        .filter { it.isLetterOrDigit() }
        .take(TIP_MAX_LENGTH)

private fun isTipValid(tip: String): Boolean = TIP_REGEX.matches(tip)

@Preview(showBackground = true)
@Composable
private fun DatosActuantesScreenPreview() {
    SinCarnetTheme {
        DatosActuantesScreen()
    }
}

