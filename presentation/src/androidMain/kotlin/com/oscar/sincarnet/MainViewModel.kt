package com.oscar.sincarnet

import android.app.Application
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oscar.sincarnet.data.pdf.SIGNER_INVESTIGATED
import com.oscar.sincarnet.data.pdf.SIGNER_INSTRUCTOR
import com.oscar.sincarnet.data.pdf.SIGNER_SECOND_DRIVER
import com.oscar.sincarnet.data.pdf.SIGNER_SECRETARY
import com.oscar.sincarnet.data.pdf.generateAtestadoContinuousPdf
import com.oscar.sincarnet.data.pdf.generateCompleteAtestadoFrontMatterPdf
import com.oscar.sincarnet.data.pdf.getInstitutionalSealBitmap
import com.oscar.sincarnet.data.pdf.mergeAtestadoPdfs
import com.oscar.sincarnet.data.pdf.generateAtestadoOdt
import com.oscar.sincarnet.data.pdf.mapSignaturesForPdf
import com.oscar.sincarnet.data.toStorage
import com.oscar.sincarnet.data.repository.ActuantesStorage
import com.oscar.sincarnet.data.repository.AtestadoInicioStorage
import com.oscar.sincarnet.data.repository.BluetoothPrinterStorage
import com.oscar.sincarnet.data.repository.JuzgadoAtestadoStorage
import com.oscar.sincarnet.data.repository.ManifestacionStorage
import com.oscar.sincarnet.data.repository.OcurrenciaDelitStorage
import com.oscar.sincarnet.data.repository.PersonaInvestigadaStorage
import com.oscar.sincarnet.data.repository.SealUnitStorage
import com.oscar.sincarnet.data.repository.VehiculoStorage
import com.oscar.sincarnet.domain.model.ActuantesData
import com.oscar.sincarnet.domain.model.AtestadoInicioModalData
import com.oscar.sincarnet.domain.model.PersonaInvestigadaData
import com.oscar.sincarnet.domain.model.VehiculoData
import com.oscar.sincarnet.domain.model.isJuicioRapido
import com.oscar.sincarnet.presentation.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel principal de SinCarnet.
 *
 * Centraliza todo el estado que originalmente vivía en [MainActivity] y expone
 * un único [StateFlow] inmutable ([uiState]) junto con un flujo de efectos
 * secundarios ([sideEffects]) para acciones que requieren Android (intents, Toast).
 *
 * Mantiene la lógica de navegación, captura de datos, generación de PDF/ODT y
 * persistencia temporal vía los *Storage de la capa de datos.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    private val actuantesStorage = ActuantesStorage(appContext.toStorage("actuantes_storage"))
    private val personaStorage = PersonaInvestigadaStorage(appContext.toStorage("persona_investigada_storage"))
    private val vehiculoStorage = VehiculoStorage(appContext.toStorage("vehiculo_storage"))
    private val manifestacionStorage = ManifestacionStorage(appContext.toStorage("manifestacion_storage"))

    private val _uiState = MutableStateFlow(
        MainUiState(
            persona = PersonaInvestigadaState(
                sex = appContext.getString(R.string.person_data_sex_unknown)
            )
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _sideEffects = MutableSharedFlow<MainSideEffect>()
    val sideEffects: SharedFlow<MainSideEffect> = _sideEffects.asSharedFlow()

    init {
        viewModelScope.launch {
            applyActuantesData(actuantesStorage.loadCurrent())
            applyVehiculoData(vehiculoStorage.loadCurrent())
            val inicioData = AtestadoInicioStorage(appContext.toStorage("atestado_inicio_storage")).loadCurrent()
            _uiState.update {
                it.copy(
                    document = it.document.copy(
                        lastGeneratedPdfPath = inicioData.atestadoPdfPath,
                        completeAtestadoPdfPath = inicioData.atestadoCompletoPdfPath
                    ),
                    actuantes = it.actuantes.copy(
                        canRecover = actuantesStorage.hasRecoverableBackup(),
                        tipHistory = actuantesStorage.getTipHistory(),
                        unitHistory = actuantesStorage.getUnitHistory()
                    )
                )
            }
            delay(3000)
            _uiState.update { it.copy(showSplash = false) }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ESTADO DE UI GLOBAL
    // ═══════════════════════════════════════════════════════════════════════

    fun onShowAboutDialogChange(show: Boolean) {
        _uiState.update { it.copy(showAboutDialog = show) }
    }

    fun onDocumentScanStatusChange(status: DocumentScanStatus) {
        _uiState.update {
            it.copy(
                documentScanStatus = status,
                document = if (status == DocumentScanStatus.NO_COMPROBADO) {
                    it.document.copy(completeAtestadoPdfPath = "")
                } else {
                    it.document
                }
            )
        }
    }

    fun saveCompleteAtestadoData(jefaturaProvincial: String, numeroBoletin: String) {
        val storage = AtestadoInicioStorage(appContext.toStorage("atestado_inicio_storage"))
        storage.saveCurrent(
            storage.loadCurrent().copy(
                jefaturaProvincial = jefaturaProvincial.trim(),
                numeroBoletin = numeroBoletin.trim()
            )
        )
    }

    fun updateCompleteAtestadoAntecedentes(
        tieneAntecedentes: Boolean,
        antecedentesGuardiaCivil: Boolean,
        antecedentesSenalamientosNacionales: Boolean,
        antecedentesDgt: Boolean,
        antecedentesOtrosCuerpos: Boolean,
        requisitoriasJudiciales: Boolean
    ) {
        val storage = AtestadoInicioStorage(appContext.toStorage("atestado_inicio_storage"))
        storage.saveCurrent(
            storage.loadCurrent().copy(
                tieneAntecedentes = tieneAntecedentes,
                antecedentesGuardiaCivil = antecedentesGuardiaCivil,
                antecedentesSenalamientosNacionales = antecedentesSenalamientosNacionales,
                antecedentesDgt = antecedentesDgt,
                antecedentesOtrosCuerpos = antecedentesOtrosCuerpos,
                requisitoriasJudiciales = requisitoriasJudiciales
            )
        )
    }

    fun updateCompleteAtestadoSendMode(enviarPorLexnet: Boolean, modoEnvio: String) {
        val storage = AtestadoInicioStorage(appContext.toStorage("atestado_inicio_storage"))
        storage.saveCurrent(
            storage.loadCurrent().copy(
                enviarPorLexnet = enviarPorLexnet,
                modoEnvio = modoEnvio
            )
        )
    }

    fun updateCompleteAtestadoSealEnabled(enabled: Boolean) {
        SealUnitStorage(appContext.toStorage("seal_unit_storage")).saveSealEnabled(enabled)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FIRMAS
    // ═══════════════════════════════════════════════════════════════════════

    fun onSignatureSaved(key: String, bitmap: ImageBitmap) {
        _uiState.update {
            it.copy(
                signature = it.signature.copy(
                    signaturesBySigner = it.signature.signaturesBySigner + (key to bitmap),
                    currentSignerKey = ""
                )
            )
        }
    }

    fun onSignatureCancelled() {
        _uiState.update { it.copy(signature = it.signature.copy(currentSignerKey = "")) }
    }

    fun onCurrentSignerKeyChange(key: String) {
        _uiState.update { it.copy(signature = it.signature.copy(currentSignerKey = key)) }
    }

    fun onWantsToSignChange(value: Boolean) {
        _uiState.update { it.copy(signature = it.signature.copy(wantsToSign = value)) }
    }

    fun onHasSecondDriverChange(value: Boolean) {
        _uiState.update {
            it.copy(
                form = it.form.copy(hasSecondDriver = value),
                signature = if (!value) {
                    it.signature.copy(
                        signaturesBySigner = it.signature.signaturesBySigner - SIGNER_SECOND_DRIVER
                    )
                } else it.signature
            )
        }
    }

    fun onSecondDriverNameChange(value: String) {
        _uiState.update { it.copy(form = it.form.copy(secondDriverName = value)) }
    }

    fun onSecondDriverIdChange(value: String) {
        _uiState.update { it.copy(form = it.form.copy(secondDriverId = value)) }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FORMULARIO DE GENERACIÓN
    // ═══════════════════════════════════════════════════════════════════════

    fun updateForm(transform: (AtestadoFormState) -> AtestadoFormState) {
        _uiState.update { it.copy(form = transform(it.form)) }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ACTUANTES
    // ═══════════════════════════════════════════════════════════════════════

    fun updateActuantes(transform: (ActuantesState) -> ActuantesState) {
        _uiState.update { it.copy(actuantes = transform(it.actuantes)) }
    }

    fun applyActuantesData(data: ActuantesData) {
        _uiState.update {
            it.copy(
                actuantes = it.actuantes.copy(
                    instructorEmployment = data.instructorEmployment,
                    instructorTip = data.instructorTip,
                    instructorUnit = data.instructorUnit,
                    secretaryEmployment = data.secretaryEmployment,
                    secretaryTip = data.secretaryTip,
                    secretaryUnit = data.secretaryUnit,
                    sameUnit = data.sameUnit
                )
            )
        }
    }

    fun saveActuantes() {
        val state = _uiState.value.actuantes
        actuantesStorage.saveCurrent(
            ActuantesData(
                instructorEmployment = state.instructorEmployment,
                instructorTip = state.instructorTip,
                instructorUnit = state.instructorUnit,
                secretaryEmployment = state.secretaryEmployment,
                secretaryTip = state.secretaryTip,
                secretaryUnit = state.secretaryUnit,
                sameUnit = state.sameUnit
            )
        )
        actuantesStorage.addTipToHistory(state.instructorTip, true)
        actuantesStorage.addTipToHistory(state.secretaryTip, false)
        actuantesStorage.addUnitToHistory(state.instructorUnit, true)
        actuantesStorage.addUnitToHistory(state.secretaryUnit, false)
        _uiState.update {
            it.copy(
                actuantes = it.actuantes.copy(
                    canRecover = actuantesStorage.hasRecoverableBackup(),
                    tipHistory = actuantesStorage.getTipHistory(),
                    unitHistory = actuantesStorage.getUnitHistory(),
                    statusMessage = appContext.getString(R.string.atestado_acting_saved_message)
                )
            )
        }
    }

    fun deleteActuantesWithBackup() {
        actuantesStorage.deleteCurrentWithBackup()
        applyActuantesData(ActuantesData())
        _uiState.update {
            it.copy(
                actuantes = it.actuantes.copy(
                    canRecover = actuantesStorage.hasRecoverableBackup(),
                    statusMessage = appContext.getString(R.string.atestado_acting_deleted_message)
                )
            )
        }
    }

    fun recoverActuantes() {
        val recovered = actuantesStorage.recoverDeleted()
        if (recovered != null) {
            applyActuantesData(recovered)
            _uiState.update {
                it.copy(
                    actuantes = it.actuantes.copy(
                        statusMessage = appContext.getString(R.string.atestado_acting_recovered_message)
                    )
                )
            }
        }
        _uiState.update {
            it.copy(actuantes = it.actuantes.copy(canRecover = actuantesStorage.hasRecoverableBackup()))
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PERSONA INVESTIGADA
    // ═══════════════════════════════════════════════════════════════════════

    fun updatePersona(transform: (PersonaInvestigadaState) -> PersonaInvestigadaState) {
        _uiState.update { it.copy(persona = transform(it.persona)) }
    }

    fun applyPersonaData(data: PersonaInvestigadaData) {
        _uiState.update {
            it.copy(
                persona = it.persona.copy(
                    nationality = data.nationality,
                    sex = data.sex,
                    firstName = data.firstName,
                    lastName1 = data.lastName1,
                    lastName2 = data.lastName2,
                    address = data.address,
                    birthDate = data.birthDate,
                    birthPlace = data.birthPlace,
                    fatherName = data.fatherName,
                    motherName = data.motherName,
                    phone = data.phone,
                    email = data.email
                )
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // VEHÍCULO
    // ═══════════════════════════════════════════════════════════════════════

    fun updateVehiculo(transform: (VehiculoState) -> VehiculoState) {
        _uiState.update { it.copy(vehiculo = transform(it.vehiculo)) }
    }

    fun applyVehiculoData(data: VehiculoData) {
        _uiState.update {
            it.copy(
                vehiculo = it.vehiculo.copy(
                    brand = data.brand,
                    model = data.model,
                    plate = data.plate,
                    registrationDate = data.registrationDate,
                    nationality = data.nationality,
                    itvDate = data.itvDate,
                    insurer = data.insurer,
                    vehicleType = data.vehicleType,
                    clasePermiso = data.clasePermiso,
                    ownerIsOther = data.ownerIsOther,
                    ownerName = data.ownerName,
                    ownerLastNames = data.ownerLastNames,
                    ownerDni = data.ownerDni,
                    ownerAddress = data.ownerAddress,
                    ownerPhone = data.ownerPhone
                )
            )
        }
    }

    fun saveVehiculo() {
        val state = _uiState.value.vehiculo
        vehiculoStorage.saveCurrent(
            VehiculoData(
                brand = state.brand,
                model = state.model,
                plate = state.plate,
                registrationDate = state.registrationDate,
                nationality = state.nationality,
                itvDate = state.itvDate,
                insurer = state.insurer,
                vehicleType = state.vehicleType,
                clasePermiso = state.clasePermiso,
                ownerIsOther = state.ownerIsOther,
                ownerName = state.ownerName,
                ownerLastNames = state.ownerLastNames,
                ownerDni = state.ownerDni,
                ownerAddress = state.ownerAddress,
                ownerPhone = state.ownerPhone
            )
        )
    }

    fun deleteVehiculo() {
        vehiculoStorage.clearCurrent()
        applyVehiculoData(VehiculoData())
    }

    fun loadVehiculoCurrent() {
        applyVehiculoData(vehiculoStorage.loadCurrent())
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RESET Y GENERACIÓN DE DOCUMENTOS
    // ═══════════════════════════════════════════════════════════════════════

    fun resetAtestadoSession() {
        _uiState.update {
            it.copy(
                signature = it.signature.copy(
                    signaturesBySigner = emptyMap(),
                    currentSignerKey = "",
                    wantsToSign = true
                ),
                actuantes = it.actuantes.copy(statusMessage = ""),
                documentScanStatus = DocumentScanStatus.NO_COMPROBADO,
                persona = PersonaInvestigadaState(
                    sex = appContext.getString(R.string.person_data_sex_unknown)
                ),
                vehiculo = VehiculoState(),
                form = AtestadoFormState(),
                document = it.document.copy(lastGeneratedPdfPath = "")
            )
        }
    }

    fun generateAtestadoPdf(
        wantsToSign: Boolean,
        hasSecondDriver: Boolean,
        reason: String,
        articleNorm: String?,
        articleText: String
    ) {
        if (_uiState.value.document.isGeneratingAtestado) return
        _uiState.update {
            it.copy(
                form = it.form.copy(
                    generateReason = reason,
                    articleNorm = articleNorm ?: it.form.articleNorm,
                    articleText = articleText
                ),
                document = it.document.copy(isGeneratingAtestado = true)
            )
        }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    generateDocument(hasSecondDriver, wantsToSign)
                }
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        document = it.document.copy(
                            lastGeneratedPdfPath = result.file.absolutePath,
                            isGeneratingAtestado = false
                        )
                    )
                }
                val storage = AtestadoInicioStorage(appContext.toStorage("atestado_inicio_storage"))
                storage.saveCurrent(
                    storage.loadCurrent().copy(
                        atestadoPdfPath = result.file.absolutePath,
                        atestadoCompletoPdfPath = ""
                    )
                )
                _sideEffects.emit(MainSideEffect.OpenPdf(result.file))
            }.onFailure { e ->
                Log.e("MainViewModel", "Error generando PDF", e)
                _uiState.update { it.copy(document = it.document.copy(isGeneratingAtestado = false)) }
                _sideEffects.emit(MainSideEffect.ShowToast(R.string.atestado_pdf_generated_error))
            }
        }
    }

    fun generateAtestadoOdt() {
        val path = _uiState.value.document.lastGeneratedPdfPath
        if (path.isBlank()) {
            viewModelScope.launch {
                _sideEffects.emit(MainSideEffect.ShowToast(R.string.atestado_odt_not_found_error))
            }
            return
        }
        if (_uiState.value.document.isGeneratingOdt) return
        _uiState.update { it.copy(document = it.document.copy(isGeneratingOdt = true)) }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val state = _uiState.value
                    val courtData = JuzgadoAtestadoStorage(appContext.toStorage("juzgado_atestado_storage")).loadCurrent()
                    val personData = personaStorage.loadCurrent()
                    val ocurrenciaData = OcurrenciaDelitStorage(appContext.toStorage("ocurrencia_delit_storage")).loadCurrent()
                    val vehicleData = vehiculoStorage.loadCurrent()
                    val manifestacionData = manifestacionStorage.loadCurrent()
                    val signaturesToUse = state.signature.signaturesBySigner.toMutableMap().apply {
                        if (!state.form.hasSecondDriver) remove(SIGNER_SECOND_DRIVER)
                    }
                    val mappedSignatures = mapSignaturesForPdf(
                        signaturesBySigner = signaturesToUse,
                        investigatedWantsToSign = state.signature.wantsToSign,
                        investigatedNoSignText = appContext.getString(R.string.atestado_signature_no_desire)
                    )
                    val inicioModalData = AtestadoInicioStorage(appContext.toStorage("atestado_inicio_storage")).loadCurrent()
                    generateAtestadoOdt(
                        context = appContext,
                        courtData = courtData,
                        personData = personData,
                        ocurrenciaData = ocurrenciaData,
                        vehicleData = vehicleData,
                        manifestacionData = manifestacionData,
                        signatures = mappedSignatures,
                        investigatedNoSignText = appContext.getString(R.string.atestado_signature_no_desire),
                        instructorTip = state.actuantes.instructorTip,
                        secretaryTip = state.actuantes.secretaryTip,
                        instructorUnit = state.actuantes.instructorUnit,
                        inicioModalData = inicioModalData,
                        hasSecondDriver = state.form.hasSecondDriver
                    )
                }
            }.onSuccess { odtFile ->
                _uiState.update { it.copy(document = it.document.copy(isGeneratingOdt = false)) }
                _sideEffects.emit(MainSideEffect.ShareOdt(odtFile))
            }.onFailure { e ->
                Log.e("MainViewModel", "Error generando ODT", e)
                _uiState.update { it.copy(document = it.document.copy(isGeneratingOdt = false)) }
                _sideEffects.emit(MainSideEffect.ShowToast(R.string.atestado_odt_generate_error))
            }
        }
    }

    fun onSharePdfClick() {
        val path = _uiState.value.document.lastGeneratedPdfPath
        if (path.isBlank()) return
        val pdfFile = File(path)
        if (!pdfFile.exists()) return
        viewModelScope.launch {
            _sideEffects.emit(MainSideEffect.SharePdf(pdfFile))
        }
    }

    fun generateCompleteAtestado(sealUnitText: String, stampSeal: Boolean) {
        if (_uiState.value.document.isGeneratingCompleteAtestado) return
        SealUnitStorage(appContext.toStorage("seal_unit_storage")).apply {
            saveSealUnitText(sealUnitText)
            saveSealEnabled(stampSeal)
        }
        _uiState.update { it.copy(document = it.document.copy(isGeneratingCompleteAtestado = true)) }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val storage = AtestadoInicioStorage(appContext.toStorage("atestado_inicio_storage"))
                    val data = storage.loadCurrent()
                    val state = _uiState.value
                    val atestado = generateDocument(
                        hasSecondDriver = state.form.hasSecondDriver,
                        wantsToSign = state.signature.wantsToSign,
                        includeCompleteDiligencias = true,
                        stampInstitutionalSeal = stampSeal,
                        sealUnitText = sealUnitText
                    ).file
                    val frontMatter = generateCompleteAtestadoFrontMatterPdf(
                        context = appContext,
                        courtData = JuzgadoAtestadoStorage(appContext.toStorage("juzgado_atestado_storage")).loadCurrent(),
                        personData = personaStorage.loadCurrent(),
                        occurrenceData = OcurrenciaDelitStorage(appContext.toStorage("ocurrencia_delit_storage")).loadCurrent(),
                        inicioData = data,
                        instructorTip = state.actuantes.instructorTip,
                        secretaryTip = state.actuantes.secretaryTip,
                        instructorUnit = state.actuantes.instructorUnit,
                        includeAnnexCover = false,
                        sealUnitText = sealUnitText,
                        stampInstitutionalSeal = stampSeal
                    )
                    val annexCover = generateCompleteAtestadoFrontMatterPdf(
                        context = appContext,
                        courtData = JuzgadoAtestadoStorage(appContext.toStorage("juzgado_atestado_storage")).loadCurrent(),
                        personData = personaStorage.loadCurrent(),
                        occurrenceData = OcurrenciaDelitStorage(appContext.toStorage("ocurrencia_delit_storage")).loadCurrent(),
                        inicioData = data,
                        instructorTip = state.actuantes.instructorTip,
                        secretaryTip = state.actuantes.secretaryTip,
                        instructorUnit = state.actuantes.instructorUnit,
                        onlyAnnexCover = true,
                        sealUnitText = sealUnitText,
                        stampInstitutionalSeal = stampSeal
                    )
                    val scanned = data.documentosEscaneadosPdfPath
                        .takeIf { it.isNotBlank() }
                        ?.let(::File)
                    val output = File(
                        File(appContext.filesDir, "atestados").apply { mkdirs() },
                        "AtestadoCompleto${data.numeroBoletin}.pdf"
                    )
                    mergeAtestadoPdfs(
                        atestadoFile = atestado,
                        scannedDocumentsFile = scanned,
                        outputFile = output,
                        prefixFiles = listOf(frontMatter),
                        suffixFiles = listOf(annexCover),
                        sealBitmap = if (stampSeal) getInstitutionalSealBitmap(appContext, sealUnitText) else null
                    )
                    storage.saveCurrent(storage.loadCurrent().copy(atestadoCompletoPdfPath = output.absolutePath))
                    output
                }
            }.onSuccess { file ->
                _uiState.update {
                    it.copy(
                        document = it.document.copy(
                            completeAtestadoPdfPath = file.absolutePath,
                            isGeneratingCompleteAtestado = false
                        )
                    )
                }
                _sideEffects.emit(MainSideEffect.ShowCompleteAtestadoMessage(R.string.atestado_complete_generated))
            }.onFailure { error ->
                Log.e("MainViewModel", "Error generando atestado completo", error)
                _uiState.update {
                    it.copy(document = it.document.copy(isGeneratingCompleteAtestado = false))
                }
                _sideEffects.emit(MainSideEffect.ShowCompleteAtestadoMessage(R.string.atestado_complete_generate_error))
            }
        }
    }

    fun openCompleteAtestado() {
        val path = _uiState.value.document.completeAtestadoPdfPath
        val file = File(path)
        if (path.isBlank() || !file.isFile) {
            viewModelScope.launch { _sideEffects.emit(MainSideEffect.ShowCompleteAtestadoMessage(R.string.atestado_complete_not_found)) }
            return
        }
        viewModelScope.launch { _sideEffects.emit(MainSideEffect.OpenPdf(file)) }
    }

    fun shareCompleteAtestado() {
        val path = _uiState.value.document.completeAtestadoPdfPath
        val file = File(path)
        if (path.isBlank() || !file.isFile) {
            viewModelScope.launch { _sideEffects.emit(MainSideEffect.ShowCompleteAtestadoMessage(R.string.atestado_complete_not_found)) }
            return
        }
        viewModelScope.launch { _sideEffects.emit(MainSideEffect.SharePdf(file)) }
    }

    fun printCitacionIfPossible() {
        val mac = BluetoothPrinterStorage(appContext).getDefaultPrinter()?.mac
        if (mac.isNullOrBlank()) {
            throw IllegalStateException("No hay impresora configurada")
        }
        val courtData = JuzgadoAtestadoStorage(appContext.toStorage("juzgado_atestado_storage")).loadCurrent()
        com.oscar.sincarnet.data.print.DocumentPrinter.run {
            if (courtData.isJuicioRapido()) {
                imprimirCitacionJuicioRapido(appContext, mac)
            } else {
                imprimirCitacionJuicio(appContext, mac)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRIVADOS
    // ═══════════════════════════════════════════════════════════════════════

    private fun generateDocument(
        hasSecondDriver: Boolean,
        wantsToSign: Boolean,
        includeCompleteDiligencias: Boolean = false,
        stampInstitutionalSeal: Boolean = false,
        sealUnitText: String = ""
    ): com.oscar.sincarnet.data.pdf.AtestadoPdfResult {
        val state = _uiState.value
        val savedInicioData = AtestadoInicioStorage(appContext.toStorage("atestado_inicio_storage")).loadCurrent()
        val courtData = JuzgadoAtestadoStorage(appContext.toStorage("juzgado_atestado_storage")).loadCurrent()
        val personData = personaStorage.loadCurrent()
        val ocurrenciaData = OcurrenciaDelitStorage(appContext.toStorage("ocurrencia_delit_storage")).loadCurrent()
        val vehicleData = vehiculoStorage.loadCurrent()
        val manifestacionData = manifestacionStorage.loadCurrent()
        val signaturesToUse = state.signature.signaturesBySigner.toMutableMap().apply {
            if (!hasSecondDriver) remove(SIGNER_SECOND_DRIVER)
        }
        val mappedSignatures = mapSignaturesForPdf(
            signaturesBySigner = signaturesToUse,
            investigatedWantsToSign = wantsToSign,
            investigatedNoSignText = appContext.getString(R.string.atestado_signature_no_desire)
        )
        val inicioModalData = AtestadoInicioModalData(
            motivo = state.form.generateReason,
            norma = state.form.articleNorm,
            articulo = state.form.articleText,
            dgtNoRecord = state.form.dgtNoRecord,
            internationalNoRecord = state.form.internationalNoRecord,
            existsRecord = state.form.existsRecord,
            vicisitudesOption = state.form.vicisitudesOption,
            jefaturaProvincial = state.form.jefaturaProvincial.ifBlank { savedInicioData.jefaturaProvincial },
            numeroBoletin = state.form.numeroBoletin.ifBlank { savedInicioData.numeroBoletin },
            tieneAntecedentes = savedInicioData.tieneAntecedentes,
            antecedentesGuardiaCivil = savedInicioData.antecedentesGuardiaCivil,
            antecedentesSenalamientosNacionales = savedInicioData.antecedentesSenalamientosNacionales,
            antecedentesDgt = savedInicioData.antecedentesDgt,
            antecedentesOtrosCuerpos = savedInicioData.antecedentesOtrosCuerpos,
            requisitoriasJudiciales = savedInicioData.requisitoriasJudiciales,
            enviarPorLexnet = savedInicioData.enviarPorLexnet,
            modoEnvio = savedInicioData.modoEnvio,
            atestadoPdfPath = savedInicioData.atestadoPdfPath,
            documentosEscaneadosPdfPath = savedInicioData.documentosEscaneadosPdfPath,
            atestadoCompletoPdfPath = savedInicioData.atestadoCompletoPdfPath,
            tiempoPrivacion = state.form.tiempoPrivacion,
            juzgadoDecreta = state.form.juzgadoDecreta
        )
        AtestadoInicioStorage(appContext.toStorage("atestado_inicio_storage")).saveCurrent(inicioModalData)
        return generateAtestadoContinuousPdf(
            context = appContext,
            courtData = courtData,
            personData = personData,
            ocurrenciaData = ocurrenciaData,
            vehicleData = vehicleData,
            manifestacionData = manifestacionData,
            signatures = mappedSignatures,
            investigatedNoSignText = appContext.getString(R.string.atestado_signature_no_desire),
            instructorTip = state.actuantes.instructorTip,
            secretaryTip = state.actuantes.secretaryTip,
            instructorUnit = state.actuantes.instructorUnit,
            inicioModalData = inicioModalData,
            hasSecondDriver = hasSecondDriver,
            includeCompleteDiligencias = includeCompleteDiligencias,
            stampInstitutionalSeal = stampInstitutionalSeal,
            sealUnitText = sealUnitText
        )
    }
}

/**
 * Efectos secundarios que requieren contexto de Activity para ejecutarse
 * (abrir intents, mostrar Toasts, etc.).
 */
sealed class MainSideEffect {
    data class OpenPdf(val file: File) : MainSideEffect()
    data class SharePdf(val file: File) : MainSideEffect()
    data class ShareOdt(val file: File) : MainSideEffect()
    data class ShowToast(val messageRes: Int) : MainSideEffect()
    data class ShowCompleteAtestadoMessage(val messageRes: Int) : MainSideEffect()
}
