package com.oscar.sincarnet

import android.nfc.Tag
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oscar.sincarnet.ui.theme.SinCarnetTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val NATIONALITY_FALLBACK = listOf("España")
private val PERSON_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
private const val PERSONA_NFC_LOG_TAG = "PersonaNfc"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatosPersonaInvestigadaScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onRightsClick: () -> Unit = {},
    onManifestacionClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val storage = PersonaInvestigadaStorage(context)
    val initialData = storage.loadCurrent()

    var nationality by rememberSaveable { mutableStateOf(initialData.nationality) }
    var sex by rememberSaveable { mutableStateOf(initialData.sex) }
    var firstName by rememberSaveable { mutableStateOf(initialData.firstName) }
    var lastName1 by rememberSaveable { mutableStateOf(initialData.lastName1) }
    var lastName2 by rememberSaveable { mutableStateOf(initialData.lastName2) }
    var documentIdentification by rememberSaveable { mutableStateOf(initialData.documentIdentification) }
    var address by rememberSaveable { mutableStateOf(initialData.address) }
    var birthDate by rememberSaveable { mutableStateOf(initialData.birthDate) }
    var birthPlace by rememberSaveable { mutableStateOf(initialData.birthPlace) }
    var birthProvince by rememberSaveable { mutableStateOf(initialData.birthProvince) }
    var fatherName by rememberSaveable { mutableStateOf(initialData.fatherName) }
    var motherName by rememberSaveable { mutableStateOf(initialData.motherName) }
    var residencePopulation by rememberSaveable { mutableStateOf(initialData.residencePopulation) }
    var residenceProvince by rememberSaveable { mutableStateOf(initialData.residenceProvince) }
    var phone by rememberSaveable { mutableStateOf(initialData.phone) }
    var email by rememberSaveable { mutableStateOf(initialData.email) }
    var rightToRemainSilentInformed by rememberSaveable { mutableStateOf(initialData.rightToRemainSilentInformed) }
    var waivesLegalAssistance by rememberSaveable { mutableStateOf(initialData.waivesLegalAssistance) }
    var requestsPrivateLawyer by rememberSaveable { mutableStateOf(initialData.requestsPrivateLawyer) }
    var requestsDutyLawyer by rememberSaveable { mutableStateOf(initialData.requestsDutyLawyer) }
    var accessesEssentialProceedings by rememberSaveable { mutableStateOf(initialData.accessesEssentialProceedings) }
    var needsInterpreter by rememberSaveable { mutableStateOf(initialData.needsInterpreter) }

    var nationalityOptions by remember { mutableStateOf(NATIONALITY_FALLBACK) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var showSaveConfirmation by rememberSaveable { mutableStateOf(false) }
    var showRequiredFieldsError by rememberSaveable { mutableStateOf(false) }
    var requiredFieldsErrorText by rememberSaveable { mutableStateOf("") }
    var showBirthDatePicker by rememberSaveable { mutableStateOf(false) }
    var rightsDialogStep by rememberSaveable { mutableStateOf<Int?>(null) }
    var showCanDialog by rememberSaveable { mutableStateOf(false) }
    var canCode by rememberSaveable { mutableStateOf("") }
    var canCodeError by rememberSaveable { mutableStateOf("") }
    var nfcReadError by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingNfcData by remember { mutableStateOf<NfcDniPersonData?>(null) }
    var showNfcDataDialog by rememberSaveable { mutableStateOf(false) }
    var showNfcScanDialog by rememberSaveable { mutableStateOf(false) }
    var isReadingNfc by rememberSaveable { mutableStateOf(false) }
    var waitingForNfcTag by rememberSaveable { mutableStateOf(false) }
    var pendingCanForNfc by rememberSaveable { mutableStateOf("") }
    var pendingAttemptId by rememberSaveable { mutableStateOf("") }
    var nfcScanStartedAtMillis by rememberSaveable { mutableStateOf(0L) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (isInPreview) { nationalityOptions = NATIONALITY_FALLBACK; return@LaunchedEffect }
        nationalityOptions = runCatching {
            withContext(Dispatchers.IO) { loadNationalitiesFromDatabase(context) }
        }.getOrElse { NATIONALITY_FALLBACK }
    }

    val sexOptions = listOf(
        stringResource(R.string.person_data_sex_male),
        stringResource(R.string.person_data_sex_female),
        stringResource(R.string.person_data_sex_unknown)
    )

    // Labels para la validación
    val nationalityLabel = stringResource(R.string.person_data_nationality)
    val sexLabel = stringResource(R.string.person_data_sex)
    val nameLabel = stringResource(R.string.person_data_name)
    val lastName1Label = stringResource(R.string.person_data_last_name_1)
    val lastName2Label = stringResource(R.string.person_data_last_name_2)
    val documentIdentificationLabel = stringResource(R.string.person_data_document_identification)
    val addressLabel = stringResource(R.string.person_data_address)
    val birthDateLabel = stringResource(R.string.person_data_birth_date)
    val birthPlaceLabel = stringResource(R.string.person_data_birth_place)
    val fatherNameLabel = stringResource(R.string.person_data_father_name)
    val motherNameLabel = stringResource(R.string.person_data_mother_name)
    val phoneLabel = stringResource(R.string.person_data_phone)
    val emailLabel = stringResource(R.string.person_data_email)
    val nfcNoTagMessage = stringResource(R.string.nfc_no_tag_message)
    val nfcMissingLibraryMessage = stringResource(R.string.nfc_missing_library_message)
    val canInvalidMessage = stringResource(R.string.can_invalid_message)
    val nfcReadErrorTitle = stringResource(R.string.nfc_read_error_title)
    val nfcScanDialogTitle = stringResource(R.string.nfc_scan_dialog_title)
    val nfcWaitingTagMessage = stringResource(R.string.nfc_waiting_tag_message)
    val nfcReadingProgressTitle = stringResource(R.string.nfc_reading_progress_title)
    val nfcReadingProgressMessage = stringResource(R.string.nfc_reading_progress_message)
    val sexMaleLabel = stringResource(R.string.person_data_sex_male)
    val sexFemaleLabel = stringResource(R.string.person_data_sex_female)
    val sexUnknownLabel = stringResource(R.string.person_data_sex_unknown)
    val canOpenManifestacion = listOf(
        rightToRemainSilentInformed,
        waivesLegalAssistance,
        requestsPrivateLawyer,
        requestsDutyLawyer,
        accessesEssentialProceedings,
        needsInterpreter
    ).any { it == true }

    fun startNfcRead(attemptId: String, can: String, tag: Tag) {
        val uid = tag.id?.joinToString(":") { "%02X".format(it) }
        Log.i(PERSONA_NFC_LOG_TAG, "[$attemptId] Inicio lectura con tag uid=${uid ?: "<null>"} techs=${tag.techList.joinToString()}")
        waitingForNfcTag = false
        showNfcScanDialog = false
        canCodeError = ""
        isReadingNfc = true
        scope.launch {
            Log.d(PERSONA_NFC_LOG_TAG, "[$attemptId] Invocando NfcDniReader.read(...) en IO")
            val result = withContext(Dispatchers.IO) { NfcDniReader.read(can, tag) }
            isReadingNfc = false
            result.onSuccess {
                Log.i(PERSONA_NFC_LOG_TAG, "[$attemptId] Lectura NFC OK. nombre='${it.firstName.take(24)}' doc='${it.documentNumber.take(12)}'")
                pendingNfcData = it
                showNfcDataDialog = true
            }.onFailure { throwable ->
                val rootCause = generateSequence(throwable) { it.cause }.last()
                Log.e(
                    PERSONA_NFC_LOG_TAG,
                    "[$attemptId] Lectura NFC fallida: type=${throwable.javaClass.name}, message=${throwable.message}, rootType=${rootCause.javaClass.name}, rootMessage=${rootCause.message}",
                    throwable
                )
                nfcReadError = if (throwable is ClassNotFoundException) {
                    nfcMissingLibraryMessage
                } else {
                    throwable.message ?: nfcReadErrorTitle
                }
                Log.w(PERSONA_NFC_LOG_TAG, "[$attemptId] Mensaje mostrado al usuario: '$nfcReadError'")
            }
        }
    }

    LaunchedEffect(waitingForNfcTag, pendingCanForNfc, pendingAttemptId) {
        if (!waitingForNfcTag) return@LaunchedEffect
        Log.i(PERSONA_NFC_LOG_TAG, "[${pendingAttemptId}] Esperando tag NFC nuevo para CAN válido desde ts=$nfcScanStartedAtMillis...")
        while (waitingForNfcTag) {
            val debugInfo = NfcTagRepository.debugInfo()
            val candidateTag = NfcTagRepository.getLatest()
            if (candidateTag != null && debugInfo.capturedAtMillis >= nfcScanStartedAtMillis) {
                Log.i(PERSONA_NFC_LOG_TAG, "[${pendingAttemptId}] Tag fresco detectado uid=${debugInfo.uid} capturedAt=${debugInfo.capturedAtMillis}")
                startNfcRead(pendingAttemptId, pendingCanForNfc, candidateTag)
                break
            }
            delay(300)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = stringResource(R.string.person_data_title), style = MaterialTheme.typography.titleMedium)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    DropdownField(
                        label = stringResource(R.string.person_data_nationality),
                        value = nationality,
                        options = nationalityOptions,
                        onValueSelected = { nationality = it },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showCanDialog = true },
                        modifier = Modifier.size(65.dp).padding(top = 8.dp).align(Alignment.Top)
                    ) {
                        AssetImage(
                            assetPath = "icons/rfid.png",
                            contentDescription = stringResource(R.string.can_button_content_description),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                DropdownField(label = stringResource(R.string.person_data_sex), value = sex, options = sexOptions, onValueSelected = { sex = it })

                OutlinedTextField(value = firstName, onValueChange = { firstName = it.uppercase() }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.person_data_name)) })
                OutlinedTextField(value = lastName1, onValueChange = { lastName1 = it.uppercase() }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.person_data_last_name_1)) })
                OutlinedTextField(value = lastName2, onValueChange = { lastName2 = it.uppercase() }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.person_data_last_name_2)) })
                OutlinedTextField(value = documentIdentification, onValueChange = { documentIdentification = it.uppercase() }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.person_data_document_identification)) })
                OutlinedTextField(value = address, onValueChange = { address = it }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.person_data_address)) })

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    OutlinedTextField(
                        value = birthDate, onValueChange = {}, modifier = Modifier.weight(1f),
                        readOnly = true, singleLine = true,
                        label = { Text(stringResource(R.string.person_data_birth_date)) },
                        supportingText = { Text(stringResource(R.string.person_data_birth_date_hint)) }
                    )
                    IconButton(onClick = { showBirthDatePicker = true }, modifier = Modifier.size(65.dp).padding(top = 8.dp).align(Alignment.Top)) {
                        AssetImage(assetPath = "icons/calendar.png", contentDescription = stringResource(R.string.person_data_birth_date_select_action), modifier = Modifier.fillMaxSize())
                    }
                }

                OutlinedTextField(value = birthPlace, onValueChange = { birthPlace = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.person_data_birth_place)) })
                OutlinedTextField(value = birthProvince, onValueChange = { birthProvince = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.person_data_birth_province)) })
                OutlinedTextField(value = fatherName, onValueChange = { fatherName = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.person_data_father_name)) })
                OutlinedTextField(value = motherName, onValueChange = { motherName = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.person_data_mother_name)) })
                OutlinedTextField(value = residencePopulation, onValueChange = { residencePopulation = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.person_data_residence_population)) })
                OutlinedTextField(value = residenceProvince, onValueChange = { residenceProvince = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.person_data_residence_province)) })
                OutlinedTextField(value = phone, onValueChange = { phone = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), label = { Text(stringResource(R.string.person_data_phone)) })
                OutlinedTextField(value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), label = { Text(stringResource(R.string.person_data_email)) })
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val requiredFields = listOf(
                        nationalityLabel to nationality, sexLabel to sex,
                        nameLabel to firstName, lastName1Label to lastName1,
                        lastName2Label to lastName2, documentIdentificationLabel to documentIdentification,
                        addressLabel to address,
                        birthDateLabel to birthDate, birthPlaceLabel to birthPlace,
                        fatherNameLabel to fatherName, motherNameLabel to motherName,
                        phoneLabel to phone, emailLabel to email
                    )
                    val missing = requiredFields.filter { it.second.trim().isEmpty() }.map { it.first }
                    if (missing.isNotEmpty()) {
                        requiredFieldsErrorText = missing.joinToString(", ")
                        showRequiredFieldsError = true
                    } else {
                        storage.saveCurrent(PersonaInvestigadaData(
                            nationality = nationality, sex = sex,
                            firstName = firstName, lastName1 = lastName1, lastName2 = lastName2,
                            documentIdentification = documentIdentification,
                            address = address, birthDate = birthDate, birthPlace = birthPlace, birthProvince = birthProvince,
                            fatherName = fatherName, motherName = motherName,
                            residencePopulation = residencePopulation, residenceProvince = residenceProvince,
                            phone = phone, email = email,
                            rightToRemainSilentInformed = rightToRemainSilentInformed,
                            waivesLegalAssistance = waivesLegalAssistance,
                            requestsPrivateLawyer = requestsPrivateLawyer,
                            requestsDutyLawyer = requestsDutyLawyer,
                            accessesEssentialProceedings = accessesEssentialProceedings,
                            needsInterpreter = needsInterpreter
                        ))
                        showSaveConfirmation = true
                    }
                },
                modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) { Text(stringResource(R.string.person_data_save)) }

            Button(
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
            ) { Text(stringResource(R.string.person_data_delete)) }
        }

        Button(
            onClick = {
                onRightsClick()
                rightsDialogStep = 1
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Text(stringResource(R.string.person_data_rights))
        }

        if (canOpenManifestacion) {
            Button(
                onClick = onManifestacionClick,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Text(stringResource(R.string.manifestacion_button))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackIconButton(onClick = onBackClick)
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.person_data_delete_confirm_title)) },
            text = { Text(stringResource(R.string.person_data_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    storage.clearCurrent()
                    nationality = "España"
                    sex = context.getString(R.string.person_data_sex_unknown)
                    firstName = ""; lastName1 = ""; lastName2 = ""
                    documentIdentification = ""
                    address = ""; birthDate = ""; birthPlace = ""
                    birthProvince = ""
                    fatherName = ""; motherName = ""; phone = ""; email = ""
                    residencePopulation = ""; residenceProvince = ""
                    rightToRemainSilentInformed = null
                    waivesLegalAssistance = null
                    requestsPrivateLawyer = null
                    requestsDutyLawyer = null
                    accessesEssentialProceedings = null
                    needsInterpreter = null
                }) { Text(stringResource(R.string.person_data_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text(stringResource(R.string.no_option)) }
            }
        )
    }

    if (showSaveConfirmation) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmation = false },
            title = { Text(stringResource(R.string.person_data_save_confirm_title)) },
            text = { Text(stringResource(R.string.person_data_save_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { showSaveConfirmation = false; onBackClick() }) {
                    Text(stringResource(R.string.close_action))
                }
            }
        )
    }

    if (showRequiredFieldsError) {
        AlertDialog(
            onDismissRequest = { showRequiredFieldsError = false },
            title = { Text(stringResource(R.string.person_data_required_fields_title)) },
            text = { Text(stringResource(R.string.person_data_required_fields_message, requiredFieldsErrorText)) },
            confirmButton = {
                TextButton(onClick = { showRequiredFieldsError = false }) { Text(stringResource(R.string.accept_action)) }
            }
        )
    }

    if (showBirthDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = birthDate.toDateMillisOrNull())
        DatePickerDialog(
            onDismissRequest = { showBirthDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.toFormattedBirthDate()?.let { birthDate = it }
                    showBirthDatePicker = false
                }) { Text(stringResource(R.string.person_data_birth_date_select_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showBirthDatePicker = false }) { Text(stringResource(R.string.no_option)) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showCanDialog) {
        AlertDialog(
            onDismissRequest = { showCanDialog = false },
            title = { Text(stringResource(R.string.can_dialog_title)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.can_dialog_message))
                    AssetImage(
                        assetPath = "icons/biometriqpass.png",
                        contentDescription = stringResource(R.string.can_dialog_biometric_description),
                        modifier = Modifier.size(80.dp)
                    )
                    OutlinedTextField(
                        value = canCode,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) canCode = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.can_code_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    if (canCodeError.isNotBlank()) {
                        Text(
                            text = canCodeError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (waitingForNfcTag) {
                        Text(
                            text = nfcWaitingTagMessage,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val attemptId = System.currentTimeMillis().toString()
                    Log.i(PERSONA_NFC_LOG_TAG, "[$attemptId] Pulsado Aceptar CAN")

                    if (!Regex("^\\d{6}$").matches(canCode)) {
                        Log.w(PERSONA_NFC_LOG_TAG, "[$attemptId] CAN inválido: '${canCode.take(10)}' (len=${canCode.length})")
                        canCodeError = canInvalidMessage
                        return@TextButton
                    }

                    Log.i(PERSONA_NFC_LOG_TAG, "[$attemptId] CAN válido (${canCode.length} dígitos)")

                    val tagDebug = NfcTagRepository.debugInfo()
                    Log.i(
                        PERSONA_NFC_LOG_TAG,
                        "[$attemptId] Estado repositorio NFC: hasTag=${tagDebug.hasTag}, uid='${tagDebug.uid}', ageMs=${tagDebug.ageMs}, techs='${tagDebug.techList}'"
                    )

                    val currentTag = NfcTagRepository.getLatest()
                    if (currentTag == null) {
                        Log.w(PERSONA_NFC_LOG_TAG, "[$attemptId] No hay Tag NFC disponible al aceptar CAN")
                    }

                    pendingCanForNfc = canCode
                    pendingAttemptId = attemptId
                    nfcScanStartedAtMillis = System.currentTimeMillis()
                    NfcTagRepository.clear()
                    waitingForNfcTag = true
                    showCanDialog = false
                    showNfcScanDialog = true
                }, enabled = !isReadingNfc) {
                    Text(stringResource(R.string.can_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    waitingForNfcTag = false
                    pendingCanForNfc = ""
                    pendingAttemptId = ""
                    showCanDialog = false
                    canCode = ""
                    canCodeError = ""
                }) {
                    Text(stringResource(R.string.cancel_action))
                }
            }
        )
    }

    if (showNfcScanDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(nfcScanDialogTitle) },
            text = { Text(nfcWaitingTagMessage) },
            confirmButton = {
                TextButton(onClick = {
                    waitingForNfcTag = false
                    showNfcScanDialog = false
                    pendingCanForNfc = ""
                    pendingAttemptId = ""
                    nfcScanStartedAtMillis = 0L
                }) {
                    Text(stringResource(R.string.cancel_action))
                }
            }
        )
    }

    // Modal de progreso: se muestra mientras se lee el chip NFC (puede tardar 2-5s)
    if (isReadingNfc) {
        AlertDialog(
            onDismissRequest = { /* no cancelable mientras lee */ },
            title = { Text(nfcReadingProgressTitle) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = nfcReadingProgressMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {}
        )
    }


    if (showNfcDataDialog && pendingNfcData != null) {
        val data = pendingNfcData!!
        val previewBirthDate = data.birthDateAammdd.toDisplayBirthDateFromAammddOrEmpty()
        AlertDialog(
            onDismissRequest = { showNfcDataDialog = false },
            title = { Text(stringResource(R.string.nfc_data_modal_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(stringResource(R.string.nfc_field_name, data.firstName))
                    Text(stringResource(R.string.nfc_field_lastname1, data.lastName1))
                    Text(stringResource(R.string.nfc_field_lastname2, data.lastName2))
                    Text(stringResource(R.string.nfc_field_nif, data.optionalData))
                    Text(stringResource(R.string.nfc_field_birth_date, previewBirthDate))
                    Text(stringResource(R.string.nfc_field_birth_place, data.birthPlace))
                    Text(stringResource(R.string.nfc_field_birth_province, data.birthProvince))
                    Text(stringResource(R.string.nfc_field_father, data.fatherName))
                    Text(stringResource(R.string.nfc_field_mother, data.motherName))
                    Text(stringResource(R.string.nfc_field_address, data.residenceAddress))
                    Text(stringResource(R.string.nfc_field_population, data.residencePopulation))
                    Text(stringResource(R.string.nfc_field_province, data.residenceProvince))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val mappedSex = normalizeSexFromDg1(
                        raw = data.sex,
                        maleLabel = sexMaleLabel,
                        femaleLabel = sexFemaleLabel,
                        unknownLabel = sexUnknownLabel
                    )
                    val mappedBirthDate = data.birthDateAammdd.toDisplayBirthDateFromAammddOrEmpty()

                    firstName = data.firstName.uppercase()
                    lastName1 = data.lastName1.uppercase()
                    lastName2 = data.lastName2.uppercase()
                    // optionalData contiene el NIF/NIE del titular (dato opcional MRZ zona 1)
                    if (data.optionalData.isNotBlank()) {
                        documentIdentification = data.optionalData.uppercase()
                    }
                    fatherName = data.fatherName
                    motherName = data.motherName
                    birthDate = mappedBirthDate
                    birthPlace = data.birthPlace
                    birthProvince = data.birthProvince
                    address = data.residenceAddress
                    residencePopulation = data.residencePopulation
                    residenceProvince = data.residenceProvince
                    if (data.nationality.isNotBlank()) {
                        nationality = data.nationality
                    }
                    sex = mappedSex

                    storage.saveCurrent(
                        PersonaInvestigadaData(
                            nationality = nationality,
                            sex = sex,
                            firstName = firstName,
                            lastName1 = lastName1,
                            lastName2 = lastName2,
                            documentIdentification = documentIdentification,
                            address = address,
                            birthDate = birthDate,
                            birthPlace = birthPlace,
                            birthProvince = birthProvince,
                            fatherName = fatherName,
                            motherName = motherName,
                            residencePopulation = residencePopulation,
                            residenceProvince = residenceProvince,
                            phone = phone,
                            email = email,
                            rightToRemainSilentInformed = rightToRemainSilentInformed,
                            waivesLegalAssistance = waivesLegalAssistance,
                            requestsPrivateLawyer = requestsPrivateLawyer,
                            requestsDutyLawyer = requestsDutyLawyer,
                            accessesEssentialProceedings = accessesEssentialProceedings,
                            needsInterpreter = needsInterpreter
                        )
                    )

                    Log.i(PERSONA_NFC_LOG_TAG, "Datos NFC aplicados y guardados en storage")

                    showNfcDataDialog = false
                }) {
                    Text(stringResource(R.string.nfc_apply_data_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNfcDataDialog = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            }
        )
    }

    if (nfcReadError != null) {
        AlertDialog(
            onDismissRequest = { nfcReadError = null },
            title = { Text(stringResource(R.string.nfc_read_error_title)) },
            text = { Text(nfcReadError.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { nfcReadError = null }) {
                    Text(stringResource(R.string.accept_action))
                }
            }
        )
    }

    if (rightsDialogStep == 1) {        AlertDialog(
            onDismissRequest = { rightsDialogStep = null },
            title = { Text(stringResource(R.string.person_data_rights_dialog_title)) },
            text = { Text(stringResource(R.string.person_data_rights_intro_message)) },
            confirmButton = {
                TextButton(onClick = { rightsDialogStep = 2 }) {
                    Text(stringResource(R.string.continue_action))
                }
            }
        )
    }

    if (rightsDialogStep == 2) {
        AlertDialog(
            onDismissRequest = { rightsDialogStep = null },
            title = { Text(stringResource(R.string.person_data_rights_dialog_title)) },
            text = {
                CompactHorizontalYesNoQuestionBlock(
                    questionText = stringResource(R.string.person_data_right_silence_message),
                    selectedValue = rightToRemainSilentInformed,
                    onValueChange = { value -> rightToRemainSilentInformed = value }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        storage.saveRightsSelections(
                            rightToRemainSilentInformed = rightToRemainSilentInformed,
                            waivesLegalAssistance = waivesLegalAssistance,
                            requestsPrivateLawyer = requestsPrivateLawyer,
                            requestsDutyLawyer = requestsDutyLawyer,
                            accessesEssentialProceedings = accessesEssentialProceedings,
                            needsInterpreter = needsInterpreter
                        )
                        rightsDialogStep = 3
                    },
                    enabled = rightToRemainSilentInformed != null
                ) {
                    Text(stringResource(R.string.continue_action))
                }
            }
        )
    }

    if (rightsDialogStep == 3) {
        AlertDialog(
            onDismissRequest = { rightsDialogStep = null },
            title = { Text(stringResource(R.string.person_data_rights_dialog_title)) },
            text = { Text(stringResource(R.string.person_data_right_non_self_incrimination_message)) },
            confirmButton = {
                TextButton(onClick = { rightsDialogStep = 4 }) {
                    Text(stringResource(R.string.continue_action))
                }
            }
        )
    }

    if (rightsDialogStep == 4) {
        AlertDialog(
            onDismissRequest = { rightsDialogStep = null },
            title = { Text(stringResource(R.string.person_data_rights_dialog_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.person_data_right_lawyer_message))
                    CompactHorizontalYesNoQuestionBlock(
                        questionText = stringResource(R.string.person_data_right_lawyer_waive_question),
                        selectedValue = waivesLegalAssistance,
                        onValueChange = { waivesLegalAssistance = it }
                    )
                    CompactHorizontalYesNoQuestionBlock(
                        questionText = stringResource(R.string.person_data_right_lawyer_private_question),
                        selectedValue = requestsPrivateLawyer,
                        onValueChange = { requestsPrivateLawyer = it }
                    )
                    CompactHorizontalYesNoQuestionBlock(
                        questionText = stringResource(R.string.person_data_right_lawyer_duty_question),
                        selectedValue = requestsDutyLawyer,
                        onValueChange = { requestsDutyLawyer = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        storage.saveRightsSelections(
                            rightToRemainSilentInformed = rightToRemainSilentInformed,
                            waivesLegalAssistance = waivesLegalAssistance,
                            requestsPrivateLawyer = requestsPrivateLawyer,
                            requestsDutyLawyer = requestsDutyLawyer,
                            accessesEssentialProceedings = accessesEssentialProceedings,
                            needsInterpreter = needsInterpreter
                        )
                        rightsDialogStep = 5
                    },
                    enabled = waivesLegalAssistance != null && requestsPrivateLawyer != null && requestsDutyLawyer != null
                ) {
                    Text(stringResource(R.string.continue_action))
                }
            }
        )
    }

    if (rightsDialogStep == 5) {
        AlertDialog(
            onDismissRequest = { rightsDialogStep = null },
            title = { Text(stringResource(R.string.person_data_rights_dialog_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    CompactHorizontalYesNoQuestionBlock(
                        questionText = stringResource(R.string.person_data_right_essential_elements_message),
                        selectedValue = accessesEssentialProceedings,
                        onValueChange = { accessesEssentialProceedings = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        storage.saveRightsSelections(
                            rightToRemainSilentInformed = rightToRemainSilentInformed,
                            waivesLegalAssistance = waivesLegalAssistance,
                            requestsPrivateLawyer = requestsPrivateLawyer,
                            requestsDutyLawyer = requestsDutyLawyer,
                            accessesEssentialProceedings = accessesEssentialProceedings,
                            needsInterpreter = needsInterpreter
                        )
                        rightsDialogStep = 6
                    },
                    enabled = accessesEssentialProceedings != null
                ) {
                    Text(stringResource(R.string.continue_action))
                }
            }
        )
    }

    if (rightsDialogStep == 6) {
        AlertDialog(
            onDismissRequest = { rightsDialogStep = null },
            title = { Text(stringResource(R.string.person_data_rights_dialog_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    CompactHorizontalYesNoQuestionBlock(
                        questionText = stringResource(R.string.person_data_right_interpreter_message),
                        selectedValue = needsInterpreter,
                        onValueChange = { needsInterpreter = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        storage.saveRightsSelections(
                            rightToRemainSilentInformed = rightToRemainSilentInformed,
                            waivesLegalAssistance = waivesLegalAssistance,
                            requestsPrivateLawyer = requestsPrivateLawyer,
                            requestsDutyLawyer = requestsDutyLawyer,
                            accessesEssentialProceedings = accessesEssentialProceedings,
                            needsInterpreter = needsInterpreter
                        )
                        rightsDialogStep = 7
                    },
                    enabled = needsInterpreter != null
                ) {
                    Text(stringResource(R.string.continue_action))
                }
            }
        )
    }

    if (rightsDialogStep == 7) {
        AlertDialog(
            onDismissRequest = { rightsDialogStep = null },
            title = { Text(stringResource(R.string.person_data_rights_dialog_title)) },
            text = { Text(stringResource(R.string.person_data_right_free_legal_aid_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        storage.saveRightsSelections(
                            rightToRemainSilentInformed = rightToRemainSilentInformed,
                            waivesLegalAssistance = waivesLegalAssistance,
                            requestsPrivateLawyer = requestsPrivateLawyer,
                            requestsDutyLawyer = requestsDutyLawyer,
                            accessesEssentialProceedings = accessesEssentialProceedings,
                            needsInterpreter = needsInterpreter
                        )
                        rightsDialogStep = null
                    }
                ) {
                    Text(stringResource(R.string.continue_action))
                }
            }
        )
    }
}

private fun Long.toFormattedBirthDate(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().format(PERSON_DATE_FORMATTER)

private fun String.toDateMillisOrNull(): Long? = runCatching {
    LocalDate.parse(this, PERSON_DATE_FORMATTER).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}.getOrNull()

@Composable
private fun CompactHorizontalYesNoQuestionBlock(
    questionText: String,
    selectedValue: Boolean?,
    onValueChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = questionText)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable { onValueChange(true) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selectedValue == true, onClick = { onValueChange(true) })
                Text(text = stringResource(R.string.yes_option))
            }

            Row(
                modifier = Modifier.clickable { onValueChange(false) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selectedValue == false, onClick = { onValueChange(false) })
                Text(text = stringResource(R.string.no_option))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(label: String, value: String, options: List<String>, onValueSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = value, onValueChange = {},
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            readOnly = true, singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onValueSelected(option); expanded = false })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DatosPersonaInvestigadaScreenPreview() {
    SinCarnetTheme { DatosPersonaInvestigadaScreen() }
}
