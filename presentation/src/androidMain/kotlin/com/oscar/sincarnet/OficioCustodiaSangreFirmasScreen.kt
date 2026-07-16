package com.oscar.sincarnet

import com.oscar.sincarnet.presentation.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oscar.sincarnet.ui.theme.SinCarnetTheme

@Composable
fun OficioCustodiaSangreFirmasScreen(
    modifier: Modifier = Modifier,
    agenteSigned: Boolean = false,
    onBackClick: () -> Unit = {},
    onAgenteFirmaClick: () -> Unit = {},
    onGenerateClick: () -> Unit = {}
) {
    val canGenerate = agenteSigned

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
                    text = stringResource(R.string.ocs_firmas_title),
                    style = MaterialTheme.typography.titleMedium
                )

                Button(
                    onClick = onAgenteFirmaClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (agenteSigned) Color(0xFF2E7D32) else Color(0xFF40407A),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        stringResource(
                            if (agenteSigned) R.string.ocs_firma_agente_done
                            else R.string.ocs_firma_agente
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        if (!canGenerate) {
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
                    Text(stringResource(R.string.ocs_generate))
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
private fun OficioCustodiaSangreFirmasScreenPreview() {
    SinCarnetTheme {
        OficioCustodiaSangreFirmasScreen()
    }
}
