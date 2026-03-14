package com.oscar.sincarnet

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun BackIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val backIconBitmap = remember {
        runCatching {
            context.assets.open("icons/retroceso.png").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            }
        }.getOrNull()
    }

    IconButton(
        onClick = onClick,
        modifier = modifier.size(44.dp)
    ) {
        if (backIconBitmap != null) {
            Image(
                bitmap = backIconBitmap,
                contentDescription = stringResource(R.string.back_icon_content_description),
                modifier = Modifier.size(30.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.back_icon_content_description),
                modifier = Modifier.size(30.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
internal fun OptionRadioRow(
    text: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(text = text)
    }
}

@Composable
internal fun YesNoQuestionBlock(
    questionText: String,
    selectedValue: Boolean?,
    onValueChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = questionText)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onValueChange(true) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selectedValue == true, onClick = { onValueChange(true) })
            Text(text = stringResource(R.string.yes_option))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onValueChange(false) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selectedValue == false, onClick = { onValueChange(false) })
            Text(text = stringResource(R.string.no_option))
        }
    }
}

@Composable
internal fun AssetImage(
    assetPath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val assetBitmap = remember(assetPath) {
        runCatching {
            context.assets.open(assetPath).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            }
        }.getOrNull()
    }

    if (assetBitmap != null) {
        Image(
            bitmap = assetBitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}
