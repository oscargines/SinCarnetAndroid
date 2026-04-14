<div align="center">

# SinCarnet Android

### Herramienta operativa para la gestión de atestados por conducción sin permiso o licencia en vigor

[![Platform](https://img.shields.io/badge/Platform-Android%2012%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Version](https://img.shields.io/badge/Version-1.30.00-blue)](app/build.gradle.kts)
[![Docs](https://img.shields.io/badge/API%20Docs-Dokka%20V2-orange)](docs/api/index.html)
[![License](https://img.shields.io/badge/License-Uso%20interno-lightgrey)](#licencia-y-uso)

</div>

---

## Índice

1. [Descripción del proyecto](#1-descripción-del-proyecto)
2. [Advertencia de uso](#2-advertencia-de-uso)
3. [Funcionalidades](#3-funcionalidades)
4. [Arquitectura](#4-arquitectura)
5. [Stack tecnológico](#5-stack-tecnológico)
6. [Estructura del repositorio](#6-estructura-del-repositorio)
7. [Compilación y distribución](#7-compilación-y-distribución)
8. [Firma de la aplicación](#8-firma-de-la-aplicación)
9. [Documentación API](#9-documentación-api)
10. [Requisitos del entorno](#10-requisitos-del-entorno)
11. [Licencia y uso](#11-licencia-y-uso)

---

## 1. Descripción del proyecto

**SinCarnet** es una aplicación Android nativa desarrollada en **Kotlin** con **Jetpack Compose** que proporciona a los agentes actuantes un flujo de trabajo integral para los supuestos de conducción sin permiso o licencia en vigor.

La aplicación centraliza en un único dispositivo móvil las tareas operativas de:

- consulta de casuística normativa y jurisprudencial,
- recogida estructurada de datos del atestado,
- generación de documentación oficial en formatos **PDF** y **ODT**,
- captura de firmas manuscritas,
- escaneado de documentos identificativos,
- lectura de DNI electrónico mediante **NFC**,
- impresión directa sobre impresoras **Zebra** vía **Bluetooth**.

> El objetivo es reducir el tiempo de tramitación en campo, minimizar errores de transcripción y garantizar la consistencia formal de la documentación generada.

---

## 2. Advertencia de uso

> ⚠️ **Aviso importante**
>
> Esta aplicación es una herramienta de consulta y apoyo operativo. **No sustituye** la normativa oficial vigente, las instrucciones de servicio internas, las resoluciones judiciales ni el criterio jurídico profesional de los agentes intervinientes. Su uso queda bajo la responsabilidad exclusiva del usuario.

---

## 3. Funcionalidades

### 3.1 Consulta de casuística

El módulo de consulta implementa un árbol de decisión interactivo que cubre los supuestos más habituales:

| Rama | Descripción |
|------|-------------|
| **Pérdida de vigencia** | Permiso caducado o no renovado |
| **Suspensión judicial** | Privación cautelar o definitiva por resolución judicial |
| **Carecer de permiso** | Nunca obtuvo habilitación |
| **Casos especiales** | Permisos extranjeros, vehículos especiales, etc. |

Cada nodo del árbol devuelve una **resolución visual** con observaciones operativas, referencias normativas y recomendaciones de diligencia.

### 3.2 Generación de atestado

Flujo guiado de recogida de datos que abarca:

- datos de lugar, hora y circunstancias del hecho,
- datos de la persona investigada,
- datos del vehículo implicado,
- juzgado competente (consultado desde base de datos interna),
- datos de los funcionarios actuantes,
- firmas manuscritas de todas las partes,
- anexos de documentación escaneada.

La salida documental puede generarse como:

- **PDF** — atestado y diligencias en formato continuo o simple,
- **ODT** — con plantilla institucional y maquetación conservada.

### 3.3 Firma digital manuscrita

- Canvas interactivo para la captura de firma en pantalla táctil.
- Exportación de la firma como imagen integrada en el documento.
- Soporte de firmas múltiples (instructor, secretario, investigado).

### 3.4 Lectura NFC de DNI electrónico

- Integración con `dniedroid` para la lectura del chip del DNI-e y NIE.
- Extracción automática de datos del titular al atestado.
- Gestión del ciclo de vida de la etiqueta NFC mediante repositorio singleton.

### 3.5 Impresión Bluetooth (Zebra)

- Detección automática de impresoras Zebra emparejadas.
- Validación de modelo mediante protocolo **SGD**.
- Generación de comandos **CPCL** para impresión de diligencias.
- Política de compatibilidad configurable por modelo.

### 3.6 Escaneado de documentos

- Captura mediante **CameraX**.
- Corrección de perspectiva y recorte automático.
- Adjunto de imágenes al atestado.

---

## 4. Arquitectura

```
┌──────────────────────────────────────────────────────────┐
│                       UI Layer                           │
│  Jetpack Compose · Screens · SharedComponents · Theme    │
└───────────────────────┬──────────────────────────────────┘
                        │ State / Events
┌───────────────────────▼──────────────────────────────────┐
│                   Application Core                        │
│  MainActivity · NavigationState · DocumentPrinter        │
└──────┬────────────────┬──────────────────┬───────────────┘
       │                │                  │
┌──────▼──────┐  ┌──────▼──────┐  ┌───────▼───────┐
│  Generación │  │ Persistencia│  │   Hardware    │
│  documental │  │  (Storage)  │  │   (NFC/BT)    │
│             │  │             │  │               │
│ PDF/ODT     │  │SharedPrefs  │  │ NfcTagRepo    │
│ Generators  │  │ + SQLite    │  │ BluetoothUtils│
└─────────────┘  └─────────────┘  └───────────────┘
```

### Principios de diseño

- **Actividad única** (`MainActivity`) con navegación controlada por estado Compose.
- **Storage managers** por dominio (`ActuantesStorage`, `PersonaInvestigadaStorage`, etc.) sobre `SharedPreferences`.
- **Data sources SQLite** para juzgados y nacionalidades (`.db` assets empaquetados).
- **Repositorio NFC singleton** (`NfcTagRepository`) con gestión del ciclo de vida.
- **Generación documental separada** del modelo: `AtestadoContinuousPdfGenerator` y `AtestadoPdfGenerator`.

---

## 5. Stack tecnológico

| Componente | Tecnología | Versión |
|------------|-----------|---------|
| Lenguaje | Kotlin | JVM 11 |
| UI Toolkit | Jetpack Compose + Material 3 | BOM estable |
| Build System | Gradle Kotlin DSL | — |
| `minSdk` | Android 12 | API 31 |
| `targetSdk` / `compileSdk` | Android 15 | API 35 |
| Cámara | CameraX | 1.3.4 |
| Criptografía | BouncyCastle (`bcprov-jdk15on`) | 1.50 |
| DNI electrónico | dniedroid + jmulticard | 2.0 |
| Localización | Google Play Services Location | 21.0.1 |
| Impresión | CPCL / Zebra SDK (local AAR) | — |
| Documentación | Dokka | V2 (2.0.0) |

---

## 6. Estructura del repositorio

```text
SinCarnetAndroid/
├── app/
│   ├── build.gradle.kts          # Configuración del módulo + Dokka
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── assets/               # Bases de datos SQLite y plantillas ODT
│       └── java/com/oscar/sincarnet/
│           ├── MainActivity.kt               # Orquestador principal
│           ├── DocumentPrinter.kt            # Coordinador de impresión
│           ├── AtestadoContinuousPdfGenerator.kt  # Generación PDF/ODT
│           ├── BluetoothPrinterUtils.kt      # Render CPCL + Bluetooth
│           ├── NfcDniReader.kt               # Lectura DNI-e
│           ├── data/                         # Repositorios y data sources
│           ├── model/                        # Data classes del dominio
│           ├── storage/                      # Storage managers
│           ├── ui/
│           │   ├── screens/                  # Pantallas Compose
│           │   ├── components/               # Componentes reutilizables
│           │   └── theme/                    # Color · Type · Theme
│           └── utils/                        # Utilidades transversales
├── docs/
│   └── api/                      # Documentación KDoc generada (Dokka V2)
│       └── index.html
├── keystore/
│   └── sincarnet-release.jks     # Keystore de firma release
├── build.gradle.kts              # Configuración raíz
├── settings.gradle.kts
├── gradle/
│   └── libs.versions.toml        # Version catalog
├── keystore.properties           # Credenciales de firma (no en VCS)
├── local.properties              # SDK local (no en VCS)
└── README.md
```

---

## 7. Compilación y distribución

### 7.1 Build de depuración

```powershell
./gradlew assembleDebug
```

### 7.2 Validación técnica completa

Ejecuta compilación, tests unitarios y publicación de documentación en un único paso:

```powershell
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:publishDokkaToDocs
```

### 7.3 Build de release firmado

```powershell
./gradlew assembleRelease
```

El APK firmado se genera en:

```text
app/build/outputs/apk/release/SinCarnet.V.1.30.apk
```

> Es necesario que `keystore.properties` esté correctamente configurado con la ruta y credenciales del keystore antes de ejecutar el build de release.

---

## 8. Firma de la aplicación

La configuración de firma utiliza el mecanismo estándar de Gradle con separación de credenciales:

| Fichero | Propósito |
|---------|-----------|
| `keystore/sincarnet-release.jks` | Keystore JKS con la clave de firma |
| `keystore.properties` | Alias, contraseñas y ruta del keystore (**no versionar**) |
| `app/build.gradle.kts` | Bloque `signingConfigs` que consume las propiedades |

El archivo `keystore.properties` debe excluirse del control de versiones (`.gitignore`) para proteger las credenciales de firma.

---

## 9. Documentación API

El proyecto genera documentación de la API interna mediante **Dokka V2** a partir de los comentarios **KDoc** presentes en el código fuente.

### 9.1 Cobertura documental

| Métrica | Valor |
|---------|-------|
| Archivos Kotlin documentados | 61 / 61 |
| Cobertura KDoc | **100 %** |
| Líneas de KDoc | ~2 300+ |
| Herramienta | Dokka V2 (2.0.0) |

### 9.2 Consultar la documentación

La documentación publicada y versionada en el repositorio está disponible en:

```text
docs/api/index.html
```

### 9.3 Regenerar y publicar la documentación

```powershell
# Genera HTML y copia la salida a docs/api/
./gradlew :app:publishDokkaToDocs
```

```powershell
# Solo generación local (salida en app/build/dokka/html/)
./gradlew :app:dokkaGeneratePublicationHtml
```

Tras regenerar, añadir la carpeta `docs/api/` al commit:

```powershell
git add docs/api README.md
git commit -m "docs: regenera documentación Dokka V2"
git push
```

> La carpeta `app/build/` es temporal y **no debe versionarse**.

---

## 10. Requisitos del entorno

| Requisito | Versión mínima recomendada |
|-----------|---------------------------|
| Android Studio | Hedgehog 2023.1.1+ |
| JDK | 11 (compatible con Gradle) |
| Android SDK | API 31 – 35 instaladas |
| Gradle | Según `gradle/wrapper/gradle-wrapper.properties` |
| `local.properties` | `sdk.dir` apuntando al SDK local |

---

## 11. Licencia y uso

Proyecto de **uso interno / operativo**. Cualquier reutilización, adaptación o despliegue institucional debe realizarse conforme a la normativa aplicable y a las políticas internas del organismo correspondiente.

---

<div align="center">

**SinCarnet Android** · v1.30.00 · API 31–35 · Kotlin · Jetpack Compose

</div>
