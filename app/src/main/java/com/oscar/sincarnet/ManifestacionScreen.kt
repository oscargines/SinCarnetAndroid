package com.oscar.sincarnet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.oscar.sincarnet.ui.theme.SinCarnetTheme

/** IDs estables para pruebas UI y trazabilidad semántica de la pantalla. */
private object ManifestacionUiIds {
    const val ROOT = "manifestacion_root"
    const val CONTENT_CARD = "manifestacion_content_card"
    const val SCROLL_CONTENT = "manifestacion_scroll_content"
    const val TITLE = "manifestacion_title"
    const val BODY = "manifestacion_body"
    const val OPCIONES_TITLE = "manifestacion_opciones_title"
    const val OPCION_RENUNCIA = "manifestacion_opcion_renuncia"
    const val OPCION_DECLARAR = "manifestacion_opcion_declarar"
    const val DOCUMENTACION_TITLE = "manifestacion_documentacion_title"
    const val PREGUNTAS_TITLE = "manifestacion_preguntas_title"
    const val ACTIONS_ROW = "manifestacion_actions_row"
    const val BTN_SAVE = "manifestacion_btn_save"
    const val BTN_DELETE = "manifestacion_btn_delete"
    const val NAV_ROW = "manifestacion_nav_row"
    const val DIALOG_DELETE = "manifestacion_dialog_delete"
    const val DIALOG_SAVE = "manifestacion_dialog_save"
    const val DIALOG_REQUIRED = "manifestacion_dialog_required"

    fun anexo(id: String): String = "manifestacion_anexo_${id.replace(" ", "_").lowercase()}"
    fun pregunta(id: Int): String = "manifestacion_pregunta_$id"
    fun preguntaTexto(id: Int): String = "manifestacion_pregunta_texto_$id"
    fun preguntaRespuesta(id: Int): String = "manifestacion_pregunta_respuesta_$id"
}

/**
 * Pantalla de manifestación de la persona investigada.
 *
 * Renderiza documento base de manifestación, opciones principales y respuestas
 * a preguntas anexas, con persistencia en [ManifestacionStorage].
 *
 * @param modifier Modificador raíz
 * @param onBackClick Vuelve a la pantalla anterior
 */
@Composable
fun ManifestacionScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val document = remember(context) { loadManifestacionDocument(context) }
    val storage = remember(context) { ManifestacionStorage(context) }
    val initialData = remember(context) { storage.loadCurrent() }
    val personData = remember(context) { PersonaInvestigadaStorage(context).loadCurrent() }
    val courtData = remember(context) { JuzgadoAtestadoStorage(context).loadCurrent() }
    val ocurrenciaData = remember(context) { OcurrenciaDelitStorage(context).loadCurrent() }
    val vehicleData = remember(context) { VehiculoStorage(context).loadCurrent() }

    var renunciaAsistenciaLetrada by rememberSaveable { mutableStateOf(initialData.renunciaAsistenciaLetrada) }
    var deseaDeclarar by rememberSaveable { mutableStateOf(initialData.deseaDeclarar) }
    val respuestasPreguntas = remember {
        mutableStateMapOf<Int, String>().apply {
            document.preguntas.forEach { pregunta ->
                put(pregunta.id, initialData.respuestasPreguntas[pregunta.id].orEmpty())
            }
        }
    }

    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var showSaveConfirmation by rememberSaveable { mutableStateOf(false) }
    var showRequiredFieldsError by rememberSaveable { mutableStateOf(false) }
    var requiredFieldsErrorText by rememberSaveable { mutableStateOf("") }
    val requiredOption1Label = stringResource(R.string.manifestacion_required_option_1)
    val requiredOption2Label = stringResource(R.string.manifestacion_required_option_2)
    val noDeseaManifestacionText = stringResource(R.string.manifestacion_no_declarar_text)
    val lastQuestionId = remember(document.preguntas) { document.preguntas.lastOrNull()?.id }
    val renunciaOption = remember(document.opciones) {
        document.opciones.firstOrNull { it.id == 1 } ?: document.opciones.firstOrNull()
    }
    val declararOption = remember(document.opciones) {
        document.opciones.firstOrNull { it.id == 2 } ?: document.opciones.getOrNull(1)
    }

    fun reloadFromStorage() {
        val latestData = storage.loadCurrent()
        renunciaAsistenciaLetrada = latestData.renunciaAsistenciaLetrada
        deseaDeclarar = latestData.deseaDeclarar
        respuestasPreguntas.clear()
        document.preguntas.forEach { pregunta ->
            respuestasPreguntas[pregunta.id] = latestData.respuestasPreguntas[pregunta.id].orEmpty()
        }
    }

    // Carga inicial al entrar en la pantalla
    LaunchedEffect(Unit) {
        reloadFromStorage()
    }

    fun buildManifestacionSnapshot(): ManifestacionData {
        val respuestasToSave = if (deseaDeclarar == false) {
            document.preguntas.associate { pregunta ->
                pregunta.id to if (pregunta.id == lastQuestionId) noDeseaManifestacionText else ""
            }
        } else {
            respuestasPreguntas.toMap()
        }

        return ManifestacionData(
            renunciaAsistenciaLetrada = renunciaAsistenciaLetrada,
            deseaDeclarar = deseaDeclarar,
            respuestasPreguntas = respuestasToSave
        )
    }

    fun persistManifestacionDraft() {
        storage.saveCurrent(buildManifestacionSnapshot())
    }

    // Sincroniza borrador al salir y recarga al volver al foco
    DisposableEffect(lifecycleOwner, document) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> persistManifestacionDraft()
                Lifecycle.Event.ON_RESUME -> reloadFromStorage()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .testTag(ManifestacionUiIds.ROOT),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag(ManifestacionUiIds.CONTENT_CARD),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .testTag(ManifestacionUiIds.SCROLL_CONTENT),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                renunciaOption?.let { option ->
                    Column(modifier = Modifier.testTag(ManifestacionUiIds.OPCION_RENUNCIA)) {
                        HorizontalYesNoQuestionBlock(
                            questionText = option.texto,
                            selectedValue = renunciaAsistenciaLetrada,
                            onValueChange = {
                                renunciaAsistenciaLetrada = it
                                persistManifestacionDraft()
                            }
                        )
                    }
                }

                declararOption?.let { option ->
                    Column(modifier = Modifier.testTag(ManifestacionUiIds.OPCION_DECLARAR)) {
                        HorizontalYesNoQuestionBlock(
                            questionText = option.texto,
                            selectedValue = deseaDeclarar,
                            onValueChange = {
                                deseaDeclarar = it
                                persistManifestacionDraft()
                            }
                        )
                    }
                }

                if (document.preguntas.isNotEmpty()) {
                    document.preguntas.forEach { pregunta ->
                        val noDeseaDeclarar = deseaDeclarar == false
                        val isLastQuestion = pregunta.id == lastQuestionId
                        val currentValue = if (noDeseaDeclarar) {
                            if (isLastQuestion) noDeseaManifestacionText else ""
                        } else {
                            respuestasPreguntas[pregunta.id].orEmpty()
                        }

                        Text(
                            text = pregunta.pregunta,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag(ManifestacionUiIds.preguntaTexto(pregunta.id))
                        )

                        OutlinedTextField(
                            value = currentValue,
                            onValueChange = {
                                if (!noDeseaDeclarar) {
                                    respuestasPreguntas[pregunta.id] = it
                                    persistManifestacionDraft()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(ManifestacionUiIds.preguntaRespuesta(pregunta.id)),
                            minLines = 3,
                            maxLines = 6,
                            label = null,
                            enabled = !noDeseaDeclarar,
                            readOnly = noDeseaDeclarar
                        )
                    }
                }

            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ManifestacionUiIds.ACTIONS_ROW),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val missing = mutableListOf<String>()
                    if (renunciaAsistenciaLetrada == null) {
                        missing += requiredOption1Label
                    }
                    if (deseaDeclarar == null) {
                        missing += requiredOption2Label
                    }
                    if (deseaDeclarar != false) {
                        document.preguntas.forEachIndexed { index, pregunta ->
                            if (respuestasPreguntas[pregunta.id].orEmpty().isBlank()) {
                                missing += context.getString(R.string.manifestacion_question_number, index + 1)
                            }
                        }
                    }

                    if (missing.isNotEmpty()) {
                        requiredFieldsErrorText = missing.joinToString(", ")
                        showRequiredFieldsError = true
                    } else {
                        persistManifestacionDraft()
                        showSaveConfirmation = true
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag(ManifestacionUiIds.BTN_SAVE),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = stringResource(R.string.manifestacion_save))
            }

            Button(
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag(ManifestacionUiIds.BTN_DELETE),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(text = stringResource(R.string.manifestacion_delete))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ManifestacionUiIds.NAV_ROW),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackIconButton(onClick = {
                persistManifestacionDraft()
                onBackClick()
            })
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = stringResource(R.string.manifestacion_delete_confirm_title),
                    modifier = Modifier.testTag("${ManifestacionUiIds.DIALOG_DELETE}_title")
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.manifestacion_delete_confirm_message),
                    modifier = Modifier.testTag("${ManifestacionUiIds.DIALOG_DELETE}_text")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        storage.clearCurrent()
                        renunciaAsistenciaLetrada = null
                        deseaDeclarar = null
                        document.preguntas.forEach { pregunta ->
                            respuestasPreguntas[pregunta.id] = ""
                        }
                    },
                    modifier = Modifier.testTag("${ManifestacionUiIds.DIALOG_DELETE}_confirm")
                ) {
                    Text(text = stringResource(R.string.manifestacion_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false },
                    modifier = Modifier.testTag("${ManifestacionUiIds.DIALOG_DELETE}_dismiss")
                ) {
                    Text(text = stringResource(R.string.no_option))
                }
            }
        )
    }

    if (showSaveConfirmation) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmation = false },
            title = {
                Text(
                    text = stringResource(R.string.manifestacion_save_confirm_title),
                    modifier = Modifier.testTag("${ManifestacionUiIds.DIALOG_SAVE}_title")
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.manifestacion_save_confirm_message),
                    modifier = Modifier.testTag("${ManifestacionUiIds.DIALOG_SAVE}_text")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showSaveConfirmation = false; onBackClick() },
                    modifier = Modifier.testTag("${ManifestacionUiIds.DIALOG_SAVE}_confirm")
                ) {
                    Text(text = stringResource(R.string.close_action))
                }
            }
        )
    }

    if (showRequiredFieldsError) {
        AlertDialog(
            onDismissRequest = { showRequiredFieldsError = false },
            title = {
                Text(
                    text = stringResource(R.string.manifestacion_required_fields_title),
                    modifier = Modifier.testTag("${ManifestacionUiIds.DIALOG_REQUIRED}_title")
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.manifestacion_required_fields_message, requiredFieldsErrorText),
                    modifier = Modifier.testTag("${ManifestacionUiIds.DIALOG_REQUIRED}_text")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showRequiredFieldsError = false },
                    modifier = Modifier.testTag("${ManifestacionUiIds.DIALOG_REQUIRED}_confirm")
                ) {
                    Text(text = stringResource(R.string.accept_action))
                }
            }
        )
    }
}

@Composable
private fun HorizontalYesNoQuestionBlock(
    questionText: String,
    selectedValue: Boolean?,
    onValueChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = questionText)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selectedValue == true, onClick = { onValueChange(true) })
                Text(text = stringResource(R.string.yes_option))
            }

            Spacer(modifier = Modifier.width(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selectedValue == false, onClick = { onValueChange(false) })
                Text(text = stringResource(R.string.no_option))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ManifestacionScreenPreview() {
    SinCarnetTheme {
        ManifestacionScreen()
    }
}
