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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
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
fun TomaDatosAtestadoScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onPrintClick: () -> Unit = {},
    onLocationTimeClick: () -> Unit = {},
    onPersonDataClick: () -> Unit = {},
    onVehicleDataClick: () -> Unit = {},
    onCourtDataClick: () -> Unit = {},
    onActingDataClick: () -> Unit = {},
    onSignaturesClick: () -> Unit = {}
) {
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
                    text = stringResource(R.string.atestado_data_title),
                    style = MaterialTheme.typography.titleMedium
                )

                AtestadoActionButton(
                    text = stringResource(R.string.atestado_data_location_time),
                    onClick = onLocationTimeClick
                )
                AtestadoActionButton(
                    text = stringResource(R.string.atestado_data_person),
                    onClick = onPersonDataClick
                )
                AtestadoActionButton(
                    text = stringResource(R.string.atestado_data_vehicle),
                    onClick = onVehicleDataClick
                )
                AtestadoActionButton(
                    text = stringResource(R.string.atestado_data_court),
                    onClick = onCourtDataClick
                )
                AtestadoActionButton(
                    text = stringResource(R.string.atestado_data_acting),
                    onClick = onActingDataClick
                )

                Spacer(modifier = Modifier.height(8.dp))

                AtestadoActionButton(
                    text = stringResource(R.string.atestado_data_signatures),
                    onClick = onSignaturesClick,
                    containerColor = Color(0xFF4A148C)
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
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(text = text)
    }
}

@Preview(showBackground = true)
@Composable
private fun TomaDatosAtestadoScreenPreview() {
    SinCarnetTheme {
        TomaDatosAtestadoScreen()
    }
}

