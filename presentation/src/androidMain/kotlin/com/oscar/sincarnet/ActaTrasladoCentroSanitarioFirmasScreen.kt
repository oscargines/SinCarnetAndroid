package com.oscar.sincarnet

import com.oscar.sincarnet.presentation.R

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.IconButton
import com.oscar.sincarnet.domain.model.PrintProgress
import com.oscar.sincarnet.ui.theme.SinCarnetTheme

@Composable
fun ActaTrasladoCentroSanitarioFirmasScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onPrintClick: () -> Unit = {},
    printProgress: PrintProgress = PrintProgress(),
    onDismissPrintProgress: () -> Unit = {},
    onPrintZebraClick: () -> Unit = {},
    onInteresadoFirmaClick: () -> Unit = {},
    onFacultativoFirmaClick: () -> Unit = {},
    onSanitarioFirmaClick: () -> Unit = {},
    onAgenteFirmaClick: () -> Unit = {},
    onGenerateClick: () -> Unit = {},
    interesadoSignature: ImageBitmap? = null,
    facultativoSignature: ImageBitmap? = null,
    sanitarioSignature: ImageBitmap? = null,
    agenteSignature: ImageBitmap? = null
) {
    val context = LocalContext.current

    val interesadoSigned = interesadoSignature != null
    val facultativoSigned = facultativoSignature != null
    val sanitarioSigned = sanitarioSignature != null
    val agenteSigned = agenteSignature != null

    val canGenerate = interesadoSigned && facultativoSigned && sanitarioSigned && agenteSigned

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
                    text = stringResource(R.string.centro_sanitario_firmas_title),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = stringResource(R.string.centro_sanitario_firmas_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )

                SectionTitle(text = stringResource(R.string.centro_sanitario_firma_section_title))
                CentroSanitarioSignatureButton(
                    text = stringResource(
                        if (interesadoSigned) R.string.centro_sanitario_firma_interesado_done
                        else R.string.centro_sanitario_firma_interesado
                    ),
                    isSigned = interesadoSigned,
                    onClick = onInteresadoFirmaClick
                )
                CentroSanitarioSignatureButton(
                    text = stringResource(
                        if (facultativoSigned) R.string.centro_sanitario_firma_facultativo_done
                        else R.string.centro_sanitario_firma_facultativo
                    ),
                    isSigned = facultativoSigned,
                    onClick = onFacultativoFirmaClick
                )
                CentroSanitarioSignatureButton(
                    text = stringResource(
                        if (sanitarioSigned) R.string.centro_sanitario_firma_sanitario_done
                        else R.string.centro_sanitario_firma_sanitario
                    ),
                    isSigned = sanitarioSigned,
                    onClick = onSanitarioFirmaClick
                )
                CentroSanitarioSignatureButton(
                    text = stringResource(
                        if (agenteSigned) R.string.centro_sanitario_firma_agente_done
                        else R.string.centro_sanitario_firma_agente
                    ),
                    isSigned = agenteSigned,
                    onClick = onAgenteFirmaClick
                )

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        if (!canGenerate) {
                            Toast.makeText(context, context.getString(R.string.centro_sanitario_firmas_required), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onGenerateClick()
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
                    Text(stringResource(R.string.centro_sanitario_generate))
                }

                CentroSanitarioSignatureButton(
                    text = stringResource(R.string.centro_sanitario_print_zebra),
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
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            BoxWithConstraints {
                val maxDialogWidth = if (maxWidth < 600.dp) maxWidth * 0.92f else 480.dp
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .widthIn(max = maxDialogWidth)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (printProgress.isError) stringResource(R.string.print_error_title)
                            else stringResource(R.string.printing_title),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (printProgress.isError) {
                            Text(
                                text = printProgress.errorMessage.ifEmpty { "Error desconocido" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onDismissPrintProgress,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.accept_action))
                            }
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(56.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = printProgress.currentDoc.ifEmpty { stringResource(R.string.centro_sanitario_printing) },
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            if (printProgress.totalDocs > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Documento ${printProgress.currentIndex} de ${printProgress.totalDocs}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CentroSanitarioSignatureButton(
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
private fun ActaTrasladoCentroSanitarioFirmasScreenPreview() {
    SinCarnetTheme {
        ActaTrasladoCentroSanitarioFirmasScreen()
    }
}
