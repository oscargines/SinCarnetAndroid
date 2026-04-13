# SinCarnet Android

Aplicación Android desarrollada en Kotlin y Jetpack Compose para consulta operativa de supuestos relacionados con la conducción sin permiso o licencia en vigor, generación de atestados y documentación de apoyo.

> **Aviso**  
> Esta aplicación es una herramienta de consulta y apoyo operativo. No sustituye normativa oficial, instrucciones internas, resoluciones judiciales ni criterio jurídico profesional.

## Descripción

`SinCarnet` está orientada a un flujo de trabajo rápido desde dispositivo móvil para:

- consultar casuística habitual,
- recoger datos del atestado,
- generar documentación en **PDF** y **ODT**,
- capturar firmas,
- escanear documentación,
- consultar juzgados,
- y preparar documentos listos para compartir o imprimir.

## Funcionalidades principales

### Consulta de casuística

La app contempla, entre otras, estas ramas principales:

- **Pérdida de vigencia**
- **Suspensión judicial**
- **Carecer de permiso**
- **Casos especiales**

Cada flujo muestra una resolución visual y observaciones operativas asociadas.

### Generación de atestado

Incluye un flujo guiado para recopilar:

- lugar y hora,
- datos de la persona investigada,
- datos del vehículo,
- datos del juzgado,
- datos de actuantes,
- firmas manuscritas,
- y anexos/documentación escaneada.

La salida puede generarse en:

- **PDF**
- **ODT** con formato de plantilla conservado

### Firma y documentación

- Captura de firma manuscrita en pantalla
- Exportación de documentos
- Compartición de PDF y ODT
- Impresión de determinados documentos

### Lectura y ayuda operativa

- Lectura de derechos
- Consulta de juzgados
- Escaneado de documentos
- Integración NFC para lectura documental (según disponibilidad/configuración)
- Búsqueda y uso de impresoras Bluetooth compatibles

## Estado actual de la release

Versión configurada actualmente en el proyecto:

- **versionName:** `1.30.00`
- **applicationId:** `com.oscar.sincarnet`

APK firmado disponible en el directorio de release:

- [`app/build/outputs/apk/release/SinCarnet.V.1.30.apk`](app/build/outputs/apk/release/SinCarnet.V.1.30.apk)

APK de salida estándar de Gradle:

- `app/build/outputs/apk/release/app-release.apk`

## Stack técnico

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Arquitectura de UI:** actividad única con navegación controlada por estado
- **Build system:** Gradle Kotlin DSL
- **SDK actual:**
  - `minSdk = 31`
  - `targetSdk = 35`
  - `compileSdk = 35`

Archivo principal de configuración:

- `app/build.gradle.kts`

## Estructura relevante del proyecto

```text
SinCarnetAndroid/
├─ app/
│  ├─ build.gradle.kts
│  └─ src/main/
│     ├─ assets/
│     │  └─ docs/
│     ├─ java/com/oscar/sincarnet/
│     │  ├─ MainActivity.kt
│     │  ├─ FirmasAtestadoScreen.kt
│     │  ├─ AtestadoContinuousPdfGenerator.kt
│     │  └─ ...
│     └─ res/values/
│        └─ strings.xml
├─ keystore/
├─ keystore.properties
└─ README.md
```

## Archivos clave

- `app/src/main/java/com/oscar/sincarnet/MainActivity.kt`
  - punto central de navegación y flujo principal
- `app/src/main/java/com/oscar/sincarnet/FirmasAtestadoScreen.kt`
  - captura y gestión de firmas
- `app/src/main/java/com/oscar/sincarnet/AtestadoContinuousPdfGenerator.kt`
  - generación de documentos PDF y ODT
- `app/src/main/res/values/strings.xml`
  - textos y mensajes de la aplicación

## Compilación

### Debug

```powershell
./gradlew assembleDebug
```

### Release firmada

```powershell
./gradlew assembleRelease
```

Si la configuración de firma está disponible, el APK firmado se genera en:

```text
app/build/outputs/apk/release/
```

## Firma de la aplicación

El proyecto está preparado para compilar `release` firmado mediante:

- `keystore.properties`
- `keystore/sincarnet-release.jks`

La configuración de firma se define en `app/build.gradle.kts`.

## Requisitos

- Android Studio / entorno Gradle compatible
- SDK de Android configurado en `local.properties`
- JDK compatible con el proyecto

## Notas

- El repositorio contiene material de apoyo, recursos y documentación técnica adicional.
- La app ha evolucionado para soportar exportación ODT con plantilla y maquetación conservada.
- El APK firmado renombrado para distribución actual es `SinCarnet.V.1.30.apk`.

## Licencia y uso

Proyecto de uso personal / operativo. Recomendable revisar y adaptar cualquier uso institucional o profesional conforme a la normativa y políticas internas aplicables.

