package com.oscar.sincarnet

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oscar.sincarnet.ui.theme.SinCarnetTheme

@Preview(
    name = "Casos - fuente grande",
    showBackground = true,
    fontScale = 2.0f,
    widthDp = 360,
    heightDp = 800
)
@Composable
private fun CasesScreenLargeFontPreview() {
    SinCarnetTheme {
        CasesScreen(
            onExpiredValidityClick = {},
            onJudicialSuspensionClick = {},
            onWithoutPermitClick = {},
            onSpecialCasesClick = {},
            onDocumentosComplementariosClick = {},
            onCourtsClick = {},
            onAboutClick = {}
        )
    }
}

@Preview(
    name = "Datos del atestado - fuente grande",
    showBackground = true,
    fontScale = 2.0f,
    widthDp = 360,
    heightDp = 800
)
@Composable
private fun TomaDatosAtestadoLargeFontPreview() {
    SinCarnetTheme {
        TomaDatosAtestadoScreen()
    }
}

@Preview(
    name = "Documentos complementarios - fuente grande",
    showBackground = true,
    fontScale = 2.0f,
    widthDp = 360,
    heightDp = 800
)
@Composable
private fun DocumentosComplementariosLargeFontPreview() {
    SinCarnetTheme {
        DocumentosComplementariosScreen()
    }
}

@Preview(
    name = "Perdida de vigencia - fuente grande",
    showBackground = true,
    fontScale = 2.0f,
    widthDp = 360,
    heightDp = 800
)
@Composable
private fun ExpiredValidityLargeFontPreview() {
    SinCarnetTheme {
        ExpiredValidityScreen()
    }
}

@Preview(
    name = "Sin permiso - fuente grande",
    showBackground = true,
    fontScale = 2.0f,
    widthDp = 360,
    heightDp = 800
)
@Composable
private fun WithoutPermitLargeFontPreview() {
    SinCarnetTheme {
        WithoutPermitScreen()
    }
}

@Preview(
    name = "Tarjeta de resultado - fuente grande",
    showBackground = true,
    fontScale = 2.0f,
    widthDp = 360,
    heightDp = 480
)
@Composable
private fun ResultCardLargeFontPreview() {
    SinCarnetTheme {
        Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            PerdidaVigenciaFuntionCard(
                modifier = Modifier.fillMaxSize(),
                messageText = "Resultado normativo de ejemplo con un texto suficientemente largo " +
                    "para comprobar que el contenido se desplaza cuando la fuente del sistema es grande.",
                isAlertBlinking = false,
                showAtestadoButton = true
            )
        }
    }
}

@Preview(
    name = "Contenido de dialogo desplazable - fuente grande",
    showBackground = true,
    fontScale = 2.0f,
    widthDp = 360,
    heightDp = 480
)
@Composable
private fun ScrollableDialogContentLargeFontPreview() {
    SinCarnetTheme {
        Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            ScrollableDialogContent {
                repeat(12) { index ->
                    Text(
                        text = "Línea de ejemplo ${index + 1} para validar el desplazamiento del " +
                            "contenido de los modales con tamaño de fuente grande."
                    )
                }
            }
        }
    }
}
