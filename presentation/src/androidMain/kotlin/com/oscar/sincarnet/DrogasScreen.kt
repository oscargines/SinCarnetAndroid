package com.oscar.sincarnet

import com.oscar.sincarnet.presentation.R

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrogasScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onContinueClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val storage = ComplementarioAlcoholStorage(context.toStorage("colaboracion_alcohol_storage"))
    val initialData = storage.loadCurrent()

    var lectorMarca by rememberSaveable { mutableStateOf(initialData.lectorDrogasMarca) }
    var lectorModelo by rememberSaveable { mutableStateOf(initialData.lectorDrogasModelo) }
    var lectorNumeroSerie by rememberSaveable { mutableStateOf(initialData.lectorDrogasNumeroSerie) }
    var lectorHoraVerificacion by rememberSaveable { mutableStateOf(initialData.lectorDrogasHoraVerificacion) }
    var lectorAgente by rememberSaveable { mutableStateOf(initialData.lectorDrogasAgente) }

    var resultado by rememberSaveable { mutableStateOf(initialData.pruebaDrogasResultado) }
    var tipoDroga by rememberSaveable { mutableStateOf(initialData.pruebaDrogasTipo) }
    var contraste by rememberSaveable { mutableStateOf(initialData.pruebaDrogasContraste) }
    val tiposDisponibles = listOf("THC", "OPI", "COC", "AMP", "MAMP")

    fun selectedDrugTypes(): Set<String> =
        tipoDroga
            .split(",", ";", "|")
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() }
            .toSet()

    fun isDrugSelected(drug: String): Boolean = selectedDrugTypes().contains(drug)

    fun toggleDrugType(drug: String) {
        val updated = selectedDrugTypes().toMutableSet()
        if (updated.contains(drug)) updated.remove(drug) else updated.add(drug)
        tipoDroga = tiposDisponibles.filter { updated.contains(it) }.joinToString(",")
    }

    val saveAndContinue: () -> Unit = {
        when {
            resultado.isBlank() -> {
                Toast.makeText(context, context.getString(R.string.drogas_select_resultado), Toast.LENGTH_SHORT).show()
            }
            resultado == "Positiva" && selectedDrugTypes().isEmpty() -> {
                Toast.makeText(context, context.getString(R.string.drogas_select_tipo), Toast.LENGTH_SHORT).show()
            }
            contraste.isBlank() -> {
                Toast.makeText(context, context.getString(R.string.drogas_select_contraste), Toast.LENGTH_SHORT).show()
            }
            else -> {
                val updatedData = initialData.copy(
                    lectorDrogasMarca = lectorMarca,
                    lectorDrogasModelo = lectorModelo,
                    lectorDrogasNumeroSerie = lectorNumeroSerie,
                    lectorDrogasHoraVerificacion = lectorHoraVerificacion,
                    lectorDrogasAgente = lectorAgente,
                    pruebaDrogasResultado = resultado,
                    pruebaDrogasTipo = tipoDroga,
                    pruebaDrogasContraste = contraste
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
                    text = stringResource(R.string.drogas_title),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = stringResource(R.string.drogas_lector_section),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = lectorMarca,
                    onValueChange = { lectorMarca = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.colaboracion_marca)) }
                )

                OutlinedTextField(
                    value = lectorModelo,
                    onValueChange = { lectorModelo = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.colaboracion_modelo)) }
                )

                OutlinedTextField(
                    value = lectorNumeroSerie,
                    onValueChange = { lectorNumeroSerie = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.colaboracion_numero_serie)) }
                )

                OutlinedTextField(
                    value = lectorHoraVerificacion,
                    onValueChange = { lectorHoraVerificacion = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.colaboracion_hora_verificacion)) }
                )

                OutlinedTextField(
                    value = lectorAgente,
                    onValueChange = { lectorAgente = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.colaboracion_agente_operador)) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.drogas_resultado_title),
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
                            selected = resultado == "Negativa",
                            onClick = { resultado = "Negativa" }
                        )
                        Text(text = stringResource(R.string.drogas_resultado_negativa))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = resultado == "Positiva",
                            onClick = { resultado = "Positiva" }
                        )
                        Text(text = stringResource(R.string.drogas_resultado_positiva))
                    }
                }

                if (resultado == "Positiva") {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.drogas_tipo_droga),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = isDrugSelected("THC"),
                                onCheckedChange = { toggleDrugType("THC") }
                            )
                            Text(text = stringResource(R.string.colaboracion_thc))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = isDrugSelected("OPI"),
                                onCheckedChange = { toggleDrugType("OPI") }
                            )
                            Text(text = stringResource(R.string.colaboracion_opi))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = isDrugSelected("COC"),
                                onCheckedChange = { toggleDrugType("COC") }
                            )
                            Text(text = stringResource(R.string.colaboracion_coc))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = isDrugSelected("AMP"),
                                onCheckedChange = { toggleDrugType("AMP") }
                            )
                            Text(text = stringResource(R.string.colaboracion_amp))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = isDrugSelected("MAMP"),
                                onCheckedChange = { toggleDrugType("MAMP") }
                            )
                            Text(text = stringResource(R.string.colaboracion_mamp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.drogas_prueba_contraste),
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
                            selected = contraste == "SI",
                            onClick = { contraste = "SI" }
                        )
                        Text(text = stringResource(R.string.drogas_si))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = contraste == "NO",
                            onClick = { contraste = "NO" }
                        )
                        Text(text = stringResource(R.string.drogas_no))
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
                    Text(stringResource(R.string.drogas_continue))
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
private fun DrogasScreenPreview() {
    SinCarnetTheme {
        DrogasScreen()
    }
}
