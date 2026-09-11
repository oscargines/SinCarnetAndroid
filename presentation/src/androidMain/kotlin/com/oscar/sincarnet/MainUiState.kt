package com.oscar.sincarnet

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Estado completo de la UI principal de SinCarnet.
 *
 * Agrupa por dominio los ~80 estados que originalmente vivían en [MainActivity].
 * Cada subclase es una instantánea inmutable que se actualiza con `copy(...)`.
 *
 * @property navigation Rutas actuales y diálogos visibles.
 * @property form Datos del modal de generación del atestado.
 * @property signature Firmas capturadas y estado del flujo de firmas.
 * @property actuantes Datos de instructor/secretario.
 * @property persona Datos de la persona investigada.
 * @property vehiculo Datos del vehículo.
 * @property document Estado de generación/último PDF.
 */
data class MainUiState(
    val showSplash: Boolean = true,
    val showAboutDialog: Boolean = false,
    val form: AtestadoFormState = AtestadoFormState(),
    val signature: SignatureState = SignatureState(),
    val actuantes: ActuantesState = ActuantesState(),
    val persona: PersonaInvestigadaState = PersonaInvestigadaState(),
    val vehiculo: VehiculoState = VehiculoState(),
    val document: DocumentGenerationState = DocumentGenerationState(),
    val documentScanStatus: DocumentScanStatus = DocumentScanStatus.NO_COMPROBADO
)

enum class DocumentScanStatus {
    NO_COMPROBADO,
    SIN_DOCUMENTOS,
    DOCUMENTOS_INCOMPLETOS,
    DOCUMENTOS_COMPLETOS
}

/**
 * Datos del modal de generación de atestado (motivo, artículos, vicisitudes...).
 */
data class AtestadoFormState(
    val generateReason: String = "Siniestro Vial",
    val articleNorm: String = "LSV",
    val articleText: String = "",
    val dgtNoRecord: Boolean = false,
    val internationalNoRecord: Boolean = false,
    val existsRecord: Boolean = false,
    val vicisitudesOption: String = "",
    val jefaturaProvincial: String = "",
    val numeroBoletin: String = "",
    val tiempoPrivacion: String = "",
    val juzgadoDecreta: String = "",
    val hasSecondDriver: Boolean = false,
    val secondDriverName: String = "",
    val secondDriverId: String = ""
)

/**
 * Estado de firmas del atestado.
 *
 * @property signaturesBySigner Mapa de clave de firmante → bitmap.
 * @property currentSignerKey Quién está firmando actualmente.
 * @property wantsToSign Si el investigado desea firmar.
 */
data class SignatureState(
    val signaturesBySigner: Map<String, ImageBitmap> = emptyMap(),
    val currentSignerKey: String = "",
    val wantsToSign: Boolean = true
)

/**
 * Estado de datos de actuantes (instructor/secretario).
 */
data class ActuantesState(
    val instructorEmployment: String = "",
    val instructorTip: String = "",
    val instructorUnit: String = "",
    val secretaryEmployment: String = "",
    val secretaryTip: String = "",
    val secretaryUnit: String = "",
    val sameUnit: Boolean = false,
    val statusMessage: String = "",
    val canRecover: Boolean = false,
    val tipHistory: List<String> = emptyList(),
    val unitHistory: List<String> = emptyList()
)

/**
 * Estado de datos de la persona investigada.
 */
data class PersonaInvestigadaState(
    val nationality: String = "España",
    val sex: String = "",
    val firstName: String = "",
    val lastName1: String = "",
    val lastName2: String = "",
    val address: String = "",
    val birthDate: String = "",
    val birthPlace: String = "",
    val fatherName: String = "",
    val motherName: String = "",
    val phone: String = "",
    val email: String = ""
)

/**
 * Estado de datos del vehículo.
 */
data class VehiculoState(
    val brand: String = "",
    val model: String = "",
    val plate: String = "",
    val registrationDate: String = "",
    val nationality: String = "España",
    val itvDate: String = "",
    val insurer: String = "",
    val vehicleType: String = "",
    val clasePermiso: String = "",
    val ownerIsOther: Boolean = false,
    val ownerName: String = "",
    val ownerLastNames: String = "",
    val ownerDni: String = "",
    val ownerAddress: String = "",
    val ownerPhone: String = ""
)

/**
 * Estado de generación de documentos PDF/ODT.
 */
data class DocumentGenerationState(
    val lastGeneratedPdfPath: String = "",
    val completeAtestadoPdfPath: String = "",
    val isGeneratingAtestado: Boolean = false,
    val isGeneratingCompleteAtestado: Boolean = false,
    val isGeneratingOdt: Boolean = false
)
