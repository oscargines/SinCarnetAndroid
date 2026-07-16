package com.oscar.sincarnet.navigation

/**
 * Rutas tipadas para Navigation Compose.
 *
 * Cada destino de la app se representa como un `data object` que encapsula
 * la cadena de ruta. Esto elimina el uso de strings mágicos en `MainActivity`
 * y permite autocompletado seguro en los callbacks de navegación.
 */
sealed class Route(val route: String) {
    data object Splash : Route("splash")
    data object Cases : Route("cases")
    data object ExpiredValidity : Route("expired_validity")
    data object JudicialSuspension : Route("judicial_suspension")
    data object WithoutPermit : Route("without_permit")
    data object SpecialCases : Route("special_cases")
    data object Courts : Route("courts")
    data object AtestadoData : Route("atestado_data")
    data object DocumentosComplementarios : Route("documentos_complementarios")
    data object ColaboracionAlcohol : Route("colaboracion_alcohol")
    data object ActaTrasladoVehiculo : Route("acta_traslado_vehiculo")
    data object ActaTrasladoVehiculoFirmas : Route("acta_traslado_vehiculo_firmas")
    data object ActaTrasladoVehiculoFirmaScreen : Route("acta_traslado_vehiculo_firma_screen")
    data object Etilometro : Route("etilometro")
    data object Drogas : Route("drogas")
    data object ColaboracionAlcoholFirmas : Route("colaboracion_alcohol_firmas")
    data object ColaboracionAlcoholFirmaScreen : Route("colaboracion_alcohol_firma_screen")
    data object AtestadoOcurrenciaDelit : Route("atestado_ocurrencia_delit")
    data object AtestadoPersonData : Route("atestado_person_data")
    data object AtestadoManifestacion : Route("atestado_manifestacion")
    data object AtestadoVehicleData : Route("atestado_vehicle_data")
    data object AtestadoCourtData : Route("atestado_court_data")
    data object AtestadoActingData : Route("atestado_acting_data")
    data object AtestadoSignatures : Route("atestado_signatures")
    data object FirmaScreen : Route("firma_screen")
    data object BluetoothPrinter : Route("bluetooth_printer")
    data object DocumentScanner : Route("document_scanner")
    data object CentroSanitario : Route("centro_sanitario")
    data object CentroSanitarioFirmas : Route("centro_sanitario_firmas")
    data object CentroSanitarioFirmaScreen : Route("centro_sanitario_firma_screen")
    data object OficioCustodiaSangre : Route("oficio_custodia_sangre")
    data object OficioCustodiaSangreDoc : Route("oficio_custodia_sangre_doc")
    data object OficioCustodiaSangreFirmas : Route("oficio_custodia_sangre_firmas")
    data object OficioCustodiaSangreFirmaScreen : Route("oficio_custodia_sangre_firma_screen")
    data object SolicitudCustodiaJuzgado : Route("solicitud_custodia_juzgado")
    data object SolicitudCustodiaJuzgadoDoc : Route("solicitud_custodia_juzgado_doc")
    data object SolicitudCustodiaJuzgadoFirmas : Route("solicitud_custodia_juzgado_firmas")
    data object SolicitudCustodiaJuzgadoFirmaScreen : Route("solicitud_custodia_juzgado_firma_screen")
}
