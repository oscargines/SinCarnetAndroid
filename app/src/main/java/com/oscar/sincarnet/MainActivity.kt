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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.navigation.compose.rememberNavController
import com.oscar.sincarnet.data.datasource.nfc.NfcTagRepository
import com.oscar.sincarnet.navigation.NavGraph
import com.oscar.sincarnet.ui.theme.SinCarnetTheme
import org.koin.androidx.compose.koinViewModel
import java.io.File

/**
 * Activity principal de SinCarnet - Single-Activity Architecture.
 *
 * Responsabilidades:
 * 1. **Configuración NFC**: ReaderMode para lectura de DNI electrónico.
 * 2. **Entry point de UI**: `setContent` con `NavGraph` de Navigation Compose.
 * 3. **Efectos Android**: abrir/compartir PDF/ODT, mostrar Toasts.
 *
 * Todo el estado y la lógica de presentación viven en [MainViewModel].
 *
 * @see NfcTagRepository Para lectura de DNI
 * @see MainViewModel Para estado y lógica de presentación
 */
class MainActivity : ComponentActivity() {
    private companion object {
        const val NFC_LOG_TAG = "MainActivityNfc"
    }

    /**
     * Adaptador NFC del dispositivo.
     */
    private var nfcAdapter: NfcAdapter? = null

    /**
     * Callback que se ejecuta cuando se detecta una etiqueta NFC en ReaderMode.
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        logNfcAdapterState("onCreate")
        processNfcIntent(intent)
        enableEdgeToEdge()
        setContent {
            SinCarnetAppContent()
        }
    }

    /**
     * Contenido raíz de Compose.
     *
     * Configura el Scaffold, el ViewModel, el NavController y los efectos secundarios.
     */
    @Composable
    private fun SinCarnetAppContent() {
        val viewModel: MainViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsState()
        val navController = rememberNavController()

        LaunchedEffect(viewModel) {
            viewModel.sideEffects.collect { effect ->
                when (effect) {
                    is MainSideEffect.OpenPdf -> {
                        val opened = openGeneratedPdf(effect.file)
                        if (!opened) {
                            Toast.makeText(
                                applicationContext,
                                getString(R.string.atestado_pdf_open_error),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    is MainSideEffect.SharePdf -> {
                        val shared = shareGeneratedPdf(effect.file)
                        if (!shared) {
                            Toast.makeText(
                                applicationContext,
                                getString(R.string.atestado_pdf_share_error),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    is MainSideEffect.ShareOdt -> {
                        val shared = shareGeneratedOdt(effect.file)
                        if (!shared) {
                            Toast.makeText(
                                applicationContext,
                                getString(R.string.atestado_odt_share_error),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    is MainSideEffect.ShowToast -> {
                        Toast.makeText(
                            applicationContext,
                            getString(effect.messageRes),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        SinCarnetTheme {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                if (uiState.showSplash) {
                    SplashScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        versionName = BuildConfig.VERSION_NAME
                    )
                } else {
                    NavGraph(
                        navController = navController,
                        viewModel = viewModel,
                        uiState = uiState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
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
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CICLO DE VIDA - onNewIntent
    // ─────────────────────────────────────────────────────────────────────────

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(NFC_LOG_TAG, "onNewIntent() action=${intent.action}")
        setIntent(intent)
        processNfcIntent(intent)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CICLO DE VIDA - onResume
    // ─────────────────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        logNfcAdapterState("onResume")
        enableNfcReaderMode()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CICLO DE VIDA - onPause
    // ─────────────────────────────────────────────────────────────────────────

    override fun onPause() {
        super.onPause()
        disableNfcReaderMode()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROCESAMIENTO DE INTENTS NFC
    // ─────────────────────────────────────────────────────────────────────────

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

    private fun logNfcAdapterState(origin: String) {
        val adapter = nfcAdapter ?: NfcAdapter.getDefaultAdapter(this)
        nfcAdapter = adapter
        val enabled = adapter?.isEnabled == true
        Log.d(NFC_LOG_TAG, "$origin nfcAdapterPresent=${adapter != null} nfcEnabled=$enabled currentIntentAction=${intent?.action}")
    }

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

    private fun disableNfcReaderMode() {
        val adapter = nfcAdapter ?: return
        Log.d(NFC_LOG_TAG, "disableReaderMode")
        adapter.disableReaderMode(this)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MANEJO DE DOCUMENTOS GENERADOS
    // ─────────────────────────────────────────────────────────────────────────

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
