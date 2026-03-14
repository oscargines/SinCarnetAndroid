package com.oscar.sincarnet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oscar.sincarnet.ui.theme.SinCarnetTheme

@Composable
fun CasesScreen(
    modifier: Modifier = Modifier,
    onExpiredValidityClick: () -> Unit = {},
    onAboutClick: () -> Unit = {}
) {
    val caseItems = listOf(
        CaseItem(R.string.case_expired_validity, Color(0xFFDCEBFF), onExpiredValidityClick),
        CaseItem(R.string.case_judicial_suspension, Color(0xFFFFF4CC)),
        CaseItem(R.string.case_without_permit, Color(0xFFFFE8D6)),
        CaseItem(R.string.case_special_cases, Color(0xFFE7E8EC))
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            caseItems.forEach { caseItem ->
                CaseCard(
                    title = stringResource(caseItem.titleRes),
                    backgroundColor = caseItem.backgroundColor,
                    onClick = caseItem.onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }

        TextButton(
            onClick = onAboutClick,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = stringResource(R.string.about_action))
        }
    }
}

@Composable
private fun CaseCard(
    title: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    } else {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private data class CaseItem(
    val titleRes: Int,
    val backgroundColor: Color,
    val onClick: (() -> Unit)? = null
)

@Preview(showBackground = true)
@Composable
fun CasesScreenPreview() {
    SinCarnetTheme {
        CasesScreen()
    }
}
