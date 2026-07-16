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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oscar.sincarnet.ui.theme.SinCarnetTheme

/**
 * Pantalla hub de documentos complementarios.
 *
 * Agrupa los accesos a actas y documentos auxiliares que no forman parte
 * del flujo principal de atestado.
 *
 * @param modifier Modificador raíz
 * @param onBackClick Vuelve a la pantalla anterior
 * @param onColaboracionAlcoholClick Abre el acta de colaboración con alcoholemia
 * @param onActaTrasladoVehiculoClick Abre el acta de traslado de vehículo
 * @param onActaTrasladoSanitarioClick Abre el acta de traslado a C. Sanitario
 */
@Composable
fun DocumentosComplementariosScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onColaboracionAlcoholClick: () -> Unit = {},
    onActaTrasladoVehiculoClick: () -> Unit = {},
    onActaTrasladoSanitarioClick: () -> Unit = {},
    onOficioCustodiaSangreClick: () -> Unit = {},
    onSolicitudCustodiaJuzgadoClick: () -> Unit = {}
) {
    var showInfoDialog by remember { mutableStateOf(false) }

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
                    text = stringResource(R.string.documentos_complementarios_title),
                    style = MaterialTheme.typography.titleMedium
                )

                AtestadoActionButton(
                    text = stringResource(R.string.colaboracion_alcohol_action),
                    onClick = onColaboracionAlcoholClick
                )

                AtestadoActionButton(
                    text = stringResource(R.string.acta_traslado_action),
                    onClick = onActaTrasladoVehiculoClick
                )

                AtestadoActionButton(
                    text = stringResource(R.string.acta_traslado_sanitario_action),
                    onClick = onActaTrasladoSanitarioClick
                )

                AtestadoActionButton(
                    text = stringResource(R.string.oficio_custodia_sangre_action),
                    onClick = onOficioCustodiaSangreClick
                )

                AtestadoActionButton(
                    text = stringResource(R.string.scj_action),
                    onClick = onSolicitudCustodiaJuzgadoClick
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { showInfoDialog = true }) {
                Text("Info")
            }
            BackIconButton(onClick = onBackClick)
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            confirmButton = {
                OutlinedButton(onClick = { showInfoDialog = false }) {
                    Text("Cerrar")
                }
            },
            text = {
                Text(text = stringResource(R.string.dc_info_text))
            }
        )
    }
}

/**
 * Botón de acción para las entradas del hub de documentos complementarios.
 *
 * @param text Texto del botón
 * @param onClick Acción al pulsar
 * @param containerColor Color de fondo
 */
@Composable
private fun AtestadoActionButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color = Color(0xFF40407A)
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Color.White
        )
    ) {
        Text(text = text)
    }
}

@Preview(showBackground = true)
@Composable
private fun DocumentosComplementariosScreenPreview() {
    SinCarnetTheme {
        DocumentosComplementariosScreen()
    }
}
