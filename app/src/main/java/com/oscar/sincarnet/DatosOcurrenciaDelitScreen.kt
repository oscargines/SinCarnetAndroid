package com.oscar.sincarnet

import android.app.TimePickerDialog
import android.util.Log
import androidx.compose.foundation.clickable  // ← IMPORTANTE
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton  // ← IMPORTANTE
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import android.Manifest
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.location.Priority
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.util.Locale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.oscar.sincarnet.ui.theme.SinCarnetTheme
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val OCURRENCIA_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd-MM-yyyy")
private const val GEO_TAG = "OcurrenciaGeo"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatosOcurrenciaDelitScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onPrintClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val storage = OcurrenciaDelitStorage(context)
    val initialData = storage.loadCurrent()

    var carretera by rememberSaveable { mutableStateOf(initialData.carretera) }
    var pk by rememberSaveable { mutableStateOf(initialData.pk) }
    var localidad by rememberSaveable { mutableStateOf(initialData.localidad) }
    var provincia by rememberSaveable { mutableStateOf(initialData.provincia) }
    var terminoMunicipal by rememberSaveable { mutableStateOf(initialData.terminoMunicipal) }
    var fecha by rememberSaveable { mutableStateOf(initialData.fecha) }
    var hora by rememberSaveable { mutableStateOf(initialData.hora) }
    // ← NUEVO: Variable de estado para el momento de información de derechos
    var derechosInformacionMomento by rememberSaveable {
        mutableStateOf(initialData.derechosInformacionMomento)
    }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showGpsLoadingModal by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Obtiene la ubicación asumiendo que ya existe permiso
    fun fetchAndFillLocationNoPermissionCheck() {
        // Verificar permiso por seguridad (puede avisar al lint)
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

                            Log.d(
                                GEO_TAG,
                                "Address raw -> thoroughfare=${it.thoroughfare}, featureName=${it.featureName}, subThoroughfare=${it.subThoroughfare}, locality=${it.locality}, subLocality=${it.subLocality}, subAdminArea=${it.subAdminArea}, adminArea=${it.adminArea}, countryName=${it.countryName}, postalCode=${it.postalCode}, lines=$addressLines, totalAddresses=${allAddresses.size}"
                            )
                            carretera = it.thoroughfare ?: it.featureName ?: carretera
                            pk = it.subThoroughfare ?: pk
                            localidad = it.locality ?: it.subAdminArea ?: localidad
                            provincia = provinciaDetectada.ifBlank { provincia }
                            // Término municipal: intentar detectar municipio real (ej. Llanera) y evitar provincia.
                            terminoMunicipal = terminoDetectado.ifBlank { terminoMunicipal }
                            Log.d(
                                GEO_TAG,
                                "Campos rellenados -> carretera=$carretera, pk=$pk, localidad=$localidad, provincia=$provincia, terminoMunicipal=$terminoMunicipal, terminoDetectado=$terminoDetectado"
                            )
                            showGpsLoadingModal = false
                        } ?: run {
                            Log.w(GEO_TAG, "Geocoder no devolvió address para las coordenadas")
                            Toast.makeText(context, "No se pudo obtener dirección desde las coordenadas", Toast.LENGTH_SHORT).show()
                            showGpsLoadingModal = false
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(loc.latitude, loc.longitude, 5) { addresses ->
                            Log.d(GEO_TAG, "Geocoder (API 33+) devuelve ${addresses.size} direcciones")
                            // Garantiza actualización del estado en el hilo principal.
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
                    Toast.makeText(context, "Error al geocodificar ubicación: ${'$'}{e.message}", Toast.LENGTH_LONG).show()
                    showGpsLoadingModal = false
                }
            }

            // Intentar la última ubicación conocida
            fusedClient.lastLocation
                .addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        Log.d(GEO_TAG, "lastLocation disponible")
                        fillFromLocation(loc)
                    } else {
                        Log.d(GEO_TAG, "lastLocation nula, solicitando currentLocation")
                        // Si no hay, pedir la ubicación actual (mayor precisión)
                        val cts = CancellationTokenSource()
                        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                            .addOnSuccessListener { currentLoc: Location? ->
                                if (currentLoc != null) {
                                    Log.d(GEO_TAG, "currentLocation obtenida")
                                    fillFromLocation(currentLoc)
                                } else {
                                    Log.w(GEO_TAG, "currentLocation nula")
                                    Toast.makeText(context, "Ubicación no disponible", Toast.LENGTH_SHORT).show()
                                    showGpsLoadingModal = false
                                }
                            }
                            .addOnFailureListener { e: Exception ->
                                Log.e(GEO_TAG, "Error al obtener currentLocation", e)
                                e.printStackTrace()
                                Toast.makeText(context, "Error al obtener ubicación: ${'$'}{e.message}", Toast.LENGTH_LONG).show()
                                showGpsLoadingModal = false
                            }
                    }
                }
                .addOnFailureListener { e: Exception ->
                    Log.e(GEO_TAG, "Error al obtener lastLocation", e)
                    e.printStackTrace()
                    Toast.makeText(context, "Error al obtener última ubicación: ${'$'}{e.message}", Toast.LENGTH_LONG).show()
                    showGpsLoadingModal = false
                }

        } catch (e: Exception) {
            Log.e(GEO_TAG, "Error inicializando proveedor de ubicación", e)
            e.printStackTrace()
            Toast.makeText(context, "Error al inicializar proveedor de ubicación: ${'$'}{e.message}", Toast.LENGTH_LONG).show()
            showGpsLoadingModal = false
        }
    }

    // Launcher para solicitar permiso y reintentar automáticamente
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            // Si se concede, obtener la ubicación
            fetchAndFillLocationNoPermissionCheck()
        } else {
            Toast.makeText(context, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            showGpsLoadingModal = false
        }
    }

    // Función que lanza la petición de ubicación: si falta permiso, lo solicita; si no, obtiene la ubicación
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
                    text = stringResource(R.string.ocurrencia_delit_title),
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = carretera,
                        onValueChange = { carretera = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.ocurrencia_delit_carretera)) }
                    )
                    IconButton(
                        onClick = {
                            if (!showGpsLoadingModal) {
                                showGpsLoadingModal = true
                                coroutineScope.launch {
                                    // Garantiza que el modal se vea al menos 1 segundo antes de iniciar la geolocalización.
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
                            contentDescription = stringResource(R.string.ocurrencia_delit_select_location),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                OutlinedTextField(
                    value = pk,
                    onValueChange = { pk = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.ocurrencia_delit_pk)) }
                )

                OutlinedTextField(
                    value = localidad,
                    onValueChange = { localidad = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.ocurrencia_delit_localidad)) }
                )

                OutlinedTextField(
                    value = terminoMunicipal,
                    onValueChange = { terminoMunicipal = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.ocurrencia_delit_termino_municipal)) }
                )

                OutlinedTextField(
                    value = provincia,
                    onValueChange = { provincia = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.ocurrencia_delit_provincia)) }
                )

                // Fecha
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
                        label = { Text(stringResource(R.string.ocurrencia_delit_fecha)) }
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
                            contentDescription = stringResource(R.string.ocurrencia_delit_select_date),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Hora
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
                        label = { Text(stringResource(R.string.ocurrencia_delit_hora)) }
                    )
                    IconButton(
                        onClick = {
                            val initialHour = hora.split(":").getOrNull(0)?.toIntOrNull() ?: 0
                            val initialMinute = hora.split(":").getOrNull(1)?.toIntOrNull() ?: 0
                            TimePickerDialog(
                                context,
                                    { _, h, m -> hora = String.format(Locale.getDefault(), "%02d:%02d", h, m) },
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
                            contentDescription = stringResource(R.string.ocurrencia_delit_select_time),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // ← NUEVO: Grupo de radio buttons
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.ocurrencia_delit_derechos_moment_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Opción 1: En el mismo momento
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { derechosInformacionMomento = "mismo_momento" }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = derechosInformacionMomento == "mismo_momento",
                            onClick = { derechosInformacionMomento = "mismo_momento" }
                        )
                        Text(
                            text = stringResource(R.string.ocurrencia_delit_derechos_moment_same_time),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    // Opción 2: Lo más inmediato posible
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { derechosInformacionMomento = "inmediato" }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = derechosInformacionMomento == "inmediato",
                            onClick = { derechosInformacionMomento = "inmediato" }
                        )
                        Text(
                            text = stringResource(R.string.ocurrencia_delit_derechos_moment_immediate),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        storage.saveCurrent(
                            OcurrenciaDelitData(
                                carretera = carretera,
                                pk = pk,
                                localidad = localidad,
                                provincia = provincia,
                                terminoMunicipal = terminoMunicipal,
                                fecha = fecha,
                                hora = hora,
                                // ← NUEVO: Guardar el campo persistente
                                derechosInformacionMomento = derechosInformacionMomento
                            )
                        )
                        onBackClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.ocurrencia_delit_save))
                }

                Button(
                    onClick = {
                        storage.clearCurrent()
                        carretera = ""
                        pk = ""
                        localidad = ""
                        provincia = ""
                        terminoMunicipal = ""
                        fecha = ""
                        hora = ""
                        // ← NUEVO: Limpiar el campo
                        derechosInformacionMomento = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.ocurrencia_delit_delete))
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
            onDismissRequest = { },
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
                        Text(text = stringResource(R.string.ocurrencia_delit_detecting_location))
                    }
                }
            }
        }
    }

    // DatePickerDialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = fecha.toOcurrenciaDateMillisOrNull()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis
                            ?.toFormattedOcurrenciaDate()
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
}

private fun Long.toFormattedOcurrenciaDate(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(OCURRENCIA_DATE_FORMATTER)

private fun String.toOcurrenciaDateMillisOrNull(): Long? = runCatching {
    LocalDate.parse(this, OCURRENCIA_DATE_FORMATTER)
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

    // Candidatos combinados de todos los resultados para detectar municipio administrativo.
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

    Log.d(GEO_TAG, "resolveTerminoMunicipal -> localityCandidates=$localityCandidates, lineCandidates=$lineCandidates")

    // Prioridad alta: localidades de resultados secundarios (suele incluir municipio como Llanera).
    val fromLocalities = localityCandidates.firstOrNull()
    if (!fromLocalities.isNullOrBlank()) return fromLocalities

    // Priorizar candidatos que parezcan municipio dentro de nombres compuestos (ej. "Lugo de Llanera" -> "Llanera").
    val fromComposite = lineCandidates
        .mapNotNull { token ->
            Regex("(?i).+\\s+de\\s+(.+)$").find(token)?.groupValues?.getOrNull(1)?.trim()
        }
        .map(::cleanToken)
        .firstOrNull(::isValidMunicipioCandidate)

    if (!fromComposite.isNullOrBlank()) return fromComposite

    // 1) Intentar sacar municipio desde líneas de dirección (suele incluir token "Llanera").
    val fromLines = lineCandidates
        .asReversed()
        .firstOrNull(::isValidMunicipioCandidate)

    if (!fromLines.isNullOrBlank()) return fromLines

    // 2) Fallback: localidad o subLocality, evitando volver a provincia.
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
private fun DatosOcurrenciaDelitScreenPreview() {
    SinCarnetTheme {
        DatosOcurrenciaDelitScreen()
    }
}