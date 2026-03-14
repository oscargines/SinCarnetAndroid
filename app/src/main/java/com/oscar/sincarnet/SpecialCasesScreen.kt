package com.oscar.sincarnet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
fun SpecialCasesScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    var selectedSpecialCase by rememberSaveable { mutableStateOf<SpecialCaseType?>(null) }

    val decision = resolveSpecialCaseDecision(selectedSpecialCase)

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
                    text = stringResource(R.string.special_cases_screen_title),
                    style = MaterialTheme.typography.titleMedium
                )

                OptionRadioRow(
                    text = stringResource(R.string.special_case_psychophysical_loss_option),
                    selected = selectedSpecialCase == SpecialCaseType.PSYCHOPHYSICAL_LOSS,
                    onSelect = { selectedSpecialCase = SpecialCaseType.PSYCHOPHYSICAL_LOSS }
                )

                OptionRadioRow(
                    text = stringResource(R.string.special_case_missing_requirements_option),
                    selected = selectedSpecialCase == SpecialCaseType.MISSING_REQUIREMENTS,
                    onSelect = { selectedSpecialCase = SpecialCaseType.MISSING_REQUIREMENTS }
                )
            }
        }

        PerdidaVigenciaFuntionCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            messageText = decision.messageRes?.let { stringResource(it) },
            isAlertBlinking = false,
            borderColor = if (decision.hasOrangeBorder) Color(0xFFEF6C00) else Color.Transparent
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { /* TODO: Observaciones */ },
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
}

private enum class SpecialCaseType {
    PSYCHOPHYSICAL_LOSS,
    MISSING_REQUIREMENTS
}

private data class SpecialCaseDecision(
    val messageRes: Int?,
    val hasOrangeBorder: Boolean
)

private fun resolveSpecialCaseDecision(selectedSpecialCase: SpecialCaseType?): SpecialCaseDecision {
    return when (selectedSpecialCase) {
        SpecialCaseType.PSYCHOPHYSICAL_LOSS -> SpecialCaseDecision(
            messageRes = R.string.special_case_psychophysical_loss_message,
            hasOrangeBorder = true
        )

        SpecialCaseType.MISSING_REQUIREMENTS -> SpecialCaseDecision(
            messageRes = R.string.special_case_missing_requirements_message,
            hasOrangeBorder = true
        )

        null -> SpecialCaseDecision(
            messageRes = null,
            hasOrangeBorder = false
        )
    }
}


@Preview(showBackground = true)
@Composable
fun SpecialCasesScreenPreview() {
    SinCarnetTheme {
        SpecialCasesScreen()
    }
}
