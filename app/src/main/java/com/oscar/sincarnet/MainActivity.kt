package com.oscar.sincarnet

import android.content.ActivityNotFoundException
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import com.oscar.sincarnet.ui.theme.SinCarnetTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ═══════════════════════════════════════════════════════════════════════════
// CONSTANTES DE NAVEGACIÓN
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Constantes de rutas para la navegación en Compose.
 *
 * Estas rutas definen todas las pantallas disponibles en la aplicación
 * y se usan en el NavController simulado (via currentRoute state).
 */
private const val CASES_ROUTE = "cases"
private const val EXPIRED_VALIDITY_ROUTE = "expired_validity"
private const val JUDICIAL_SUSPENSION_ROUTE = "judicial_suspension"
private const val WITHOUT_PERMIT_ROUTE = "without_permit"
private const val SPECIAL_CASES_ROUTE = "special_cases"
private const val COURTS_ROUTE = "courts"
private const val ATESTADO_DATA_ROUTE = "atestado_data"
private const val ATESTADO_OCURRENCIA_DELIT_ROUTE = "atestado_ocurrencia_delit"
private const val ATESTADO_PERSON_DATA_ROUTE = "atestado_person_data"
private const val ATESTADO_MANIFESTACION_ROUTE = "atestado_manifestacion"
private const val ATESTADO_VEHICLE_DATA_ROUTE = "atestado_vehicle_data"
private const val ATESTADO_COURT_DATA_ROUTE = "atestado_court_data"
private const val ATESTADO_ACTING_DATA_ROUTE = "atestado_acting_data"
private const val ATESTADO_SIGNATURES_ROUTE = "atestado_signatures"
private const val FIRMA_SCREEN_ROUTE = "firma_screen"
private const val BLUETOOTH_PRINTER_ROUTE = "bluetooth_printer"
private const val DOCUMENT_SCANNER_ROUTE = "document_scanner"

/**
 * Activity principal de SinCarnet - Single-Activity Architecture.
 *
 * Responsabilidades principales:
 * 1. **Orquestación de Navegación**: Maneja todas las 18 rutas/pantallas
 * 2. **Configuración NFC**: Establece ReaderMode para lectura de DNI electrónico
 * 3. **Coordinación de Impresión**: Integración con Bluetooth y DocumentPrinter
 * 4. **Generación de Documentos**: PDF/ODT desde datos capturados
 * 5. **Gestión de Estado**: Almacenamiento temporal de datos de atestados
 *
 * **Patrón Arquitectónico**:
 * - Jetpack Compose 100%
 * - Estados con rememberSaveable para persistencia en configuración
 * - Coroutines para operaciones asincrónicas (lectura NFC, generación PDF)
 * - Storage managers para persistencia entre sesiones
 *
 * **Flujo Típico de Atestado**:
 * 1. Usuario selecciona tipo en CasesScreen → ATESTADO_DATA_ROUTE
 * 2. Llena datos: persona, vehículo, juzgado, actuantes
 * 3. Captura firmas → FIRMA_SCREEN_ROUTE (canvas)
 * 4. Genera PDF → DocumentPrinter.imprimirAtestadoCompleto
 * 5. Vuelve a CasesScreen o descarga PDF
 *
 * @see NfcTagRepository Para lectura de DNI
 * @see DocumentPrinter Para generación de PDF
 * @see BluetoothPrinterStorage Para gestión de impresoras
 */
class MainActivity : ComponentActivity() {
    private companion object {
        const val NFC_LOG_TAG = "MainActivityNfc"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIGURACIÓN NFC
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Adaptador NFC del dispositivo.
     *
     * Se inicializa en onCreate y se usa para activar/desactivar ReaderMode.
     * Permite detectar automáticamente etiquetas NFC sin necesidad de Intent dispatch.
     */
    private var nfcAdapter: NfcAdapter? = null

    /**
     * Callback que se ejecuta cuando se detecta una etiqueta NFC en ReaderMode.
     *
     * - Valida que la etiqueta no sea nula
     * - Extrae el UID y tecnologías soportadas
     * - Actualiza el repositorio singleton NfcTagRepository
     * - Registra en log con información de debug
     *
     * Usado por pantallas que necesitan leer DNI electrónico:
     * - DatosPersonaInvestigadaScreen (auto-llenar datos DNI)
     * - Cualquier pantalla que tenga botón "Leer desde NFC"
     */
    private val readerCallback = NfcAdapter.ReaderCallback { tag ->
        if (tag == null) {
            Log.w(NFC_LOG_TAG, "ReaderMode callback con tag=null")
            return@ReaderCallback
        }
        val uid = tag.id?.joinToString(":") { "%02X".format(it) }.orEmpty()
        Log.i(NFC_LOG_TAG, "ReaderMode tag detectado uid=$uid techs=${tag.techList.joinToString()}")
        NfcTagRepository.update(tag)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CICLO DE VIDA - onCreate
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Inicializa la Activity.
     *
     * Pasos:
     * 1. Obtiene adaptador NFC
     * 2. Registra estado NFC para debug
     * 3. Procesa intents NFC si la app se abrió desde uno
     * 4. Habilita edge-to-edge display
     * 5. Construye toda la interfaz con Compose (18 pantallas)
     *
     * @param savedInstanceState Estado guardado por el sistema (usado por rememberSaveable)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        logNfcAdapterState("onCreate")
        processNfcIntent(intent)
        enableEdgeToEdge()
        setContent {
            SinCarnetTheme {
                var showSplash by remember { mutableStateOf(true) }
                var currentRoute by rememberSaveable { mutableStateOf(CASES_ROUTE) }
                var showAboutDialog by rememberSaveable { mutableStateOf(false) }
                var atestadoReturnRoute by rememberSaveable { mutableStateOf(CASES_ROUTE) }
                var printerReturnRoute by rememberSaveable { mutableStateOf(ATESTADO_DATA_ROUTE) }
                var lastGeneratedPdfPath by rememberSaveable { mutableStateOf("") }
                var isGeneratingAtestado by rememberSaveable { mutableStateOf(false) }
                var isGeneratingOdt by rememberSaveable { mutableStateOf(false) }
                var atestadoGenerateReason by rememberSaveable { mutableStateOf("Siniestro Vial") }
                var atestadoGenerateArticleNorm by rememberSaveable { mutableStateOf("LSV") }
                var atestadoGenerateArticleText by rememberSaveable { mutableStateOf("") }
                var atestadoDgtNoRecord by rememberSaveable { mutableStateOf(false) }
                var atestadoInternationalNoRecord by rememberSaveable { mutableStateOf(false) }
                var atestadoExistsRecord by rememberSaveable { mutableStateOf(false) }
                var atestadoVicisitudesOption by rememberSaveable { mutableStateOf("") }
                var atestadoJefaturaProvincial by rememberSaveable { mutableStateOf("") }
                var atestadoTiempoPrivacion by rememberSaveable { mutableStateOf("") }
                var atestadoJuzgadoDecreta by rememberSaveable { mutableStateOf("") }
                var hasSecondDriver by rememberSaveable { mutableStateOf(false) }
                var secondDriverName by rememberSaveable { mutableStateOf("") }
                var secondDriverId by rememberSaveable { mutableStateOf("") }
                var wantsToSign by rememberSaveable { mutableStateOf(true) }
                val mainScope = rememberCoroutineScope()
                val actuantesStorage = remember { ActuantesStorage(applicationContext) }
                val personaStorage = remember { PersonaInvestigadaStorage(applicationContext) }
                val vehiculoStorage = remember { VehiculoStorage(applicationContext) }
                val manifestacionStorage = remember { ManifestacionStorage(applicationContext) }
                val tipHistory = actuantesStorage.getTipHistory()
                val unitHistory = actuantesStorage.getUnitHistory()

                // Estado en memoria de todas las firmas de la sesión actual de la app.
                val signaturesBySigner = remember { mutableStateMapOf<String, ImageBitmap>() }

                // Estado en memoria de datos de actuantes para el atestado actual.
                var instructorEmployment by rememberSaveable { mutableStateOf("") }
                var instructorTip by rememberSaveable { mutableStateOf("") }
                var instructorUnit by rememberSaveable { mutableStateOf("") }
                var secretaryEmployment by rememberSaveable { mutableStateOf("") }
                var secretaryTip by rememberSaveable { mutableStateOf("") }
                var secretaryUnit by rememberSaveable { mutableStateOf("") }
                var sameUnit by rememberSaveable { mutableStateOf(false) }
                var actingStatusMessage by rememberSaveable { mutableStateOf("") }
                var canRecoverActingData by rememberSaveable { mutableStateOf(false) }

                // Estado en memoria de datos de persona investigada para el atestado actual.
                var investigatedNationality by rememberSaveable { mutableStateOf("España") }
                var investigatedSex by rememberSaveable { mutableStateOf(getString(R.string.person_data_sex_unknown)) }
                var investigatedFirstName by rememberSaveable { mutableStateOf("") }
                var investigatedLastName1 by rememberSaveable { mutableStateOf("") }
                var investigatedLastName2 by rememberSaveable { mutableStateOf("") }
                var investigatedAddress by rememberSaveable { mutableStateOf("") }
                var investigatedBirthDate by rememberSaveable { mutableStateOf("") }
                var investigatedBirthPlace by rememberSaveable { mutableStateOf("") }
                var investigatedFatherName by rememberSaveable { mutableStateOf("") }
                var investigatedMotherName by rememberSaveable { mutableStateOf("") }
                var investigatedPhone by rememberSaveable { mutableStateOf("") }
                var investigatedEmail by rememberSaveable { mutableStateOf("") }

                // Estado en memoria de datos del vehículo para el atestado actual.
                var vehicleBrand by rememberSaveable { mutableStateOf("") }
                var vehicleModel by rememberSaveable { mutableStateOf("") }
                var vehiclePlate by rememberSaveable { mutableStateOf("") }
                var vehicleRegistrationDate by rememberSaveable { mutableStateOf("") }
                var vehicleNationality by rememberSaveable { mutableStateOf("España") }
                var vehicleItvDate by rememberSaveable { mutableStateOf("") }
                var vehicleInsurer by rememberSaveable { mutableStateOf("") }
                var vehicleType by rememberSaveable { mutableStateOf("") }
                var vehicleClasePermiso by rememberSaveable { mutableStateOf("") }
                var vehicleOwnerIsOther by rememberSaveable { mutableStateOf(false) }
                var vehicleOwnerName by rememberSaveable { mutableStateOf("") }
                var vehicleOwnerLastNames by rememberSaveable { mutableStateOf("") }
                var vehicleOwnerDni by rememberSaveable { mutableStateOf("") }
                var vehicleOwnerAddress by rememberSaveable { mutableStateOf("") }
                var vehicleOwnerPhone by rememberSaveable { mutableStateOf("") }

                fun applyVehiculoData(data: VehiculoData) {
                    vehicleBrand = data.brand
                    vehicleModel = data.model
                    vehiclePlate = data.plate
                    vehicleRegistrationDate = data.registrationDate
                    vehicleNationality = data.nationality
                    vehicleItvDate = data.itvDate
                    vehicleInsurer = data.insurer
                    vehicleType = data.vehicleType
                    vehicleClasePermiso = data.clasePermiso
                    vehicleOwnerIsOther = data.ownerIsOther
                    vehicleOwnerName = data.ownerName
                    vehicleOwnerLastNames = data.ownerLastNames
                    vehicleOwnerDni = data.ownerDni
                    vehicleOwnerAddress = data.ownerAddress
                    vehicleOwnerPhone = data.ownerPhone
                }

                fun applyActuantesData(data: ActuantesData) {
                    instructorEmployment = data.instructorEmployment
                    instructorTip = data.instructorTip
                    instructorUnit = data.instructorUnit
                    secretaryEmployment = data.secretaryEmployment
                    secretaryTip = data.secretaryTip
                    secretaryUnit = data.secretaryUnit
                    sameUnit = data.sameUnit
                }

                fun applyPersonaData(data: PersonaInvestigadaData) {
                    investigatedNationality = data.nationality
                    investigatedSex = data.sex
                    investigatedFirstName = data.firstName
                    investigatedLastName1 = data.lastName1
                    investigatedLastName2 = data.lastName2
                    investigatedAddress = data.address
                    investigatedBirthDate = data.birthDate
                    investigatedBirthPlace = data.birthPlace
                    investigatedFatherName = data.fatherName
                    investigatedMotherName = data.motherName
                    investigatedPhone = data.phone
                    investigatedEmail = data.email
                }

                val resetAtestadoSession: () -> Unit = {
                    signaturesBySigner.clear()
                    actingStatusMessage = ""
                    // Mantener borrador de manifestación entre reinicios de app/sesión.
                    investigatedNationality = "España"
                    investigatedSex = getString(R.string.person_data_sex_unknown)
                    investigatedFirstName = ""
                    investigatedLastName1 = ""
                    investigatedLastName2 = ""
                    investigatedAddress = ""
                    investigatedBirthDate = ""
                    investigatedBirthPlace = ""
                    investigatedFatherName = ""
                    investigatedMotherName = ""
                    investigatedPhone = ""
                    investigatedEmail = ""
                    vehicleBrand = ""
                    vehicleModel = ""
                    vehiclePlate = ""
                    vehicleRegistrationDate = ""
                    vehicleNationality = "España"
                    vehicleItvDate = ""
                    vehicleInsurer = ""
                    vehicleType = ""
                    vehicleClasePermiso = ""
                    vehicleOwnerIsOther = false
                    vehicleOwnerName = ""
                    vehicleOwnerLastNames = ""
                    vehicleOwnerDni = ""
                    vehicleOwnerAddress = ""
                    vehicleOwnerPhone = ""
                    atestadoGenerateReason = "Siniestro Vial"
                    atestadoGenerateArticleNorm = "LSV"
                    atestadoGenerateArticleText = ""
                    atestadoDgtNoRecord = false
                    atestadoInternationalNoRecord = false
                    atestadoExistsRecord = false
                    atestadoVicisitudesOption = ""
                    atestadoJefaturaProvincial = ""
                    atestadoTiempoPrivacion = ""
                    atestadoJuzgadoDecreta = ""
                    secondDriverName = ""
                    secondDriverId = ""
                }

                // Quién está firmando actualmente
                var currentSignerKey by rememberSaveable { mutableStateOf("") }
                // Indica que FirmasAtestadoScreen fue abierto desde DatosJuzgadoAtestadoScreen
                var signaturesOpenedFromCourt by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    applyActuantesData(actuantesStorage.loadCurrent())
                    applyVehiculoData(vehiculoStorage.loadCurrent())
                    canRecoverActingData = actuantesStorage.hasRecoverableBackup()
                    delay(3000)
                    showSplash = false
                }

                LaunchedEffect(currentRoute) {
                    if (currentRoute == ATESTADO_VEHICLE_DATA_ROUTE) {
                        applyVehiculoData(vehiculoStorage.loadCurrent())
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (showSplash) {
                        SplashScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    } else {
                        when (currentRoute) {
                            EXPIRED_VALIDITY_ROUTE -> ExpiredValidityScreen(
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                onBackClick = { currentRoute = CASES_ROUTE },
                                onStartAtestadoClick = {
                                    resetAtestadoSession()
                                    atestadoReturnRoute = EXPIRED_VALIDITY_ROUTE
                                    currentRoute = ATESTADO_DATA_ROUTE
                                }
                            )

                            JUDICIAL_SUSPENSION_ROUTE -> JudicialSuspensionScreen(
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                onBackClick = { currentRoute = CASES_ROUTE },
                                onStartAtestadoClick = {
                                    resetAtestadoSession()
                                    atestadoReturnRoute = JUDICIAL_SUSPENSION_ROUTE
                                    currentRoute = ATESTADO_DATA_ROUTE
                                }
                            )

                            WITHOUT_PERMIT_ROUTE -> WithoutPermitScreen(
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                onBackClick = { currentRoute = CASES_ROUTE },
                                onStartAtestadoClick = {
                                    resetAtestadoSession()
                                    atestadoReturnRoute = WITHOUT_PERMIT_ROUTE
                                    currentRoute = ATESTADO_DATA_ROUTE
                                }
                            )

                            SPECIAL_CASES_ROUTE -> SpecialCasesScreen(
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                onBackClick = { currentRoute = CASES_ROUTE }
                            )

                            COURTS_ROUTE -> ConsultaJuzgadosScreen(
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                onBackClick = { currentRoute = CASES_ROUTE }
                            )

                            ATESTADO_DATA_ROUTE -> TomaDatosAtestadoScreen(
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                onBackClick = { currentRoute = atestadoReturnRoute },
                                onPrintClick = {
                                    printerReturnRoute = ATESTADO_DATA_ROUTE
                                    currentRoute = BLUETOOTH_PRINTER_ROUTE
                                },
                                onLocationTimeClick = { currentRoute = ATESTADO_OCURRENCIA_DELIT_ROUTE },
                                onPersonDataClick = { currentRoute = ATESTADO_PERSON_DATA_ROUTE },
                                onVehicleDataClick = { currentRoute = ATESTADO_VEHICLE_DATA_ROUTE },
                                onCourtDataClick = { currentRoute = ATESTADO_COURT_DATA_ROUTE },
                                onActingDataClick = { currentRoute = ATESTADO_ACTING_DATA_ROUTE },
                                onSignaturesClick = {
                                    currentRoute = ATESTADO_SIGNATURES_ROUTE
                                },
                                onScanDocumentClick = {
                                    currentRoute = DOCUMENT_SCANNER_ROUTE
                                },
                                printSignatures = PrintSignatures(
                                    instructor    = signaturesBySigner[SIGNER_INSTRUCTOR],
                                    secretary     = signaturesBySigner[SIGNER_SECRETARY],
                                    investigated  = signaturesBySigner[SIGNER_INVESTIGATED],
                                    instructorTip = instructorTip,
                                    secretaryTip  = secretaryTip
                                )
                            )

                            ATESTADO_OCURRENCIA_DELIT_ROUTE -> DatosOcurrenciaDelitScreen(
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                onBackClick = { currentRoute = ATESTADO_DATA_ROUTE },
                                onPrintClick = {
                                    printerReturnRoute = ATESTADO_OCURRENCIA_DELIT_ROUTE
                                    currentRoute = BLUETOOTH_PRINTER_ROUTE
                                }
                            )

                            ATESTADO_COURT_DATA_ROUTE -> DatosJuzgadoAtestadoScreen(
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                onBackClick = { currentRoute = ATESTADO_DATA_ROUTE },
                                onPrintClick = {
                                    printerReturnRoute = ATESTADO_COURT_DATA_ROUTE
                                    currentRoute = BLUETOOTH_PRINTER_ROUTE
                                },
                                onPrintSummons = {
                                    val mac = BluetoothPrinterStorage(applicationContext).getDefaultPrinter()?.mac
                                    if (mac.isNullOrBlank()) {
                                        throw IllegalStateException("No hay impresora configurada")
                                    } else {
                                        val courtData = JuzgadoAtestadoStorage(applicationContext).loadCurrent()
                                        android.util.Log.d("CITACION_DECISION", "courtData.tipoJuicio='${courtData.tipoJuicio}', fechaJuicioRapido='${courtData.fechaJuicioRapido}', horaJuicioRapido='${courtData.horaJuicioRapido}', isJuicioRapido=${courtData.isJuicioRapido()}")
                                        if (courtData.isJuicioRapido()) {
                                            DocumentPrinter.imprimirCitacionJuicioRapido(applicationContext, mac)
                                        } else {
                                            DocumentPrinter.imprimirCitacionJuicio(applicationContext, mac)
                                        }
                                    }
                                }
                            )

                            BLUETOOTH_PRINTER_ROUTE -> BluetoothPrinterScreen(
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                onBackClick = { currentRoute = printerReturnRoute }
                            )

                            ATESTADO_VEHICLE_DATA_ROUTE -> DatosVehiculoScreen(
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                onBackClick = { currentRoute = ATESTADO_DATA_ROUTE },
                                brand = vehicleBrand,
                                onBrandChange = { vehicleBrand = it },
                                model = vehicleModel,
                                onModelChange = { vehicleModel = it },
                                plate = vehiclePlate,
                                onPlateChange = { vehiclePlate = it },
                                registrationDate = vehicleRegistrationDate,
                                onRegistrationDateChange = { vehicleRegistrationDate = it },
                                nationality = vehicleNationality,
                                onNationalityChange = { vehicleNationality = it },
                                itvDate = vehicleItvDate,
                                onItvDateChange = { vehicleItvDate = it },
                                insurer = vehicleInsurer,
                                onInsurerChange = { vehicleInsurer = it },
                                vehicleType = vehicleType,
                                onVehicleTypeChange = { vehicleType = it },
                                clasePermiso = vehicleClasePermiso,
                                onClasePermisoChange = { vehicleClasePermiso = it },
                                ownerIsOther = vehicleOwnerIsOther,
                                onOwnerIsOtherChange = { vehicleOwnerIsOther = it },
                                ownerName = vehicleOwnerName,
                                onOwnerNameChange = { vehicleOwnerName = it },
                                ownerLastNames = vehicleOwnerLastNames,
                                onOwnerLastNamesChange = { vehicleOwnerLastNames = it },
                                ownerDni = vehicleOwnerDni,
                                onOwnerDniChange = { vehicleOwnerDni = it },
                                ownerAddress = vehicleOwnerAddress,
                                onOwnerAddressChange = { vehicleOwnerAddress = it },
                                ownerPhone = vehicleOwnerPhone,
                                onOwnerPhoneChange = { vehicleOwnerPhone = it },
                                onSaveClick = {
                                    vehiculoStorage.saveCurrent(
                                        VehiculoData(
                                            brand = vehicleBrand,
                                            model = vehicleModel,
                                            plate = vehiclePlate,
                                            registrationDate = vehicleRegistrationDate,
                                            nationality = vehicleNationality,
                                            itvDate = vehicleItvDate,
                                            insurer = vehicleInsurer,
                                            vehicleType = vehicleType,
                                            clasePermiso = vehicleClasePermiso,
                                            ownerIsOther = vehicleOwnerIsOther,
                                            ownerName = vehicleOwnerName,
                                            ownerLastNames = vehicleOwnerLastNames,
                                            ownerDni = vehicleOwnerDni,
                                            ownerAddress = vehicleOwnerAddress,
                                            ownerPhone = vehicleOwnerPhone
                                        )
                                    )
                                },
                                onDeleteClick = {
                                    vehiculoStorage.clearCurrent()
                                    applyVehiculoData(VehiculoData())
                                }
                            )

                            ATESTADO_PERSON_DATA_ROUTE -> DatosPersonaInvestigadaScreen(
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                onBackClick = { currentRoute = ATESTADO_DATA_ROUTE },
                                onRightsClick = {
                                    // Se implementará en una iteración posterior.
                                },
                                onManifestacionClick = {
                                    currentRoute = ATESTADO_MANIFESTACION_ROUTE
                                }
                            )

                            ATESTADO_MANIFESTACION_ROUTE -> ManifestacionScreen(
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                onBackClick = { currentRoute = ATESTADO_PERSON_DATA_ROUTE }
                            )

                            ATESTADO_ACTING_DATA_ROUTE -> DatosActuantesScreen(
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                onBackClick = { currentRoute = ATESTADO_DATA_ROUTE },
                                instructorEmployment = instructorEmployment,
                                onInstructorEmploymentChange = { instructorEmployment = it },
                                instructorTip = instructorTip,
                                onInstructorTipChange = { instructorTip = it },
                                instructorUnit = instructorUnit,
                                onInstructorUnitChange = {
                                    instructorUnit = it
                                    if (sameUnit) secretaryUnit = it
                                },
                                secretaryEmployment = secretaryEmployment,
                                onSecretaryEmploymentChange = { secretaryEmployment = it },
                                secretaryTip = secretaryTip,
                                onSecretaryTipChange = { secretaryTip = it },
                                secretaryUnit = secretaryUnit,
                                onSecretaryUnitChange = { secretaryUnit = it },
                                sameUnit = sameUnit,
                                onSameUnitChange = {
                                    sameUnit = it
                                    if (it) secretaryUnit = instructorUnit
                                },
                                tipHistory = tipHistory,
                                unitHistory = unitHistory,
                                onSaveClick = {
                                    actuantesStorage.saveCurrent(
                                        ActuantesData(
                                            instructorEmployment = instructorEmployment,
                                            instructorTip = instructorTip,
                                            instructorUnit = instructorUnit,
                                            secretaryEmployment = secretaryEmployment,
                                            secretaryTip = secretaryTip,
                                            secretaryUnit = secretaryUnit,
                                            sameUnit = sameUnit
                                        )
                                    )
                                    actuantesStorage.addTipToHistory(instructorTip, true)
                                    actuantesStorage.addTipToHistory(secretaryTip, false)
                                    actuantesStorage.addUnitToHistory(instructorUnit, true)
                                    actuantesStorage.addUnitToHistory(secretaryUnit, false)
                                    canRecoverActingData = actuantesStorage.hasRecoverableBackup()
                                    actingStatusMessage = getString(R.string.atestado_acting_saved_message)
                                    currentRoute = ATESTADO_DATA_ROUTE
                                },
                                onDeleteClick = {
                                    actuantesStorage.deleteCurrentWithBackup()
                                    applyActuantesData(ActuantesData())
                                    canRecoverActingData = actuantesStorage.hasRecoverableBackup()
                                    actingStatusMessage = getString(R.string.atestado_acting_deleted_message)
                                },
                                onRecoverClick = {
                                    val recovered = actuantesStorage.recoverDeleted()
                                    if (recovered != null) {
                                        applyActuantesData(recovered)
                                        actingStatusMessage = getString(R.string.atestado_acting_recovered_message)
                                    }
                                    canRecoverActingData = actuantesStorage.hasRecoverableBackup()
                                },
                                canRecover = canRecoverActingData,
                                statusMessage = actingStatusMessage
                            )

                            ATESTADO_SIGNATURES_ROUTE -> FirmasAtestadoScreen(
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                onBackClick = {
                                    if (signaturesOpenedFromCourt) {
                                        signaturesOpenedFromCourt = false
                                        currentRoute = ATESTADO_COURT_DATA_ROUTE
                                    } else {
                                        currentRoute = ATESTADO_DATA_ROUTE
                                    }
                                },
                                onPrintClick = {
                                    printerReturnRoute = ATESTADO_SIGNATURES_ROUTE
                                    currentRoute = BLUETOOTH_PRINTER_ROUTE
                                },
                                instructorSignature = signaturesBySigner[SIGNER_INSTRUCTOR],
                                secretarySignature = signaturesBySigner[SIGNER_SECRETARY],
                                investigatedSignature = signaturesBySigner[SIGNER_INVESTIGATED],
                                secondDriverSignature = signaturesBySigner[SIGNER_SECOND_DRIVER],
                                onInstructorClick = {
                                    currentSignerKey = SIGNER_INSTRUCTOR
                                    currentRoute = FIRMA_SCREEN_ROUTE
                                },
                                onSecretaryClick = {
                                    currentSignerKey = SIGNER_SECRETARY
                                    currentRoute = FIRMA_SCREEN_ROUTE
                                },
                                onInvestigatedClick = {
                                    currentSignerKey = SIGNER_INVESTIGATED
                                    currentRoute = FIRMA_SCREEN_ROUTE
                                },
                                onSecondDriverClick = {
                                    currentSignerKey = SIGNER_SECOND_DRIVER
                                    currentRoute = FIRMA_SCREEN_ROUTE
                                },
                                selectedGenerateReason = atestadoGenerateReason,
                                onSelectedGenerateReasonChange = { atestadoGenerateReason = it },
                                selectedArticleNorm = atestadoGenerateArticleNorm,
                                onSelectedArticleNormChange = { atestadoGenerateArticleNorm = it },
                                selectedArticleText = atestadoGenerateArticleText,
                                onSelectedArticleTextChange = { atestadoGenerateArticleText = it },
                                dgtNoRecord = atestadoDgtNoRecord,
                                onDgtNoRecordChange = { atestadoDgtNoRecord = it },
                                internationalNoRecord = atestadoInternationalNoRecord,
                                onInternationalNoRecordChange = { atestadoInternationalNoRecord = it },
                                existsRecord = atestadoExistsRecord,
                                onExistsRecordChange = { atestadoExistsRecord = it },
                                vicisitudesOption = atestadoVicisitudesOption,
                                onVicisitudesOptionChange = { atestadoVicisitudesOption = it },
                                jefaturaProvincial = atestadoJefaturaProvincial,
                                onJefaturaProvincialChange = { atestadoJefaturaProvincial = it },
                                tiempoPrivacion = atestadoTiempoPrivacion,
                                onTiempoPrivacionChange = { atestadoTiempoPrivacion = it },
                                juzgadoDecreta = atestadoJuzgadoDecreta,
                                onJuzgadoDecretaChange = { atestadoJuzgadoDecreta = it },
                                wantsToSign = wantsToSign,
                                onWantsToSignChange = { wantsToSign = it },
                                hasSecondDriver = hasSecondDriver,
                                onHasSecondDriverChange = { hasSecondDriver = it },
                                onGenerateAtestadoClick = { wantsToSign, hasSecondDriver, reason, articleNorm, articleText ->
                                    if (isGeneratingAtestado) return@FirmasAtestadoScreen
                                    atestadoGenerateReason = reason
                                    if (articleNorm != null) {
                                        atestadoGenerateArticleNorm = articleNorm
                                    }
                                    atestadoGenerateArticleText = articleText
                                    isGeneratingAtestado = true
                                    mainScope.launch {
                                        runCatching {
                                            withContext(Dispatchers.IO) {
                                                // Incluir siempre datos del juzgado y persona investigada en el PDF
                                                val courtData = JuzgadoAtestadoStorage(applicationContext).loadCurrent()
                                                val pdfPersonData = PersonaInvestigadaStorage(applicationContext).loadCurrent()
                                                val ocurrenciaData = OcurrenciaDelitStorage(applicationContext).loadCurrent()
                                                val vehicleData = VehiculoStorage(applicationContext).loadCurrent()
                                                val manifestacionData = ManifestacionStorage(applicationContext).loadCurrent()
                                                val signaturesToUse = signaturesBySigner.toMutableMap().apply {
                                                    if (!hasSecondDriver) remove(SIGNER_SECOND_DRIVER)
                                                }
                                                val mappedSignatures = mapSignaturesForPdf(
                                                    signaturesBySigner = signaturesToUse,
                                                    investigatedWantsToSign = wantsToSign,
                                                    investigatedNoSignText = getString(R.string.atestado_signature_no_desire)
                                                )
                                                val inicioModalData = AtestadoInicioModalData(
                                                    motivo = atestadoGenerateReason,
                                                    norma = atestadoGenerateArticleNorm,
                                                    articulo = atestadoGenerateArticleText,
                                                    dgtNoRecord = atestadoDgtNoRecord,
                                                    internationalNoRecord = atestadoInternationalNoRecord,
                                                    existsRecord = atestadoExistsRecord,
                                                    vicisitudesOption = atestadoVicisitudesOption,
                                                    jefaturaProvincial = atestadoJefaturaProvincial,
                                                    tiempoPrivacion = atestadoTiempoPrivacion,
                                                    juzgadoDecreta = atestadoJuzgadoDecreta
                                                )
                                                AtestadoInicioStorage(applicationContext).saveCurrent(inicioModalData)
                                                generateAtestadoContinuousPdf(
                                                    context = applicationContext,
                                                    courtData = courtData,
                                                    personData = pdfPersonData,
                                                    ocurrenciaData = ocurrenciaData,
                                                    vehicleData = vehicleData,
                                                    manifestacionData = manifestacionData,
                                                    signatures = mappedSignatures,
                                                    investigatedNoSignText = getString(R.string.atestado_signature_no_desire),
                                                    instructorTip = instructorTip,
                                                    secretaryTip = secretaryTip,
                                                    instructorUnit = instructorUnit,
                                                    inicioModalData = inicioModalData,
                                                    hasSecondDriver = hasSecondDriver
                                                )
                                            }
                                        }.onSuccess { result ->
                                            lastGeneratedPdfPath = result.file.absolutePath
                                            val opened = openGeneratedPdf(result.file)
                                            if (!opened) {
                                                Toast.makeText(
                                                    applicationContext,
                                                    getString(R.string.atestado_pdf_open_error),
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                            // Si veníamos de la pantalla de juzgado, volver a ella
                                            if (signaturesOpenedFromCourt) {
                                                signaturesOpenedFromCourt = false
                                                currentRoute = ATESTADO_COURT_DATA_ROUTE
                                            }
                                        }.onFailure {
                                            Toast.makeText(
                                                applicationContext,
                                                getString(R.string.atestado_pdf_generated_error),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        isGeneratingAtestado = false
                                    }
                                },
                                investigatedCopyEnabled = lastGeneratedPdfPath.isNotBlank(),
                                onPrintInvestigatedCopyClick = {
                                    // Se implementará en una iteración posterior.
                                },
                                shareEnabled = lastGeneratedPdfPath.isNotBlank(),
                                onSharePdfClick = {
                                    val pdfFile = File(lastGeneratedPdfPath)
                                    val shared = pdfFile.exists() && shareGeneratedPdf(pdfFile)
                                    if (!shared) {
                                        Toast.makeText(
                                            applicationContext,
                                            getString(R.string.atestado_pdf_share_error),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                shareOdtEnabled = lastGeneratedPdfPath.isNotBlank(),
                                onShareOdtClick = {
                                    if (lastGeneratedPdfPath.isBlank()) {
                                        Toast.makeText(
                                            applicationContext,
                                            getString(R.string.atestado_odt_not_found_error),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@FirmasAtestadoScreen
                                    }
                                    if (isGeneratingOdt) return@FirmasAtestadoScreen

                                    mainScope.launch {
                                        isGeneratingOdt = true
                                        runCatching {
                                            withContext(Dispatchers.IO) {
                                                val courtData = JuzgadoAtestadoStorage(applicationContext).loadCurrent()
                                                val pdfPersonData = PersonaInvestigadaStorage(applicationContext).loadCurrent()
                                                val ocurrenciaData = OcurrenciaDelitStorage(applicationContext).loadCurrent()
                                                val vehicleData = VehiculoStorage(applicationContext).loadCurrent()
                                                val manifestacionData = ManifestacionStorage(applicationContext).loadCurrent()
                                                val signaturesToUse = signaturesBySigner.toMutableMap().apply {
                                                    if (!hasSecondDriver) remove(SIGNER_SECOND_DRIVER)
                                                }
                                                val mappedSignatures = mapSignaturesForPdf(
                                                    signaturesBySigner = signaturesToUse,
                                                    investigatedWantsToSign = wantsToSign,
                                                    investigatedNoSignText = getString(R.string.atestado_signature_no_desire)
                                                )
                                                val inicioModalData = AtestadoInicioStorage(applicationContext).loadCurrent()
                                                generateAtestadoOdt(
                                                    context = applicationContext,
                                                    courtData = courtData,
                                                    personData = pdfPersonData,
                                                    ocurrenciaData = ocurrenciaData,
                                                    vehicleData = vehicleData,
                                                    manifestacionData = manifestacionData,
                                                    signatures = mappedSignatures,
                                                    investigatedNoSignText = getString(R.string.atestado_signature_no_desire),
                                                    instructorTip = instructorTip,
                                                    secretaryTip = secretaryTip,
                                                    instructorUnit = instructorUnit,
                                                    inicioModalData = inicioModalData,
                                                    hasSecondDriver = hasSecondDriver
                                                )
                                            }
                                        }.onSuccess { odtFile ->
                                            isGeneratingOdt = false
                                            val shared = shareGeneratedOdt(odtFile)
                                            if (!shared) {
                                                Toast.makeText(
                                                    applicationContext,
                                                    getString(R.string.atestado_odt_share_error),
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }.onFailure {
                                            isGeneratingOdt = false
                                            Toast.makeText(
                                                applicationContext,
                                                getString(R.string.atestado_odt_generate_error),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                },
                                isGeneratingAtestado = isGeneratingAtestado,
                                isGeneratingOdt = isGeneratingOdt
                            )

                            DOCUMENT_SCANNER_ROUTE -> DocumentScannerScreen(
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                onBackClick = { currentRoute = ATESTADO_DATA_ROUTE },
                                onOpenPdf = { pdfFile ->
                                    val opened = openGeneratedPdf(pdfFile)
                                    if (!opened) {
                                        Toast.makeText(
                                            applicationContext,
                                            getString(R.string.scan_pdf_open_error),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                onSharePdf = { pdfFile ->
                                    val shared = shareGeneratedPdf(pdfFile)
                                    if (!shared) {
                                        Toast.makeText(
                                            applicationContext,
                                            getString(R.string.atestado_pdf_share_error),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            )

                            FIRMA_SCREEN_ROUTE -> FirmaManuscritaScreen(                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                signerName = when (currentSignerKey) {
                                    SIGNER_INSTRUCTOR -> stringResource(R.string.atestado_signature_instructor)
                                    SIGNER_SECRETARY -> stringResource(R.string.atestado_signature_secretary)
                                    SIGNER_INVESTIGATED -> stringResource(R.string.atestado_signature_investigated)
                                    SIGNER_SECOND_DRIVER -> stringResource(R.string.atestado_signature_second_driver)
                                    else -> ""
                                },
                                onSignatureSaved = { bitmap ->
                                    when (currentSignerKey) {
                                        SIGNER_INSTRUCTOR,
                                        SIGNER_SECRETARY,
                                        SIGNER_INVESTIGATED,
                                        SIGNER_SECOND_DRIVER -> signaturesBySigner[currentSignerKey] = bitmap
                                    }
                                    currentRoute = ATESTADO_SIGNATURES_ROUTE
                                },
                                onCancel = { currentRoute = ATESTADO_SIGNATURES_ROUTE }
                            )

                            else -> CasesScreen(
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                onExpiredValidityClick = { currentRoute = EXPIRED_VALIDITY_ROUTE },
                                onJudicialSuspensionClick = { currentRoute = JUDICIAL_SUSPENSION_ROUTE },
                                onWithoutPermitClick = { currentRoute = WITHOUT_PERMIT_ROUTE },
                                onSpecialCasesClick = { currentRoute = SPECIAL_CASES_ROUTE },
                                onCourtsClick = { currentRoute = COURTS_ROUTE },
                                onAboutClick = { showAboutDialog = true }
                            )
                        }
                    }
                }

                if (showAboutDialog) {
                    AboutDialog(onDismissRequest = { showAboutDialog = false })
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CICLO DE VIDA - onNewIntent
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Maneja nuevos intents mientras la Activity está en primer plano.
     *
     * Se usa para recibir intents NFC cuando la app ya está abierta.
     *
     * - Registra el nuevo intent
     * - Procesa el intent NFC recibido
     *
     * @param intent El intent que activa esta llamada
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(NFC_LOG_TAG, "onNewIntent() action=${intent.action}")
        setIntent(intent)
        processNfcIntent(intent)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CICLO DE VIDA - onResume
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Se llama cuando la Activity pasa a primer plano.
     *
     * - Reactiva el ReaderMode NFC para seguir recibiendo lecturas de etiquetas
     */
    override fun onResume() {
        super.onResume()
        logNfcAdapterState("onResume")
        enableNfcReaderMode()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CICLO DE VIDA - onPause
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Se llama cuando la Activity pasa a segundo plano.
     *
     * - Desactiva el ReaderMode NFC para evitar lecturas innecesarias
     */
    override fun onPause() {
        super.onPause()
        disableNfcReaderMode()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROCESAMIENTO DE INTENTS NFC
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Procesa el intent recibido por la Activity para detectar etiquetas NFC.
     *
     * - Valida que el intent no sea nulo
     * - Extrae la acción del intent
     * - Si es una acción NFC válida, obtiene la etiqueta y actualiza el repositorio
     * - Registra en log la información de la etiqueta detectada
     *
     * @param intent El intent a procesar
     */
    private fun processNfcIntent(intent: Intent?) {
        if (intent == null) {
            Log.d(NFC_LOG_TAG, "processNfcIntent intent=null")
            return
        }
        val action = intent.action
        if (action.isNullOrBlank()) {
            Log.d(NFC_LOG_TAG, "processNfcIntent sin action")
            return
        }
        Log.d(NFC_LOG_TAG, "processNfcIntent action=$action")
        if (
            action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            action == NfcAdapter.ACTION_NDEF_DISCOVERED
        ) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            val uid = tag?.id?.joinToString(":") { "%02X".format(it) }
            Log.i(NFC_LOG_TAG, "Tag detectado uid=${uid ?: "<null>"} techs=${tag?.techList?.joinToString() ?: "<none>"}")
            NfcTagRepository.update(tag)
        } else {
            Log.d(NFC_LOG_TAG, "Intent no NFC recibido: $action")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ESTADO Y CONFIGURACIÓN NFC
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Registra el estado del adaptador NFC en el log para depuración.
     *
     * Muestra si el adaptador está presente, habilitado y la acción del intent actual.
     *
     * @param origin El origen de la llamada (usado para identificar el ciclo de vida)
     */
    private fun logNfcAdapterState(origin: String) {
        val adapter = nfcAdapter ?: NfcAdapter.getDefaultAdapter(this)
        nfcAdapter = adapter
        val enabled = adapter?.isEnabled == true
        Log.d(NFC_LOG_TAG, "$origin nfcAdapterPresent=${adapter != null} nfcEnabled=$enabled currentIntentAction=${intent?.action}")
    }

    /**
     * Habilita el modo lector NFC para detectar etiquetas automáticamente.
     *
     * - Configura flags para leer diferentes tipos de etiquetas NFC
     * - Establece un delay para la detección de presencia
     * - Activa el ReaderMode del adaptador NFC
     *
     * @see NfcAdapter.enableReaderMode
     */
    private fun enableNfcReaderMode() {
        val adapter = nfcAdapter ?: return
        if (!adapter.isEnabled) {
            Log.w(NFC_LOG_TAG, "enableNfcReaderMode omitido: NFC desactivado")
            return
        }

        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        val options = Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 300)
        }
        Log.d(NFC_LOG_TAG, "enableReaderMode flags=$flags")
        adapter.enableReaderMode(this, readerCallback, flags, options)
    }

     /**
      * Desactiva el modo lector NFC.
      *
      * Se llama en onPause() para dejar de procesar etiquetas NFC cuando la Activity
      * está en segundo plano, evitando lecturas innecesarias y economizando batería.
      *
      * @see NfcAdapter.disableReaderMode
      */
     private fun disableNfcReaderMode() {
         val adapter = nfcAdapter ?: return
         Log.d(NFC_LOG_TAG, "disableReaderMode")
         adapter.disableReaderMode(this)
     }

     // ─────────────────────────────────────────────────────────────────────────
     // MANEJO DE DOCUMENTOS GENERADOS
     // ─────────────────────────────────────────────────────────────────────────

     /**
      * Abre un archivo PDF generado para su visualización.
      *
      * Flujo:
      * 1. Valida que el archivo PDF existe
      * 2. Crea un URI seguro usando FileProvider (requerido en Android 7+)
      * 3. Crea un Intent ACTION_VIEW con tipo MIME "application/pdf"
      * 4. Inicia la actividad del visor PDF instalado en el dispositivo
      * 5. Captura ActivityNotFoundException si no hay visor disponible
      *
      * Se usa tras generar un atestado para mostrar el resultado al usuario.
      *
      * @param pdfFile El archivo PDF a abrir
      * @return true si se pudo abrir el PDF sin errores, false si hubo una excepción
      */
     private fun openGeneratedPdf(pdfFile: File): Boolean {
        return runCatching {
            val uri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
            true
        }.recoverCatching {
            if (it is ActivityNotFoundException) {
                false
            } else {
                throw it
            }
        }.getOrDefault(false)
    }

     /**
      * Comparte un archivo PDF generado usando un Intent ACTION_SEND.
      *
      * Flujo:
      * 1. Crea un URI seguro usando FileProvider
      * 2. Construye un Intent ACTION_SEND con el tipo MIME "application/pdf"
      * 3. Crea un chooser para que el usuario seleccione la aplicación
      * 4. Agrega permisos READ_URI_PERMISSION al intent
      * 5. Inicia el chooser
      * 6. Captura ActivityNotFoundException si no hay aplicaciones para compartir
      *
      * Usado desde:
      * - FirmasAtestadoScreen → botón "Compartir PDF"
      * - DocumentScannerScreen → compartir PDF escaneado
      *
      * @param pdfFile El archivo PDF a compartir
      * @return true si se abrió el chooser, false si hubo un error
      */
     private fun shareGeneratedPdf(pdfFile: File): Boolean {
        return runCatching {
            val uri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, getString(R.string.atestado_signature_share_pdf)).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(chooser)
            true
        }.recoverCatching {
            if (it is ActivityNotFoundException) {
                false
            } else {
                throw it
            }
        }.getOrDefault(false)
    }

     /**
      * Comparte un archivo ODT (OpenDocument Text) generado.
      *
      * Similar a [shareGeneratedPdf], pero para archivos ODT.
      * Se usa para compartir atestados en formato editable (LibreOffice/Writer).
      *
      * Flujo:
      * 1. Crea un URI seguro usando FileProvider
      * 2. Construye un Intent ACTION_SEND con tipo MIME "application/vnd.oasis.opendocument.text"
      * 3. Crea un chooser para que el usuario seleccione la aplicación
      * 4. Agrega permisos READ_URI_PERMISSION
      * 5. Inicia el chooser
      *
      * Llamado desde:
      * - FirmasAtestadoScreen → botón "Compartir ODT"
      *
      * @param odtFile El archivo ODT a compartir
      * @return true si se abrió el chooser, false si hubo un error
      */
     private fun shareGeneratedOdt(odtFile: File): Boolean {
        return runCatching {
            val uri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                odtFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.oasis.opendocument.text"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, getString(R.string.atestado_signature_share_odt)).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(chooser)
            true
        }.recoverCatching {
            if (it is ActivityNotFoundException) {
                false
            } else {
                throw it
            }
        }.getOrDefault(false)
    }
}