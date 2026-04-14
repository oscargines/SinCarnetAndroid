📚 ÍNDICE DE DOCUMENTACIÓN - SinCarnet Android

## 📋 Archivos de Documentación Generados

### 📖 Documentación Principal
1. **DOCUMENTACION_DOCUMENTPRINTER.md** (9.58 KB)
   - Resumen exhaustivo de DocumentPrinter.kt
   - Responsabilidades principales
   - API pública y métodos internos
   - Flujos de impresión
   - Secciones JSON soportadas

2. **DOCUMENTACION_DOCUMENTPRINTER_DETALLADO.md** (10.48 KB)
   - Análisis detallado de cambios
   - Tablas de métodos documentados
   - Notas técnicas
   - Características de calidad
   - Recomendaciones de uso

### 📑 Documentación General del Proyecto
3. **DOCUMENTACION_KDOC.md** (9.7 KB)
   - Guía completa de documentación
   - Estructura del proyecto
   - Convenciones de KDoc
   - Estado por archivo

4. **DOCUMENTACION_COMPLETADA.md** (7.85 KB)
   - Resumen de trabajo realizado
   - Instrucciones para Dokka
   - Beneficios de la documentación

5. **DOKKA_CONFIGURACION.kt**
   - Configuración para generación de HTML
   - Ejemplos de uso
   - Troubleshooting

### 📊 Resumen de Progreso
6. **RESUMEN_DOCUMENTACION.txt** (Actualizado)
   - Estadísticas globales (100% completado – Fases 1-6)
   - Todos los archivos documentados (Tier 1, 2 y 3)
   - Dokka V2 activo

---

## 🎯 Archivo Principal Documentado

### DocumentPrinter.kt (1478 líneas)
- **Tipo**: Object (Singleton)
- **Propósito**: Orquestador central de impresión
- **Métodos documentados**: 27
- **Líneas KDoc agregadas**: ~300
- **Estado**: ✅ COMPLETO

#### Métodos Públicos (4)
- `imprimirDerechos()` - Derechos investigado
- `imprimirCitacionJuicioRapido()` - Juicio rápido
- `imprimirCitacionJuicio()` - Juicio ordinario
- `imprimirManifestacion()` - Manifestación investigado

#### Métodos Suspend (7)
- `imprimirInicioSuspend()`
- `imprimirDerechosSuspend()`
- `imprimirCitacionJuicioRapidoSuspend()`
- `imprimirCitacionJuicioSuspend()`
- `imprimirManifestacionSuspend()`
- `imprimirLetradoGratisSuspend()`
- `imprimirInmovilizacionSuspend()`

#### Método Orquestador
- `imprimirAtestadoCompleto()` - Imprime 6 documentos en lote

#### Métodos Helper (10)
- `buildNombreCompleto()`, `buildLugar()`, `buildDatosJuzgado()`
- `buildInicioBody()`, `manifestacionSiNo()`, `validateManifestacionForPrint()`
- `findJsonOptionById()`, `resolve()`, `buildDiligenciaBody()`
- `printDiligencia()`

#### Constantes (6)
- `DELAY_BETWEEN_DOCS_MS = 5000L`
- `FINAL_DRAIN_BEFORE_CLOSE_MS = 15000L`
- `BATCH_LOG_TAG`
- `MANIFESTACION_REQUIRED_MSG`
- `JUICIO_BOXES_MARKER`

---

## 🔗 Relaciones entre Archivos

```
DocumentPrinter.kt
├─ Depende de:
│  ├─ BluetoothPrinterUtils.kt
│  │  ├─ openSharedBtConnection()
│  │  ├─ closeSharedBtConnection()
│  │  ├─ printDocumentSuspend()
│  │  └─ printDocumentResolvedSuspend()
│  ├─ Storage Managers (7):
│  │  ├─ OcurrenciaDelitStorage.kt
│  │  ├─ JuzgadoAtestadoStorage.kt
│  │  ├─ ActuantesStorage.kt
│  │  ├─ PersonaInvestigadaStorage.kt
│  │  ├─ VehiculoStorage.kt
│  │  ├─ ManifestacionStorage.kt
│  │  └─ AtestadoInicioStorage.kt
│  ├─ Data Classes:
│  │  ├─ PrintSignatures.kt
│  │  ├─ PersonaInvestigadaData
│  │  ├─ OcurrenciaDelitData
│  │  ├─ JuzgadoAtestadoData
│  │  ├─ ActuantesData
│  │  ├─ VehiculoData
│  │  ├─ ManifestacionData
│  │  └─ AtestadoInicioModalData
│  └─ JSON Assets (7):
│     ├─ docs/01inicio.json
│     ├─ docs/02derechos.json
│     ├─ docs/03letradogratis.json
│     ├─ docs/04manifestacion.json
│     ├─ docs/05inmovilizacion.json
│     ├─ docs/citacionjuicio.json
│     └─ docs/citacionjuiciorapido.json
└─ Usado por:
   ├─ ImprimirAtestadoCompleto.kt
   ├─ Todas las pantallas Compose
   └─ UI Screens (impresión)
```

---

## 📈 Estadísticas de Documentación

| Métrica | Valor |
|---------|-------|
| Archivos Kotlin documentados | 61 / 61 |
| Líneas KDoc agregadas | ~2300+ |
| Métodos documentados | 100+ |
| Fases completadas | 6 / 6 |
| Cobertura del proyecto | 100% |
| Warnings de deprecación Compose | 0 (resueltos en Fase 6) |
| Dokka | V2 activo (`dokkaGeneratePublicationHtml`) |

---

## 🎓 Cómo Usar Esta Documentación

### Para Desarrolladores
1. Lee **DOCUMENTACION_DOCUMENTPRINTER.md** para entender qué hace DocumentPrinter
2. Abre el archivo **DocumentPrinter.kt** en tu IDE
3. Hovera sobre cualquier método para ver la documentación KDoc
4. Consulta **DOCUMENTACION_DOCUMENTPRINTER_DETALLADO.md** para detalles técnicos

### Para Mantener la Documentación
1. Mantén los comentarios KDoc sincronizados con el código
2. Actualiza DOCUMENTACION_DOCUMENTPRINTER.md cuando cambies la API
3. Ejecuta `./gradlew :app:publishDokkaToDocs` para regenerar y publicar HTML

### Para Generar Documentación HTML
```bash
# Dokka V2 – comando recomendado
./gradlew :app:publishDokkaToDocs

# Resultado
docs/api/index.html
```

---

## ✅ Checklist de Documentación

### DocumentPrinter.kt
- ✅ Clase principal documentada
- ✅ Constantes documentadas
- ✅ 4 métodos públicos documentados
- ✅ 10 métodos helper documentados
- ✅ 7 métodos suspend documentados
- ✅ 1 método orquestador documentado
- ✅ @param, @return, @throws documentados
- ✅ @see referencias incluidas
- ✅ Ejemplos de uso incluidos
- ✅ Flujos diagramados

### Archivos de Referencia
- ✅ DOCUMENTACION_DOCUMENTPRINTER.md
- ✅ DOCUMENTACION_DOCUMENTPRINTER_DETALLADO.md
- ✅ RESUMEN_DOCUMENTACION.txt actualizado

---

## 🚀 Estado Final – Todas las Fases Completadas

| Fase | Contenido | Estado |
|------|-----------|--------|
| Tier 1 | MainActivity, NfcDniReader, BluetoothPrinterStorage | ✅ |
| Tier 2 | BluetoothPrinterUtils, PDF generators, CitacionLoader | ✅ |
| Tier 3 | 18 pantallas Compose, 6 componentes UI/theme, tests | ✅ |
| Fase 4 | Cierre técnico – build/tests/Dokka | ✅ |
| Fase 5 | Migración Dokka V1 → V2 | ✅ |
| Fase 6 | Deprecaciones Compose eliminadas (`menuAnchor`) | ✅ |

> **No hay tareas pendientes.** El proyecto está completamente documentado
> y libre de warnings de deprecación accionables.

---

## 📚 Convenciones Usadas

### KDoc Format
```kotlin
/**
 * Descripción breve de la función.
 *
 * Descripción detallada si es necesario.
 *
 * Ejemplo:
 * ```kotlin
 * val resultado = miVariable()
 * ```
 *
 * @param param1 Descripción del parámetro
 * @param param2 Otra descripción
 * @return Qué devuelve
 * @throws ExceptionType Si ocurre algo
 * @see OtraClase Para ver relaciones
 */
fun miFuncion(param1: String, param2: Int): String
```

### Tags Usados
- `@param` - Parámetros de función
- `@return` - Valor de retorno
- `@throws` - Excepciones posibles
- `@see` - Referencias a otras clases
- `@property` - Propiedades de clase
- `@deprecated` - Métodos obsoletos

---

## 🔍 Búsqueda Rápida

### Por Tipo de Método
- **Públicos**: imprimirDerechos(), imprimirCitacion*()
- **Helpers**: buildNombre*(), manifestacionSiNo()
- **Render**: printDiligencia*Suspend()
- **Validación**: validateManifestacionForPrint()

### Por Funcionalidad
- **Lectura datos**: todas las funciones públicas
- **Construcción placeholders**: resolve(), buildNombre*()
- **Renderizado JSON**: buildDiligenciaBody()
- **Impresión BT**: printDocument*Suspend()

### Por Diligencia
- Derechos: `imprimirDerechos()`
- Citación: `imprimirCitacion(JuicioRapido|Juicio)()`
- Manifestación: `imprimirManifestacion()`
- Inmovilización: `imprimirInmovilizacion()`

---

## 📞 Contacto & Soporte

**Documentado por**: GitHub Copilot  
**Fecha**: 2026-04-14  
**Proyecto**: SinCarnet Android v1.3  
**Lenguaje**: Kotlin 100%

Para preguntas sobre la documentación, consulta:
- IDE Hover (F1)
- DOCUMENTACION_DOCUMENTPRINTER.md
- DOCUMENTACION_DOCUMENTPRINTER_DETALLADO.md

---

**Estado**: ✅ Proyecto completamente documentado – Fases 1-6 cerradas

