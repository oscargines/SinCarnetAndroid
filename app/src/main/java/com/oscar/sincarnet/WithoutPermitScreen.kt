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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oscar.sincarnet.ui.theme.SinCarnetTheme

@Composable
fun WithoutPermitScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    var hasEverObtainedPermit by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var isValidForDrivingInSpain by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var showObservationsModal by rememberSaveable { mutableStateOf(false) }

    val observationCase = if (hasEverObtainedPermit == false) {
        WithoutPermitObservationCase.NEVER_OBTAINED_PERMIT
    } else {
        null
    }

    if (hasEverObtainedPermit != true) {
        isValidForDrivingInSpain = null
    }

    val decision = resolveWithoutPermitDecision(
        hasEverObtainedPermit = hasEverObtainedPermit,
        isValidForDrivingInSpain = isValidForDrivingInSpain
    )

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
                    text = stringResource(R.string.without_permit_ever_had_license_question),
                    style = MaterialTheme.typography.titleMedium
                )

                OptionRadioRow(
                    text = stringResource(R.string.yes_option),
                    selected = hasEverObtainedPermit == true,
                    onSelect = {
                        hasEverObtainedPermit = true
                        isValidForDrivingInSpain = null
                        showObservationsModal = false
                    }
                )

                OptionRadioRow(
                    text = stringResource(R.string.no_option),
                    selected = hasEverObtainedPermit == false,
                    onSelect = {
                        hasEverObtainedPermit = false
                        isValidForDrivingInSpain = null
                        showObservationsModal = false
                    }
                )

                if (hasEverObtainedPermit == true) {
                    Text(
                        text = stringResource(R.string.without_permit_valid_in_spain_question),
                        style = MaterialTheme.typography.titleMedium
                    )

                    OptionRadioRow(
                        text = stringResource(R.string.yes_option),
                        selected = isValidForDrivingInSpain == true,
                        onSelect = { isValidForDrivingInSpain = true }
                    )

                    OptionRadioRow(
                        text = stringResource(R.string.no_option),
                        selected = isValidForDrivingInSpain == false,
                        onSelect = { isValidForDrivingInSpain = false }
                    )
                }
            }
        }

        PerdidaVigenciaFuntionCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            messageText = decision.messageRes?.let { stringResource(it) },
            isAlertBlinking = decision.borderBehavior == WithoutPermitBorderBehavior.RED_BLINK,
            blinkingColor = Color(0xFFD32F2F),
            borderColor = when (decision.borderBehavior) {
                WithoutPermitBorderBehavior.GREEN_SOLID -> Color(0xFF2E7D32)
                WithoutPermitBorderBehavior.ORANGE_SOLID -> Color(0xFFEF6C00)
                else -> Color.Transparent
            }
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
                        WithoutPermitObservationCase.NEVER_OBTAINED_PERMIT -> {
                            Text(text = stringResource(R.string.obs_without_permit_never_obtained_actuacion))
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
}

private enum class WithoutPermitObservationCase {
    NEVER_OBTAINED_PERMIT
}

private enum class WithoutPermitBorderBehavior {
    NONE,
    RED_BLINK,
    GREEN_SOLID,
    ORANGE_SOLID
}

private data class WithoutPermitDecision(
    val messageRes: Int?,
    val borderBehavior: WithoutPermitBorderBehavior
)

private fun resolveWithoutPermitDecision(
    hasEverObtainedPermit: Boolean?,
    isValidForDrivingInSpain: Boolean?
): WithoutPermitDecision {
    return when {
        hasEverObtainedPermit == false -> {
            WithoutPermitDecision(
                messageRes = R.string.expired_validity_crime_message,
                borderBehavior = WithoutPermitBorderBehavior.RED_BLINK
            )
        }

        hasEverObtainedPermit == true && isValidForDrivingInSpain == true -> {
            WithoutPermitDecision(
                messageRes = R.string.continue_trip_message,
                borderBehavior = WithoutPermitBorderBehavior.GREEN_SOLID
            )
        }

        hasEverObtainedPermit == true && isValidForDrivingInSpain == false -> {
            WithoutPermitDecision(
                messageRes = R.string.without_permit_admin_report_message,
                borderBehavior = WithoutPermitBorderBehavior.ORANGE_SOLID
            )
        }

        else -> {
            WithoutPermitDecision(
                messageRes = null,
                borderBehavior = WithoutPermitBorderBehavior.NONE
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun WithoutPermitScreenPreview() {
    SinCarnetTheme {
        WithoutPermitScreen()
    }
}
