package com.oscar.sincarnet

import com.oscar.sincarnet.presentation.R

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oscar.sincarnet.ui.theme.SinCarnetTheme
import com.oscar.sincarnet.data.device.getDeviceIdentifierSuffix

/**
 * Pantalla de splash que se muestra al iniciar la aplicación.
 *
 * Presenta:
 * - El escudo de España (desde assets o ícono de la aplicación como fallback)
 * - Título de la aplicación
 * - Subtítulo descriptivo
 * - Versión de la aplicación (desde BuildConfig)
 *
 * El tamaño del escudo se calcula mediante [BoxWithConstraints] en lugar de
 * [androidx.compose.ui.platform.LocalConfiguration], evitando así las
 * recomposiciones espurias que Android 15 provoca al ajustar insets del
 * sistema (causa del bucle reportado en Pixel 8 Pro).
 *
 * @param modifier Modificador Compose opcional para personalizar el layout.
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier, versionName: String = "") {
    val context = LocalContext.current
    val versionText = "Versión $versionName"
    val deviceId = remember(context) { getDeviceIdentifierSuffix(context) }
    val splashBitmap = remember {
        runCatching {
            context.assets.open("images/escudo_bw.png").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            }
        }.getOrNull()
    }

    // BoxWithConstraints mide el espacio disponible en el árbol de layout, sin
    // suscribirse a LocalConfiguration. Así no se recompone cuando Android 15
    // notifica cambios de insets/ventana durante el arranque en Pixel 8 Pro.
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val imageSize = minOf(maxWidth, maxHeight) * 0.75f

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (splashBitmap != null) {
                Image(
                    bitmap = splashBitmap,
                    contentDescription = stringResource(R.string.splash_logo_content_description),
                    modifier = Modifier.size(imageSize),
                    contentScale = ContentScale.Fit
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.splash_logo_content_description),
                    modifier = Modifier.size(imageSize),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.splash_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.splash_subtitle),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        Text(
            text = "$versionText\n${stringResource(R.string.device_identifier_format, deviceId)}",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Vista previa de la pantalla de splash para Compose Preview.
 *
 * @see SplashScreen
 */
@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SinCarnetTheme {
        SplashScreen(versionName = "preview")
    }
}
