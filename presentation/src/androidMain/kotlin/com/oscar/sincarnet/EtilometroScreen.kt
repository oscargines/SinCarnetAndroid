package com.oscar.sincarnet

import com.oscar.sincarnet.presentation.R

import android.app.TimePickerDialog
import android.widget.Toast
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oscar.sincarnet.data.repository.ComplementarioAlcoholStorage
import com.oscar.sincarnet.data.toStorage
import com.oscar.sincarnet.domain.model.ComplementarioAlcoholData
import com.oscar.sincarnet.ui.theme.SinCarnetTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EtilometroScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onContinueClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val storage = ComplementarioAlcoholStorage(context.toStorage("colaboracion_alcohol_storage"))
    val initialData = storage.loadCurrent()

    var etilometroTipo by rememberSaveable {
        mutableStateOf(
            initialData.run {
                if (initialData.pruebasAlcohol) {
                    context.getString(R.string.etilometro_tipo_precision)
                } else {
                    ""
                }
            }
        )
    }
    var etilometroMarca by rememberSaveable { mutableStateOf(initialData.etilometroMarca) }
    var etilometroModelo by rememberSaveable { mutableStateOf(initialData.etilometroModelo) }
    var etilometroNumeroSerie by rememberSaveable { mutableStateOf(initialData.etilometroNumeroSerie) }
    var etilometroHoraPrueba by rememberSaveable { mutableStateOf(initialData.etilometroHoraVerificacion) }
    var etilometroResultado by rememberSaveable {
        mutableStateOf(
            if (initialData.primeraPruebaResultado.isNotBlank() || initialData.segundaPruebaResultado.isNotBlank()) {
                "POSITIVO"
            } else {
                ""
            }
        )
    }
    var primeraPruebaHora by rememberSaveable { mutableStateOf(initialData.primeraPruebaHora) }
    var primeraPruebaResultado by rememberSaveable { mutableStateOf(initialData.primeraPruebaResultado) }
    var segundaPruebaHora by rememberSaveable { mutableStateOf(initialData.segundaPruebaHora) }
    var segundaPruebaResultado by rememberSaveable { mutableStateOf(initialData.segundaPruebaResultado) }
    var pruebaContraste by rememberSaveable { mutableStateOf(initialData.pruebaContrasteAlcohol) }

    var expandedTipo by rememberSaveable { mutableStateOf(false) }

    val tipoOptions = listOf(
        stringResource(R.string.etilometro_tipo_aproximacion),
        stringResource(R.string.etilometro_tipo_precision)
    )

    val saveAndContinue: () -> Unit = {
        when {
            etilometroTipo.isBlank() -> {
                Toast.makeText(context, context.getString(R.string.etilometro_select_tipo), Toast.LENGTH_SHORT).show()
            }
            etilometroResultado.isBlank() -> {
                Toast.makeText(context, context.getString(R.string.etilometro_select_resultado), Toast.LENGTH_SHORT).show()
            }
            pruebaContraste.isBlank() -> {
                Toast.makeText(context, context.getString(R.string.etilometro_select_contraste), Toast.LENGTH_SHORT).show()
            }
            else -> {
                val updatedData = initialData.copy(
                    etilometroMarca = etilometroMarca,
                    etilometroModelo = etilometroModelo,
                    etilometroNumeroSerie = etilometroNumeroSerie,
                    etilometroHoraVerificacion = etilometroHoraPrueba,
                    primeraPruebaHora = primeraPruebaHora,
                    primeraPruebaResultado = primeraPruebaResultado,
                    segundaPruebaHora = segundaPruebaHora,
                    segundaPruebaResultado = segundaPruebaResultado,
                    pruebaContrasteAlcohol = pruebaContraste
                )
                storage.saveCurrent(updatedData)
                onContinueClick()
            }
        }
    }

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
                    text = stringResource(R.string.etilometro_title),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )

                ExposedDropdownMenuBox(
                    expanded = expandedTipo,
                    onExpandedChange = { expandedTipo = !expandedTipo }
                ) {
                    OutlinedTextField(
                        value = etilometroTipo,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        singleLine = true,
                        label = { Text(stringResource(R.string.etilometro_tipo)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTipo,
                        onDismissRequest = { expandedTipo = false }
                    ) {
                        tipoOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    etilometroTipo = option
                                    expandedTipo = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = etilometroMarca,
                    onValueChange = { etilometroMarca = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.etilometro_marca)) }
                )

                OutlinedTextField(
                    value = etilometroModelo,
                    onValueChange = { etilometroModelo = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.etilometro_modelo)) }
                )

                OutlinedTextField(
                    value = etilometroNumeroSerie,
                    onValueChange = { etilometroNumeroSerie = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.etilometro_numero_serie)) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = etilometroHoraPrueba,
                        onValueChange = {},
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        singleLine = true,
                        label = { Text(stringResource(R.string.etilometro_hora_prueba)) }
                    )
                    IconButton(
                        onClick = {
                            val initialHour = etilometroHoraPrueba.split(":").getOrNull(0)?.toIntOrNull() ?: 0
                            val initialMinute = etilometroHoraPrueba.split(":").getOrNull(1)?.toIntOrNull() ?: 0
                            TimePickerDialog(
                                context,
                                { _, h, m ->
                                    etilometroHoraPrueba = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                                },
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
                            contentDescription = stringResource(R.string.etilometro_hora_prueba),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.etilometro_resultado),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = etilometroResultado == "POSITIVO",
                            onClick = { etilometroResultado = "POSITIVO" }
                        )
                        Text(text = stringResource(R.string.etilometro_resultado_positivo))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = etilometroResultado == "NEGATIVO",
                            onClick = { etilometroResultado = "NEGATIVO" }
                        )
                        Text(text = stringResource(R.string.etilometro_resultado_negativo))
                    }
                }

                if (etilometroResultado == "POSITIVO") {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.etilometro_primera_prueba_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        OutlinedTextField(
                            value = primeraPruebaHora,
                            onValueChange = {},
                            modifier = Modifier.weight(1f),
                            readOnly = true,
                            singleLine = true,
                            label = { Text(stringResource(R.string.etilometro_prueba_hora)) }
                        )
                        IconButton(
                            onClick = {
                                val initialHour = primeraPruebaHora.split(":").getOrNull(0)?.toIntOrNull() ?: 0
                                val initialMinute = primeraPruebaHora.split(":").getOrNull(1)?.toIntOrNull() ?: 0
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        primeraPruebaHora = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                                    },
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
                                contentDescription = stringResource(R.string.etilometro_prueba_hora),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    OutlinedTextField(
                        value = primeraPruebaResultado,
                        onValueChange = { primeraPruebaResultado = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.etilometro_prueba_resultado)) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.etilometro_segunda_prueba_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        OutlinedTextField(
                            value = segundaPruebaHora,
                            onValueChange = {},
                            modifier = Modifier.weight(1f),
                            readOnly = true,
                            singleLine = true,
                            label = { Text(stringResource(R.string.etilometro_prueba_hora)) }
                        )
                        IconButton(
                            onClick = {
                                val initialHour = segundaPruebaHora.split(":").getOrNull(0)?.toIntOrNull() ?: 0
                                val initialMinute = segundaPruebaHora.split(":").getOrNull(1)?.toIntOrNull() ?: 0
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        segundaPruebaHora = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                                    },
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
                                contentDescription = stringResource(R.string.etilometro_prueba_hora),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    OutlinedTextField(
                        value = segundaPruebaResultado,
                        onValueChange = { segundaPruebaResultado = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.etilometro_prueba_resultado)) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.etilometro_prueba_contraste),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = pruebaContraste == "SI",
                            onClick = { pruebaContraste = "SI" }
                        )
                        Text(text = stringResource(R.string.etilometro_prueba_contraste_si))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = pruebaContraste == "NO",
                            onClick = { pruebaContraste = "NO" }
                        )
                        Text(text = stringResource(R.string.etilometro_prueba_contraste_no))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { saveAndContinue() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.etilometro_continue))
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
}

@Preview(showBackground = true)
@Composable
private fun EtilometroScreenPreview() {
    SinCarnetTheme {
        EtilometroScreen()
    }
}
