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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.oscar.sincarnet.PrintProgress
import com.oscar.sincarnet.imprimirAtestadoCompleto
import androidx.compose.material3.LinearProgressIndicator
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
    secondDriverName: String = "",
    onSecondDriverNameChange: (String) -> Unit = {},
    secondDriverId: String = "",
    onSecondDriverIdChange: (String) -> Unit = {},
    selectedGenerateReason: String = GenerateReasonOption.SiniestroVial.value,
    onSelectedGenerateReasonChange: (String) -> Unit = {},
    selectedArticleNorm: String = ArticleNormOption.LSV.value,
    onSelectedArticleNormChange: (String) -> Unit = {},
    selectedArticleText: String = "",
    onSelectedArticleTextChange: (String) -> Unit = {},
    dgtNoRecord: Boolean = false,
    onDgtNoRecordChange: (Boolean) -> Unit = {},
    internationalNoRecord: Boolean = false,
    onInternationalNoRecordChange: (Boolean) -> Unit = {},
    existsRecord: Boolean = false,
    onExistsRecordChange: (Boolean) -> Unit = {},
    vicisitudesOption: String = "",
    onVicisitudesOptionChange: (String) -> Unit = {},
    jefaturaProvincial: String = "",
    onJefaturaProvincialChange: (String) -> Unit = {},
    tiempoPrivacion: String = "",
    onTiempoPrivacionChange: (String) -> Unit = {},
    juzgadoDecreta: String = "",
    onJuzgadoDecretaChange: (String) -> Unit = {},
    wantsToSign: Boolean = true,
    onWantsToSignChange: (Boolean) -> Unit = {},
    hasSecondDriver: Boolean = false,
    onHasSecondDriverChange: (Boolean) -> Unit = {},
    onGenerateAtestadoClick: (wantsToSign: Boolean, hasSecondDriver: Boolean, reason: String, articleNorm: String?, articleText: String) -> Unit = { _, _, _, _, _ -> },
    investigatedCopyEnabled: Boolean = false,
    onPrintInvestigatedCopyClick: () -> Unit = {},
    shareEnabled: Boolean = false,
    onSharePdfClick: () -> Unit = {},
    isGeneratingAtestado: Boolean = false
) {
    var showGenerateReasonDialog by rememberSaveable { mutableStateOf(false) }
    var showArticleNormDialog by rememberSaveable { mutableStateOf(false) }
    var showBaseDatosDialog by rememberSaveable { mutableStateOf(false) }
    var showVicisitudesDialog by rememberSaveable { mutableStateOf(false) }
    var showJefaturaDialog by rememberSaveable { mutableStateOf(false) }
    var showCondenaDetailsDialog by rememberSaveable { mutableStateOf(false) }
    var showSecondDriverDialog by rememberSaveable { mutableStateOf(false) }

    var printProgress by remember { mutableStateOf<PrintProgress>(PrintProgress()) }

    val selectedReasonOption = GenerateReasonOption.fromValue(selectedGenerateReason)
    val selectedNormOption = ArticleNormOption.fromValue(selectedArticleNorm)

    // Las firmas vienen del estado externo en memoria.
    val instructorSigned = instructorSignature != null
    val secretarySigned = secretarySignature != null
    val investigatedSigned = investigatedSignature != null
    val secondDriverSigned = secondDriverSignature != null

    val generateEnabled = instructorSigned &&
            secretarySigned &&
            (!wantsToSign || investigatedSigned) &&
            (!hasSecondDriver || secondDriverSigned)

    val context = LocalContext.current

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

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = wantsToSign,
                            onCheckedChange = { onWantsToSignChange(it) }
                        )
                        Text(text = stringResource(R.string.atestado_signature_wants_to_sign))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = hasSecondDriver,
                            onCheckedChange = { onHasSecondDriverChange(it) }
                        )
                        Text(text = stringResource(R.string.atestado_signature_has_second_driver))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                    onClick = { showSecondDriverDialog = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                AtestadoSignatureButton(
                    text = stringResource(R.string.atestado_signature_generate),
                    enabled = generateEnabled && !isGeneratingAtestado,
                    isSigned = false,
                    onClick = { showGenerateReasonDialog = true }
                )

                AtestadoSignatureButton(
                    text = stringResource(R.string.atestado_signature_print_investigated_copy),
                    enabled = investigatedCopyEnabled && !isGeneratingAtestado && !printProgress.isVisible,
                    isSigned = false,
                    onClick = {
                        val mac = BluetoothPrinterStorage(context).getDefaultPrinter()?.mac
                        if (mac.isNullOrBlank()) {
                            printProgress = PrintProgress(
                                isVisible    = true,
                                isError      = true,
                                errorMessage = "No hay impresora configurada"
                            )
                            return@AtestadoSignatureButton
                        }
                        // PASAR LAS FIRMAS ACTUALES
                        val sigs = PrintSignatures(
                            instructor  = instructorSignature,
                            secretary   = secretarySignature,
                            investigated = investigatedSignature
                        )
                        imprimirAtestadoCompleto(
                            context  = context,
                            mac      = mac,
                            sigs     = sigs,
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
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                AtestadoSignatureButton(
                    text = stringResource(R.string.atestado_signature_share_pdf),
                    enabled = shareEnabled,
                    isSigned = false,
                    onClick = onSharePdfClick
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

    if (isGeneratingAtestado) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = stringResource(R.string.atestado_signature_generate)) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(text = stringResource(R.string.generating_pdf_message))
                }
            },
            confirmButton = {}
        )
    }

    if (showGenerateReasonDialog && !isGeneratingAtestado) {
        AlertDialog(
            onDismissRequest = { showGenerateReasonDialog = false },
            title = {
                Text(text = stringResource(R.string.atestado_generate_reason_title))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = stringResource(R.string.atestado_generate_reason_message))
                    OptionRadioRow(
                        text = stringResource(R.string.atestado_generate_reason_siniestro_vial),
                        selected = selectedReasonOption == GenerateReasonOption.SiniestroVial,
                        onSelect = {
                            onSelectedGenerateReasonChange(GenerateReasonOption.SiniestroVial.value)
                        }
                    )
                    OptionRadioRow(
                        text = stringResource(R.string.atestado_generate_reason_control_preventivo),
                        selected = selectedReasonOption == GenerateReasonOption.ControlPreventivo,
                        onSelect = {
                            onSelectedGenerateReasonChange(GenerateReasonOption.ControlPreventivo.value)
                        }
                    )
                    OptionRadioRow(
                        text = stringResource(R.string.atestado_generate_reason_infraccion),
                        selected = selectedReasonOption == GenerateReasonOption.CometerInfraccion,
                        onSelect = {
                            onSelectedGenerateReasonChange(GenerateReasonOption.CometerInfraccion.value)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGenerateReasonDialog = false
                        if (selectedReasonOption == GenerateReasonOption.CometerInfraccion) {
                            showArticleNormDialog = true
                        } else {
                            showBaseDatosDialog = true
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.continue_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateReasonDialog = false }) {
                    Text(text = stringResource(R.string.cancel_action))
                }
            }
        )
    }

    if (showArticleNormDialog && !isGeneratingAtestado) {
        AlertDialog(
            onDismissRequest = { showArticleNormDialog = false },
            title = {
                Text(text = stringResource(R.string.atestado_generate_article_norm_title))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = stringResource(R.string.atestado_generate_article_norm_message))
                    OutlinedTextField(
                        value = selectedArticleText,
                        onValueChange = onSelectedArticleTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(text = stringResource(R.string.atestado_generate_article_label)) },
                        placeholder = { Text(text = stringResource(R.string.atestado_generate_article_hint)) }
                    )
                    OptionRadioRow(
                        text = stringResource(R.string.atestado_generate_norm_lsv),
                        selected = selectedNormOption == ArticleNormOption.LSV,
                        onSelect = { onSelectedArticleNormChange(ArticleNormOption.LSV.value) }
                    )
                    OptionRadioRow(
                        text = stringResource(R.string.atestado_generate_norm_rgcir),
                        selected = selectedNormOption == ArticleNormOption.RGCir,
                        onSelect = { onSelectedArticleNormChange(ArticleNormOption.RGCir.value) }
                    )
                    OptionRadioRow(
                        text = stringResource(R.string.atestado_generate_norm_rgcond),
                        selected = selectedNormOption == ArticleNormOption.RGCond,
                        onSelect = { onSelectedArticleNormChange(ArticleNormOption.RGCond.value) }
                    )
                    OptionRadioRow(
                        text = stringResource(R.string.atestado_generate_norm_rgveh),
                        selected = selectedNormOption == ArticleNormOption.RGVeh,
                        onSelect = { onSelectedArticleNormChange(ArticleNormOption.RGVeh.value) }
                    )
                    OptionRadioRow(
                        text = stringResource(R.string.atestado_generate_norm_soa),
                        selected = selectedNormOption == ArticleNormOption.SOA,
                        onSelect = { onSelectedArticleNormChange(ArticleNormOption.SOA.value) }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showArticleNormDialog = false
                        showBaseDatosDialog = true
                    }
                ) {
                    Text(text = stringResource(R.string.continue_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showArticleNormDialog = false }) {
                    Text(text = stringResource(R.string.cancel_action))
                }
            }
        )
    }

    // --- Modal 3: Bases de datos consultadas ---
    if (showBaseDatosDialog && !isGeneratingAtestado) {
        AlertDialog(
            onDismissRequest = { showBaseDatosDialog = false },
            title = { Text(text = stringResource(R.string.atestado_bases_datos_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = dgtNoRecord,
                                onValueChange = { onDgtNoRecordChange(it) }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = dgtNoRecord, onCheckedChange = null)
                        Text(text = stringResource(R.string.atestado_bases_datos_no_dgt))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = internationalNoRecord,
                                onValueChange = { onInternationalNoRecordChange(it) }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = internationalNoRecord, onCheckedChange = null)
                        Text(text = stringResource(R.string.atestado_bases_datos_no_international))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = existsRecord,
                                onValueChange = { onExistsRecordChange(it) }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = existsRecord, onCheckedChange = null)
                        Text(text = stringResource(R.string.atestado_bases_datos_exists))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBaseDatosDialog = false
                        if (existsRecord) {
                            showVicisitudesDialog = true
                        } else {
                            onGenerateAtestadoClick(
                                wantsToSign,
                                hasSecondDriver,
                                selectedReasonOption.value,
                                if (selectedReasonOption == GenerateReasonOption.CometerInfraccion) selectedNormOption.value else null,
                                selectedArticleText
                            )
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.continue_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBaseDatosDialog = false }) {
                    Text(text = stringResource(R.string.cancel_action))
                }
            }
        )
    }

    // --- Modal 4: Vicisitudes Registro de Conductores ---
    if (showVicisitudesDialog && !isGeneratingAtestado) {
        val selectedVicisitudesOpt = VicisitudesOption.fromValue(vicisitudesOption)
        AlertDialog(
            onDismissRequest = { showVicisitudesDialog = false },
            title = { Text(text = stringResource(R.string.atestado_vicisitudes_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionRadioRow(
                        text = stringResource(R.string.atestado_vicisitudes_no_obtenido),
                        selected = selectedVicisitudesOpt == VicisitudesOption.NoObtenidoNunca,
                        onSelect = { onVicisitudesOptionChange(VicisitudesOption.NoObtenidoNunca.value) }
                    )
                    OptionRadioRow(
                        text = stringResource(R.string.atestado_vicisitudes_perdida_puntos),
                        selected = selectedVicisitudesOpt == VicisitudesOption.PerdidaVigenciaPuntos,
                        onSelect = { onVicisitudesOptionChange(VicisitudesOption.PerdidaVigenciaPuntos.value) }
                    )
                    OptionRadioRow(
                        text = stringResource(R.string.atestado_vicisitudes_condena_firme),
                        selected = selectedVicisitudesOpt == VicisitudesOption.CondenaFirme,
                        onSelect = { onVicisitudesOptionChange(VicisitudesOption.CondenaFirme.value) }
                    )
                    OptionRadioRow(
                        text = stringResource(R.string.atestado_vicisitudes_no_consta_cursos),
                        selected = selectedVicisitudesOpt == VicisitudesOption.NoConstaCursos,
                        onSelect = { onVicisitudesOptionChange(VicisitudesOption.NoConstaCursos.value) }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showVicisitudesDialog = false
                        when (VicisitudesOption.fromValue(vicisitudesOption)) {
                            VicisitudesOption.PerdidaVigenciaPuntos -> showJefaturaDialog = true
                            VicisitudesOption.CondenaFirme -> showCondenaDetailsDialog = true
                            else -> onGenerateAtestadoClick(
                                wantsToSign,
                                hasSecondDriver,
                                selectedReasonOption.value,
                                if (selectedReasonOption == GenerateReasonOption.CometerInfraccion) selectedNormOption.value else null,
                                selectedArticleText
                            )
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.continue_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showVicisitudesDialog = false }) {
                    Text(text = stringResource(R.string.cancel_action))
                }
            }
        )
    }

    // --- Modal 5: Jefatura Provincial (Pérdida de vigencia por pérdida de puntos) ---
    if (showJefaturaDialog && !isGeneratingAtestado) {
        AlertDialog(
            onDismissRequest = { showJefaturaDialog = false },
            title = { Text(text = stringResource(R.string.atestado_jefatura_title)) },
            text = {
                OutlinedTextField(
                    value = jefaturaProvincial,
                    onValueChange = onJefaturaProvincialChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(text = stringResource(R.string.atestado_jefatura_label)) }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showJefaturaDialog = false
                        onGenerateAtestadoClick(
                            wantsToSign,
                            hasSecondDriver,
                            selectedReasonOption.value,
                            if (selectedReasonOption == GenerateReasonOption.CometerInfraccion) selectedNormOption.value else null,
                            selectedArticleText
                        )
                    }
                ) {
                    Text(text = stringResource(R.string.continue_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showJefaturaDialog = false }) {
                    Text(text = stringResource(R.string.cancel_action))
                }
            }
        )
    }

    // --- Modal 6: Condena firme en vigor ---
    if (showCondenaDetailsDialog && !isGeneratingAtestado) {
        AlertDialog(
            onDismissRequest = { showCondenaDetailsDialog = false },
            title = { Text(text = stringResource(R.string.atestado_condena_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tiempoPrivacion,
                        onValueChange = onTiempoPrivacionChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(text = stringResource(R.string.atestado_condena_tiempo_privacion)) }
                    )
                    OutlinedTextField(
                        value = juzgadoDecreta,
                        onValueChange = onJuzgadoDecretaChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(text = stringResource(R.string.atestado_condena_juzgado_decreta)) }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCondenaDetailsDialog = false
                        onGenerateAtestadoClick(
                            wantsToSign,
                            hasSecondDriver,
                            selectedReasonOption.value,
                            if (selectedReasonOption == GenerateReasonOption.CometerInfraccion) selectedNormOption.value else null,
                            selectedArticleText
                        )
                    }
                ) {
                    Text(text = stringResource(R.string.continue_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCondenaDetailsDialog = false }) {
                    Text(text = stringResource(R.string.cancel_action))
                }
            }
        )
    }
    if (showSecondDriverDialog) {
        var localName by remember { mutableStateOf(secondDriverName) }
        var localId by remember { mutableStateOf(secondDriverId) }

        AlertDialog(
            onDismissRequest = { showSecondDriverDialog = false },
            title = { Text(text = stringResource(R.string.atestado_second_driver_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = localName,
                        onValueChange = { localName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(text = stringResource(R.string.atestado_second_driver_name)) }
                    )
                    OutlinedTextField(
                        value = localId,
                        onValueChange = { localId = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(text = stringResource(R.string.atestado_second_driver_id)) }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSecondDriverNameChange(localName)
                        onSecondDriverIdChange(localId)
                        showSecondDriverDialog = false
                        onSecondDriverClick()
                    }
                ) {
                    Text(text = stringResource(R.string.continue_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecondDriverDialog = false }) {
                    Text(text = stringResource(R.string.cancel_action))
                }
            }
        )
    }

    if (printProgress.isVisible) {
        AlertDialog(
            onDismissRequest = {
                if (!printProgress.isError) printProgress = PrintProgress()
            },
            title = {
                Text(
                    text = if (printProgress.isError)
                        stringResource(R.string.print_error_title)
                    else
                        stringResource(R.string.printing_title),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                if (printProgress.isError) {
                    Text(text = printProgress.errorMessage ?: "Error desconocido")
                } else {
                    Column {
                        Text(text = printProgress.currentDoc ?: "Imprimiendo...")
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(progress = if (printProgress.totalDocs > 0) printProgress.currentIndex.toFloat() / printProgress.totalDocs else 0f)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "${printProgress.currentIndex} / ${printProgress.totalDocs}")
                    }
                }
            },
            confirmButton = {
                if (printProgress.isError) {
                    TextButton(onClick = { printProgress = PrintProgress() }) {
                        Text(text = stringResource(R.string.close_action))
                    }
                }
            }
        )
    }
}

private enum class VicisitudesOption(val value: String) {
    NoObtenidoNunca("No ha obtenido nunca"),
    PerdidaVigenciaPuntos("Pérdida de vigencia por pérdida de puntos"),
    CondenaFirme("Condena firme en vigor"),
    NoConstaCursos("No consta realización y superación de cursos");

    companion object {
        fun fromValue(value: String): VicisitudesOption? =
            entries.firstOrNull { it.value == value }
    }
}

private enum class GenerateReasonOption(val value: String) {
    SiniestroVial("Siniestro Vial"),
    ControlPreventivo("Control preventivo"),
    CometerInfraccion("Cometer infracción");

    companion object {
        fun fromValue(value: String): GenerateReasonOption =
            entries.firstOrNull { it.value == value } ?: SiniestroVial
    }
}

private enum class ArticleNormOption(val value: String) {
    LSV("LSV"),
    RGCir("RGCir"),
    RGCond("RGCond"),
    RGVeh("RGVeh"),
    SOA("SOA");

    companion object {
        fun fromValue(value: String): ArticleNormOption =
            entries.firstOrNull { it.value == value } ?: LSV
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