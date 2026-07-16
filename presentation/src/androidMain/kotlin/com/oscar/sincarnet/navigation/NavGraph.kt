package com.oscar.sincarnet.navigation

import android.widget.Toast
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.oscar.sincarnet.AboutDialog
import com.oscar.sincarnet.ActaTrasladoVehiculoFirmasScreen
import com.oscar.sincarnet.ActaTrasladoVehiculoScreen
import com.oscar.sincarnet.BluetoothPrinterScreen
import com.oscar.sincarnet.CasesScreen
import com.oscar.sincarnet.ColaboracionAlcoholScreen
import com.oscar.sincarnet.ColaboracionAlcoholFirmasScreen
import com.oscar.sincarnet.ActaTrasladoCentroSanitarioScreen
import com.oscar.sincarnet.ActaTrasladoCentroSanitarioFirmasScreen
import com.oscar.sincarnet.ConsultaJuzgadosScreen
import com.oscar.sincarnet.DatosActuantesScreen
import com.oscar.sincarnet.DocumentosComplementariosScreen
import com.oscar.sincarnet.OficioCustodiaSangreScreen
import com.oscar.sincarnet.OficioCustodiaSangreDocScreen
import com.oscar.sincarnet.OficioCustodiaSangreFirmasScreen
import com.oscar.sincarnet.SolicitudCustodiaJuzgadoScreen
import com.oscar.sincarnet.SolicitudCustodiaJuzgadoDocScreen
import com.oscar.sincarnet.SolicitudCustodiaJuzgadoFirmasScreen
import com.oscar.sincarnet.DatosJuzgadoAtestadoScreen
import com.oscar.sincarnet.DatosOcurrenciaDelitScreen
import com.oscar.sincarnet.DatosPersonaInvestigadaScreen
import com.oscar.sincarnet.DatosVehiculoScreen
import com.oscar.sincarnet.DocumentScannerScreen
import com.oscar.sincarnet.EtilometroScreen
import com.oscar.sincarnet.DrogasScreen
import com.oscar.sincarnet.ExpiredValidityScreen
import com.oscar.sincarnet.FirmaManuscritaScreen
import com.oscar.sincarnet.FirmasAtestadoScreen
import com.oscar.sincarnet.JudicialSuspensionScreen
import com.oscar.sincarnet.MainUiState
import com.oscar.sincarnet.MainViewModel
import com.oscar.sincarnet.ManifestacionScreen
import com.oscar.sincarnet.presentation.R
import com.oscar.sincarnet.SpecialCasesScreen
import com.oscar.sincarnet.TomaDatosAtestadoScreen
import com.oscar.sincarnet.WithoutPermitScreen
import com.oscar.sincarnet.data.pdf.SIGNER_INVESTIGATED
import com.oscar.sincarnet.data.pdf.SIGNER_INSTRUCTOR
import com.oscar.sincarnet.data.pdf.SIGNER_SECOND_DRIVER
import com.oscar.sincarnet.data.pdf.SIGNER_SECRETARY
import com.oscar.sincarnet.data.pdf.ActaTrasladoVehiculoPdfGenerator
import com.oscar.sincarnet.data.pdf.ActaTrasladoCentroSanitarioPdfGenerator
import com.oscar.sincarnet.data.pdf.OficioCustodiaSangrePdfGenerator
import com.oscar.sincarnet.data.pdf.SolicitudCustodiaJuzgadoPdfGenerator
import com.oscar.sincarnet.data.print.PrintSignatures
import com.oscar.sincarnet.data.repository.ActaTrasladoVehiculoStorage
import com.oscar.sincarnet.data.repository.ActaTrasladoCentroSanitarioStorage
import com.oscar.sincarnet.data.repository.OficioCustodiaSangreStorage
import com.oscar.sincarnet.data.repository.SolicitudCustodiaJuzgadoStorage
import com.oscar.sincarnet.data.toStorage
import com.oscar.sincarnet.domain.model.PrintProgress
import java.io.File

/**
 * Grafo de navegación principal de SinCarnet.
 *
 * Define los 18 destinos de la aplicación usando `NavHost` de Navigation Compose.
 * Cada pantalla recibe su estado desde [uiState] y delega las acciones de
 * navegación al [navController]. La lógica de negocio permanece en [viewModel].
 *
 * @param navController Controlador de navegación gestionado por `MainActivity`.
 * @param viewModel ViewModel principal que expone el estado y efectos.
 * @param uiState Estado actual observado desde `MainActivity`.
 * @param modifier Modificador opcional aplicado al `NavHost`.
 * @param onOpenPdf Callback para abrir un PDF generado por el escáner.
 * @param onSharePdf Callback para compartir un PDF generado por el escáner.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: MainViewModel,
    uiState: MainUiState,
    modifier: Modifier = Modifier,
    onOpenPdf: (File) -> Unit = {},
    onSharePdf: (File) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Route.Cases.route,
        modifier = modifier
    ) {
        composable(Route.Cases.route) {
            CasesScreen(
                modifier = Modifier.fillMaxSize(),
                onExpiredValidityClick = { navController.navigate(Route.ExpiredValidity.route) },
                onJudicialSuspensionClick = { navController.navigate(Route.JudicialSuspension.route) },
                onWithoutPermitClick = { navController.navigate(Route.WithoutPermit.route) },
                onSpecialCasesClick = { navController.navigate(Route.SpecialCases.route) },
                onDocumentosComplementariosClick = { navController.navigate(Route.DocumentosComplementarios.route) },
                onCourtsClick = { navController.navigate(Route.Courts.route) },
                onAboutClick = { viewModel.onShowAboutDialogChange(true) }
            )
        }

        composable(Route.ExpiredValidity.route) {
            ExpiredValidityScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onStartAtestadoClick = {
                    viewModel.resetAtestadoSession()
                    navController.navigate(Route.AtestadoData.route)
                }
            )
        }

        composable(Route.JudicialSuspension.route) {
            JudicialSuspensionScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onStartAtestadoClick = {
                    viewModel.resetAtestadoSession()
                    navController.navigate(Route.AtestadoData.route)
                }
            )
        }

        composable(Route.WithoutPermit.route) {
            WithoutPermitScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onStartAtestadoClick = {
                    viewModel.resetAtestadoSession()
                    navController.navigate(Route.AtestadoData.route)
                }
            )
        }

        composable(Route.SpecialCases.route) {
            SpecialCasesScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Route.Courts.route) {
            ConsultaJuzgadosScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Route.DocumentosComplementarios.route) {
            DocumentosComplementariosScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onColaboracionAlcoholClick = { navController.navigate(Route.ColaboracionAlcohol.route) },
                onActaTrasladoVehiculoClick = { navController.navigate(Route.ActaTrasladoVehiculo.route) },
                onActaTrasladoSanitarioClick = { navController.navigate(Route.CentroSanitario.route) },
                onOficioCustodiaSangreClick = { navController.navigate(Route.OficioCustodiaSangre.route) },
                onSolicitudCustodiaJuzgadoClick = { navController.navigate(Route.SolicitudCustodiaJuzgado.route) }
            )
        }

        composable(Route.CentroSanitario.route) {
            ActaTrasladoCentroSanitarioScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onContinueClick = { navController.navigate(Route.CentroSanitarioFirmas.route) }
            )
        }

        composable(Route.CentroSanitarioFirmas.route) {
            val context = LocalContext.current
            var printCsProgress by remember { mutableStateOf(PrintProgress()) }
            ActaTrasladoCentroSanitarioFirmasScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onPrintClick = { navController.navigate(Route.BluetoothPrinter.route) },
                printProgress = printCsProgress,
                onDismissPrintProgress = { printCsProgress = PrintProgress() },
                onInteresadoFirmaClick = {
                    viewModel.onCurrentSignerKeyChange("cs_interesado")
                    navController.navigate(Route.CentroSanitarioFirmaScreen.route)
                },
                onFacultativoFirmaClick = {
                    viewModel.onCurrentSignerKeyChange("cs_facultativo")
                    navController.navigate(Route.CentroSanitarioFirmaScreen.route)
                },
                onSanitarioFirmaClick = {
                    viewModel.onCurrentSignerKeyChange("cs_sanitario")
                    navController.navigate(Route.CentroSanitarioFirmaScreen.route)
                },
                onAgenteFirmaClick = {
                    viewModel.onCurrentSignerKeyChange("cs_agente")
                    navController.navigate(Route.CentroSanitarioFirmaScreen.route)
                },
                onGenerateClick = {
                    try {
                        val storage = ActaTrasladoCentroSanitarioStorage(
                            context.toStorage("centro_sanitario_storage")
                        )
                        val data = storage.loadCurrent()
                        val interesadoBitmap = uiState.signature.signaturesBySigner["cs_interesado"]?.asAndroidBitmap()
                        val facultativoBitmap = uiState.signature.signaturesBySigner["cs_facultativo"]?.asAndroidBitmap()
                        val sanitarioBitmap = uiState.signature.signaturesBySigner["cs_sanitario"]?.asAndroidBitmap()
                        val agenteBitmap = uiState.signature.signaturesBySigner["cs_agente"]?.asAndroidBitmap()
                        val result = ActaTrasladoCentroSanitarioPdfGenerator.generatePdf(
                            context = context,
                            data = data,
                            interesadoSignature = interesadoBitmap,
                            facultativoSignature = facultativoBitmap,
                            sanitarioSignature = sanitarioBitmap,
                            agenteSignature = agenteBitmap
                        )
                        Toast.makeText(context, "PDF generado: ${result.file.name}", Toast.LENGTH_LONG).show()
                        try {
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                result.file
                            )
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("CentroSanitario", "Error al abrir PDF", e)
                            Toast.makeText(context, "PDF generado pero no se pudo abrir: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e("CentroSanitario", "Error al generar PDF", e)
                        Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                onPrintZebraClick = {
                    val mac = com.oscar.sincarnet.data.repository.BluetoothPrinterStorage(
                        context
                    ).getDefaultPrinter()?.mac
                    if (mac.isNullOrBlank()) {
                        printCsProgress = PrintProgress(
                            isVisible = true,
                            isError = true,
                            errorMessage = context.getString(R.string.centro_sanitario_no_printer)
                        )
                        return@ActaTrasladoCentroSanitarioFirmasScreen
                    }
                    val interesadoSig = uiState.signature.signaturesBySigner["cs_interesado"]
                    val facultativoSig = uiState.signature.signaturesBySigner["cs_facultativo"]
                    val sanitarioSig = uiState.signature.signaturesBySigner["cs_sanitario"]
                    val agenteSig = uiState.signature.signaturesBySigner["cs_agente"]
                    printCsProgress = PrintProgress(
                        isVisible = true,
                        currentDoc = context.getString(R.string.centro_sanitario_connecting)
                    )
                    com.oscar.sincarnet.data.print.DocumentPrinter.imprimirActaTrasladoCentroSanitarioCompleto(
                        context = context,
                        mac = mac,
                        interesadoSignature = interesadoSig,
                        facultativoSignature = facultativoSig,
                        sanitarioSignature = sanitarioSig,
                        agenteSignature = agenteSig,
                        onProgress = { index, total, docName ->
                            Log.d("CentroSanitario", "Imprimiendo [$index/$total]: $docName")
                            printCsProgress = PrintProgress(
                                isVisible = true,
                                currentDoc = docName,
                                currentIndex = index,
                                totalDocs = total
                            )
                        },
                        onFinished = {
                            Log.d("CentroSanitario", "Impresión completada")
                            printCsProgress = PrintProgress()
                        },
                        onError = { msg ->
                            Log.e("CentroSanitario", "Error al imprimir: $msg")
                            printCsProgress = PrintProgress(
                                isVisible = true,
                                isError = true,
                                errorMessage = msg.ifEmpty { "Error desconocido al imprimir" }
                            )
                        }
                    )
                },
                interesadoSignature = uiState.signature.signaturesBySigner["cs_interesado"],
                facultativoSignature = uiState.signature.signaturesBySigner["cs_facultativo"],
                sanitarioSignature = uiState.signature.signaturesBySigner["cs_sanitario"],
                agenteSignature = uiState.signature.signaturesBySigner["cs_agente"]
            )
        }

        composable(Route.CentroSanitarioFirmaScreen.route) {
            val signerName = when (uiState.signature.currentSignerKey) {
                "cs_interesado" -> stringResource(R.string.centro_sanitario_firma_interesado)
                "cs_facultativo" -> stringResource(R.string.centro_sanitario_firma_facultativo)
                "cs_sanitario" -> stringResource(R.string.centro_sanitario_firma_sanitario)
                "cs_agente" -> stringResource(R.string.centro_sanitario_firma_agente)
                else -> "Firma"
            }
            FirmaManuscritaScreen(
                modifier = Modifier.fillMaxSize(),
                signerName = signerName,
                onSignatureSaved = { bitmap ->
                    viewModel.onSignatureSaved(uiState.signature.currentSignerKey, bitmap)
                    navController.popBackStack()
                },
                onCancel = {
                    viewModel.onSignatureCancelled()
                    navController.popBackStack()
                }
            )
        }

        composable(Route.ActaTrasladoVehiculo.route) {
            ActaTrasladoVehiculoScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onContinueClick = { navController.navigate(Route.ActaTrasladoVehiculoFirmas.route) }
            )
        }

        composable(Route.ActaTrasladoVehiculoFirmas.route) {
            val context = LocalContext.current
            var printActaProgress by remember { mutableStateOf(PrintProgress()) }
            ActaTrasladoVehiculoFirmasScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onPrintClick = {
                    Log.d("ActaTraslado", "Abriendo selector de impresora desde firmas de traslado")
                    navController.navigate(Route.BluetoothPrinter.route)
                },
                printProgress = printActaProgress,
                onDismissPrintProgress = { printActaProgress = PrintProgress() },
                onAgenteFirmaClick = {
                    viewModel.onCurrentSignerKeyChange("traslado_agente")
                    navController.navigate(Route.ActaTrasladoVehiculoFirmaScreen.route)
                },
                onTitularFirmaClick = {
                    viewModel.onCurrentSignerKeyChange("traslado_titular")
                    navController.navigate(Route.ActaTrasladoVehiculoFirmaScreen.route)
                },
                onGenerateClick = { consentimientoTraslado, autorizaTitular, autorizaConductor, firmanteAutorizaTraslado, incidenciaDuranteTraslado, entregaLlaves ->
                    try {
                        Log.d("ActaTraslado", "Iniciando generación de PDF")
                        val storage = com.oscar.sincarnet.data.repository.ActaTrasladoVehiculoStorage(
                            context.toStorage("acta_traslado_vehiculo_storage")
                        )
                        val mergedData = storage.loadCurrent().copy(
                            consentimientoTraslado = consentimientoTraslado,
                            autorizaTitular = autorizaTitular,
                            autorizaConductor = autorizaConductor,
                            firmanteAutorizaTraslado = firmanteAutorizaTraslado,
                            incidenciaDuranteTraslado = incidenciaDuranteTraslado,
                            entregaLlaves = entregaLlaves
                        )
                        Log.d(
                            "ActaTraslado",
                            "Datos cargados: vehiculo=${mergedData.vehiculoMarca}/${mergedData.vehiculoModelo}/${mergedData.vehiculoMatricula}, " +
                                "firmante=${mergedData.nombre} ${mergedData.primerApellido}, consentimiento=${mergedData.consentimientoTraslado}"
                        )
                        storage.saveCurrent(mergedData)

                        val agenteBitmap = uiState.signature.signaturesBySigner["traslado_agente"]?.asAndroidBitmap()
                        val titularBitmap = uiState.signature.signaturesBySigner["traslado_titular"]?.asAndroidBitmap()
                        Log.d(
                            "ActaTraslado",
                            "Firmas disponibles: agente=${agenteBitmap != null}, titular=${titularBitmap != null}"
                        )
                        val result = com.oscar.sincarnet.data.pdf.ActaTrasladoVehiculoPdfGenerator.generatePdf(
                            context = context,
                            data = mergedData,
                            agenteSignature = agenteBitmap,
                            titularConductorSignature = titularBitmap
                        )
                        Log.d("ActaTraslado", "PDF generado en: ${result.file.absolutePath}")
                        Toast.makeText(context, "PDF generado: ${result.file.name}", Toast.LENGTH_LONG).show()
                        try {
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                result.file
                            )
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("ActaTraslado", "Error al abrir PDF", e)
                            Toast.makeText(context, "PDF generado pero no se pudo abrir: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e("ActaTraslado", "Error al generar PDF", e)
                        Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                onPrintZebraClick = {
                    val mac = com.oscar.sincarnet.data.repository.BluetoothPrinterStorage(
                        context
                    ).getDefaultPrinter()?.mac
                    if (mac.isNullOrBlank()) {
                        printActaProgress = PrintProgress(
                            isVisible = true,
                            isError = true,
                            errorMessage = context.getString(R.string.acta_traslado_no_printer)
                        )
                        return@ActaTrasladoVehiculoFirmasScreen
                    }
                    val agenteSig = uiState.signature.signaturesBySigner["traslado_agente"]
                    val titularSig = uiState.signature.signaturesBySigner["traslado_titular"]
                    printActaProgress = PrintProgress(
                        isVisible = true,
                        currentDoc = context.getString(R.string.acta_traslado_connecting)
                    )
                    com.oscar.sincarnet.data.print.DocumentPrinter.imprimirActaTrasladoCompleto(
                        context = context,
                        mac = mac,
                        agenteSignature = agenteSig,
                        titularSignature = titularSig,
                        onProgress = { index, total, docName ->
                            Log.d("ActaTraslado", "Imprimiendo [$index/$total]: $docName")
                            printActaProgress = PrintProgress(
                                isVisible = true,
                                currentDoc = docName,
                                currentIndex = index,
                                totalDocs = total
                            )
                        },
                        onFinished = {
                            Log.d("ActaTraslado", "Impresión Zebra completada")
                            printActaProgress = PrintProgress()
                        },
                        onError = { msg ->
                            Log.e("ActaTraslado", "Error al imprimir en Zebra: $msg")
                            printActaProgress = PrintProgress(
                                isVisible = true,
                                isError = true,
                                errorMessage = msg.ifEmpty { "Error desconocido al imprimir" }
                            )
                        }
                    )
                },
                agenteSignature = uiState.signature.signaturesBySigner["traslado_agente"],
                titularConductorSignature = uiState.signature.signaturesBySigner["traslado_titular"]
            )
        }

        composable(Route.ActaTrasladoVehiculoFirmaScreen.route) {
            val signerName = when (uiState.signature.currentSignerKey) {
                "traslado_agente" -> stringResource(R.string.acta_traslado_firma_agente)
                "traslado_titular" -> stringResource(R.string.acta_traslado_firma_titular)
                else -> "Firma"
            }
            FirmaManuscritaScreen(
                modifier = Modifier.fillMaxSize(),
                signerName = signerName,
                onSignatureSaved = { bitmap ->
                    viewModel.onSignatureSaved(uiState.signature.currentSignerKey, bitmap)
                    navController.popBackStack()
                },
                onCancel = {
                    viewModel.onSignatureCancelled()
                    navController.popBackStack()
                }
            )
        }

        composable(Route.ColaboracionAlcohol.route) {
            ColaboracionAlcoholScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onPrintClick = { navController.navigate(Route.BluetoothPrinter.route) },
                onContinueClick = {
                    val storage = com.oscar.sincarnet.data.repository.ComplementarioAlcoholStorage(
                        navController.context.toStorage("colaboracion_alcohol_storage")
                    )
                    val data = storage.loadCurrent()
                    when {
                        data.pruebasAlcohol -> navController.navigate(Route.Etilometro.route)
                        data.pruebasDrogas -> navController.navigate(Route.Drogas.route)
                        else -> navController.navigate(Route.ColaboracionAlcoholFirmas.route)
                    }
                }
            )
        }

        composable(Route.Etilometro.route) {
            EtilometroScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onContinueClick = { navController.navigate(Route.ColaboracionAlcoholFirmas.route) }
            )
        }

        composable(Route.Drogas.route) {
            DrogasScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onContinueClick = { navController.navigate(Route.ColaboracionAlcoholFirmas.route) }
            )
        }

        composable(Route.ColaboracionAlcoholFirmas.route) {
            val context = LocalContext.current
            ColaboracionAlcoholFirmasScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onAgenteClick = {
                    viewModel.onCurrentSignerKeyChange("colaboracion_agente")
                    navController.navigate(Route.ColaboracionAlcoholFirmaScreen.route)
                },
                onOperadorClick = {
                    viewModel.onCurrentSignerKeyChange("colaboracion_operador")
                    navController.navigate(Route.ColaboracionAlcoholFirmaScreen.route)
                },
                onPersonaClick = {
                    viewModel.onCurrentSignerKeyChange("colaboracion_persona")
                    navController.navigate(Route.ColaboracionAlcoholFirmaScreen.route)
                },
                onGenerateClick = {
                    android.util.Log.d("AlcoholPDF", "Iniciando generación de PDF...")
                    try {
                        // Cargar datos del storage
                        val storage = com.oscar.sincarnet.data.repository.ComplementarioAlcoholStorage(
                            context.toStorage("colaboracion_alcohol_storage")
                        )
                        val data = storage.loadCurrent()
                        android.util.Log.d("AlcoholPDF", "Datos cargados: fecha=${data.fecha}, lugar=${data.lugar}")
                        
                        // Convertir firmas de ImageBitmap a Bitmap
                        val agenteBitmap = uiState.signature.signaturesBySigner["colaboracion_agente"]?.asAndroidBitmap()
                        val operadorBitmap = uiState.signature.signaturesBySigner["colaboracion_operador"]?.asAndroidBitmap()
                        val personaBitmap = uiState.signature.signaturesBySigner["colaboracion_persona"]?.asAndroidBitmap()
                        android.util.Log.d("AlcoholPDF", "Firmas: agente=${agenteBitmap != null}, operador=${operadorBitmap != null}, persona=${personaBitmap != null}")
                        
                        // Generar PDF
                        android.util.Log.d("AlcoholPDF", "Llamando a generateAlcoholPdf...")
                        val result = com.oscar.sincarnet.data.pdf.AlcoholPdfGenerator.generateAlcoholPdf(
                            context = context,
                            data = data,
                            agenteSignature = agenteBitmap,
                            operadorSignature = operadorBitmap,
                            personaSignature = personaBitmap
                        )
                        
                        android.util.Log.d("AlcoholPDF", "PDF generado: ${result.file.absolutePath}")
                        Toast.makeText(context, "PDF generado: ${result.file.name}", Toast.LENGTH_LONG).show()
                        
                        // Abrir el PDF
                        try {
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                result.file
                            )
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.util.Log.e("AlcoholPDF", "Error al abrir PDF", e)
                            Toast.makeText(context, "PDF generado pero no se pudo abrir: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AlcoholPDF", "Error al generar PDF", e)
                        Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_LONG).show()
                        e.printStackTrace()
                    }
                },
                agenteSignature = uiState.signature.signaturesBySigner["colaboracion_agente"],
                operadorSignature = uiState.signature.signaturesBySigner["colaboracion_operador"],
                personaSignature = uiState.signature.signaturesBySigner["colaboracion_persona"]
            )
        }

        composable(Route.ColaboracionAlcoholFirmaScreen.route) {
            val signerName = when (uiState.signature.currentSignerKey) {
                "colaboracion_agente" -> stringResource(R.string.colaboracion_firmas_agente)
                "colaboracion_operador" -> stringResource(R.string.colaboracion_firmas_operador)
                "colaboracion_persona" -> stringResource(R.string.colaboracion_firmas_persona)
                else -> "Firma"
            }
            FirmaManuscritaScreen(
                modifier = Modifier.fillMaxSize(),
                signerName = signerName,
                onSignatureSaved = { bitmap ->
                    viewModel.onSignatureSaved(uiState.signature.currentSignerKey, bitmap)
                    navController.popBackStack()
                },
                onCancel = {
                    viewModel.onSignatureCancelled()
                    navController.popBackStack()
                }
            )
        }

        composable(Route.AtestadoData.route) {
            TomaDatosAtestadoScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onPrintClick = { navController.navigate(Route.BluetoothPrinter.route) },
                onLocationTimeClick = { navController.navigate(Route.AtestadoOcurrenciaDelit.route) },
                onPersonDataClick = { navController.navigate(Route.AtestadoPersonData.route) },
                onVehicleDataClick = { navController.navigate(Route.AtestadoVehicleData.route) },
                onCourtDataClick = { navController.navigate(Route.AtestadoCourtData.route) },
                onActingDataClick = { navController.navigate(Route.AtestadoActingData.route) },
                onSignaturesClick = { navController.navigate(Route.AtestadoSignatures.route) },
                onScanDocumentClick = { navController.navigate(Route.DocumentScanner.route) },
                printSignatures = PrintSignatures(
                    instructor = uiState.signature.signaturesBySigner[SIGNER_INSTRUCTOR],
                    secretary = uiState.signature.signaturesBySigner[SIGNER_SECRETARY],
                    investigated = uiState.signature.signaturesBySigner[SIGNER_INVESTIGATED],
                    instructorTip = uiState.actuantes.instructorTip,
                    secretaryTip = uiState.actuantes.secretaryTip
                )
            )
        }

        composable(Route.AtestadoOcurrenciaDelit.route) {
            DatosOcurrenciaDelitScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onPrintClick = { navController.navigate(Route.BluetoothPrinter.route) }
            )
        }

        composable(Route.AtestadoCourtData.route) {
            DatosJuzgadoAtestadoScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onPrintClick = { navController.navigate(Route.BluetoothPrinter.route) },
                onPrintSummons = { viewModel.printCitacionIfPossible() }
            )
        }

        composable(Route.AtestadoPersonData.route) {
            DatosPersonaInvestigadaScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onRightsClick = { /* Se implementará en una iteración posterior. */ },
                onManifestacionClick = { navController.navigate(Route.AtestadoManifestacion.route) }
            )
        }

        composable(Route.AtestadoManifestacion.route) {
            ManifestacionScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Route.AtestadoVehicleData.route) {
            LaunchedEffect(Unit) { viewModel.loadVehiculoCurrent() }
            DatosVehiculoScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                brand = uiState.vehiculo.brand,
                onBrandChange = { value -> viewModel.updateVehiculo { it.copy(brand = value) } },
                model = uiState.vehiculo.model,
                onModelChange = { value -> viewModel.updateVehiculo { it.copy(model = value) } },
                plate = uiState.vehiculo.plate,
                onPlateChange = { value -> viewModel.updateVehiculo { it.copy(plate = value) } },
                registrationDate = uiState.vehiculo.registrationDate,
                onRegistrationDateChange = { value -> viewModel.updateVehiculo { it.copy(registrationDate = value) } },
                nationality = uiState.vehiculo.nationality,
                onNationalityChange = { value -> viewModel.updateVehiculo { it.copy(nationality = value) } },
                itvDate = uiState.vehiculo.itvDate,
                onItvDateChange = { value -> viewModel.updateVehiculo { it.copy(itvDate = value) } },
                insurer = uiState.vehiculo.insurer,
                onInsurerChange = { value -> viewModel.updateVehiculo { it.copy(insurer = value) } },
                vehicleType = uiState.vehiculo.vehicleType,
                onVehicleTypeChange = { value -> viewModel.updateVehiculo { it.copy(vehicleType = value) } },
                clasePermiso = uiState.vehiculo.clasePermiso,
                onClasePermisoChange = { value -> viewModel.updateVehiculo { it.copy(clasePermiso = value) } },
                ownerIsOther = uiState.vehiculo.ownerIsOther,
                onOwnerIsOtherChange = { value -> viewModel.updateVehiculo { it.copy(ownerIsOther = value) } },
                ownerName = uiState.vehiculo.ownerName,
                onOwnerNameChange = { value -> viewModel.updateVehiculo { it.copy(ownerName = value) } },
                ownerLastNames = uiState.vehiculo.ownerLastNames,
                onOwnerLastNamesChange = { value -> viewModel.updateVehiculo { it.copy(ownerLastNames = value) } },
                ownerDni = uiState.vehiculo.ownerDni,
                onOwnerDniChange = { value -> viewModel.updateVehiculo { it.copy(ownerDni = value) } },
                ownerAddress = uiState.vehiculo.ownerAddress,
                onOwnerAddressChange = { value -> viewModel.updateVehiculo { it.copy(ownerAddress = value) } },
                ownerPhone = uiState.vehiculo.ownerPhone,
                onOwnerPhoneChange = { value -> viewModel.updateVehiculo { it.copy(ownerPhone = value) } },
                onSaveClick = { viewModel.saveVehiculo() },
                onDeleteClick = { viewModel.deleteVehiculo() }
            )
        }

        composable(Route.AtestadoActingData.route) {
            DatosActuantesScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                instructorEmployment = uiState.actuantes.instructorEmployment,
                onInstructorEmploymentChange = { value -> viewModel.updateActuantes { it.copy(instructorEmployment = value) } },
                instructorTip = uiState.actuantes.instructorTip,
                onInstructorTipChange = { value -> viewModel.updateActuantes { it.copy(instructorTip = value) } },
                instructorUnit = uiState.actuantes.instructorUnit,
                onInstructorUnitChange = { value ->
                    viewModel.updateActuantes {
                        val updated = it.copy(instructorUnit = value)
                        if (updated.sameUnit) updated.copy(secretaryUnit = value) else updated
                    }
                },
                secretaryEmployment = uiState.actuantes.secretaryEmployment,
                onSecretaryEmploymentChange = { value -> viewModel.updateActuantes { it.copy(secretaryEmployment = value) } },
                secretaryTip = uiState.actuantes.secretaryTip,
                onSecretaryTipChange = { value -> viewModel.updateActuantes { it.copy(secretaryTip = value) } },
                secretaryUnit = uiState.actuantes.secretaryUnit,
                onSecretaryUnitChange = { value -> viewModel.updateActuantes { it.copy(secretaryUnit = value) } },
                sameUnit = uiState.actuantes.sameUnit,
                onSameUnitChange = { value ->
                    viewModel.updateActuantes {
                        val updated = it.copy(sameUnit = value)
                        if (value) updated.copy(secretaryUnit = updated.instructorUnit) else updated
                    }
                },
                tipHistory = uiState.actuantes.tipHistory,
                unitHistory = uiState.actuantes.unitHistory,
                onSaveClick = {
                    viewModel.saveActuantes()
                    navController.popBackStack()
                },
                onDeleteClick = { viewModel.deleteActuantesWithBackup() },
                onRecoverClick = { viewModel.recoverActuantes() },
                canRecover = uiState.actuantes.canRecover,
                statusMessage = uiState.actuantes.statusMessage
            )
        }

        composable(Route.AtestadoSignatures.route) {
            FirmasAtestadoScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onPrintClick = { navController.navigate(Route.BluetoothPrinter.route) },
                instructorSignature = uiState.signature.signaturesBySigner[SIGNER_INSTRUCTOR],
                secretarySignature = uiState.signature.signaturesBySigner[SIGNER_SECRETARY],
                investigatedSignature = uiState.signature.signaturesBySigner[SIGNER_INVESTIGATED],
                secondDriverSignature = uiState.signature.signaturesBySigner[SIGNER_SECOND_DRIVER],
                onInstructorClick = {
                    viewModel.onCurrentSignerKeyChange(SIGNER_INSTRUCTOR)
                    navController.navigate(Route.FirmaScreen.route)
                },
                onSecretaryClick = {
                    viewModel.onCurrentSignerKeyChange(SIGNER_SECRETARY)
                    navController.navigate(Route.FirmaScreen.route)
                },
                onInvestigatedClick = {
                    viewModel.onCurrentSignerKeyChange(SIGNER_INVESTIGATED)
                    navController.navigate(Route.FirmaScreen.route)
                },
                onSecondDriverClick = {
                    viewModel.onCurrentSignerKeyChange(SIGNER_SECOND_DRIVER)
                    navController.navigate(Route.FirmaScreen.route)
                },
                selectedGenerateReason = uiState.form.generateReason,
                onSelectedGenerateReasonChange = { value -> viewModel.updateForm { it.copy(generateReason = value) } },
                selectedArticleNorm = uiState.form.articleNorm,
                onSelectedArticleNormChange = { value -> viewModel.updateForm { it.copy(articleNorm = value) } },
                selectedArticleText = uiState.form.articleText,
                onSelectedArticleTextChange = { value -> viewModel.updateForm { it.copy(articleText = value) } },
                dgtNoRecord = uiState.form.dgtNoRecord,
                onDgtNoRecordChange = { value -> viewModel.updateForm { it.copy(dgtNoRecord = value) } },
                internationalNoRecord = uiState.form.internationalNoRecord,
                onInternationalNoRecordChange = { value -> viewModel.updateForm { it.copy(internationalNoRecord = value) } },
                existsRecord = uiState.form.existsRecord,
                onExistsRecordChange = { value -> viewModel.updateForm { it.copy(existsRecord = value) } },
                vicisitudesOption = uiState.form.vicisitudesOption,
                onVicisitudesOptionChange = { value -> viewModel.updateForm { it.copy(vicisitudesOption = value) } },
                jefaturaProvincial = uiState.form.jefaturaProvincial,
                onJefaturaProvincialChange = { value -> viewModel.updateForm { it.copy(jefaturaProvincial = value) } },
                tiempoPrivacion = uiState.form.tiempoPrivacion,
                onTiempoPrivacionChange = { value -> viewModel.updateForm { it.copy(tiempoPrivacion = value) } },
                juzgadoDecreta = uiState.form.juzgadoDecreta,
                onJuzgadoDecretaChange = { value -> viewModel.updateForm { it.copy(juzgadoDecreta = value) } },
                wantsToSign = uiState.signature.wantsToSign,
                onWantsToSignChange = { value -> viewModel.onWantsToSignChange(value) },
                hasSecondDriver = uiState.form.hasSecondDriver,
                onHasSecondDriverChange = { value -> viewModel.onHasSecondDriverChange(value) },
                onGenerateAtestadoClick = { wantsToSign, hasSecondDriver, reason, articleNorm, articleText ->
                    viewModel.generateAtestadoPdf(
                        wantsToSign = wantsToSign,
                        hasSecondDriver = hasSecondDriver,
                        reason = reason,
                        articleNorm = articleNorm,
                        articleText = articleText
                    )
                },
                investigatedCopyEnabled = uiState.document.lastGeneratedPdfPath.isNotBlank(),
                onPrintInvestigatedCopyClick = { /* Se implementará en una iteración posterior. */ },
                shareEnabled = uiState.document.lastGeneratedPdfPath.isNotBlank(),
                onSharePdfClick = { viewModel.onSharePdfClick() },
                shareOdtEnabled = uiState.document.lastGeneratedPdfPath.isNotBlank(),
                onShareOdtClick = { viewModel.generateAtestadoOdt() },
                isGeneratingAtestado = uiState.document.isGeneratingAtestado,
                isGeneratingOdt = uiState.document.isGeneratingOdt
            )
        }

        composable(Route.FirmaScreen.route) {
            FirmaManuscritaScreen(
                modifier = Modifier.fillMaxSize(),
                signerName = when (uiState.signature.currentSignerKey) {
                    SIGNER_INSTRUCTOR -> stringResource(R.string.atestado_signature_instructor)
                    SIGNER_SECRETARY -> stringResource(R.string.atestado_signature_secretary)
                    SIGNER_INVESTIGATED -> stringResource(R.string.atestado_signature_investigated)
                    SIGNER_SECOND_DRIVER -> stringResource(R.string.atestado_signature_second_driver)
                    else -> ""
                },
                onSignatureSaved = { bitmap ->
                    viewModel.onSignatureSaved(uiState.signature.currentSignerKey, bitmap)
                    navController.popBackStack()
                },
                onCancel = {
                    viewModel.onSignatureCancelled()
                    navController.popBackStack()
                }
            )
        }

        composable(Route.BluetoothPrinter.route) {
            BluetoothPrinterScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Route.DocumentScanner.route) {
            DocumentScannerScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onOpenPdf = onOpenPdf,
                onSharePdf = onSharePdf
            )
        }

        composable(Route.OficioCustodiaSangre.route) {
            OficioCustodiaSangreScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onContinueClick = { navController.navigate(Route.OficioCustodiaSangreDoc.route) }
            )
        }

        composable(Route.OficioCustodiaSangreDoc.route) {
            OficioCustodiaSangreDocScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onContinueClick = { navController.navigate(Route.OficioCustodiaSangreFirmas.route) }
            )
        }

        composable(Route.OficioCustodiaSangreFirmas.route) {
            val context = LocalContext.current
            OficioCustodiaSangreFirmasScreen(
                modifier = Modifier.fillMaxSize(),
                agenteSigned = uiState.signature.signaturesBySigner["ocs_agente"] != null,
                onBackClick = { navController.popBackStack() },
                onAgenteFirmaClick = {
                    viewModel.onCurrentSignerKeyChange("ocs_agente")
                    navController.navigate(Route.OficioCustodiaSangreFirmaScreen.route)
                },
                onGenerateClick = {
                    try {
                        val storage = OficioCustodiaSangreStorage(
                            context.toStorage("oficio_custodia_sangre_storage")
                        )
                        val data = storage.loadCurrent()
                        val agenteBitmap = uiState.signature.signaturesBySigner["ocs_agente"]?.asAndroidBitmap()
                        val result = OficioCustodiaSangrePdfGenerator.generatePdf(
                            context = context,
                            data = data,
                            agenteSignature = agenteBitmap
                        )
                        Toast.makeText(context, "PDF generado: ${result.file.name}", Toast.LENGTH_LONG).show()
                        try {
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                result.file
                            )
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("OficioCustodiaSangre", "Error al abrir PDF", e)
                            Toast.makeText(context, "PDF generado pero no se pudo abrir: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e("OficioCustodiaSangre", "Error al generar PDF", e)
                        Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        composable(Route.OficioCustodiaSangreFirmaScreen.route) {
            val signerName = stringResource(R.string.ocs_firma_agente)
            FirmaManuscritaScreen(
                modifier = Modifier.fillMaxSize(),
                signerName = signerName,
                onSignatureSaved = { bitmap ->
                    viewModel.onSignatureSaved("ocs_agente", bitmap)
                    navController.popBackStack()
                },
                onCancel = {
                    viewModel.onSignatureCancelled()
                    navController.popBackStack()
                }
            )
        }

        composable(Route.SolicitudCustodiaJuzgado.route) {
            SolicitudCustodiaJuzgadoScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onContinueClick = { navController.navigate(Route.SolicitudCustodiaJuzgadoDoc.route) }
            )
        }

        composable(Route.SolicitudCustodiaJuzgadoDoc.route) {
            SolicitudCustodiaJuzgadoDocScreen(
                modifier = Modifier.fillMaxSize(),
                onBackClick = { navController.popBackStack() },
                onContinueClick = { navController.navigate(Route.SolicitudCustodiaJuzgadoFirmas.route) }
            )
        }

        composable(Route.SolicitudCustodiaJuzgadoFirmas.route) {
            val context = LocalContext.current
            SolicitudCustodiaJuzgadoFirmasScreen(
                modifier = Modifier.fillMaxSize(),
                agenteSigned = uiState.signature.signaturesBySigner["scj_agente"] != null,
                onBackClick = { navController.popBackStack() },
                onAgenteFirmaClick = {
                    viewModel.onCurrentSignerKeyChange("scj_agente")
                    navController.navigate(Route.SolicitudCustodiaJuzgadoFirmaScreen.route)
                },
                onGenerateClick = {
                    try {
                        val storage = SolicitudCustodiaJuzgadoStorage(
                            context.toStorage("solicitud_custodia_juzgado_storage")
                        )
                        val data = storage.loadCurrent()
                        val agenteBitmap = uiState.signature.signaturesBySigner["scj_agente"]?.asAndroidBitmap()
                        val result = SolicitudCustodiaJuzgadoPdfGenerator.generatePdf(
                            context = context,
                            data = data,
                            agenteSignature = agenteBitmap
                        )
                        Toast.makeText(context, "PDF generado: ${result.file.name}", Toast.LENGTH_LONG).show()
                        try {
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                result.file
                            )
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("SolicitudCustodiaJuzgado", "Error al abrir PDF", e)
                            Toast.makeText(context, "PDF generado pero no se pudo abrir: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e("SolicitudCustodiaJuzgado", "Error al generar PDF", e)
                        Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        composable(Route.SolicitudCustodiaJuzgadoFirmaScreen.route) {
            val signerName = stringResource(R.string.scj_firma_agente)
            FirmaManuscritaScreen(
                modifier = Modifier.fillMaxSize(),
                signerName = signerName,
                onSignatureSaved = { bitmap ->
                    viewModel.onSignatureSaved("scj_agente", bitmap)
                    navController.popBackStack()
                },
                onCancel = {
                    viewModel.onSignatureCancelled()
                    navController.popBackStack()
                }
            )
        }
    }

    if (uiState.showAboutDialog) {
        AboutDialog(onDismissRequest = { viewModel.onShowAboutDialogChange(false) })
    }
}
