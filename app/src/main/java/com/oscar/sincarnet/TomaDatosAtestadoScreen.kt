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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.ui.unit.dp


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
    onSignaturesClick: () -> Unit = {},
    onScanDocumentClick: () -> Unit = {},
    // Firmas capturadas en FirmasAtestadoScreen (estado elevado en MainActivity)
    printSignatures: PrintSignatures? = null
) {
    val context = LocalContext.current
    var printProgress by remember { mutableStateOf(PrintProgress()) }

    // ── Modal de progreso de impresión ────────────────────────
    if (printProgress.isVisible) {
        AlertDialog(
            onDismissRequest = { /* No se puede cerrar mientras imprime */ },
            title = {
                Text(
                    text = if (printProgress.isError) "Error de impresión"
                    else "Imprimiendo documentos",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!printProgress.isError) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(56.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                        Text(
                            text = printProgress.currentDoc,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Documento ${printProgress.currentIndex} de ${printProgress.totalDocs}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = printProgress.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                // Solo visible si hay error — mientras imprime no se puede cerrar
                if (printProgress.isError) {
                    Button(onClick = { printProgress = PrintProgress() }) {
                        Text("Cerrar")
                    }
                }
            }
        )
    }

    // ── Layout principal ─────────────────────────────────────
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
                Spacer(modifier = Modifier.height(30.dp))

                AtestadoActionButton(
                    text = stringResource(R.string.atestado_data_scan_document),
                    onClick = onScanDocumentClick,
                    containerColor = Color(0xFF263238)

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

// printDocumentFromJsonSuspend es ahora una función real en BluetoothPrinterUtils
// que bloquea hasta que el trabajo se envía completamente a la impresora.
// No necesita wrapper aquí.

// ============================================================
// Composable interno
// ============================================================
@Composable
private fun AtestadoActionButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color = Color(0xFF40407A)   // color por defecto
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
// ====================== PREVIEW ======================

@Preview(showBackground = true)
@Composable
fun TomaDatosAtestadoScreenPreview() {
    MaterialTheme {   // Importante: envuelve con el tema de tu app
        TomaDatosAtestadoScreen(
            onBackClick = {},
            onPrintClick = {},
            onLocationTimeClick = {},
            onPersonDataClick = {},
            onVehicleDataClick = {},
            onCourtDataClick = {},
            onActingDataClick = {},
            onSignaturesClick = {},
            onScanDocumentClick = {},
            printSignatures = null
        )
    }
}