package com.oscar.sincarnet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oscar.sincarnet.ui.theme.SinCarnetTheme

/**
 * Flujo de decisión para conducción con pérdida de vigencia del permiso.
 *
 * Presenta preguntas guiadas y deriva en tres salidas visuales:
 * delito (rojo), infracción administrativa (amarillo) o continuación
 * del viaje (verde). Cuando procede delito, habilita iniciar atestado.
 *
 * @param modifier Modificador raíz
 * @param onBackClick Vuelve a la pantalla anterior
 * @param onStartAtestadoClick Inicia flujo de atestado para casos penales
 */
@Composable
fun ExpiredValidityScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onStartAtestadoClick: () -> Unit = {}
) {
    val options = listOf(
        stringResource(R.string.expired_validity_option_1),
        stringResource(R.string.expired_validity_option_2),
        stringResource(R.string.expired_validity_option_3)
    )
    var selectedOption by rememberSaveable { mutableIntStateOf(0) }
    var showCompletedLossModal by rememberSaveable { mutableStateOf(false) }
    var showEdictalModal by rememberSaveable { mutableStateOf(false) }
    var showEdictalAppealModal by rememberSaveable { mutableStateOf(false) }
    var showObservationsModal by rememberSaveable { mutableStateOf(false) }
    var hasCompletedCourses by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var hasPassedExams by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var hasKnowledge by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var isInAppealPeriod by rememberSaveable { mutableStateOf<Boolean?>(null) }

    fun selectMainOption(index: Int) {
        selectedOption = index
        when (index) {
            1 -> {
                hasCompletedCourses = null
                hasPassedExams = null
                showCompletedLossModal = true
                showEdictalModal = false
            }

            2 -> {
                hasKnowledge = null
                isInAppealPeriod = null
                showEdictalModal = true
                showEdictalAppealModal = false
                showCompletedLossModal = false
            }

            else -> {
                showCompletedLossModal = false
                showEdictalModal = false
                showEdictalAppealModal = false
            }
        }
    }

    val hasAnyNegativeAnswer = hasCompletedCourses == false || hasPassedExams == false
    val hasAllPositiveAnswers = hasCompletedCourses == true && hasPassedExams == true

    val decision = resolveExpiredValidityDecision(
        selectedOption = selectedOption,
        hasAnyNegativeAnswer = hasAnyNegativeAnswer,
        hasAllPositiveAnswers = hasAllPositiveAnswers,
        hasKnowledge = hasKnowledge,
        isInAppealPeriod = isInAppealPeriod
    )
    val lowerCardMessage = decision.messageRes?.let { stringResource(it) }
    val observationCase = when {
        selectedOption == 0 || (selectedOption == 1 && hasAnyNegativeAnswer) -> {
            ObservationCase.LOSS_OF_VALIDITY_PERIOD
        }

        selectedOption == 2 && hasKnowledge == true && isInAppealPeriod == true -> {
            ObservationCase.EDICTAL_APPEAL_PERIOD
        }

        else -> null
    }

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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectMainOption(index) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOption == index,
                            onClick = { selectMainOption(index) }
                        )
                        Text(text = option)
                    }
                }
            }
        }

        PerdidaVigenciaFuntionCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            messageText = lowerCardMessage,
            isAlertBlinking =
                decision.borderBehavior == BorderBehavior.RED_BLINK ||
                    decision.borderBehavior == BorderBehavior.YELLOW_BLINK,
            blinkingColor = when (decision.borderBehavior) {
                BorderBehavior.YELLOW_BLINK -> Color(0xFFFBC02D)
                else -> Color(0xFFD32F2F)
            },
            borderColor = if (decision.borderBehavior == BorderBehavior.GREEN_SOLID) {
                Color(0xFF2E7D32)
            } else {
                Color.Transparent
            },
            showAtestadoButton = decision.isCrimeCase,
            onAtestadoClick = onStartAtestadoClick
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (observationCase != null) showObservationsModal = true
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF40407A),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(text = stringResource(R.string.observations_action))
            }
            BackIconButton(onClick = onBackClick)
        }
    }

    if (showObservationsModal) {
        AlertDialog(
            onDismissRequest = { showObservationsModal = false },
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AssetImage(
                        assetPath = "icons/error.png",
                        contentDescription = stringResource(R.string.error_icon_content_description),
                        modifier = Modifier.size(100.dp)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (observationCase) {
                        ObservationCase.LOSS_OF_VALIDITY_PERIOD -> {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(stringResource(R.string.observations_label_hecho))
                                    }
                                    append(stringResource(R.string.obs_expired_option1_hecho))
                                }
                            )
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(stringResource(R.string.observations_label_actuacion))
                                    }
                                    append(stringResource(R.string.obs_expired_option1_actuacion))
                                }
                            )
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(stringResource(R.string.observations_label_requisito))
                                    }
                                    append(stringResource(R.string.obs_expired_option1_requisito))
                                }
                            )
                        }

                        ObservationCase.EDICTAL_APPEAL_PERIOD -> {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(stringResource(R.string.observations_label_hecho))
                                    }
                                    append(stringResource(R.string.obs_expired_option3_appeal_hecho_1))
                                }
                            )
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(stringResource(R.string.observations_label_actuacion))
                                    }
                                    append(stringResource(R.string.obs_expired_option3_appeal_actuacion_1))
                                }
                            )
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(stringResource(R.string.observations_label_hecho))
                                    }
                                    append(stringResource(R.string.obs_expired_option3_appeal_hecho_2))
                                }
                            )
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(stringResource(R.string.observations_label_actuacion))
                                    }
                                    append(stringResource(R.string.obs_expired_option3_appeal_actuacion_2))
                                }
                            )
                        }

                        null -> Unit
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showObservationsModal = false }) {
                    Text(text = stringResource(R.string.accept_action))
                }
            }
        )
    }

    if (showCompletedLossModal) {
        AlertDialog(
            onDismissRequest = { showCompletedLossModal = false },
            title = { Text(text = stringResource(R.string.expired_validity_modal_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    YesNoQuestionBlock(
                        questionText = stringResource(R.string.expired_validity_courses_question),
                        selectedValue = hasCompletedCourses,
                        onValueChange = { hasCompletedCourses = it }
                    )
                    YesNoQuestionBlock(
                        questionText = stringResource(R.string.expired_validity_exams_question),
                        selectedValue = hasPassedExams,
                        onValueChange = { hasPassedExams = it }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCompletedLossModal = false }) {
                    Text(text = stringResource(R.string.accept_action))
                }
            }
        )
    }

    if (showEdictalModal) {
        AlertDialog(
            onDismissRequest = { showEdictalModal = false },
            title = { Text(text = stringResource(R.string.expired_validity_option_3)) },
            text = {
                YesNoQuestionBlock(
                    questionText = stringResource(R.string.edictal_knowledge_question),
                    selectedValue = hasKnowledge,
                    onValueChange = {
                        hasKnowledge = it
                        showEdictalModal = false
                        if (it) {
                            showEdictalAppealModal = true
                        } else {
                            showEdictalAppealModal = false
                        }
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showEdictalModal = false }) {
                    Text(text = stringResource(R.string.accept_action))
                }
            }
        )
    }

    if (showEdictalAppealModal) {
        AlertDialog(
            onDismissRequest = { showEdictalAppealModal = false },
            title = { Text(text = stringResource(R.string.expired_validity_option_3)) },
            text = {
                YesNoQuestionBlock(
                    questionText = stringResource(R.string.edictal_appeal_period_question),
                    selectedValue = isInAppealPeriod,
                    onValueChange = {
                        isInAppealPeriod = it
                        showEdictalAppealModal = false
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showEdictalAppealModal = false }) {
                    Text(text = stringResource(R.string.accept_action))
                }
            }
        )
    }
}

/** Variantes de contenido para el modal de observaciones. */
private enum class ObservationCase {
    LOSS_OF_VALIDITY_PERIOD,
    EDICTAL_APPEAL_PERIOD
}

/** Estados visuales del borde de resultado en `PerdidaVigenciaFuntionCard`. */
internal enum class BorderBehavior {
    NONE,
    RED_BLINK,
    YELLOW_BLINK,
    GREEN_SOLID
}

/** Resultado de negocio para el caso de pérdida de vigencia. */
internal data class ExpiredValidityDecision(
    val messageRes: Int?,
    val borderBehavior: BorderBehavior,
    val isCrimeCase: Boolean = false
)

/**
 * Resuelve el resultado normativo del caso de pérdida de vigencia.
 *
 * Encapsula la lógica para que la UI y los tests compartan el mismo
 * comportamiento.
 */
internal fun resolveExpiredValidityDecision(
    selectedOption: Int,
    hasAnyNegativeAnswer: Boolean,
    hasAllPositiveAnswers: Boolean,
    hasKnowledge: Boolean?,
    isInAppealPeriod: Boolean?
): ExpiredValidityDecision {
    return when {
        // Reusable risk behavior: any penalizable situation shares red blinking border + crime message.
        selectedOption == 0 || (selectedOption == 1 && hasAnyNegativeAnswer) -> {
            ExpiredValidityDecision(
                messageRes = R.string.expired_validity_crime_message,
                borderBehavior = BorderBehavior.RED_BLINK,
                isCrimeCase = true
            )
        }

        selectedOption == 2 && hasKnowledge == false -> {
            ExpiredValidityDecision(
                messageRes = R.string.expired_validity_infringement_message,
                borderBehavior = BorderBehavior.YELLOW_BLINK
            )
        }

        selectedOption == 2 && hasKnowledge == true && isInAppealPeriod == true -> {
            ExpiredValidityDecision(
                messageRes = R.string.expired_validity_infringement_message,
                borderBehavior = BorderBehavior.YELLOW_BLINK
            )
        }

        selectedOption == 2 && hasKnowledge == true && isInAppealPeriod == false -> {
            ExpiredValidityDecision(
                messageRes = R.string.expired_validity_crime_message,
                borderBehavior = BorderBehavior.RED_BLINK,
                isCrimeCase = true
            )
        }

        selectedOption == 1 && hasAllPositiveAnswers -> {
            ExpiredValidityDecision(
                messageRes = R.string.continue_trip_message,
                borderBehavior = BorderBehavior.GREEN_SOLID
            )
        }

        else -> {
            ExpiredValidityDecision(
                messageRes = null,
                borderBehavior = BorderBehavior.NONE
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ExpiredValidityScreenPreview() {
    SinCarnetTheme {
        ExpiredValidityScreen()
    }
}
