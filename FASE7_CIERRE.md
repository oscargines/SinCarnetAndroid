# Cierre de Fase 7 – Denominación de juzgados (LO 1/2025) y mejoras

Fecha: 2026-08-10

## Objetivo

Adaptar la aplicación a la nueva organización judicial introducida por la
**Ley Orgánica 1/2025, de 2 de enero, de medidas en materia de eficiencia del
Servicio Público de Justicia**, que modifica la denominación de los antiguos
juzgados unipersonales.

---

## Cambios aplicados

### 1. Actualización de denominación de juzgados

| Denominación anterior | Denominación vigente (LO 1/2025) |
|----------------------|----------------------------------|
| Juzgado de Instrucción | Plaza nº X de la **Sección de Instrucción del Tribunal de Instancia** |
| Juzgado de lo Penal | Plaza nº X de la **Sección de lo Penal del Tribunal de Instancia** |
| Juzgado de Primera Instancia e Instrucción | Plaza nº X de la **Sección Civil y de Instrucción del Tribunal de Instancia** |

### 2. Modal informativo al seleccionar juzgado

Se añadió un `AlertDialog` informativo que se muestra automáticamente al abrir
las pantallas de selección de juzgado:

- `ConsultaJuzgadosScreen.kt`
- `DatosJuzgadoAtestadoScreen.kt`

El modal informa del cambio de nomenclatura y referencia la LO 1/2025.

### 3. Versión en el diálogo "Acerca de"

El diálogo "Acerca de..." ahora muestra la versión de la aplicación
("Versión 1.51.3"), siguiendo el mismo patrón que el SplashScreen.

### 4. Base de datos de juzgados actualizada

Se actualizó `juzgados.db` con la denominación vigente de los órganos
judiciales según el nuevo modelo organizativo.

---

## Archivos modificados

| Archivo | Cambio |
|---------|--------|
| `app/build.gradle.kts` | Versión 1.50.3 → 1.51.3 (versionCode 3) |
| `data/.../pdf/SolicitudCustodiaJuzgadoPdfGenerator.kt` | Denominación actualizada en PDF |
| `data/.../pdf/OficioCustodiaSangrePdfGenerator.kt` | Denominación actualizada en PDF |
| `presentation/.../ConsultaJuzgadosScreen.kt` | Preview data + modal informativo |
| `presentation/.../DatosJuzgadoAtestadoScreen.kt` | Preview data + modal informativo |
| `data/.../datasource/JuzgadosDataSource.kt` | Preview data actualizada |
| `app/src/main/assets/docs/citacionjuiciorapido.json` | Texto de citación actualizado |
| `app/src/main/assets/docs/citacionjuicio.json` | Texto de citación actualizado |
| `presentation/.../res/values/strings.xml` | Strings del modal y denominaciones |
| `data/.../document/CitacionDocumentLoaderTest.kt` | Test data actualizada |
| `presentation/.../AboutDialog.kt` | Muestra versión de la app |
| `presentation/.../navigation/NavGraph.kt` | Pasa versionName a AboutDialog |
| `app/.../MainActivity.kt` | Pasa BuildConfig.VERSION_NAME a NavGraph |
| `README.md` | Badge, download, changelog v1.51.3 |
| `app/src/main/assets/databases/juzgados.db` | BD actualizada con nueva denominación |

---

## Validaciones

- Compilación `:presentation:compileReleaseKotlinAndroid` ✅
- Build release con R8 ✅
- APK firmado generado ✅

---

## Estado final de todas las fases

| Fase | Contenido | Estado |
|------|-----------|--------|
| Fase 1 / Tier 1 | MainActivity, NfcDniReader, BluetoothPrinterStorage | ✅ |
| Fase 2 / Tier 2 | BluetoothPrinterUtils, PDF generators, CitacionLoader | ✅ |
| Fase 3 / Tier 3 | Pantallas Compose, UI components, tests | ✅ |
| Fase 4 | Cierre técnico (build + tests + Dokka) | ✅ |
| Fase 5 | Migración Dokka V1 → V2 | ✅ |
| Fase 6 | Deprecaciones Compose corregidas | ✅ |
| Fase 7 | Denominación juzgados LO 1/2025 + mejoras | ✅ |
