package com.oscar.sincarnet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oscar.sincarnet.ui.theme.SinCarnetTheme

@Composable
fun ExpiredValidityScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val options = listOf(
        stringResource(R.string.expired_validity_option_1),
        stringResource(R.string.expired_validity_option_2),
        stringResource(R.string.expired_validity_option_3)
    )
    var selectedOption by rememberSaveable { mutableIntStateOf(0) }
    val showCriminalAlert = selectedOption == 0
    val lowerCardMessage = if (showCriminalAlert) {
        stringResource(R.string.expired_validity_crime_message)
    } else {
        stringResource(R.string.expired_validity_second_card_placeholder)
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = index },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOption == index,
                            onClick = { selectedOption = index }
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
            isAlertBlinking = showCriminalAlert
        )

        TextButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = stringResource(R.string.back_action))
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

