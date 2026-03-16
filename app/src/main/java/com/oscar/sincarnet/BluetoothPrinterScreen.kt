package com.oscar.sincarnet

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.oscar.sincarnet.ui.theme.SinCarnetTheme
import kotlinx.coroutines.delay

private const val BT_TAG = "BluetoothPrinter"

private data class ScannedBluetoothPrinter(
    val nombre: String,
    val mac: String
)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothPrinterScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val storage = remember(context) { BluetoothPrinterStorage(context) }
    val bluetoothAdapter = remember(context) {
        context.getSystemService(BluetoothManager::class.java)?.adapter
    }

    var savedPrinters by remember { mutableStateOf(emptyList<SavedBluetoothPrinter>()) }
    var selectedSavedPrinterMac by rememberSaveable { mutableStateOf("") }
    var defaultPrinterName by rememberSaveable { mutableStateOf("") }
    var savedPrintersExpanded by remember { mutableStateOf(false) }
    var statusMessage by rememberSaveable { mutableStateOf("") }
    var isScanning by rememberSaveable { mutableStateOf(false) }
    var scanSecondsLeft by rememberSaveable { mutableStateOf(10) }
    var showSaveConfirmation by rememberSaveable { mutableStateOf(false) }

    val discoveredPrinters = remember { mutableStateListOf<ScannedBluetoothPrinter>() }
    var selectedDiscoveredPrinterMac by rememberSaveable { mutableStateOf("") }

    fun addDiscoveredPrinter(printer: ScannedBluetoothPrinter) {
        if (printer.mac.isBlank()) return
        if (discoveredPrinters.none { it.mac == printer.mac }) {
            discoveredPrinters += printer
        }
    }

    fun refreshSavedPrinters() {
        if (isInPreview) {
            savedPrinters = listOf(
                SavedBluetoothPrinter(1, "Zebra GC-01", "00:11:22:33:44:55"),
                SavedBluetoothPrinter(2, "Brother Patrol", "AA:BB:CC:DD:EE:FF")
            )
            val defaultPrinter = savedPrinters.firstOrNull()
            selectedSavedPrinterMac = selectedSavedPrinterMac.ifBlank { defaultPrinter?.mac.orEmpty() }
            defaultPrinterName = defaultPrinter?.nombre.orEmpty()
        } else {
            savedPrinters = storage.loadSavedPrinters()
            val defaultPrinter = storage.getDefaultPrinter()
            if (selectedSavedPrinterMac.isBlank()) {
                selectedSavedPrinterMac = defaultPrinter?.mac ?: savedPrinters.firstOrNull()?.mac.orEmpty()
            }
            defaultPrinterName = defaultPrinter?.nombre.orEmpty()
        }
    }

    @SuppressLint("MissingPermission")
    fun addBondedDevicesAsFallback() {
        if (bluetoothAdapter == null || !hasBluetoothPermissions(context)) return
        runCatching {
            bluetoothAdapter.bondedDevices?.forEach { device ->
                val printer = device.toScannedBluetoothPrinter(context)
                addDiscoveredPrinter(printer)
            }
        }.onFailure { Log.e(BT_TAG, "addBondedDevicesAsFallback error", it) }
    }

    @SuppressLint("MissingPermission")
    fun startBluetoothScan() {
        Log.d(BT_TAG, "startBluetoothScan() called")
        if (bluetoothAdapter == null) {
            Log.w(BT_TAG, "startBluetoothScan: adapter is null → abort")
            statusMessage = context.getString(R.string.bluetooth_printer_not_supported)
            return
        }

        if (!hasBluetoothPermissions(context)) {
            Log.w(BT_TAG, "startBluetoothScan: missing permissions → abort")
            statusMessage = context.getString(R.string.bluetooth_printer_permissions_required)
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Log.w(BT_TAG, "startBluetoothScan: bluetooth disabled → abort")
            statusMessage = context.getString(R.string.bluetooth_printer_bluetooth_disabled)
            return
        }

        // Check if Location Services are enabled (required for discovery on most devices)
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val locationEnabled = locationManager?.let {
            it.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    it.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } ?: false
        Log.d(BT_TAG, "startBluetoothScan: locationEnabled=$locationEnabled")

        if (!locationEnabled) {
            Log.w(BT_TAG, "startBluetoothScan: location disabled → showing paired devices as fallback")
            discoveredPrinters.clear()
            selectedDiscoveredPrinterMac = ""
            addBondedDevicesAsFallback()
            statusMessage = if (discoveredPrinters.isEmpty()) {
                context.getString(R.string.bluetooth_printer_location_disabled)
            } else {
                context.getString(R.string.bluetooth_printer_showing_paired_only)
            }
            return
        }

        Log.d(BT_TAG, "startBluetoothScan: cancelling previous discovery (if any)")
        bluetoothAdapter.cancelDiscoverySafely(context)

        discoveredPrinters.clear()
        selectedDiscoveredPrinterMac = ""
        isScanning = true
        scanSecondsLeft = 10
        statusMessage = context.getString(R.string.bluetooth_printer_scanning)

        Log.d(BT_TAG, "startBluetoothScan: calling bluetoothAdapter.startDiscovery()")
        val started = bluetoothAdapter.startDiscovery()
        Log.d(BT_TAG, "startBluetoothScan: startDiscovery() returned $started")
        if (!started) {
            Log.e(BT_TAG, "startBluetoothScan: discovery failed to start! Falling back to bonded devices.")
            isScanning = false
            addBondedDevicesAsFallback()
            statusMessage = if (discoveredPrinters.isEmpty()) {
                context.getString(R.string.bluetooth_printer_scan_error)
            } else {
                context.getString(R.string.bluetooth_printer_showing_paired_only)
            }
        }
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        Log.d(BT_TAG, "enableBluetoothLauncher: result received")
        try {
            if (bluetoothAdapter?.isEnabled == true && hasBluetoothPermissions(context)) {
                Log.d(BT_TAG, "enableBluetoothLauncher: BT now enabled → starting scan")
                startBluetoothScan()
            } else {
                Log.w(BT_TAG, "enableBluetoothLauncher: BT still disabled or missing perms")
                statusMessage = context.getString(R.string.bluetooth_printer_bluetooth_disabled)
            }
        } catch (e: SecurityException) {
            Log.e(BT_TAG, "enableBluetoothLauncher: SecurityException", e)
            statusMessage = context.getString(R.string.bluetooth_printer_permissions_required)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        Log.d(BT_TAG, "permissionLauncher result: $result")
        val granted = result.values.all { it }
        if (granted) {
            Log.d(BT_TAG, "permissionLauncher: all permissions granted")
            if (bluetoothAdapter?.isEnabled == true) {
                Log.d(BT_TAG, "permissionLauncher: BT enabled → starting scan")
                startBluetoothScan()
            } else if (bluetoothAdapter != null) {
                Log.d(BT_TAG, "permissionLauncher: BT disabled → requesting enable")
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        } else {
            Log.w(BT_TAG, "permissionLauncher: some permissions DENIED")
            statusMessage = context.getString(R.string.bluetooth_printer_permissions_required)
        }
    }


    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d(BT_TAG, "BroadcastReceiver.onReceive action=${intent?.action}")
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = intent.extractBluetoothDevice()
                        Log.d(BT_TAG, "ACTION_FOUND: device=$device, address=${device?.address}, name=${device?.name}")
                        if (device == null) {
                            Log.w(BT_TAG, "ACTION_FOUND: extractBluetoothDevice returned null → skipping")
                            return
                        }
                        val printer = device.toScannedBluetoothPrinter(context ?: return)
                        Log.d(BT_TAG, "ACTION_FOUND: printer=${printer.nombre} / ${printer.mac}")
                        addDiscoveredPrinter(printer)
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        Log.d(BT_TAG, "ACTION_DISCOVERY_FINISHED: found ${discoveredPrinters.size} devices")
                        isScanning = false
                        statusMessage = if (discoveredPrinters.isEmpty()) {
                            context?.getString(R.string.bluetooth_printer_no_results).orEmpty()
                        } else {
                            context?.getString(R.string.bluetooth_printer_scan_finished).orEmpty()
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(context) {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        Log.d(BT_TAG, "DisposableEffect: registering BroadcastReceiver with filter=$filter (RECEIVER_EXPORTED)")
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        onDispose {
            Log.d(BT_TAG, "DisposableEffect: unregistering BroadcastReceiver")
            runCatching { context.unregisterReceiver(receiver) }
            bluetoothAdapter.cancelDiscoverySafely(context)
        }
    }

    LaunchedEffect(Unit) {
        refreshSavedPrinters()
        if (isInPreview) {
            discoveredPrinters.clear()
            discoveredPrinters += listOf(
                ScannedBluetoothPrinter("Zebra GC-01", "00:11:22:33:44:55"),
                ScannedBluetoothPrinter("Brother Patrol", "AA:BB:CC:DD:EE:FF"),
                ScannedBluetoothPrinter("Printer Mobile", "10:20:30:40:50:60")
            )
            statusMessage = context.getString(R.string.bluetooth_printer_ready)
        }
    }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            Log.d(BT_TAG, "LaunchedEffect: scan countdown started (10s)")
            scanSecondsLeft = 10
            repeat(10) { secondIndex ->
                delay(1_000)
                if (!isScanning) {
                    Log.d(BT_TAG, "LaunchedEffect: scan stopped externally at second $secondIndex")
                    return@LaunchedEffect
                }
                scanSecondsLeft = 9 - secondIndex
                Log.d(BT_TAG, "LaunchedEffect: scanSecondsLeft=$scanSecondsLeft, discoveredPrinters=${discoveredPrinters.size}")
            }
            Log.d(BT_TAG, "LaunchedEffect: countdown finished, cancelling discovery")
            bluetoothAdapter.cancelDiscoverySafely(context)
            if (isScanning) {
                isScanning = false
                statusMessage = if (discoveredPrinters.isEmpty()) {
                    context.getString(R.string.bluetooth_printer_no_results)
                } else {
                    context.getString(R.string.bluetooth_printer_scan_finished)
                }
                Log.d(BT_TAG, "LaunchedEffect: scan ended → ${discoveredPrinters.size} devices found")
            }
        }
    }

    val selectedSavedPrinterName = savedPrinters.firstOrNull { it.mac == selectedSavedPrinterMac }?.nombre.orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.bluetooth_printer_title),
                    style = MaterialTheme.typography.titleMedium
                )

                if (savedPrinters.isEmpty()) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        label = { Text(stringResource(R.string.bluetooth_printer_saved_devices_label)) },
                        placeholder = { Text(stringResource(R.string.bluetooth_printer_no_saved_devices)) }
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = savedPrintersExpanded,
                        onExpandedChange = { savedPrintersExpanded = !savedPrintersExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedSavedPrinterName,
                            onValueChange = {},
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            readOnly = true,
                            singleLine = true,
                            label = { Text(stringResource(R.string.bluetooth_printer_saved_devices_label)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = savedPrintersExpanded)
                            }
                        )

                        DropdownMenu(
                            expanded = savedPrintersExpanded,
                            onDismissRequest = { savedPrintersExpanded = false }
                        ) {
                            savedPrinters.forEach { printer ->
                                DropdownMenuItem(
                                    text = { Text(printer.nombre) },
                                    onClick = {
                                        selectedSavedPrinterMac = printer.mac
                                        savedPrintersExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Text(
                    text = defaultPrinterName.ifBlank {
                        stringResource(R.string.bluetooth_printer_not_linked)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (defaultPrinterName.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = statusMessage.ifBlank { stringResource(R.string.bluetooth_printer_ready) },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.bluetooth_printer_table_name),
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.bluetooth_printer_table_mac),
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (discoveredPrinters.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = stringResource(R.string.bluetooth_printer_no_results))
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(discoveredPrinters, key = { it.mac }) { printer ->
                                    val isSelected = selectedDiscoveredPrinterMac == printer.mac
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isSelected) Color(0xFFD6EAF8)
                                                else Color.Transparent
                                            )
                                            .clickable { selectedDiscoveredPrinterMac = printer.mac }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = printer.nombre,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = printer.mac,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    Log.d(BT_TAG, "── Scan button pressed ──")
                    Log.d(BT_TAG, "hasBluetoothPermissions=${hasBluetoothPermissions(context)}")
                    Log.d(BT_TAG, "bluetoothAdapter=$bluetoothAdapter")
                    Log.d(BT_TAG, "bluetoothAdapter.isEnabled=${bluetoothAdapter?.isEnabled}")
                    if (!hasBluetoothPermissions(context)) {
                        Log.d(BT_TAG, "→ Requesting permissions: ${requiredBluetoothPermissions().toList()}")
                        permissionLauncher.launch(requiredBluetoothPermissions())
                    } else if (bluetoothAdapter == null) {
                        Log.d(BT_TAG, "→ Bluetooth not supported (adapter is null)")
                        statusMessage = context.getString(R.string.bluetooth_printer_not_supported)
                    } else if (!bluetoothAdapter.isEnabled) {
                        Log.d(BT_TAG, "→ Bluetooth disabled, requesting enable")
                        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    } else {
                        Log.d(BT_TAG, "→ All checks passed, calling startBluetoothScan()")
                        startBluetoothScan()
                    }
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = stringResource(R.string.bluetooth_printer_scan_action))
            }

            Button(
                onClick = {
                    val scannedSelection = discoveredPrinters.firstOrNull { it.mac == selectedDiscoveredPrinterMac }
                    when {
                        scannedSelection != null -> {
                            val saved = storage.savePrinter(scannedSelection.nombre, scannedSelection.mac)
                            refreshSavedPrinters()
                            selectedSavedPrinterMac = saved.mac
                            defaultPrinterName = saved.nombre
                            statusMessage = context.getString(R.string.bluetooth_printer_saved_status, saved.nombre)
                            showSaveConfirmation = true
                        }

                        savedPrinters.any { it.mac == selectedSavedPrinterMac } -> {
                            storage.setDefaultPrinterMac(selectedSavedPrinterMac)
                            defaultPrinterName = savedPrinters.firstOrNull { it.mac == selectedSavedPrinterMac }?.nombre.orEmpty()
                            statusMessage = context.getString(
                                R.string.bluetooth_printer_saved_status,
                                defaultPrinterName.ifBlank { context.getString(R.string.bluetooth_printer_not_linked) }
                            )
                            showSaveConfirmation = true
                        }

                        else -> {
                            statusMessage = context.getString(R.string.bluetooth_printer_select_one)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text(text = stringResource(R.string.bluetooth_printer_save_action))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackIconButton(onClick = onBackClick)
        }
    }

    if (showSaveConfirmation) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmation = false },
            title = { Text(text = stringResource(R.string.bluetooth_printer_saved_title)) },
            text = { Text(text = statusMessage) },
            confirmButton = {
                TextButton(onClick = { showSaveConfirmation = false }) {
                    Text(text = stringResource(R.string.accept_action))
                }
            }
        )
    }

    if (isScanning) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = stringResource(R.string.bluetooth_printer_title)) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = stringResource(R.string.bluetooth_printer_scanning))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.bluetooth_printer_scanning_countdown,
                            scanSecondsLeft
                        )
                    )
                }
            },
            confirmButton = {}
        )
    }
}

private fun requiredBluetoothPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

private fun hasBluetoothPermissions(context: Context): Boolean {
    val results = requiredBluetoothPermissions().map { permission ->
        val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        Log.d(BT_TAG, "  permission $permission → granted=$granted")
        granted
    }
    val allGranted = results.all { it }
    Log.d(BT_TAG, "hasBluetoothPermissions=$allGranted")
    return allGranted
}

@SuppressLint("MissingPermission")
private fun BluetoothAdapter?.cancelDiscoverySafely(context: Context) {
    if (this == null || !hasBluetoothPermissions(context)) {
        Log.d(BT_TAG, "cancelDiscoverySafely: skipped (adapter=$this, permissions=${this != null && hasBluetoothPermissions(context)})")
        return
    }

    runCatching {
        Log.d(BT_TAG, "cancelDiscoverySafely: isDiscovering=$isDiscovering")
        if (isDiscovering) {
            cancelDiscovery()
            Log.d(BT_TAG, "cancelDiscoverySafely: discovery cancelled")
        }
    }.onFailure {
        Log.e(BT_TAG, "cancelDiscoverySafely: error", it)
        if (it is SecurityException) {
            // El usuario puede revocar permisos en caliente; ignoramos el fallo de forma segura.
        } else {
            throw it
        }
    }
}


@SuppressLint("MissingPermission")
private fun BluetoothDevice.toScannedBluetoothPrinter(context: Context): ScannedBluetoothPrinter {
    val safeMac = address.orEmpty()
    val safeName = name?.takeIf { it.isNotBlank() }
        ?: context.getString(R.string.bluetooth_printer_unknown_name, safeMac)
    return ScannedBluetoothPrinter(nombre = safeName, mac = safeMac)
}

private fun Intent.extractBluetoothDevice(): BluetoothDevice? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    }

@Preview(showBackground = true)
@Composable
private fun BluetoothPrinterScreenPreview() {
    SinCarnetTheme {
        BluetoothPrinterScreen()
    }
}

