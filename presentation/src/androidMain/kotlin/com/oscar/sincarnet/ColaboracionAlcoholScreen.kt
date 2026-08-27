package com.oscar.sincarnet

import com.oscar.sincarnet.presentation.R

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.oscar.sincarnet.data.repository.ComplementarioAlcoholStorage
import com.oscar.sincarnet.data.toStorage
import com.oscar.sincarnet.domain.model.ComplementarioAlcoholData
import com.oscar.sincarnet.nfc.LocalNfcReaderController
import com.oscar.sincarnet.ui.theme.SinCarnetTheme
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.Manifest

private val COLABORACION_ALCOHOL_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd-MM-yyyy")
private const val GEO_TAG = "ColaboracionAlcoholGeo"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColaboracionAlcoholScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onPrintClick: () -> Unit = {},
    onContinueClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val storage = if (isInPreview) null else ComplementarioAlcoholStorage(context.toStorage("colaboracion_alcohol_storage"))
    val initialData = storage?.loadCurrent() ?: ComplementarioAlcoholData()

    var fecha by rememberSaveable { mutableStateOf(initialData.fecha) }
    var hora by rememberSaveable { mutableStateOf(initialData.hora) }
    var lugar by rememberSaveable { mutableStateOf(initialData.lugar) }
    var terminoMunicipal by rememberSaveable { mutableStateOf(initialData.terminoMunicipal) }
    var partidoJudicial by rememberSaveable { mutableStateOf(initialData.partidoJudicial) }
    var cuerpoSolicitante by rememberSaveable { mutableStateOf(initialData.cuerpoSolicitante) }
    var otroCuerpo by rememberSaveable { mutableStateOf(initialData.otroCuerpo) }
    var agenteSolicitanteId by rememberSaveable { mutableStateOf(initialData.agenteSolicitanteId) }
    var personaNombre by rememberSaveable { mutableStateOf(initialData.personaNombre) }
    var personaDni by rememberSaveable { mutableStateOf(initialData.personaDni) }
    var personaFechaExpedicion by rememberSaveable { mutableStateOf(initialData.personaFechaExpedicion) }
    var personaFechaNacimiento by rememberSaveable { mutableStateOf(initialData.personaFechaNacimiento) }
    var personaDomicilio by rememberSaveable { mutableStateOf(initialData.personaDomicilio) }
    var personaTelefono by rememberSaveable { mutableStateOf(initialData.personaTelefono) }
    var enCalidadDe by rememberSaveable { mutableStateOf(initialData.enCalidadDe) }
    var enCalidadOtros by rememberSaveable { mutableStateOf(initialData.enCalidadOtros) }
    var vehiculoMarca by rememberSaveable { mutableStateOf(initialData.vehiculoMarca) }
    var vehiculoModelo by rememberSaveable { mutableStateOf(initialData.vehiculoModelo) }
    var vehiculoMatricula by rememberSaveable { mutableStateOf(initialData.vehiculoMatricula) }
    var operadorTip by rememberSaveable { mutableStateOf(initialData.operadorTip) }
    var operadorUnidad by rememberSaveable { mutableStateOf(initialData.operadorUnidad) }
    var pruebasAlcohol by rememberSaveable { mutableStateOf(initialData.pruebasAlcohol) }
    var pruebasDrogas by rememberSaveable { mutableStateOf(initialData.pruebasDrogas) }
    var caso by rememberSaveable { mutableStateOf(initialData.caso) }
    var tipoUnidad by rememberSaveable { mutableStateOf(initialData.tipoUnidad) }
    var unidadNombre by rememberSaveable { mutableStateOf(initialData.unidadNombre) }
    var diligenciasExpediente by rememberSaveable { mutableStateOf(initialData.diligenciasExpediente) }
    var motivoSiniestroVial by rememberSaveable { mutableStateOf(initialData.motivoSiniestroVial) }
    var motivoSignosEvidentes by rememberSaveable { mutableStateOf(initialData.motivoSignosEvidentes) }
    var motivoRequerimientoJudicial by rememberSaveable { mutableStateOf(initialData.motivoRequerimientoJudicial) }
    var motivoInfraccionTrafico by rememberSaveable { mutableStateOf(initialData.motivoInfraccionTrafico) }
    var motivoControlPreventivo by rememberSaveable { mutableStateOf(initialData.motivoControlPreventivo) }
    var motivoOtros by rememberSaveable { mutableStateOf(initialData.motivoOtros) }
    var motivoOtrosDetalle by rememberSaveable { mutableStateOf(initialData.motivoOtrosDetalle) }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showGpsLoadingModal by rememberSaveable { mutableStateOf(false) }
    var showTipoConductorDialog by rememberSaveable { mutableStateOf(false) }
    var showMotivoDialog by rememberSaveable { mutableStateOf(false) }
    var tipoConductorSeleccionado by rememberSaveable { mutableStateOf(initialData.tipoConductor) }

    val nfcController = LocalNfcReaderController.current
    val nfcHelper = if (!isInPreview) {
        rememberNfcReadingHelper(
            onDataRead = { data ->
                personaNombre = "${data.firstName} ${data.lastName1} ${data.lastName2}".trim().uppercase()
                personaDni = data.optionalData.ifBlank { data.documentNumber }.uppercase()
                personaDomicilio = data.residenceAddress
            },
            onEnableNfcReader = { nfcController.enableNfcReaderModeForDniRead() },
            onDisableNfcReader = { nfcController.disableNfcReaderModeForDniRead() }
        )
    } else null

    val coroutineScope = rememberCoroutineScope()

    val guardiaCivil = stringResource(R.string.colaboracion_alcohol_guardia_civil)
    val policiaNacional = stringResource(R.string.colaboracion_alcohol_policia_nacional)
    val policiaLocal = stringResource(R.string.colaboracion_alcohol_policia_local)
    val otros = stringResource(R.string.colaboracion_alcohol_otros)

    val cuerpoOptions = listOf(guardiaCivil, policiaNacional, policiaLocal, otros)
    var expandedDropdown by rememberSaveable { mutableStateOf(false) }

    fun resolveCuerpoSolicitante(): String {
        return if (cuerpoSolicitante == otros) {
            otroCuerpo
        } else {
            cuerpoSolicitante
        }
    }

    fun buildCurrentData() = ComplementarioAlcoholData(
        fecha = fecha,
        hora = hora,
        lugar = lugar,
        terminoMunicipal = terminoMunicipal,
        partidoJudicial = partidoJudicial,
        cuerpoSolicitante = resolveCuerpoSolicitante(),
        otroCuerpo = otroCuerpo,
        agenteSolicitanteId = agenteSolicitanteId,
        pruebasAlcohol = pruebasAlcohol,
        pruebasDrogas = pruebasDrogas,
        caso = caso,
        tipoUnidad = tipoUnidad,
        unidadNombre = unidadNombre,
        diligenciasExpediente = diligenciasExpediente,
        motivoSiniestroVial = motivoSiniestroVial,
        motivoSignosEvidentes = motivoSignosEvidentes,
        motivoRequerimientoJudicial = motivoRequerimientoJudicial,
        motivoInfraccionTrafico = motivoInfraccionTrafico,
        motivoControlPreventivo = motivoControlPreventivo,
        motivoOtros = motivoOtros,
        personaNombre = personaNombre,
        personaDni = personaDni,
        personaFechaExpedicion = personaFechaExpedicion,
        personaFechaNacimiento = personaFechaNacimiento.ifBlank { personaFechaExpedicion },
        personaDomicilio = personaDomicilio,
        personaTelefono = personaTelefono,
        enCalidadDe = enCalidadDe,
        enCalidadOtros = enCalidadOtros,
        motivoOtrosDetalle = motivoOtrosDetalle,
        vehiculoMarca = vehiculoMarca,
        vehiculoModelo = vehiculoModelo,
        vehiculoMatricula = vehiculoMatricula,
        operadorTip = operadorTip,
        operadorUnidad = operadorUnidad,
        tipoConductor = tipoConductorSeleccionado
    )

    fun resetForm() {
        fecha = ""
        hora = ""
        lugar = ""
        terminoMunicipal = ""
        partidoJudicial = ""
        cuerpoSolicitante = ""
        otroCuerpo = ""
        agenteSolicitanteId = ""
        personaNombre = ""
        personaDni = ""
        personaFechaExpedicion = ""
        personaFechaNacimiento = ""
        personaDomicilio = ""
        personaTelefono = ""
        vehiculoMarca = ""
        vehiculoModelo = ""
        vehiculoMatricula = ""
        operadorTip = ""
        operadorUnidad = ""
        pruebasAlcohol = false
        pruebasDrogas = false
        caso = ""
        tipoUnidad = ""
        unidadNombre = ""
        diligenciasExpediente = ""
        motivoSiniestroVial = false
        motivoSignosEvidentes = false
        motivoRequerimientoJudicial = false
        motivoInfraccionTrafico = false
        motivoControlPreventivo = false
        motivoOtros = false
        motivoOtrosDetalle = ""
        enCalidadDe = ""
        enCalidadOtros = ""
        tipoConductorSeleccionado = ""
    }

    fun fillFromLocation(loc: Location) {
        try {
            Log.d(
                GEO_TAG,
                "Coordenadas obtenidas lat=${loc.latitude}, lon=${loc.longitude}, provider=${loc.provider}"
            )
            val geocoder = Geocoder(context, Locale.getDefault())
            fun applyAddress(address: Address?, allAddresses: List<Address> = listOf()) {
                address?.let {
                    val addressLines = (0..it.maxAddressLineIndex)
                        .mapNotNull { index -> it.getAddressLine(index) }
                    val provinciaDetectada = it.subAdminArea ?: it.adminArea ?: ""
                    val terminoDetectado = resolveTerminoMunicipal(
                        address = it,
                        provincia = provinciaDetectada,
                        localidad = it.locality ?: "",
                        addressLines = addressLines,
                        allAddresses = allAddresses
                    )

                    lugar = addressLines.firstOrNull() ?: it.thoroughfare ?: it.featureName ?: lugar
                    terminoMunicipal = terminoDetectado.ifBlank { terminoMunicipal }
                    Log.d(
                        GEO_TAG,
                        "Campos rellenados -> lugar=$lugar, terminoMunicipal=$terminoMunicipal, terminoDetectado=$terminoDetectado"
                    )
                    showGpsLoadingModal = false
                } ?: run {
                    Log.w(GEO_TAG, "Geocoder no devolvió address para las coordenadas")
                    Toast.makeText(
                        context,
                        "No se pudo obtener dirección desde las coordenadas",
                        Toast.LENGTH_SHORT
                    ).show()
                    showGpsLoadingModal = false
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(loc.latitude, loc.longitude, 5) { addresses ->
                    Log.d(GEO_TAG, "Geocoder (API 33+) devuelve ${addresses.size} direcciones")
                    Handler(Looper.getMainLooper()).post {
                        applyAddress(addresses.firstOrNull(), addresses)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 5)
                Log.d(GEO_TAG, "Geocoder (legacy) devuelve ${addresses?.size ?: 0} direcciones")
                applyAddress(addresses?.firstOrNull(), addresses.orEmpty())
            }
        } catch (e: Exception) {
            Log.e(GEO_TAG, "Error geocodificando ubicación", e)
            e.printStackTrace()
            Toast.makeText(
                context,
                "Error al geocodificar ubicación: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            showGpsLoadingModal = false
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchAndFillLocationNoPermissionCheck() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(context, permission) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(GEO_TAG, "Permiso ACCESS_FINE_LOCATION no concedido")
            Toast.makeText(context, "Permiso de ubicación no concedido", Toast.LENGTH_SHORT).show()
            showGpsLoadingModal = false
            return
        }
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            Log.d(GEO_TAG, "FusedLocationProviderClient inicializado")

            fusedClient.lastLocation
                .addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        Log.d(GEO_TAG, "lastLocation disponible")
                        fillFromLocation(loc)
                    } else {
                        Log.d(GEO_TAG, "lastLocation nula, solicitando currentLocation")
                        val cts = CancellationTokenSource()
                        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                            .addOnSuccessListener { currentLoc: Location? ->
                                if (currentLoc != null) {
                                    Log.d(GEO_TAG, "currentLocation obtenida")
                                    fillFromLocation(currentLoc)
                                } else {
                                    Log.w(GEO_TAG, "currentLocation nula")
                                    Toast.makeText(
                                        context,
                                        "Ubicación no disponible",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    showGpsLoadingModal = false
                                }
                            }
                            .addOnFailureListener { e: Exception ->
                                Log.e(GEO_TAG, "Error al obtener currentLocation", e)
                                e.printStackTrace()
                                Toast.makeText(
                                    context,
                                    "Error al obtener ubicación: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                showGpsLoadingModal = false
                            }
                    }
                }
                .addOnFailureListener { e: Exception ->
                    Log.e(GEO_TAG, "Error al obtener lastLocation", e)
                    e.printStackTrace()
                    Toast.makeText(
                        context,
                        "Error al obtener última ubicación: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    showGpsLoadingModal = false
                }
        } catch (e: Exception) {
            Log.e(GEO_TAG, "Error inicializando proveedor de ubicación", e)
            e.printStackTrace()
            Toast.makeText(
                context,
                "Error al inicializar proveedor de ubicación: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            showGpsLoadingModal = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            fetchAndFillLocationNoPermissionCheck()
        } else {
            Toast.makeText(context, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            showGpsLoadingModal = false
        }
    }

    fun fetchAndFillLocation() {
        Log.d(GEO_TAG, "Click en botón de geolocalización")
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(context, permission) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(GEO_TAG, "Permiso no concedido: se solicita al usuario")
            permissionLauncher.launch(permission)
            return
        }
        Log.d(GEO_TAG, "Permiso concedido: obteniendo ubicación")
        fetchAndFillLocationNoPermissionCheck()
    }

    fun saveAndContinue() {
        if (!pruebasAlcohol && !pruebasDrogas) {
            Toast.makeText(context, context.getString(R.string.colaboracion_pruebas_required), Toast.LENGTH_SHORT).show()
            return
        }
        storage?.saveCurrent(buildCurrentData())
        showMotivoDialog = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.colaboracion_alcohol_title),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = fecha,
                        onValueChange = {},
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        singleLine = true,
                        label = { Text(stringResource(R.string.colaboracion_alcohol_fecha)) }
                    )
                    IconButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .size(48.dp)
                            .padding(top = 8.dp)
                            .align(Alignment.Top)
                    ) {
                        AssetImage(
                            assetPath = "icons/calendar.png",
                            contentDescription = stringResource(R.string.colaboracion_alcohol_select_date),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = hora,
                        onValueChange = {},
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        singleLine = true,
                        label = { Text(stringResource(R.string.colaboracion_alcohol_hora)) }
                    )
                    IconButton(
                        onClick = {
                            val initialHour = hora.split(":").getOrNull(0)?.toIntOrNull() ?: 0
                            val initialMinute = hora.split(":").getOrNull(1)?.toIntOrNull() ?: 0
                            TimePickerDialog(
                                context,
                                { _, h, m ->
                                    hora = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                                },
                                initialHour,
                                initialMinute,
                                true
                            ).show()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .padding(top = 8.dp)
                            .align(Alignment.Top)
                    ) {
                        AssetImage(
                            assetPath = "icons/clock.png",
                            contentDescription = stringResource(R.string.colaboracion_alcohol_select_time),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = lugar,
                        onValueChange = { lugar = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.colaboracion_alcohol_lugar)) }
                    )
                    IconButton(
                        onClick = {
                            if (!showGpsLoadingModal) {
                                showGpsLoadingModal = true
                                coroutineScope.launch {
                                    delay(1000)
                                    fetchAndFillLocation()
                                }
                            }
                        },
                        enabled = !showGpsLoadingModal,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(top = 8.dp)
                            .align(Alignment.Top)
                    ) {
                        AssetImage(
                            assetPath = "icons/ubicacion.png",
                            contentDescription = stringResource(R.string.colaboracion_alcohol_select_location),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                OutlinedTextField(
                    value = terminoMunicipal,
                    onValueChange = { terminoMunicipal = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.colaboracion_alcohol_termino_municipal)) }
                )

                OutlinedTextField(
                    value = partidoJudicial,
                    onValueChange = { partidoJudicial = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.colaboracion_alcohol_partido_judicial)) }
                )

                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = cuerpoSolicitante,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        singleLine = true,
                        label = { Text(stringResource(R.string.colaboracion_alcohol_cuerpo_solicitante)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        cuerpoOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    cuerpoSolicitante = option
                                    expandedDropdown = false
                                    if (option != otros) {
                                        otroCuerpo = ""
                                    }
                                }
                            )
                        }
                    }
                }

                if (cuerpoSolicitante == otros) {
                    OutlinedTextField(
                        value = otroCuerpo,
                        onValueChange = { otroCuerpo = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.colaboracion_alcohol_otro_cuerpo)) }
                    )
                }

                SectionTitle(text = stringResource(R.string.colaboracion_alcohol_agente_solicitante))

                OutlinedTextField(
                    value = agenteSolicitanteId,
                    onValueChange = { agenteSolicitanteId = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.colaboracion_alcohol_agente_solicitante)) }
                )

                SectionTitle(text = stringResource(R.string.colaboracion_pruebas_realizadas))
                CheckboxOptionRow(
                    text = stringResource(R.string.colaboracion_pruebas_alcohol),
                    checked = pruebasAlcohol,
                    onCheckedChange = { pruebasAlcohol = it }
                )
                CheckboxOptionRow(
                    text = stringResource(R.string.colaboracion_pruebas_drogas),
                    checked = pruebasDrogas,
                    onCheckedChange = { pruebasDrogas = it }
                )

                SectionTitle(text = stringResource(R.string.colaboracion_unidad_que_presta_apoyo))
                OptionRadioRow(
                    text = stringResource(R.string.colaboracion_unidad_sector),
                    selected = tipoUnidad == "Sector",
                    onSelect = { tipoUnidad = "Sector" }
                )
                OptionRadioRow(
                    text = stringResource(R.string.colaboracion_unidad_subsector),
                    selected = tipoUnidad == "Subsector",
                    onSelect = { tipoUnidad = "Subsector" }
                )
                OptionRadioRow(
                    text = stringResource(R.string.colaboracion_unidad_destacamento),
                    selected = tipoUnidad == "Destacamento",
                    onSelect = { tipoUnidad = "Destacamento" }
                )

                OutlinedTextField(
                    value = unidadNombre,
                    onValueChange = { unidadNombre = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.colaboracion_unidad_nombre)) }
                )

                OutlinedTextField(
                    value = diligenciasExpediente,
                    onValueChange = { diligenciasExpediente = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.colaboracion_diligencias_expediente)) }
                )

                SectionTitle(text = stringResource(R.string.colaboracion_alcohol_persona_sometida_section))

                OutlinedTextField(
                    value = personaNombre,
                    onValueChange = { personaNombre = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.colaboracion_alcohol_persona_nombre)) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = personaDni,
                        onValueChange = { personaDni = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.colaboracion_alcohol_persona_dni)) }
                    )
                    if (nfcHelper != null) {
                        IconButton(
                            onClick = { nfcHelper.startCanDialog() },
                            modifier = Modifier
                                .size(48.dp)
                                .padding(top = 8.dp)
                                .align(Alignment.Top)
                        ) {
                            AssetImage(
                                assetPath = "icons/rfid.png",
                                contentDescription = stringResource(R.string.can_button_content_description),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = personaFechaExpedicion,
                    onValueChange = { personaFechaExpedicion = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.colaboracion_alcohol_persona_fecha_expedicion)) }
                )

                OutlinedTextField(
                    value = personaFechaNacimiento,
                    onValueChange = { personaFechaNacimiento = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.colaboracion_alcohol_persona_fecha_nacimiento)) }
                )

                OutlinedTextField(
                    value = personaDomicilio,
                    onValueChange = { personaDomicilio = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.colaboracion_alcohol_persona_domicilio)) }
                )

                SectionTitle(text = stringResource(R.string.colaboracion_en_calidad_de))
                OptionRadioRow(
                    text = stringResource(R.string.colaboracion_conductor_vehiculo),
                    selected = enCalidadDe == "Conductor",
                    onSelect = { enCalidadDe = "Conductor" }
                )
                OptionRadioRow(
                    text = stringResource(R.string.colaboracion_otros_usuarios_via),
                    selected = enCalidadDe == "OtrosUsuarios",
                    onSelect = { enCalidadDe = "OtrosUsuarios" }
                )
                OptionRadioRow(
                    text = stringResource(R.string.colaboracion_otros),
                    selected = enCalidadDe == "Otros",
                    onSelect = { enCalidadDe = "Otros" }
                )

                SectionTitle(text = stringResource(R.string.colaboracion_alcohol_vehiculo_section))

                OutlinedTextField(
                    value = vehiculoMarca,
                    onValueChange = { vehiculoMarca = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.colaboracion_alcohol_vehiculo_marca)) }
                )

                OutlinedTextField(
                    value = vehiculoModelo,
                    onValueChange = { vehiculoModelo = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.colaboracion_alcohol_vehiculo_modelo)) }
                )

                OutlinedTextField(
                    value = vehiculoMatricula,
                    onValueChange = { vehiculoMatricula = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.colaboracion_alcohol_vehiculo_matricula)) }
                )

                SectionTitle(text = stringResource(R.string.colaboracion_alcohol_operador_section))

                OutlinedTextField(
                    value = operadorTip,
                    onValueChange = { operadorTip = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.colaboracion_alcohol_operador_tip)) }
                )

                OutlinedTextField(
                    value = operadorUnidad,
                    onValueChange = { operadorUnidad = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.colaboracion_alcohol_operador_unidad)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { saveAndContinue() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.colaboracion_alcohol_continue))
                }

                Button(
                    onClick = {
                        storage?.clearCurrent()
                        resetForm()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.colaboracion_alcohol_delete))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrintClick,
                modifier = Modifier.size(44.dp)
            ) {
                AssetImage(
                    assetPath = "icons/impresora.png",
                    contentDescription = stringResource(R.string.print_icon_content_description),
                    modifier = Modifier.size(30.dp)
                )
            }
            BackIconButton(onClick = onBackClick)
        }
    }

    if (showGpsLoadingModal) {
        Dialog(
            onDismissRequest = { }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .widthIn(min = 180.dp, max = 260.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Text(text = stringResource(R.string.colaboracion_alcohol_detecting_location))
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = fecha.toColaboracionAlcoholDateMillisOrNull()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis
                            ?.toFormattedColaboracionAlcoholDate()
                            ?.let { fecha = it }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.person_data_birth_date_select_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.no_option))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showMotivoDialog) {
        AlertDialog(
            onDismissRequest = { showMotivoDialog = false },
            title = { Text(text = stringResource(R.string.colaboracion_alcohol_motivo_requerimiento)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CheckboxOptionRow(
                        text = stringResource(R.string.colaboracion_motivo_siniestro_vial),
                        checked = motivoSiniestroVial,
                        onCheckedChange = { motivoSiniestroVial = it }
                    )
                    CheckboxOptionRow(
                        text = stringResource(R.string.colaboracion_motivo_signos_evidentes),
                        checked = motivoSignosEvidentes,
                        onCheckedChange = { motivoSignosEvidentes = it }
                    )
                    CheckboxOptionRow(
                        text = stringResource(R.string.colaboracion_motivo_requerimiento_judicial),
                        checked = motivoRequerimientoJudicial,
                        onCheckedChange = { motivoRequerimientoJudicial = it }
                    )
                    CheckboxOptionRow(
                        text = stringResource(R.string.colaboracion_motivo_infraccion_trafico),
                        checked = motivoInfraccionTrafico,
                        onCheckedChange = { motivoInfraccionTrafico = it }
                    )
                    CheckboxOptionRow(
                        text = stringResource(R.string.colaboracion_motivo_control_preventivo),
                        checked = motivoControlPreventivo,
                        onCheckedChange = { motivoControlPreventivo = it }
                    )
                    CheckboxOptionRow(
                        text = stringResource(R.string.colaboracion_motivo_otros),
                        checked = motivoOtros,
                        onCheckedChange = { motivoOtros = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (
                            !motivoSiniestroVial &&
                            !motivoSignosEvidentes &&
                            !motivoRequerimientoJudicial &&
                            !motivoInfraccionTrafico &&
                            !motivoControlPreventivo &&
                            !motivoOtros
                        ) {
                            Toast.makeText(context, context.getString(R.string.colaboracion_alcohol_motivo_required), Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        storage?.saveCurrent(buildCurrentData())
                        showMotivoDialog = false
                        showTipoConductorDialog = true
                    }
                ) {
                    Text(stringResource(R.string.continue_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showMotivoDialog = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            }
        )
    }

    if (showTipoConductorDialog) {
        AlertDialog(
            onDismissRequest = { showTipoConductorDialog = false },
            title = { Text(text = stringResource(R.string.colaboracion_alcohol_tipo_conductor_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionRadioRow(
                        text = stringResource(R.string.colaboracion_caso_conductor_vehiculos),
                        selected = caso == "ConductorVehiculos",
                        onSelect = { caso = "ConductorVehiculos" }
                    )
                    OptionRadioRow(
                        text = stringResource(R.string.colaboracion_caso_usuario_accidente),
                        selected = caso == "UsuarioAccidente",
                        onSelect = { caso = "UsuarioAccidente" }
                    )
                    OptionRadioRow(
                        text = stringResource(R.string.colaboracion_caso_mercancias),
                        selected = caso == "Mercancias",
                        onSelect = { caso = "Mercancias" }
                    )
                    OptionRadioRow(
                        text = stringResource(R.string.colaboracion_caso_novato),
                        selected = caso == "Novato",
                        onSelect = { caso = "Novato" }
                    )
                    OptionRadioRow(
                        text = stringResource(R.string.colaboracion_caso_otros),
                        selected = caso == "Otros",
                        onSelect = { caso = "Otros" }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (caso.isBlank()) {
                            Toast.makeText(context, context.getString(R.string.colaboracion_caso_required), Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        storage?.saveCurrent(buildCurrentData())
                        showTipoConductorDialog = false
                        onContinueClick()
                    }
                ) {
                    Text(stringResource(R.string.continue_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTipoConductorDialog = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            }
        )
    }

    if (nfcHelper != null) {
        nfcHelper.NfcDialogs()
    }
}

private fun Long.toFormattedColaboracionAlcoholDate(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(COLABORACION_ALCOHOL_DATE_FORMATTER)

private fun String.toColaboracionAlcoholDateMillisOrNull(): Long? = runCatching {
    LocalDate.parse(this, COLABORACION_ALCOHOL_DATE_FORMATTER)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}.getOrNull()

private fun resolveTerminoMunicipal(
    address: Address,
    provincia: String,
    localidad: String,
    addressLines: List<String>,
    allAddresses: List<Address>
): String {
    fun normalizeForCompare(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.getDefault())

    val country = normalizeForCompare(address.countryName.orEmpty())
    val provinciaNorm = normalizeForCompare(provincia)
    val localidadNorm = normalizeForCompare(localidad)

    val genericTokens = setOf(
        "lugar", "parroquia", "barrio", "urbanizacion", "urbanización",
        "diseminado", "poligono", "polígono", "carretera", "calle", "avenida", "plaza"
    )

    fun cleanToken(raw: String): String {
        var token = raw.trim()
        token = token.replace(Regex("^\\d{5}\\s+"), "")
        token = token.replace(Regex("(?i)^(lugar|parroquia|barrio|urbanizacion|urbanización|diseminado)\\s+"), "")
        token = token.replace("[", "").replace("]", "")
        token = token.replace(Regex("\\|.*$"), "")
        token = token.replace(Regex("\\s*\\[CTA\\s*\\d+]", RegexOption.IGNORE_CASE), "")
        return token.trim()
    }

    fun isValidMunicipioCandidate(token: String): Boolean {
        if (token.isBlank()) return false
        val norm = normalizeForCompare(token)
        return norm != provinciaNorm &&
            norm != country &&
            norm != localidadNorm &&
            norm !in genericTokens.map(::normalizeForCompare) &&
            !Regex("^([A-Z]{1,3}-)?\\d+[A-Z]?$", RegexOption.IGNORE_CASE).containsMatchIn(token)
    }

    val allLines = allAddresses.flatMap { addr ->
        (0..addr.maxAddressLineIndex).mapNotNull { index -> addr.getAddressLine(index) }
    }
    val lineCandidates = (addressLines + allLines)
        .asSequence()
        .flatMap { line -> line.split(',').asSequence() }
        .map { token -> cleanToken(token) }
        .filter(::isValidMunicipioCandidate)
        .toList()

    val localityCandidates = allAddresses
        .mapNotNull { it.locality }
        .map(::cleanToken)
        .filter(::isValidMunicipioCandidate)
        .distinct()

    val fromLocalities = localityCandidates.firstOrNull()
    if (!fromLocalities.isNullOrBlank()) return fromLocalities

    val fromComposite = lineCandidates
        .mapNotNull { token ->
            Regex("(?i).+\\s+de\\s+(.+)$").find(token)?.groupValues?.getOrNull(1)?.trim()
        }
        .map(::cleanToken)
        .firstOrNull(::isValidMunicipioCandidate)

    if (!fromComposite.isNullOrBlank()) return fromComposite

    val fromLines = lineCandidates
        .asReversed()
        .firstOrNull(::isValidMunicipioCandidate)

    if (!fromLines.isNullOrBlank()) return fromLines

    val candidateLocal = address.locality.orEmpty().trim()
    if (isValidMunicipioCandidate(cleanToken(candidateLocal))) {
        return cleanToken(candidateLocal)
    }

    val candidateSubLocal = address.subLocality.orEmpty().trim()
    if (isValidMunicipioCandidate(cleanToken(candidateSubLocal))) {
        return cleanToken(candidateSubLocal)
    }

    return ""
}

@Preview(showBackground = true)
@Composable
private fun ColaboracionAlcoholScreenPreview() {
    SinCarnetTheme {
        ColaboracionAlcoholScreen()
    }
}
