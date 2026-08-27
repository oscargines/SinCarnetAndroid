package com.oscar.sincarnet

import android.nfc.Tag
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oscar.sincarnet.data.datasource.nfc.NfcDniReader
import com.oscar.sincarnet.data.datasource.nfc.NfcTagRepository
import com.oscar.sincarnet.domain.model.NfcDniPersonData
import com.oscar.sincarnet.presentation.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val NFC_LOG_TAG = "NfcReadingHelper"

class NfcReadingHelper(
    private val showCanDialogState: MutableState<Boolean>,
    private val canCodeState: MutableState<String>,
    private val canCodeErrorState: MutableState<String>,
    private val nfcReadErrorState: MutableState<String?>,
    private val pendingNfcDataState: MutableState<NfcDniPersonData?>,
    private val showNfcScanDialogState: MutableState<Boolean>,
    private val isReadingNfcState: MutableState<Boolean>,
    private val waitingForNfcTagState: MutableState<Boolean>,
    private val pendingCanForNfcState: MutableState<String>,
    private val pendingAttemptIdState: MutableState<String>,
    private val nfcScanStartedAtMillisState: MutableState<Long>,
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val canInvalidMessage: String,
    private val nfcMissingLibraryMessage: String,
    private val nfcReadErrorTitle: String,
    private val onEnableNfcReader: () -> Unit,
    private val onDisableNfcReader: () -> Unit
) {
    val showCanDialog: Boolean by showCanDialogState
    val canCode: String by canCodeState
    val canCodeError: String by canCodeErrorState
    val nfcReadError: String? by nfcReadErrorState
    val pendingNfcData: NfcDniPersonData? by pendingNfcDataState
    val showNfcScanDialog: Boolean by showNfcScanDialogState
    val isReadingNfc: Boolean by isReadingNfcState

    fun startCanDialog() {
        showCanDialogState.value = true
        canCodeState.value = ""
        canCodeErrorState.value = ""
    }

    fun confirmCan(code: String) {
        if (!Regex("^\\d{6}$").matches(code)) {
            canCodeErrorState.value = canInvalidMessage
            return
        }
        val attemptId = System.currentTimeMillis().toString()
        val tagDebug = NfcTagRepository.debugInfo()
        Log.i(NFC_LOG_TAG, "CAN válido. tagDebug: hasTag=${tagDebug.hasTag}, uid=${tagDebug.uid}, ageMs=${tagDebug.ageMs}")
        pendingCanForNfcState.value = code
        pendingAttemptIdState.value = attemptId
        nfcScanStartedAtMillisState.value = System.currentTimeMillis()
        NfcTagRepository.clear()
        onEnableNfcReader()
        waitingForNfcTagState.value = true
        showCanDialogState.value = false
        showNfcScanDialogState.value = true
    }

    fun cancelScan() {
        waitingForNfcTagState.value = false
        showNfcScanDialogState.value = false
        pendingCanForNfcState.value = ""
        pendingAttemptIdState.value = ""
        nfcScanStartedAtMillisState.value = 0L
        showCanDialogState.value = false
        canCodeState.value = ""
        canCodeErrorState.value = ""
        onDisableNfcReader()
    }

    fun clearError() {
        nfcReadErrorState.value = null
    }

    fun dismissDataDialog() {
        pendingNfcDataState.value = null
    }

    fun updateCanCode(code: String) {
        canCodeState.value = code
        canCodeErrorState.value = ""
    }

    fun startNfcRead(attemptId: String, can: String, tag: Tag) {
        val uid = tag.id?.joinToString(":") { "%02X".format(it) }
        Log.i(NFC_LOG_TAG, "[$attemptId] Inicio lectura con tag uid=${uid ?: "<null>"} techs=${tag.techList.joinToString()}")
        waitingForNfcTagState.value = false
        showNfcScanDialogState.value = false
        canCodeErrorState.value = ""
        onDisableNfcReader()
        isReadingNfcState.value = true
        scope.launch {
            Log.d(NFC_LOG_TAG, "[$attemptId] Invocando NfcDniReader.read(...) en IO")
            val result = withContext(Dispatchers.IO) { NfcDniReader.read(can, tag) }
            isReadingNfcState.value = false
            result.onSuccess {
                Log.i(NFC_LOG_TAG, "[$attemptId] Lectura NFC OK. nombre='${it.firstName.take(24)}' doc='${it.documentNumber.take(12)}'")
                pendingNfcDataState.value = it
            }.onFailure { throwable ->
                val rootCause = generateSequence(throwable) { it.cause }.last()
                Log.e(
                    NFC_LOG_TAG,
                    "[$attemptId] Lectura NFC fallida: type=${throwable.javaClass.name}, message=${throwable.message}, rootType=${rootCause.javaClass.name}, rootMessage=${rootCause.message}",
                    throwable
                )
                nfcReadErrorState.value = if (throwable is ClassNotFoundException) {
                    nfcMissingLibraryMessage
                } else {
                    throwable.message ?: nfcReadErrorTitle
                }
                Log.w(NFC_LOG_TAG, "[$attemptId] Mensaje mostrado al usuario: '${nfcReadErrorState.value}'")
                onDisableNfcReader()
            }
        }
    }
}

@Composable
fun rememberNfcReadingHelper(
    onDataRead: (NfcDniPersonData) -> Unit,
    onEnableNfcReader: () -> Unit,
    onDisableNfcReader: () -> Unit
): NfcReadingHelper {
    val showCanDialogState = rememberSaveable { mutableStateOf(false) }
    val canCodeState = rememberSaveable { mutableStateOf("") }
    val canCodeErrorState = rememberSaveable { mutableStateOf("") }
    val nfcReadErrorState = rememberSaveable { mutableStateOf<String?>(null) }
    val pendingNfcDataState = remember { mutableStateOf<NfcDniPersonData?>(null) }
    val showNfcScanDialogState = rememberSaveable { mutableStateOf(false) }
    val isReadingNfcState = rememberSaveable { mutableStateOf(false) }
    val waitingForNfcTagState = rememberSaveable { mutableStateOf(false) }
    val pendingCanForNfcState = rememberSaveable { mutableStateOf("") }
    val pendingAttemptIdState = rememberSaveable { mutableStateOf("") }
    val nfcScanStartedAtMillisState = rememberSaveable { mutableStateOf(0L) }
    val scope = rememberCoroutineScope()

    val canInvalidMessage = stringResource(R.string.can_invalid_message)
    val nfcMissingLibraryMessage = stringResource(R.string.nfc_missing_library_message)
    val nfcReadErrorTitle = stringResource(R.string.nfc_read_error_title)

    val helper = remember {
        NfcReadingHelper(
            showCanDialogState = showCanDialogState,
            canCodeState = canCodeState,
            canCodeErrorState = canCodeErrorState,
            nfcReadErrorState = nfcReadErrorState,
            pendingNfcDataState = pendingNfcDataState,
            showNfcScanDialogState = showNfcScanDialogState,
            isReadingNfcState = isReadingNfcState,
            waitingForNfcTagState = waitingForNfcTagState,
            pendingCanForNfcState = pendingCanForNfcState,
            pendingAttemptIdState = pendingAttemptIdState,
            nfcScanStartedAtMillisState = nfcScanStartedAtMillisState,
            scope = scope,
            canInvalidMessage = canInvalidMessage,
            nfcMissingLibraryMessage = nfcMissingLibraryMessage,
            nfcReadErrorTitle = nfcReadErrorTitle,
            onEnableNfcReader = onEnableNfcReader,
            onDisableNfcReader = onDisableNfcReader
        )
    }

    LaunchedEffect(waitingForNfcTagState.value, pendingCanForNfcState.value, pendingAttemptIdState.value) {
        if (!waitingForNfcTagState.value) return@LaunchedEffect
        Log.i(NFC_LOG_TAG, "[${pendingAttemptIdState.value}] Esperando tag NFC nuevo para CAN válido desde ts=${nfcScanStartedAtMillisState.value}...")
        while (waitingForNfcTagState.value) {
            val debugInfo = NfcTagRepository.debugInfo()
            val candidateTag = NfcTagRepository.getLatest()
            if (candidateTag != null && debugInfo.capturedAtMillis >= nfcScanStartedAtMillisState.value) {
                Log.i(NFC_LOG_TAG, "[${pendingAttemptIdState.value}] Tag fresco detectado uid=${debugInfo.uid} capturedAt=${debugInfo.capturedAtMillis}")
                helper.startNfcRead(pendingAttemptIdState.value, pendingCanForNfcState.value, candidateTag)
                break
            }
            delay(300)
        }
    }

    LaunchedEffect(pendingNfcDataState.value) {
        pendingNfcDataState.value?.let { data ->
            onDataRead(data)
        }
    }

    return helper
}

@Composable
fun NfcReadingHelper.NfcDialogs() {
    val nfcScanDialogTitle = stringResource(R.string.nfc_scan_dialog_title)
    val nfcWaitingTagMessage = stringResource(R.string.nfc_waiting_tag_message)
    val nfcReadingProgressTitle = stringResource(R.string.nfc_reading_progress_title)
    val nfcReadingProgressMessage = stringResource(R.string.nfc_reading_progress_message)
    val canDialogTitle = stringResource(R.string.can_dialog_title)
    val canDialogMessage = stringResource(R.string.can_dialog_message)
    val canCodeLabel = stringResource(R.string.can_code_label)
    val canDialogConfirm = stringResource(R.string.can_dialog_confirm)
    val cancelAction = stringResource(R.string.cancel_action)
    val acceptAction = stringResource(R.string.accept_action)
    val nfcReadErrorTitle = stringResource(R.string.nfc_read_error_title)
    val biometricDescription = stringResource(R.string.can_dialog_biometric_description)

    if (showCanDialog) {
        AlertDialog(
            onDismissRequest = { cancelScan() },
            title = { Text(canDialogTitle) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(canDialogMessage)
                    AssetImage(
                        assetPath = "icons/biometriqpass.png",
                        contentDescription = biometricDescription,
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    OutlinedTextField(
                        value = canCode,
                        onValueChange = { value ->
                            if (value.length <= 6 && value.all { it.isDigit() }) {
                                updateCanCode(value)
                            }
                        },
                        label = { Text(canCodeLabel) },
                        singleLine = true,
                        isError = canCodeError.isNotEmpty()
                    )
                    if (canCodeError.isNotEmpty()) {
                        Text(canCodeError, color = Color.Red)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { confirmCan(canCode) },
                    enabled = !isReadingNfc
                ) {
                    Text(canDialogConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelScan() }) {
                    Text(cancelAction)
                }
            }
        )
    }

    if (showNfcScanDialog && !isReadingNfc) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(nfcScanDialogTitle) },
            text = { Text(nfcWaitingTagMessage) },
            confirmButton = {
                TextButton(onClick = { cancelScan() }) {
                    Text(cancelAction)
                }
            }
        )
    }

    if (isReadingNfc) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(nfcReadingProgressTitle) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator()
                    Text(nfcReadingProgressMessage)
                }
            },
            confirmButton = { }
        )
    }

    if (nfcReadError != null) {
        AlertDialog(
            onDismissRequest = { clearError() },
            title = { Text(nfcReadErrorTitle) },
            text = { Text(nfcReadError.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { clearError() }) {
                    Text(acceptAction)
                }
            }
        )
    }
}
