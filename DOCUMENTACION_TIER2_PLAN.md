📚 PLAN DE COMPLETACIÓN TIER 2 - RESUMEN EJECUTIVO
================================================

**Objetivo**: Completar documentación de 4 archivos críticos del Tier 2

**Estado Actual**:
- ✅ CitacionDocumentLoader.kt (366 líneas) - COMPLETO
- 🟡 BluetoothPrinterUtils.kt (1477 líneas) - INICIADO (constantes documentadas)
- ⏳ AtestadoContinuousPdfGenerator.kt (1817 líneas) - PENDIENTE
- ⏳ AtestadoPdfGenerator.kt (578 líneas) - PENDIENTE

---

## 📋 BLUETOOTHPRINTERUTILS.KT - GUÍA DE DOCUMENTACIÓN

### Secciones ya documentadas ✅:
- PAPER SPECIFICATION (línea 21-30)
- ESCUDOS specification (línea 32-43)
- TITLE typography (línea 45-57)
- BODY typography (línea 59-70)
- QR CODE (línea 72-81)
- SIGNATURE BLOCK LAYOUT (línea 83-115)
- EgImage data class (línea 117-130)
- Funciones de conversión: pngToEg(), bitmapToEg(), imageBitmapToEg() (línea 132-181)

### Secciones pendientes 🔲:

#### 1. JUICIO BOXES (línea 184-350)
```kotlin
// Renderizado de cajas de citación a juicio
// Marcar con: @see CitacionDocumentLoader
// Documentar: appendJuicioBoxes()
// Incluir: diagrama ASCII de layout
```

**Documentación requerida**:
- Descripción del bloque de citación
- Parámetros de altura/separación
- Ejemplo de uso

#### 2. API PÚBLICAS (línea 375-414)
```kotlin
fun printDocumentFromJson(context, mac, jsonAsset, ...)
fun printDocument(context, mac, title, body, ...)
fun printDocumentResolved(context, mac, title, body, ...)
fun printImageTest(context, mac)
```

**Documentación requerida**:
- Descripción de cada función
- Diferencia entre variantes (Json vs Resolved)
- @param y @return
- Ejemplo de uso

#### 3. API SUSPEND (línea 420-662)
```kotlin
suspend fun printDocumentFromJsonSuspend(...)
suspend fun printDocumentResolvedSuspend(...)
suspend fun printDocumentSuspend(...)
```

**Documentación requerida**:
- Explicar async/suspend nature
- Conexión BT y CPCL rendering
- Manejo de errores
- Flujo completo (openShared → render → close)

#### 4. UTILIDADES INTERNAS (línea 675-760)
```kotlin
private fun openSharedBtConnection(...)
private fun sendToPrinter(...)
private fun showNoMac(...)
private fun showError(...)
private fun wrapText(...)
private fun appendSignatureBlock(...)
```

**Documentación requerida**:
- Propósito y responsabilidad
- Parámetros
- Casos de uso

---

## 📋 ATTESTATOCONTINUOUSPDFFENERATOR.KT - GUÍA DE DOCUMENTACIÓN

### Estructura:
- **Líneas 1-50**: Imports + class header
- **Líneas 51-363**: `generateAtestadoContinuousPdf()` (función principal)
- **Líneas 365-469**: `generateAtestadoOdt()`
- **Líneas 471-550**: Helpers para ODT/XML
- **Líneas 551-820**: Renderizado continuo de página
- **Líneas 821-1085**: Renderizado de elementos gráficos (citación, firmas)
- **Líneas 1086-1365**: Carga de plantillas y reemplazo de placeholders
- **Líneas 1366-1817**: Utilidades de formato y wrapping de texto

### Documentación requerida:

1. **Clase + Constantes**:
   - Explicar que genera PDF/ODT continuo
   - Estructura de 5 documentos encadenados
   - Manejo de placeholders dinámicos

2. **Funciones principales**:
   - `generateAtestadoContinuousPdf()` - API pública
   - `generateAtestadoOdt()` - API pública ODT
   - `startContinuousPage()` - Inicialización
   - `drawCitacionHeader()` - Encabezado con escudos
   - `drawCitacionSignatures()` - Cajas de firma

3. **Placeholders**:
   - Documentar que se delegan a CitacionDocumentLoader
   - Listar los ~40 placeholders soportados
   - Explicar resolución de dato

4. **Helpers**:
   - `wrapTextLines()` - Text wrapping
   - `replaceCitacionPlaceholders()` - External call
   - `loadAtestadoTemplateAsCitacion()` - Carga JSON

---

## 📋 ATESTADOPDFFENERATOR.KT - GUÍA DE DOCUMENTACIÓN

### Estructura:
- **Líneas 1-36**: Constantes de layout A4
- **Líneas 38-451**: `generateAtestadoSignaturesPdf()` (función única)
- **Líneas 453-563**: Helpers privados

### Documentación requerida:

1. **Constantes de layout**:
   - Especificación A4 (595×842 pt)
   - Márgenes y columnas
   - Comparación con BluetoothPrinterUtils (diferencia: puntos vs dots)

2. **Función principal**:
   - API simple: genera 1 página con firmas
   - Diferencia vs AtestadoContinuousPdfGenerator
   - Usuarios y casos de uso

3. **Helpers**:
   - `drawSignatureBlock()` - Renderizado de caja
   - `drawMultilineText()` - Wrapping de texto

---

## 🎯 ESTRATEGIA DE DOCUMENTACIÓN RÁPIDA

### Enfoque recomendado:
1. **No documentar línea por línea** - Agrupar por función/sección
2. **Usar @see referencias** - Vincular entre clases relacionadas
3. **Copiar patrones** - Reutilizar ejemplos de CitacionDocumentLoader
4. **Priorizar APIs públicas** - Luego helpers privados
5. **Incluir gráficos** - ASCII diagrams para layouts complejos

### Tiempo estimado:
- **BluetoothPrinterUtils**: 2-3 horas (completar constantes + APIs)
- **AtestadoContinuousPdfGenerator**: 3-4 horas (complejo, muchas funciones)
- **AtestadoPdfGenerator**: 1.5-2 horas (más simple)
- **TOTAL TIER 2**: 7-9 horas

---

## ✅ CHECKLIST PARA COMPLETAR TIER 2

### BluetoothPrinterUtils.kt
- [x] Constantes de layout (PAPEL, ESCUDOS, TIPOGRAFÍA)
- [x] EgImage data class
- [x] Funciones de conversión de imágenes
- [ ] Función appendJuicioBoxes()
- [ ] Función appendSignatureBlock()
- [ ] APIs públicas (3 funciones)
- [ ] APIs suspend (3 funciones)
- [ ] Utilidades internas (5 funciones)

### AtestadoContinuousPdfGenerator.kt
- [ ] Clase principal + documentación general
- [ ] generateAtestadoContinuousPdf() - API principal
- [ ] generateAtestadoOdt() - API ODT
- [ ] Funciones de renderizado (citación, firmas, página)
- [ ] Funciones de carga de plantillas
- [ ] Funciones de reemplazo de placeholders

### AtestadoPdfGenerator.kt
- [ ] Constantes de layout
- [ ] generateAtestadoSignaturesPdf() - API única
- [ ] drawSignatureBlock()
- [ ] drawMultilineText()

---

## 🚀 PRÓXIMA ACCIÓN

Proceder a documentar las funciones restantes de BluetoothPrinterUtils.kt siguiendo el patrón:

```kotlin
/**
 * Descripción breve de qué hace.
 *
 * Descripción detallada:
 * - Punto 1
 * - Punto 2
 *
 * Ejemplo:
 * ```kotlin
 * val result = miFunc(param1, param2)
 * ```
 *
 * @param param1 Descripción
 * @param param2 Descripción
 * @return Qué retorna
 * @see OtraFuncion Para relaciones
 */
```

---

**Documentado por**: GitHub Copilot  
**Fecha**: 2026-04-14  
**Estado**: Tier 2 in progress, 25% completado

