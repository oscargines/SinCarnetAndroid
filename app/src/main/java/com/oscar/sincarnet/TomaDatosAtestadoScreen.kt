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
import com.oscar.sincarnet.ui.theme.SinCarnetTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ============================================================
// Estado del progreso de impresión
// ============================================================
private data class PrintProgress(
    val isVisible: Boolean = false,
    val currentDoc: String = "",
    val currentIndex: Int = 0,
    val totalDocs: Int = 0,
    val isError: Boolean = false,
    val errorMessage: String = ""
) {
    val progressFraction: Float
        get() = if (totalDocs == 0) 0f else currentIndex.toFloat() / totalDocs
}

// Lista de documentos a imprimir en orden, con su nombre visible
// y la función de DocumentPrinter que los imprime.
// Ajustar el delay entre documentos según la velocidad de la impresora.
private const val DELAY_BETWEEN_DOCS_MS = 3000L  // ms entre un doc y el siguiente

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

                // ── Botón de impresión del atestado completo ─
                Button(
                    onClick = {
                        val mac = BluetoothPrinterStorage(context).getDefaultPrinter()?.mac
                        if (mac.isNullOrBlank()) {
                            printProgress = PrintProgress(
                                isVisible    = true,
                                isError      = true,
                                errorMessage = "No hay impresora configurada"
                            )
                            return@Button
                        }
                        imprimirAtestadoCompleto(
                            context  = context,
                            mac      = mac,
                            onProgress = { index, total, docName ->
                                printProgress = PrintProgress(
                                    isVisible    = true,
                                    currentDoc   = docName,
                                    currentIndex = index,
                                    totalDocs    = total
                                )
                            },
                            onFinished = {
                                printProgress = PrintProgress()  // cierra el modal
                            },
                            onError = { msg ->
                                printProgress = PrintProgress(
                                    isVisible    = true,
                                    isError      = true,
                                    errorMessage = msg
                                )
                            }
                        )
                    },
                    enabled  = !printProgress.isVisible,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) {
                    Text(text = "Imprimir atestado")
                }
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

// ============================================================
// imprimirAtestadoCompleto
//
// Imprime los documentos del atestado en orden secuencial,
// con delay entre cada uno para no saturar la impresora.
//
// Cada documento se imprime en IO dispatcher y los callbacks
// de progreso se devuelven en el Main dispatcher para
// actualizar la UI sin race conditions.
//
// Orden de documentos:
//   1. Información derechos asistencia jurídica gratuita
//   2. Diligencia de derechos del investigado
//   3. Citación a juicio (rápido o normal según tipoJuicio)
//   4. Manifestación del investigado
// ============================================================
private fun imprimirAtestadoCompleto(
    context: android.content.Context,
    mac: String,
    onProgress: (index: Int, total: Int, docName: String) -> Unit,
    onFinished: () -> Unit,
    onError: (String) -> Unit
) {
    // Determinar si es juicio rápido o normal para elegir el documento correcto
    val juzgado     = JuzgadoAtestadoStorage(context).loadCurrent()
    val esJuicioRapido = juzgado.tipoJuicio.contains("rápido", ignoreCase = true) ||
            juzgado.tipoJuicio.contains("rapido", ignoreCase = true)

    // Lista ordenada: (nombre visible para el usuario, lambda que lanza la impresión)
    // Cada lambda debe ser una función SUSPENDIBLE que bloquea hasta que termina.
    // Como DocumentPrinter usa corutinas internamente, usamos un wrapper
    // que espera a que la impresora procese antes de continuar.
    data class DocJob(val name: String, val print: suspend () -> Unit)

    val docs = listOf(
        DocJob("Información asistencia jurídica gratuita") {
            printDocumentFromJsonSuspend(context, mac, "docs/13letradogratis.json")
        },
        DocJob("Diligencia de derechos del investigado") {
            DocumentPrinter.imprimirDerechosSuspend(context, mac)
        },
        DocJob(if (esJuicioRapido) "Citación a juicio rápido" else "Citación a juicio") {
            if (esJuicioRapido)
                DocumentPrinter.imprimirCitacionJuicioRapidoSuspend(context, mac)
            else
                DocumentPrinter.imprimirCitacionJuicioSuspend(context, mac)
        },
        DocJob("Manifestación del investigado") {
            DocumentPrinter.imprimirManifestacionSuspend(context, mac)
        }
    )

    val total = docs.size

    CoroutineScope(Dispatchers.IO).launch {
        try {
            docs.forEachIndexed { idx, doc ->
                // Actualizar UI antes de imprimir este documento
                withContext(Dispatchers.Main) {
                    onProgress(idx + 1, total, "Preparando: ${doc.name}")
                }

                // Pequeña pausa para que el modal se muestre antes de bloquear IO
                delay(200)

                withContext(Dispatchers.Main) {
                    onProgress(idx + 1, total, "Imprimiendo: ${doc.name}")
                }

                // Imprimir (bloquea hasta que el trabajo llega a la impresora)
                doc.print()

                // Esperar a que la impresora procese antes del siguiente documento
                if (idx < docs.size - 1) {
                    delay(DELAY_BETWEEN_DOCS_MS)
                }
            }

            withContext(Dispatchers.Main) { onFinished() }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Error al imprimir: ${e.message ?: "Error desconocido"}")
            }
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