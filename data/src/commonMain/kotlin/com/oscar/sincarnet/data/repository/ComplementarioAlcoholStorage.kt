package com.oscar.sincarnet.data.repository

import com.oscar.sincarnet.data.PlatformStorage
import com.oscar.sincarnet.domain.model.ComplementarioAlcoholData

/**
 * Almacenamiento persistente del acta de colaboración con alcoholemia.
 *
 * Utiliza [PlatformStorage] para guardar los datos de forma clave-valor en
 * SharedPreferences (Android) o NSUserDefaults (iOS).
 */
class ComplementarioAlcoholStorage(private val storage: PlatformStorage) {

    fun loadCurrent(): ComplementarioAlcoholData = ComplementarioAlcoholData(
        fecha = storage.getString(KEY_FECHA, ""),
        hora = storage.getString(KEY_HORA, ""),
        lugar = storage.getString(KEY_LUGAR, ""),
        terminoMunicipal = storage.getString(KEY_TERMINO_MUNICIPAL, ""),
        partidoJudicial = storage.getString(KEY_PARTIDO_JUDICIAL, ""),
        cuerpoSolicitante = storage.getString(KEY_CUERPO_SOLICITANTE, ""),
        otroCuerpo = storage.getString(KEY_OTRO_CUERPO, ""),
        agenteSolicitanteId = storage.getString(KEY_AGENTE_SOLICITANTE_ID, ""),
        
        pruebasAlcohol = storage.getBoolean(KEY_PRUEBAS_ALCOHOL, false),
        pruebasDrogas = storage.getBoolean(KEY_PRUEBAS_DROGAS, false),
        caso = storage.getString(KEY_CASO, ""),
        
        tipoUnidad = storage.getString(KEY_TIPO_UNIDAD, ""),
        unidadNombre = storage.getString(KEY_UNIDAD_NOMBRE, ""),
        diligenciasExpediente = storage.getString(KEY_DILIGENCIAS_EXPEDIENTE, ""),
        
        motivoSiniestroVial = storage.getBoolean(KEY_MOTIVO_SINIESTRO_VIAL, false),
        motivoSignosEvidentes = storage.getBoolean(KEY_MOTIVO_SIGNOS_EVIDENTES, false),
        motivoRequerimientoJudicial = storage.getBoolean(KEY_MOTIVO_REQUERIMIENTO_JUDICIAL, false),
        motivoInfraccionTrafico = storage.getBoolean(KEY_MOTIVO_INFRACCION_TRAFICO, false),
        motivoControlPreventivo = storage.getBoolean(KEY_MOTIVO_CONTROL_PREVENTIVO, false),
        motivoOtros = storage.getBoolean(KEY_MOTIVO_OTROS, false),
        motivoOtrosDetalle = storage.getString(KEY_MOTIVO_OTROS_DETALLE, ""),
        
        personaNombre = storage.getString(KEY_PERSONA_NOMBRE, ""),
        personaDni = storage.getString(KEY_PERSONA_DNI, ""),
        personaFechaExpedicion = storage.getString(KEY_PERSONA_FECHA_EXPEDICION, ""),
        personaFechaNacimiento = storage.getString(KEY_PERSONA_FECHA_NACIMIENTO, ""),
        personaDomicilio = storage.getString(KEY_PERSONA_DOMICILIO, ""),
        personaTelefono = storage.getString(KEY_PERSONA_TELEFONO, ""),
        
        enCalidadDe = storage.getString(KEY_EN_CALIDAD_DE, ""),
        enCalidadOtros = storage.getString(KEY_EN_CALIDAD_OTROS, ""),
        
        vehiculoMarca = storage.getString(KEY_VEHICULO_MARCA, ""),
        vehiculoModelo = storage.getString(KEY_VEHICULO_MODELO, ""),
        vehiculoMatricula = storage.getString(KEY_VEHICULO_MATRICULA, ""),
        
        operadorTip = storage.getString(KEY_OPERADOR_TIP, ""),
        operadorUnidad = storage.getString(KEY_OPERADOR_UNIDAD, ""),
        tipoConductor = storage.getString(KEY_TIPO_CONDUCTOR, ""),
        
        etilometroMarca = storage.getString(KEY_ETILOMETRO_MARCA, ""),
        etilometroModelo = storage.getString(KEY_ETILOMETRO_MODELO, ""),
        etilometroNumeroSerie = storage.getString(KEY_ETILOMETRO_NUMERO_SERIE, ""),
        etilometroHoraVerificacion = storage.getString(KEY_ETILOMETRO_HORA_VERIFICACION, ""),
        etilometroAgente = storage.getString(KEY_ETILOMETRO_AGENTE, ""),
        
        lectorDrogasMarca = storage.getString(KEY_LECTOR_DROGAS_MARCA, ""),
        lectorDrogasModelo = storage.getString(KEY_LECTOR_DROGAS_MODELO, ""),
        lectorDrogasNumeroSerie = storage.getString(KEY_LECTOR_DROGAS_NUMERO_SERIE, ""),
        lectorDrogasHoraVerificacion = storage.getString(KEY_LECTOR_DROGAS_HORA_VERIFICACION, ""),
        lectorDrogasAgente = storage.getString(KEY_LECTOR_DROGAS_AGENTE, ""),
        
        primeraPruebaHora = storage.getString(KEY_PRIMERA_PRUEBA_HORA, ""),
        primeraPruebaResultado = storage.getString(KEY_PRIMERA_PRUEBA_RESULTADO, ""),
        segundaPruebaHora = storage.getString(KEY_SEGUNDA_PRUEBA_HORA, ""),
        segundaPruebaResultado = storage.getString(KEY_SEGUNDA_PRUEBA_RESULTADO, ""),
        pruebaContrasteAlcohol = storage.getString(KEY_PRUEBA_CONTRASTE_ALCOHOL, ""),
        
        pruebaDrogasIndiciaría = storage.getBoolean(KEY_PRUEBA_DROGAS_INDICIARÍA, false),
        pruebaDrogasResultado = storage.getString(KEY_PRUEBA_DROGAS_RESULTADO, ""),
        pruebaDrogasTipo = storage.getString(KEY_PRUEBA_DROGAS_TIPO, ""),
        pruebaDrogasContraste = storage.getString(KEY_PRUEBA_DROGAS_CONTRASTE, "")
    )

    fun saveCurrent(data: ComplementarioAlcoholData) {
        storage.putString(KEY_FECHA, data.fecha)
        storage.putString(KEY_HORA, data.hora)
        storage.putString(KEY_LUGAR, data.lugar)
        storage.putString(KEY_TERMINO_MUNICIPAL, data.terminoMunicipal)
        storage.putString(KEY_PARTIDO_JUDICIAL, data.partidoJudicial)
        storage.putString(KEY_CUERPO_SOLICITANTE, data.cuerpoSolicitante)
        storage.putString(KEY_OTRO_CUERPO, data.otroCuerpo)
        storage.putString(KEY_AGENTE_SOLICITANTE_ID, data.agenteSolicitanteId)
        
        storage.putBoolean(KEY_PRUEBAS_ALCOHOL, data.pruebasAlcohol)
        storage.putBoolean(KEY_PRUEBAS_DROGAS, data.pruebasDrogas)
        storage.putString(KEY_CASO, data.caso)
        
        storage.putString(KEY_TIPO_UNIDAD, data.tipoUnidad)
        storage.putString(KEY_UNIDAD_NOMBRE, data.unidadNombre)
        storage.putString(KEY_DILIGENCIAS_EXPEDIENTE, data.diligenciasExpediente)
        
        storage.putBoolean(KEY_MOTIVO_SINIESTRO_VIAL, data.motivoSiniestroVial)
        storage.putBoolean(KEY_MOTIVO_SIGNOS_EVIDENTES, data.motivoSignosEvidentes)
        storage.putBoolean(KEY_MOTIVO_REQUERIMIENTO_JUDICIAL, data.motivoRequerimientoJudicial)
        storage.putBoolean(KEY_MOTIVO_INFRACCION_TRAFICO, data.motivoInfraccionTrafico)
        storage.putBoolean(KEY_MOTIVO_CONTROL_PREVENTIVO, data.motivoControlPreventivo)
        storage.putBoolean(KEY_MOTIVO_OTROS, data.motivoOtros)
        storage.putString(KEY_MOTIVO_OTROS_DETALLE, data.motivoOtrosDetalle)
        
        storage.putString(KEY_PERSONA_NOMBRE, data.personaNombre)
        storage.putString(KEY_PERSONA_DNI, data.personaDni)
        storage.putString(KEY_PERSONA_FECHA_EXPEDICION, data.personaFechaExpedicion)
        storage.putString(KEY_PERSONA_FECHA_NACIMIENTO, data.personaFechaNacimiento)
        storage.putString(KEY_PERSONA_DOMICILIO, data.personaDomicilio)
        storage.putString(KEY_PERSONA_TELEFONO, data.personaTelefono)
        
        storage.putString(KEY_EN_CALIDAD_DE, data.enCalidadDe)
        storage.putString(KEY_EN_CALIDAD_OTROS, data.enCalidadOtros)
        
        storage.putString(KEY_VEHICULO_MARCA, data.vehiculoMarca)
        storage.putString(KEY_VEHICULO_MODELO, data.vehiculoModelo)
        storage.putString(KEY_VEHICULO_MATRICULA, data.vehiculoMatricula)
        
        storage.putString(KEY_OPERADOR_TIP, data.operadorTip)
        storage.putString(KEY_OPERADOR_UNIDAD, data.operadorUnidad)
        storage.putString(KEY_TIPO_CONDUCTOR, data.tipoConductor)
        
        storage.putString(KEY_ETILOMETRO_MARCA, data.etilometroMarca)
        storage.putString(KEY_ETILOMETRO_MODELO, data.etilometroModelo)
        storage.putString(KEY_ETILOMETRO_NUMERO_SERIE, data.etilometroNumeroSerie)
        storage.putString(KEY_ETILOMETRO_HORA_VERIFICACION, data.etilometroHoraVerificacion)
        storage.putString(KEY_ETILOMETRO_AGENTE, data.etilometroAgente)
        
        storage.putString(KEY_LECTOR_DROGAS_MARCA, data.lectorDrogasMarca)
        storage.putString(KEY_LECTOR_DROGAS_MODELO, data.lectorDrogasModelo)
        storage.putString(KEY_LECTOR_DROGAS_NUMERO_SERIE, data.lectorDrogasNumeroSerie)
        storage.putString(KEY_LECTOR_DROGAS_HORA_VERIFICACION, data.lectorDrogasHoraVerificacion)
        storage.putString(KEY_LECTOR_DROGAS_AGENTE, data.lectorDrogasAgente)
        
        storage.putString(KEY_PRIMERA_PRUEBA_HORA, data.primeraPruebaHora)
        storage.putString(KEY_PRIMERA_PRUEBA_RESULTADO, data.primeraPruebaResultado)
        storage.putString(KEY_SEGUNDA_PRUEBA_HORA, data.segundaPruebaHora)
        storage.putString(KEY_SEGUNDA_PRUEBA_RESULTADO, data.segundaPruebaResultado)
        storage.putString(KEY_PRUEBA_CONTRASTE_ALCOHOL, data.pruebaContrasteAlcohol)
        
        storage.putBoolean(KEY_PRUEBA_DROGAS_INDICIARÍA, data.pruebaDrogasIndiciaría)
        storage.putString(KEY_PRUEBA_DROGAS_RESULTADO, data.pruebaDrogasResultado)
        storage.putString(KEY_PRUEBA_DROGAS_TIPO, data.pruebaDrogasTipo)
        storage.putString(KEY_PRUEBA_DROGAS_CONTRASTE, data.pruebaDrogasContraste)
    }

    fun clearCurrent() {
        storage.remove(KEY_FECHA)
        storage.remove(KEY_HORA)
        storage.remove(KEY_LUGAR)
        storage.remove(KEY_TERMINO_MUNICIPAL)
        storage.remove(KEY_PARTIDO_JUDICIAL)
        storage.remove(KEY_CUERPO_SOLICITANTE)
        storage.remove(KEY_OTRO_CUERPO)
        storage.remove(KEY_AGENTE_SOLICITANTE_ID)
         
        storage.remove(KEY_PRUEBAS_ALCOHOL)
        storage.remove(KEY_PRUEBAS_DROGAS)
        storage.remove(KEY_CASO)
         
        storage.remove(KEY_TIPO_UNIDAD)
        storage.remove(KEY_UNIDAD_NOMBRE)
        storage.remove(KEY_DILIGENCIAS_EXPEDIENTE)
         
        storage.remove(KEY_MOTIVO_SINIESTRO_VIAL)
        storage.remove(KEY_MOTIVO_SIGNOS_EVIDENTES)
        storage.remove(KEY_MOTIVO_REQUERIMIENTO_JUDICIAL)
        storage.remove(KEY_MOTIVO_INFRACCION_TRAFICO)
        storage.remove(KEY_MOTIVO_CONTROL_PREVENTIVO)
        storage.remove(KEY_MOTIVO_OTROS)
        storage.remove(KEY_MOTIVO_OTROS_DETALLE)
         
        storage.remove(KEY_PERSONA_NOMBRE)
        storage.remove(KEY_PERSONA_DNI)
        storage.remove(KEY_PERSONA_FECHA_EXPEDICION)
        storage.remove(KEY_PERSONA_FECHA_NACIMIENTO)
        storage.remove(KEY_PERSONA_DOMICILIO)
        storage.remove(KEY_PERSONA_TELEFONO)
        
        storage.remove(KEY_EN_CALIDAD_DE)
        storage.remove(KEY_EN_CALIDAD_OTROS)
        
        storage.remove(KEY_VEHICULO_MARCA)
        storage.remove(KEY_VEHICULO_MODELO)
        storage.remove(KEY_VEHICULO_MATRICULA)
        
        storage.remove(KEY_OPERADOR_TIP)
        storage.remove(KEY_OPERADOR_UNIDAD)
        storage.remove(KEY_TIPO_CONDUCTOR)
        
        storage.remove(KEY_ETILOMETRO_MARCA)
        storage.remove(KEY_ETILOMETRO_MODELO)
        storage.remove(KEY_ETILOMETRO_NUMERO_SERIE)
        storage.remove(KEY_ETILOMETRO_HORA_VERIFICACION)
        storage.remove(KEY_ETILOMETRO_AGENTE)
        
        storage.remove(KEY_LECTOR_DROGAS_MARCA)
        storage.remove(KEY_LECTOR_DROGAS_MODELO)
        storage.remove(KEY_LECTOR_DROGAS_NUMERO_SERIE)
        storage.remove(KEY_LECTOR_DROGAS_HORA_VERIFICACION)
        storage.remove(KEY_LECTOR_DROGAS_AGENTE)
        
        storage.remove(KEY_PRIMERA_PRUEBA_HORA)
        storage.remove(KEY_PRIMERA_PRUEBA_RESULTADO)
        storage.remove(KEY_SEGUNDA_PRUEBA_HORA)
        storage.remove(KEY_SEGUNDA_PRUEBA_RESULTADO)
        storage.remove(KEY_PRUEBA_CONTRASTE_ALCOHOL)
        
        storage.remove(KEY_PRUEBA_DROGAS_INDICIARÍA)
        storage.remove(KEY_PRUEBA_DROGAS_RESULTADO)
        storage.remove(KEY_PRUEBA_DROGAS_TIPO)
        storage.remove(KEY_PRUEBA_DROGAS_CONTRASTE)
    }

    private companion object {
        const val KEY_FECHA = "fecha"
        const val KEY_HORA = "hora"
        const val KEY_LUGAR = "lugar"
        const val KEY_TERMINO_MUNICIPAL = "termino_municipal"
        const val KEY_PARTIDO_JUDICIAL = "partido_judicial"
        const val KEY_CUERPO_SOLICITANTE = "cuerpo_solicitante"
        const val KEY_OTRO_CUERPO = "otro_cuerpo"
        const val KEY_AGENTE_SOLICITANTE_ID = "agente_solicitante_id"
        
        const val KEY_PRUEBAS_ALCOHOL = "pruebas_alcohol"
        const val KEY_PRUEBAS_DROGAS = "pruebas_drogas"
        const val KEY_CASO = "caso"
        
        const val KEY_TIPO_UNIDAD = "tipo_unidad"
        const val KEY_UNIDAD_NOMBRE = "unidad_nombre"
        const val KEY_DILIGENCIAS_EXPEDIENTE = "diligencias_expediente"
        
        const val KEY_MOTIVO_SINIESTRO_VIAL = "motivo_siniestro_vial"
        const val KEY_MOTIVO_SIGNOS_EVIDENTES = "motivo_signos_evidentes"
        const val KEY_MOTIVO_REQUERIMIENTO_JUDICIAL = "motivo_requerimiento_judicial"
        const val KEY_MOTIVO_INFRACCION_TRAFICO = "motivo_infraccion_trafico"
        const val KEY_MOTIVO_CONTROL_PREVENTIVO = "motivo_control_preventivo"
        const val KEY_MOTIVO_OTROS = "motivo_otros"
        const val KEY_MOTIVO_OTROS_DETALLE = "motivo_otros_detalle"
        
        const val KEY_PERSONA_NOMBRE = "persona_nombre"
        const val KEY_PERSONA_DNI = "persona_dni"
        const val KEY_PERSONA_FECHA_EXPEDICION = "persona_fecha_expedicion"
        const val KEY_PERSONA_FECHA_NACIMIENTO = "persona_fecha_nacimiento"
        const val KEY_PERSONA_DOMICILIO = "persona_domicilio"
        const val KEY_PERSONA_TELEFONO = "persona_telefono"
        
        const val KEY_EN_CALIDAD_DE = "en_calidad_de"
        const val KEY_EN_CALIDAD_OTROS = "en_calidad_otros"
        
        const val KEY_VEHICULO_MARCA = "vehiculo_marca"
        const val KEY_VEHICULO_MODELO = "vehiculo_modelo"
        const val KEY_VEHICULO_MATRICULA = "vehiculo_matricula"
        
        const val KEY_OPERADOR_TIP = "operador_tip"
        const val KEY_OPERADOR_UNIDAD = "operador_unidad"
        const val KEY_TIPO_CONDUCTOR = "tipo_conductor"
        
        const val KEY_ETILOMETRO_MARCA = "etilometro_marca"
        const val KEY_ETILOMETRO_MODELO = "etilometro_modelo"
        const val KEY_ETILOMETRO_NUMERO_SERIE = "etilometro_numero_serie"
        const val KEY_ETILOMETRO_HORA_VERIFICACION = "etilometro_hora_verificacion"
        const val KEY_ETILOMETRO_AGENTE = "etilometro_agente"
        
        const val KEY_LECTOR_DROGAS_MARCA = "lector_drogas_marca"
        const val KEY_LECTOR_DROGAS_MODELO = "lector_drogas_modelo"
        const val KEY_LECTOR_DROGAS_NUMERO_SERIE = "lector_drogas_numero_serie"
        const val KEY_LECTOR_DROGAS_HORA_VERIFICACION = "lector_drogas_hora_verificacion"
        const val KEY_LECTOR_DROGAS_AGENTE = "lector_drogas_agente"
        
        const val KEY_PRIMERA_PRUEBA_HORA = "primera_prueba_hora"
        const val KEY_PRIMERA_PRUEBA_RESULTADO = "primera_prueba_resultado"
        const val KEY_SEGUNDA_PRUEBA_HORA = "segunda_prueba_hora"
        const val KEY_SEGUNDA_PRUEBA_RESULTADO = "segunda_prueba_resultado"
        const val KEY_PRUEBA_CONTRASTE_ALCOHOL = "prueba_contraste_alcohol"
        
        const val KEY_PRUEBA_DROGAS_INDICIARÍA = "prueba_drogas_indiciaría"
        const val KEY_PRUEBA_DROGAS_RESULTADO = "prueba_drogas_resultado"
        const val KEY_PRUEBA_DROGAS_TIPO = "prueba_drogas_tipo"
        const val KEY_PRUEBA_DROGAS_CONTRASTE = "prueba_drogas_contraste"
    }
}
