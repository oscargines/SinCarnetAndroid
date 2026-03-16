package com.oscar.sincarnet

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oscar.sincarnet.ui.theme.SinCarnetTheme

@Composable
fun CasesScreen(
    modifier: Modifier = Modifier,
    onExpiredValidityClick: () -> Unit,
    onJudicialSuspensionClick: () -> Unit,
    onWithoutPermitClick: () -> Unit,
    onSpecialCasesClick: () -> Unit,
    onCourtsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {}
) {
    val caseItems = listOf(
        CaseItem(CaseType.EXPIRED_VALIDITY, R.string.case_expired_validity, Color(0xFFDCEBFF)),
        CaseItem(CaseType.JUDICIAL_SUSPENSION, R.string.case_judicial_suspension, Color(0xFFFFF4CC)),
        CaseItem(CaseType.WITHOUT_PERMIT, R.string.case_without_permit, Color(0xFFFFE8D6)),
        CaseItem(CaseType.SPECIAL_CASES, R.string.case_special_cases, Color(0xFFE7E8EC))
    )

    fun handleCaseClick(caseType: CaseType) {
        when (caseType) {
            CaseType.EXPIRED_VALIDITY -> onExpiredValidityClick()
            CaseType.JUDICIAL_SUSPENSION -> onJudicialSuspensionClick()
            CaseType.WITHOUT_PERMIT -> onWithoutPermitClick()
            CaseType.SPECIAL_CASES -> onSpecialCasesClick()
        }
    }

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
                    onClick = { handleCaseClick(caseItem.type) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onCourtsClick,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF40407A),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(text = stringResource(R.string.courts_action))
            }

            AboutIconButton(onClick = onAboutClick)
        }
    }
}

@Composable
private fun AboutIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val aboutIconBitmap = remember(isInPreview) {
        if (isInPreview) {
            null
        } else {
            runCatching {
                context.assets.open("icons/sobre_nosotros.png").use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    IconButton(
        onClick = onClick,
        modifier = modifier.size(44.dp)
    ) {
        if (aboutIconBitmap != null) {
            Image(
                bitmap = aboutIconBitmap,
                contentDescription = stringResource(R.string.about_icon_content_description),
                modifier = Modifier.size(30.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.about_icon_content_description),
                modifier = Modifier.size(30.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun CaseCard(
    title: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
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

private data class CaseItem(
    val type: CaseType,
    val titleRes: Int,
    val backgroundColor: Color
)

private enum class CaseType {
    EXPIRED_VALIDITY,
    JUDICIAL_SUSPENSION,
    WITHOUT_PERMIT,
    SPECIAL_CASES
}

@Preview(showBackground = true)
@Composable
fun CasesScreenPreview() {
    SinCarnetTheme {
        CasesScreen(
            onExpiredValidityClick = {},
            onJudicialSuspensionClick = {},
            onWithoutPermitClick = {},
            onSpecialCasesClick = {}
        )
    }
}
