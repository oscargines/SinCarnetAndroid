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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.IconButton
import com.oscar.sincarnet.domain.model.PrintProgress
import com.oscar.sincarnet.ui.theme.SinCarnetTheme

@Composable
fun ActaTrasladoVehiculoFirmasScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onPrintClick: () -> Unit = {},
    printProgress: PrintProgress = PrintProgress(),
    onDismissPrintProgress: () -> Unit = {},
    onPrintZebraClick: () -> Unit = {},
    onAgenteFirmaClick: () -> Unit = {},
    onTitularFirmaClick: () -> Unit = {},
    onGenerateClick: (
        consentimientoTraslado: Boolean?,
        autorizaTitular: Boolean,
        autorizaConductor: Boolean,
        firmanteAutorizaTraslado: Boolean?,
        incidenciaDuranteTraslado: Boolean?,
        entregaLlaves: Boolean?
    ) -> Unit = { _, _, _, _, _, _ -> },
    agenteSignature: ImageBitmap? = null,
    titularConductorSignature: ImageBitmap? = null
) {
    val context = LocalContext.current

    var consentimientoTraslado by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var autorizaTitular by rememberSaveable { mutableStateOf(true) }
    var autorizaConductor by rememberSaveable { mutableStateOf(false) }
    var firmanteAutorizaTraslado by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var incidenciaDuranteTraslado by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var entregaLlaves by rememberSaveable { mutableStateOf<Boolean?>(null) }

    val agenteSigned = agenteSignature != null
    val titularSigned = titularConductorSignature != null
    val canGenerate = agenteSigned && titularSigned

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
                    text = stringResource(R.string.acta_traslado_firmas_title),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = stringResource(R.string.acta_traslado_firmas_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )

                SectionTitle(text = stringResource(R.string.acta_traslado_consentimiento_title))
                YesNoQuestionBlock(
                    questionText = stringResource(R.string.acta_traslado_consentimiento_expreso),
                    selectedValue = consentimientoTraslado,
                    onValueChange = { consentimientoTraslado = it }
                )

                Text(text = stringResource(R.string.acta_traslado_persona_autoriza))
                OptionRadioRow(
                    text = stringResource(R.string.acta_traslado_persona_titular),
                    selected = autorizaTitular,
                    onSelect = {
                        autorizaTitular = true
                        autorizaConductor = false
                    }
                )
                OptionRadioRow(
                    text = stringResource(R.string.acta_traslado_persona_conductor),
                    selected = autorizaConductor,
                    onSelect = {
                        autorizaConductor = true
                        autorizaTitular = false
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(text = stringResource(R.string.acta_traslado_firma_section_title))
                TrasladoSignatureButton(
                    text = stringResource(
                        if (agenteSigned) R.string.acta_traslado_firma_agente_done
                        else R.string.acta_traslado_firma_agente
                    ),
                    isSigned = agenteSigned,
                    onClick = onAgenteFirmaClick
                )
                TrasladoSignatureButton(
                    text = stringResource(
                        if (titularSigned) R.string.acta_traslado_firma_titular_done
                        else R.string.acta_traslado_firma_titular
                    ),
                    isSigned = titularSigned,
                    onClick = onTitularFirmaClick
                )

                YesNoQuestionBlock(
                    questionText = stringResource(R.string.acta_traslado_firmante_autoriza),
                    selectedValue = firmanteAutorizaTraslado,
                    onValueChange = { firmanteAutorizaTraslado = it }
                )
                YesNoQuestionBlock(
                    questionText = stringResource(R.string.acta_traslado_incidencia),
                    selectedValue = incidenciaDuranteTraslado,
                    onValueChange = { incidenciaDuranteTraslado = it }
                )
                YesNoQuestionBlock(
                    questionText = stringResource(R.string.acta_traslado_entrega_llaves),
                    selectedValue = entregaLlaves,
                    onValueChange = { entregaLlaves = it }
                )

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        if (!canGenerate) {
                            Toast.makeText(context, context.getString(R.string.acta_traslado_firmas_required), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onGenerateClick(
                            consentimientoTraslado,
                            autorizaTitular,
                            autorizaConductor,
                            firmanteAutorizaTraslado,
                            incidenciaDuranteTraslado,
                            entregaLlaves
                        )
                    },
                    enabled = canGenerate,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF9A9AB8),
                        disabledContentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(stringResource(R.string.acta_traslado_generate))
                }

                TrasladoSignatureButton(
                    text = stringResource(R.string.acta_traslado_print_zebra),
                    enabled = canGenerate && !printProgress.isVisible,
                    isSigned = false,
                    onClick = onPrintZebraClick
                )
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

    if (printProgress.isVisible) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = if (printProgress.isError) stringResource(R.string.print_error_title)
                    else stringResource(R.string.printing_title)
                )
            },
            text = {
                if (printProgress.isError) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = printProgress.errorMessage.ifEmpty { "Error desconocido" },
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = printProgress.currentDoc.ifEmpty { stringResource(R.string.acta_traslado_printing) },
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        if (printProgress.totalDocs > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${printProgress.currentIndex} / ${printProgress.totalDocs}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (printProgress.isError) {
                    TextButton(onClick = onDismissPrintProgress) {
                        Text(text = stringResource(R.string.accept_action))
                    }
                }
            }
        )
    }
}

@Composable
private fun TrasladoSignatureButton(
    text: String,
    enabled: Boolean = true,
    isSigned: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSigned) Color(0xFF2E7D32) else Color(0xFF40407A),
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF9A9AB8),
            disabledContentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(text = text)
    }
}

@Preview(showBackground = true)
@Composable
private fun ActaTrasladoVehiculoFirmasScreenPreview() {
    SinCarnetTheme {
        ActaTrasladoVehiculoFirmasScreen()
    }
}
