package com.oscar.sincarnet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Tarjeta de resultado legal con borde dinámico y acción opcional de atestado.
 *
 * Se reutiliza en flujos de decisión (pérdida de vigencia, suspensión judicial,
 * sin permiso, casos especiales) para mostrar el resultado normativo.
 *
 * @param modifier Modificador raíz
 * @param messageText Mensaje legal a mostrar (null oculta texto)
 * @param isAlertBlinking Activa animación de parpadeo del borde
 * @param blinkingColor Color base cuando hay parpadeo
 * @param borderColor Color fijo del borde cuando no parpadea
 * @param showAtestadoButton Si true, muestra botón para iniciar atestado
 * @param onAtestadoClick Acción del botón de atestado
 */
@Composable
fun PerdidaVigenciaFuntionCard(
    modifier: Modifier = Modifier,
    messageText: String? = null,
    isAlertBlinking: Boolean,
    blinkingColor: Color = Color(0xFFD32F2F),
    borderColor: Color = Color.Transparent,
    showAtestadoButton: Boolean = false,
    onAtestadoClick: () -> Unit = {}
) {
    val borderAlpha = if (isAlertBlinking) {
        val transition = rememberInfiniteTransition(label = "alertBorderTransition")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.45f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 550),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alertBorderAlpha"
        ).value
    } else {
        0f
    }
    val animatedBlinkingBorderColor = blinkingColor.copy(alpha = borderAlpha)
    val resolvedBorderColor = if (isAlertBlinking) {
        animatedBlinkingBorderColor
    } else {
        borderColor.copy(alpha = 0.95f)
    }
    val borderWidth = if (isAlertBlinking || borderColor != Color.Transparent) 6.dp else 2.dp

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(borderWidth, resolvedBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!messageText.isNullOrBlank()) {
                Text(
                    text = messageText,
                    textAlign = TextAlign.Center
                )
            }

            if (showAtestadoButton) {
                Button(
                    onClick = onAtestadoClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF40407A),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(text = androidx.compose.ui.res.stringResource(R.string.atestado_action))
                }
            }
        }
    }
}

