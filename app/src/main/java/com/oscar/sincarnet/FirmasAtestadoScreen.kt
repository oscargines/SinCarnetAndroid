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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oscar.sincarnet.ui.theme.SinCarnetTheme

@Composable
fun FirmasAtestadoScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onPrintClick: () -> Unit = {},
    // Firmas capturadas externamente (estado elevado en MainActivity)
    instructorSignature: ImageBitmap? = null,
    secretarySignature: ImageBitmap? = null,
    investigatedSignature: ImageBitmap? = null,
    secondDriverSignature: ImageBitmap? = null,
    onInstructorClick: () -> Unit = {},
    onSecretaryClick: () -> Unit = {},
    onInvestigatedClick: () -> Unit = {},
    onSecondDriverClick: () -> Unit = {},
    onGenerateAtestadoClick: (wantsToSign: Boolean) -> Unit = {}
) {
    var wantsToSign by rememberSaveable { mutableStateOf(true) }
    var hasSecondDriver by rememberSaveable { mutableStateOf(false) }

    // Las firmas vienen del estado externo en memoria.
    val instructorSigned = instructorSignature != null
    val secretarySigned = secretarySignature != null
    val investigatedSigned = investigatedSignature != null
    val secondDriverSigned = secondDriverSignature != null

    val generateEnabled = instructorSigned &&
        secretarySigned &&
        (!wantsToSign || investigatedSigned) &&
        (!hasSecondDriver || secondDriverSigned)

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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.atestado_signatures_title),
                    style = MaterialTheme.typography.titleMedium
                )

                AtestadoSignatureButton(
                    text = stringResource(
                        if (instructorSigned) R.string.atestado_signature_instructor_done
                        else R.string.atestado_signature_instructor
                    ),
                    enabled = true,
                    isSigned = instructorSigned,
                    onClick = onInstructorClick
                )
                AtestadoSignatureButton(
                    text = stringResource(
                        if (secretarySigned) R.string.atestado_signature_secretary_done
                        else R.string.atestado_signature_secretary
                    ),
                    enabled = true,
                    isSigned = secretarySigned,
                    onClick = onSecretaryClick
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = wantsToSign, onValueChange = {
                            wantsToSign = it
                        }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = wantsToSign, onCheckedChange = null)
                    Text(text = stringResource(R.string.atestado_signature_wants_to_sign))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = hasSecondDriver, onValueChange = {
                            hasSecondDriver = it
                        }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = hasSecondDriver, onCheckedChange = null)
                    Text(text = stringResource(R.string.atestado_signature_has_second_driver))
                }

                AtestadoSignatureButton(
                    text = stringResource(
                        if (investigatedSigned) R.string.atestado_signature_investigated_done
                        else R.string.atestado_signature_investigated
                    ),
                    enabled = wantsToSign,
                    isSigned = investigatedSigned,
                    onClick = onInvestigatedClick
                )
                AtestadoSignatureButton(
                    text = stringResource(
                        if (secondDriverSigned) R.string.atestado_signature_second_driver_done
                        else R.string.atestado_signature_second_driver
                    ),
                    enabled = hasSecondDriver,
                    isSigned = secondDriverSigned,
                    onClick = onSecondDriverClick
                )

                Spacer(modifier = Modifier.height(16.dp))

                AtestadoSignatureButton(
                    text = stringResource(R.string.atestado_signature_generate),
                    enabled = generateEnabled,
                    isSigned = false,
                    onClick = { onGenerateAtestadoClick(wantsToSign) }
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
}

@Composable
private fun AtestadoSignatureButton(
    text: String,
    enabled: Boolean,
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
private fun FirmasAtestadoScreenPreview() {
    SinCarnetTheme {
        FirmasAtestadoScreen()
    }
}
