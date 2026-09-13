# Documentacion tecnica del atestado completo v1.66.1

## Alcance

La version 1.66.1 añade trazabilidad local del dispositivo y control explicito
de la impresion del sello institucional en el atestado completo.

## Identificador de trazabilidad

La aplicacion obtiene `Settings.Secure.ANDROID_ID` en la primera ejecucion y
guarda unicamente sus cuatro ultimos caracteres en la preferencia local
`device_identifier_storage`, bajo la clave `device_identifier_suffix`.

El valor se presenta con el formato `ID: XXXX` en:

- Splash de inicio.
- Dialogo About.
- Sello del PDF, con el formato `Id XXXX` bajo el escudo de España.

La preferencia se excluye de las reglas de copia de seguridad de Android para
evitar trasladar el identificador a otro dispositivo. Android ID es estable
para la combinacion dispositivo, usuario y firma de la aplicacion; no es una
identidad criptografica global ni sustituye una firma digital del documento.

## Control del sello

En el dialogo `Sello de la unidad` se muestra el switch `¿Pintar el sello en
el PDF?`. Su estado se persiste en `SealUnitStorage` con la clave
`seal_enabled` y queda activado por defecto para conservar el comportamiento
anterior.

Cuando esta activado, el sello se genera con:

- Denominacion de unidad confirmada por el usuario.
- Escudo de España.
- Texto `Id XXXX` en cuerpo equivalente a 5 pt.

Cuando esta desactivado, no se crea ni se estampa el sello en:

- Portada.
- Resumen.
- Diligencias del atestado completo.
- Portada del anexo.
- Paginas de documentos escaneados.

## Generacion del paquete documental

1. Se validan provincia y numero de boletin.
2. Se generan las diligencias mediante `AtestadoContinuousPdfGenerator`.
3. Se generan portada, resumen y portada de anexo mediante
   `CompleteAtestadoFrontMatterPdfGenerator`.
4. Se incorporan los documentos escaneados mediante `PdfRenderer`.
5. `CompleteAtestadoPdfMerger` compone el PDF final y aplica el sello a las
   paginas escaneadas solo si el switch esta activado.
6. El resultado se guarda en `filesDir/atestados/AtestadoCompleto<boletin>.pdf`.

## Comprobacion funcional del PDF

La comprobacion debe hacerse generando dos atestados de prueba con los mismos
datos:

### Caso con sello

- Activar el switch.
- Generar el PDF completo.
- Confirmar que las paginas selladas muestran la unidad y `Id XXXX`.
- Confirmar que el mismo `XXXX` aparece en Splash y About.
- Cerrar y abrir la aplicacion, repetir la generacion y comprobar que la
  opcion permanece activada.

### Caso sin sello

- Desactivar el switch.
- Generar de nuevo el PDF completo.
- Confirmar visualmente que no aparece ningun sello, incluido en las paginas
  escaneadas.
- Cerrar y abrir la aplicacion y confirmar que la opcion permanece desactivada.

La comprobacion visual del PDF demuestra la trazabilidad operativa del origen
de la generacion. No constituye una firma digital ni garantiza por si sola la
integridad criptografica del PDF; para esa garantia seria necesario añadir una
firma digital de documento con certificado.

## Certificacion de la APK de produccion

La APK release se construye con R8 y con el keystore release configurado en
`keystore.properties`:

```powershell
./gradlew.bat :app:assembleRelease
```

El artefacto se genera en:

```text
app/build/outputs/apk/release/app-release.apk
```

La comprobacion de firma puede realizarse con `apksigner`:

```powershell
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
```

Debe confirmarse que la verificacion de firma es correcta y que el APK
contiene `versionName 1.66.1` y `versionCode 6`. El binario distribuible se
publica en la raiz como `SinCarnet_V.1.66.1.apk`.

## Archivos principales

```text
data/src/androidMain/kotlin/com/oscar/sincarnet/data/device/DeviceIdentifier.kt
data/src/androidMain/kotlin/com/oscar/sincarnet/data/pdf/InstitutionalSealRenderer.kt
data/src/androidMain/kotlin/com/oscar/sincarnet/data/pdf/CompleteAtestadoFrontMatterPdfGenerator.kt
data/src/androidMain/kotlin/com/oscar/sincarnet/data/pdf/CompleteAtestadoPdfMerger.kt
data/src/commonMain/kotlin/com/oscar/sincarnet/data/repository/SealUnitStorage.kt
presentation/src/androidMain/kotlin/com/oscar/sincarnet/GenerateCompleteAtestadoScreen.kt
presentation/src/androidMain/kotlin/com/oscar/sincarnet/MainViewModel.kt
```
