# Cierre de Fase 5 – Migración Dokka V1 → V2

Fecha: 2026-04-14

## Objetivo
Seguir la recomendación post-cierre de Fase 4: eliminar el warning de deprecación
migrando la configuración de Dokka del API V1 (`DokkaTask`) al API V2 (`dokka { }`).

---

## Cambios aplicados

### 1. `gradle.properties` – Activación del modo V2

```properties
# Dokka V2 plugin mode (migración de V1 → V2, mantiene helpers para compatibilidad)
org.jetbrains.dokka.experimental.gradle.pluginMode=V2EnabledWithHelpers
```

**Nota**: `V2EnabledWithHelpers` activa el nuevo API V2 pero mantiene aliases de los
tasks V1 (p.ej. `dokkaHtml`) como stubs SKIPPED para una transición suave.
El paso final (opcional) sería cambiar a `V2Enabled` cuando se quieran eliminar
esos stubs por completo.

### 2. `app/build.gradle.kts` – Bloque `dokka { }` (API V2)

**Antes (V1):**
```kotlin
tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    dokkaSourceSets.configureEach {
        includeNonPublic.set(true)                  // ← deprecado en V2
        sourceRoots.from(file("src/main/java"))     // ← automático en V2
        sourceRoots.from(file("src/main/kotlin"))   // ← automático en V2
        suppress.set(false)
        documentedVisibilities.set(setOf(
            org.jetbrains.dokka.DokkaConfiguration.Visibility.PUBLIC, ...
        ))
        skipDeprecated.set(false)
        reportUndocumented.set(false)
    }
}
```

**Después (V2):**
```kotlin
dokka {
    dokkaSourceSets.configureEach {
        documentedVisibilities.set(setOf(
            org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Public,
            org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Internal,
            org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Protected,
            org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Private
        ))
        skipDeprecated.set(false)
        reportUndocumented.set(false)
    }
}
```

**Diferencias clave:**
| Aspecto | V1 | V2 |
|---------|----|----|
| Bloque config | `tasks.withType<DokkaTask>()` | `dokka { }` |
| Tipo visibilidad | `DokkaConfiguration.Visibility` | `VisibilityModifier` (nuevo) |
| `includeNonPublic` | sí (deprecado) | eliminado; usar `documentedVisibilities` |
| `sourceRoots` manuales | necesario | auto-detectados desde source sets |
| `suppress` | necesario | eliminado |

---

## Task name actualizado

| Propósito | V1 | V2 |
|-----------|----|----|
| Generar HTML | `./gradlew :app:dokkaHtml` | `./gradlew :app:dokkaGeneratePublicationHtml` |
| Generar todo | N/A | `./gradlew :app:dokkaGenerate` |
| Solo módulo | `./gradlew :app:dokkaHtmlPartial` | `./gradlew :app:dokkaGenerateModuleHtml` |

> **Compatibilidad**: en `V2EnabledWithHelpers` el task `dokkaHtml` se mantiene
> registrado (aparece como `SKIPPED`) para no romper scripts existentes.
> En `V2Enabled` se eliminaría.

---

## Validaciones ejecutadas

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:dokkaHtml --no-daemon
.\gradlew.bat :app:dokkaGeneratePublicationHtml --no-daemon
```

Resultados:
- `:app:assembleDebug` ✅
- `:app:testDebugUnitTest` ✅
- `:app:dokkaGeneratePublicationHtml` ✅
- Artefacto: `app/build/dokka/html/index.html`

**Warnings residuales (no bloqueantes):**
- `warning: Dokka Gradle plugin V2 migration helpers are enabled` → esperado con `V2EnabledWithHelpers`
- `AndroidExtensionWrapper could not get Android Extension for project :app` → issue conocido de Dokka V2 con proyectos Android; no afecta a la generación

---

## Estado final

- ✅ **Fase 2**: cierre documental (BluetoothPrinterUtils, PDF generators)
- ✅ **Fase 3**: cierre UI/tests/theme
- ✅ **Fase 4**: cierre técnico (build/tests/dokka)
- ✅ **Fase 5**: migración Dokka V2 completada

## Próximo paso recomendado (opcional)

Cambiar en `gradle.properties`:
```properties
org.jetbrains.dokka.experimental.gradle.pluginMode=V2Enabled
```
Esto elimina los stubs de compatibilidad V1 y el warning de "migration helpers".
Requiere actualizar cualquier script que aún use `dokkaHtml` → `dokkaGeneratePublicationHtml`.

