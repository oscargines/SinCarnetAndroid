package com.oscar.sincarnet

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

@Composable
fun JudicialSuspensionScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onStartAtestadoClick: () -> Unit = {}
) {
    var sentenceType by rememberSaveable { mutableStateOf<SentenceType?>(null) }
    var drivingMoment by rememberSaveable { mutableStateOf<DrivingMoment?>(null) }
    var showObservationsModal by rememberSaveable { mutableStateOf(false) }

    var showLowerSentenceModal by rememberSaveable { mutableStateOf(false) }
    var hasCompletedCourseForLowerSentence by rememberSaveable { mutableStateOf<Boolean?>(null) }

    var showHigherSentenceModal by rememberSaveable { mutableStateOf(false) }
    var hasCompletedCourseForHigherSentence by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var hasPassedExamsForHigherSentence by rememberSaveable { mutableStateOf<Boolean?>(null) }

    fun selectSentenceType(type: SentenceType) {
        sentenceType = type
        drivingMoment = null
        showObservationsModal = false

        showLowerSentenceModal = false
        hasCompletedCourseForLowerSentence = null

        showHigherSentenceModal = false
        hasCompletedCourseForHigherSentence = null
        hasPassedExamsForHigherSentence = null
    }

    fun selectDrivingMoment(moment: DrivingMoment) {
        drivingMoment = moment
        showObservationsModal = false
        showLowerSentenceModal = false
        showHigherSentenceModal = false

        if (moment == DrivingMoment.AFTER_PERIOD) {
            when (sentenceType) {
                SentenceType.UP_TO_TWO_YEARS -> {
                    hasCompletedCourseForLowerSentence = null
                    showLowerSentenceModal = true
                }

                SentenceType.OVER_TWO_YEARS -> {
                    hasCompletedCourseForHigherSentence = null
                    hasPassedExamsForHigherSentence = null
                    showHigherSentenceModal = true
                }

                null -> Unit
            }
        }
    }

    val decision = resolveJudicialSuspensionDecision(
        sentenceType = sentenceType,
        drivingMoment = drivingMoment,
        hasCompletedCourseForLowerSentence = hasCompletedCourseForLowerSentence,
        hasCompletedCourseForHigherSentence = hasCompletedCourseForHigherSentence,
        hasPassedExamsForHigherSentence = hasPassedExamsForHigherSentence
    )
    val observationCase = when {
        sentenceType == SentenceType.UP_TO_TWO_YEARS && drivingMoment == DrivingMoment.WITHIN_PERIOD -> {
            JudicialObservationCase.LOWER_SENTENCE_WITHIN_PERIOD
        }

        sentenceType == SentenceType.OVER_TWO_YEARS && drivingMoment == DrivingMoment.WITHIN_PERIOD -> {
            JudicialObservationCase.HIGHER_SENTENCE_WITHIN_PERIOD
        }

        sentenceType == SentenceType.UP_TO_TWO_YEARS &&
            drivingMoment == DrivingMoment.AFTER_PERIOD &&
            hasCompletedCourseForLowerSentence == false -> {
            JudicialObservationCase.LOWER_SENTENCE_AFTER_PERIOD_WITHOUT_COURSE
        }

        sentenceType == SentenceType.OVER_TWO_YEARS &&
            drivingMoment == DrivingMoment.AFTER_PERIOD &&
            hasCompletedCourseForHigherSentence == false &&
            hasPassedExamsForHigherSentence == false -> {
            JudicialObservationCase.HIGHER_SENTENCE_AFTER_PERIOD_WITHOUT_COURSE_AND_EXAM
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.judicial_suspension_sentence_question),
                    style = MaterialTheme.typography.titleMedium
                )

                OptionRadioRow(
                    text = stringResource(R.string.judicial_sentence_up_to_two_years),
                    selected = sentenceType == SentenceType.UP_TO_TWO_YEARS,
                    onSelect = { selectSentenceType(SentenceType.UP_TO_TWO_YEARS) }
                )

                OptionRadioRow(
                    text = stringResource(R.string.judicial_sentence_over_two_years),
                    selected = sentenceType == SentenceType.OVER_TWO_YEARS,
                    onSelect = { selectSentenceType(SentenceType.OVER_TWO_YEARS) }
                )

                if (sentenceType != null) {
                    Text(
                        text = stringResource(R.string.judicial_drive_moment_question),
                        style = MaterialTheme.typography.titleMedium
                    )

                    OptionRadioRow(
                        text = stringResource(R.string.judicial_drive_within_period),
                        selected = drivingMoment == DrivingMoment.WITHIN_PERIOD,
                        onSelect = { selectDrivingMoment(DrivingMoment.WITHIN_PERIOD) }
                    )

                    OptionRadioRow(
                        text = stringResource(R.string.judicial_drive_after_period),
                        selected = drivingMoment == DrivingMoment.AFTER_PERIOD,
                        onSelect = { selectDrivingMoment(DrivingMoment.AFTER_PERIOD) }
                    )
                }
            }
        }

        PerdidaVigenciaFuntionCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            messageText = decision.messageRes?.let { stringResource(it) },
            isAlertBlinking = decision.borderBehavior == JudicialBorderBehavior.RED_BLINK ||
                decision.borderBehavior == JudicialBorderBehavior.YELLOW_BLINK,
            blinkingColor = if (decision.borderBehavior == JudicialBorderBehavior.YELLOW_BLINK) {
                Color(0xFFFBC02D)
            } else {
                Color(0xFFD32F2F)
            },
            borderColor = if (decision.borderBehavior == JudicialBorderBehavior.GREEN_SOLID) {
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
                        JudicialObservationCase.LOWER_SENTENCE_WITHIN_PERIOD -> {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(stringResource(R.string.observations_label_hecho))
                                    }
                                    append(stringResource(R.string.obs_judicial_within_period_lower_sentence_hecho))
                                }
                            )
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(stringResource(R.string.observations_label_actuacion))
                                    }
                                    append(stringResource(R.string.obs_judicial_within_period_lower_sentence_actuacion))
                                }
                            )
                        }

                        JudicialObservationCase.LOWER_SENTENCE_AFTER_PERIOD_WITHOUT_COURSE -> {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(stringResource(R.string.observations_label_hecho))
                                    }
                                    append(stringResource(R.string.obs_judicial_after_period_lower_sentence_without_course_hecho))
                                }
                            )
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(stringResource(R.string.observations_label_actuacion))
                                    }
                                    append(stringResource(R.string.obs_judicial_after_period_lower_sentence_without_course_actuacion))
                                }
                            )
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(stringResource(R.string.observations_label_requisito))
                                    }
                                    append(stringResource(R.string.obs_judicial_after_period_lower_sentence_without_course_requisito))
                                }
                            )
                        }

                        JudicialObservationCase.HIGHER_SENTENCE_WITHIN_PERIOD -> {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(stringResource(R.string.observations_label_hecho))
                                    }
                                    append(stringResource(R.string.obs_judicial_within_period_higher_sentence_hecho))
                                }
                            )
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(stringResource(R.string.observations_label_actuacion))
                                    }
                                    append(stringResource(R.string.obs_judicial_within_period_higher_sentence_actuacion))
                                }
                            )
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(stringResource(R.string.observations_label_requisito))
                                    }
                                    append(stringResource(R.string.obs_judicial_within_period_higher_sentence_requisito))
                                }
                            )
                        }

                        JudicialObservationCase.HIGHER_SENTENCE_AFTER_PERIOD_WITHOUT_COURSE_AND_EXAM -> {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(stringResource(R.string.observations_label_hecho))
                                    }
                                    append(stringResource(R.string.obs_judicial_after_period_higher_sentence_without_course_and_exam_hecho))
                                }
                            )
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(stringResource(R.string.observations_label_actuacion))
                                    }
                                    append(stringResource(R.string.obs_judicial_after_period_higher_sentence_without_course_and_exam_actuacion))
                                }
                            )
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(stringResource(R.string.observations_label_requisito))
                                    }
                                    append(stringResource(R.string.obs_judicial_after_period_higher_sentence_without_course_and_exam_requisito))
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

    if (showLowerSentenceModal) {
        AlertDialog(
            onDismissRequest = { showLowerSentenceModal = false },
            title = { Text(text = stringResource(R.string.judicial_suspension_screen_title)) },
            text = {
                YesNoQuestionBlock(
                    questionText = stringResource(R.string.judicial_course_question),
                    selectedValue = hasCompletedCourseForLowerSentence,
                    onValueChange = {
                        hasCompletedCourseForLowerSentence = it
                        showLowerSentenceModal = false
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showLowerSentenceModal = false }) {
                    Text(text = stringResource(R.string.accept_action))
                }
            }
        )
    }

    if (showHigherSentenceModal) {
        AlertDialog(
            onDismissRequest = { showHigherSentenceModal = false },
            title = { Text(text = stringResource(R.string.judicial_suspension_screen_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    YesNoQuestionBlock(
                        questionText = stringResource(R.string.judicial_course_question),
                        selectedValue = hasCompletedCourseForHigherSentence,
                        onValueChange = { hasCompletedCourseForHigherSentence = it }
                    )
                    YesNoQuestionBlock(
                        questionText = stringResource(R.string.judicial_exams_question),
                        selectedValue = hasPassedExamsForHigherSentence,
                        onValueChange = { hasPassedExamsForHigherSentence = it }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHigherSentenceModal = false }) {
                    Text(text = stringResource(R.string.accept_action))
                }
            }
        )
    }
}

internal enum class SentenceType {
    UP_TO_TWO_YEARS,
    OVER_TWO_YEARS
}

internal enum class DrivingMoment {
    WITHIN_PERIOD,
    AFTER_PERIOD
}

private enum class JudicialObservationCase {
    LOWER_SENTENCE_WITHIN_PERIOD,
    LOWER_SENTENCE_AFTER_PERIOD_WITHOUT_COURSE,
    HIGHER_SENTENCE_WITHIN_PERIOD,
    HIGHER_SENTENCE_AFTER_PERIOD_WITHOUT_COURSE_AND_EXAM
}

internal enum class JudicialBorderBehavior {
    NONE,
    RED_BLINK,
    YELLOW_BLINK,
    GREEN_SOLID
}

internal data class JudicialSuspensionDecision(
    val messageRes: Int?,
    val borderBehavior: JudicialBorderBehavior,
    val isCrimeCase: Boolean = false
)

internal fun resolveJudicialSuspensionDecision(
    sentenceType: SentenceType?,
    drivingMoment: DrivingMoment?,
    hasCompletedCourseForLowerSentence: Boolean?,
    hasCompletedCourseForHigherSentence: Boolean?,
    hasPassedExamsForHigherSentence: Boolean?
): JudicialSuspensionDecision {
    return when {
        drivingMoment == DrivingMoment.WITHIN_PERIOD -> {
            JudicialSuspensionDecision(
                messageRes = R.string.expired_validity_crime_message,
                borderBehavior = JudicialBorderBehavior.RED_BLINK,
                isCrimeCase = true
            )
        }

        sentenceType == SentenceType.UP_TO_TWO_YEARS && drivingMoment == DrivingMoment.AFTER_PERIOD -> {
            when (hasCompletedCourseForLowerSentence) {
                true -> JudicialSuspensionDecision(
                    messageRes = R.string.continue_trip_message,
                    borderBehavior = JudicialBorderBehavior.GREEN_SOLID
                )

                false -> JudicialSuspensionDecision(
                    messageRes = R.string.judicial_infringement_lsv_message,
                    borderBehavior = JudicialBorderBehavior.YELLOW_BLINK
                )

                null -> JudicialSuspensionDecision(
                    messageRes = null,
                    borderBehavior = JudicialBorderBehavior.NONE
                )
            }
        }

        sentenceType == SentenceType.OVER_TWO_YEARS && drivingMoment == DrivingMoment.AFTER_PERIOD -> {
            if (hasCompletedCourseForHigherSentence == true && hasPassedExamsForHigherSentence == true) {
                JudicialSuspensionDecision(
                    messageRes = R.string.continue_trip_message,
                    borderBehavior = JudicialBorderBehavior.GREEN_SOLID
                )
            } else if (
                hasCompletedCourseForHigherSentence != null &&
                hasPassedExamsForHigherSentence != null
            ) {
                JudicialSuspensionDecision(
                    messageRes = R.string.expired_validity_infringement_message,
                    borderBehavior = JudicialBorderBehavior.YELLOW_BLINK
                )
            } else {
                JudicialSuspensionDecision(
                    messageRes = null,
                    borderBehavior = JudicialBorderBehavior.NONE
                )
            }
        }

        else -> JudicialSuspensionDecision(
            messageRes = null,
            borderBehavior = JudicialBorderBehavior.NONE
        )
    }
}


@Preview(showBackground = true)
@Composable
fun JudicialSuspensionScreenPreview() {
    SinCarnetTheme {
        JudicialSuspensionScreen()
    }
}
