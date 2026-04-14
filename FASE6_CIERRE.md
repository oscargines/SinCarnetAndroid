# Cierre de Fase 6 – Deprecaciones Compose corregidas

Fecha: 2026-04-14

## Objetivo
Seguir la segunda recomendación post-cierre de Fase 4: reducir warnings de
deprecación de Compose (`menuAnchor`, etc.).

---

## Cambios aplicados

### API deprecada → nueva: `menuAnchor()`

En Material3 1.3.0 (incluido en Compose BOM 2024.09.00), el modificador
`Modifier.menuAnchor()` sin argumentos está deprecado. El reemplazo es:

```kotlin
// Antes (deprecado):
.menuAnchor()

// Después (correcto):
.menuAnchor(MenuAnchorType.PrimaryNotEditable)   // TextField readOnly
```

### Archivos modificados (6 archivos, 11 ocurrencias)

| Archivo | Ocurrencias | Import añadido |
|---------|------------|----------------|
| `ConsultaJuzgadosScreen.kt` | 4 | `MenuAnchorType` ✅ |
| `DatosVehiculoScreen.kt` | 1 | `MenuAnchorType` ✅ |
| `DatosActuantesScreen.kt` | 3 | `MenuAnchorType` ✅ |
| `DatosPersonaInvestigadaScreen.kt` | 1 | `MenuAnchorType` ✅ |
| `DatosJuzgadoAtestadoScreen.kt` | 1 | `MenuAnchorType` ✅ |
| `BluetoothPrinterScreen.kt` | 1 | `MenuAnchorType` ✅ |

**Total**: 11 deprecaciones eliminadas en 6 archivos.

### Justificación del tipo elegido

Todos los usos son `OutlinedTextField` con `readOnly = true` dentro de un
`ExposedDropdownMenuBox`. El tipo correcto es:

- `MenuAnchorType.PrimaryNotEditable` → campo de solo lectura que actúa
  como anchor del menú desplegable.

---

## Validaciones ejecutadas

```powershell
.\gradlew.bat :app:compileDebugKotlin --no-daemon
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --no-daemon
```

Resultados:
- `:app:compileDebugKotlin` ✅  
- `:app:assembleDebug` ✅  
- `:app:testDebugUnitTest` ✅

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

## Warnings residuales (no bloqueantes, no accionables)

- `warning: Dokka Gradle plugin V2 migration helpers are enabled`  
  → Esperado con `V2EnabledWithHelpers`; desaparece cambiando a `V2Enabled`.
- `AndroidExtensionWrapper could not get Android Extension for project :app`  
  → Issue conocido de Dokka 2.x en proyectos Android; no afecta la generación.

