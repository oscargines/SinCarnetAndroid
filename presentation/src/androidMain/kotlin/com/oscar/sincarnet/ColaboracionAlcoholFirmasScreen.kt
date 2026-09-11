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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oscar.sincarnet.ui.theme.SinCarnetTheme

@Composable
fun ColaboracionAlcoholFirmasScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onAgenteClick: () -> Unit = {},
    onOperadorClick: () -> Unit = {},
    onPersonaClick: () -> Unit = {},
    onGenerateClick: () -> Unit = {},
    agenteSignature: ImageBitmap? = null,
    operadorSignature: ImageBitmap? = null,
    personaSignature: ImageBitmap? = null
) {
    val context = LocalContext.current

    val agenteSigned = agenteSignature != null
    val operadorSigned = operadorSignature != null
    val personaSigned = personaSignature != null

    val generateEnabled = agenteSigned && operadorSigned

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
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.colaboracion_firmas_title),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = stringResource(R.string.colaboracion_firmas_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(8.dp))

                ColaboracionSignatureButton(
                    text = stringResource(
                        if (agenteSigned) R.string.colaboracion_firmas_agente_done
                        else R.string.colaboracion_firmas_agente
                    ),
                    enabled = true,
                    isSigned = agenteSigned,
                    onClick = onAgenteClick
                )

                ColaboracionSignatureButton(
                    text = stringResource(
                        if (operadorSigned) R.string.colaboracion_firmas_operador_done
                        else R.string.colaboracion_firmas_operador
                    ),
                    enabled = true,
                    isSigned = operadorSigned,
                    onClick = onOperadorClick
                )

                Spacer(modifier = Modifier.height(8.dp))

                ColaboracionSignatureButton(
                    text = stringResource(
                        if (personaSigned) R.string.colaboracion_firmas_persona_done
                        else R.string.colaboracion_firmas_persona
                    ),
                    enabled = true,
                    isSigned = personaSigned,
                    onClick = onPersonaClick
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (!generateEnabled) {
                            Toast.makeText(context, context.getString(R.string.colaboracion_firmas_required), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onGenerateClick()
                    },
                    enabled = generateEnabled,
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
                    Text(stringResource(R.string.colaboracion_firmas_generate))
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
private fun ColaboracionAlcoholFirmasScreenPreview() {
    SinCarnetTheme {
        ColaboracionAlcoholFirmasScreen()
    }
}

@Composable
private fun ColaboracionSignatureButton(
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
