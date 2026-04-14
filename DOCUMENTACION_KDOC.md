# Documentación KDoc - Proyecto SinCarnet Android

## Estado actual

**Fecha de actualización**: 2026-04-14  
**Proyecto**: SinCarnet Android v1.3  
**Estado**: ✅ Documentación completa (Fases 1-6 cerradas)

El proyecto tiene documentación KDoc y generación HTML con **Dokka V2**.
La salida publicada y versionable del repositorio se encuentra en:

- `docs/api/index.html`

La salida temporal generada por Gradle se encuentra en:

- `app/build/dokka/html/index.html`

---

## Cobertura documental

### Estado global

| Métrica | Valor |
|---------|-------|
| Archivos Kotlin documentados | 61 / 61 |
| Cobertura documental | 100% |
| Líneas KDoc añadidas | ~2300+ |
| Fases completadas | 6 / 6 |
| Dokka | V2 |

### Bloques cubiertos

- **Tier 1**: actividad principal, NFC y Bluetooth
- **Tier 2**: utilidades de impresión, cargadores y generadores PDF/ODT
- **Tier 3**: pantallas Compose, componentes compartidos, theme y tests
- **Fase 4**: cierre técnico (build, tests, Dokka)
- **Fase 5**: migración Dokka V1 → V2
- **Fase 6**: eliminación de deprecaciones Compose accionables

---

## Arquitectura documentada

### Núcleo de aplicación
- `MainActivity.kt` → orquestación principal, navegación, NFC y flujo general
- `DocumentPrinter.kt` → coordinación de impresión de diligencias
- `BluetoothPrinterUtils.kt` → render CPCL y envío a impresoras Zebra

### Generación documental
- `AtestadoContinuousPdfGenerator.kt` → generación PDF continuo + ODT
- `AtestadoPdfGenerator.kt` → generación PDF simple
- `CitacionDocumentLoader.kt` y `ManifestacionDocumentLoader.kt` → carga de plantillas JSON

### Persistencia y datos
- storages en `SharedPreferences`
- carga SQLite para juzgados/nacionalidades
- repositorio NFC

### UI Compose
- pantallas de toma de datos
- resolución de casuística
- firmas, escáner documental, impresoras Bluetooth
- componentes compartidos y `ui/theme`

---

## Convenciones KDoc usadas

### Data classes

```kotlin
/**
 * Descripción breve del modelo.
 *
 * @property campo1 Significado del campo.
 * @property campo2 Significado del campo.
 */
data class MiModelo(
    val campo1: String,
    val campo2: Int
)
```

### Funciones

```kotlin
/**
 * Describe qué hace la función y en qué contexto se usa.
 *
 * @param param1 Descripción del parámetro.
 * @param param2 Descripción del parámetro.
 * @return Resultado devuelto.
 * @throws IllegalStateException Si el estado no permite continuar.
 * @see OtraClase
 */
fun miFuncion(param1: String, param2: Int): Boolean
```

### Composables

```kotlin
/**
 * Renderiza un bloque visual reutilizable.
 *
 * @param modifier Permite personalizar layout y estilo externo.
 * @param onConfirm Callback invocado al confirmar.
 * @param onDismiss Callback invocado al cerrar.
 */
@Composable
fun MiComposable(
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {}
)
```

---

## Generación de documentación HTML

### Comando recomendado

```powershell
./gradlew :app:publishDokkaToDocs
```

Este comando:

1. ejecuta Dokka V2 para el módulo `app`,
2. genera HTML navegable,
3. copia la salida a `docs/api/`,
4. deja la documentación lista para commit/push.

### Solo generación local

```powershell
./gradlew :app:dokkaGeneratePublicationHtml
```

### Validación técnica completa

```powershell
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:publishDokkaToDocs
```

---

## Estructura publicada

```text
SinCarnetAndroid/
├─ docs/
│  └─ api/
│     ├─ index.html
│     ├─ navigation.html
│     └─ ...assets Dokka...
├─ app/
│  └─ src/main/java/com/oscar/sincarnet/
├─ README.md
└─ DOCUMENTACION_KDOC.md
```

---

## Publicación en repositorio

La carpeta `docs/api/` no está ignorada por Git, por lo que puede versionarse sin
problemas. La salida bajo `app/build/` **no** debe subirse, porque es temporal.

Flujo recomendado:

```powershell
./gradlew :app:publishDokkaToDocs
git add docs/api README.md DOCUMENTACION_KDOC.md
```

Si además hay cambios KDoc en código fuente, añadir también los `.kt` afectados.

---

## Notas técnicas

- El proyecto usa `org.jetbrains.dokka` `2.0.0`.
- El modo activo en `gradle.properties` es `V2EnabledWithHelpers` para conservar
  compatibilidad con tareas legacy durante la transición.
- La tarea publicada para repositorio es `publishDokkaToDocs`.
- La documentación resultante se valida junto con `assembleDebug` y `testDebugUnitTest`.

### Warnings residuales no bloqueantes

- `Dokka Gradle plugin V2 migration helpers are enabled`
- `AndroidExtensionWrapper could not get Android Extension for project :app`

Estos avisos no impiden generar ni publicar la documentación HTML.

---

## Referencias

- [Kotlin KDoc Syntax](https://kotlinlang.org/docs/kotlin-doc.html)
- [Dokka Official Docs](https://kotlin.github.io/dokka/)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Android Architecture Components](https://developer.android.com/guide/architecture)
