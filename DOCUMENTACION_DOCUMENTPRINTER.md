# Documentación - DocumentPrinter.kt (1478 líneas)

## 📋 Resumen General

**DocumentPrinter.kt** es el **orquestador central de impresión** de SinCarnet. Su responsabilidad principal es coordinar la preparación, renderizado e impresión de todas las diligencias legales en impresoras Zebra.

### Cambios Realizados

Se agregó documentación KDoc completa con:
- ✅ Descripción general de la clase (69 líneas)
- ✅ Documentación de constantes (6 constantes)
- ✅ Documentación de 4 funciones de impresión público
- ✅ Documentación de 10 funciones helper privadas
- ✅ Documentación de 2 métodos de renderizado (suspend y no-suspend)
- ✅ Documentación de 2 métodos de resolución de placeholders
- ✅ Documentación de 1 método de construcción de body

---

## 🎯 Responsabilidades Principales

### 1. **Lectura de Datos desde Storage**
Los métodos públicos `imprimirX()` leen datos desde múltiples Storage managers:
- `OcurrenciaDelitStorage` → Lugar, fecha, hora del delito
- `JuzgadoAtestadoStorage` → Juzgado competente
- `ActuantesStorage` → Instructor y secretario
- `PersonaInvestigadaStorage` → Datos del investigado
- `VehiculoStorage` → Matrícula, marca, modelo
- `ManifestacionStorage` → Respuestas SI/NO
- `AtestadoInicioStorage` → Datos iniciales del atestado

### 2. **Construcción de Placeholders**
Mapea datos reales a **[[clave]] → valor**:
- Nombres: `[[nombrecompletoinvestigado]]` → "Juan García López"
- Ubicaciones: `[[lugar]]` → "N-I, PK 23.5, Alcalá"
- Fechas: `[[lugarfechahoralecturaderechos]]` → "N-I, 15/03/2024 a las 14:30 horas"

### 3. **Carga de Plantillas JSON**
Lee documentos desde `assets/docs/`:
- `01inicio.json` → Diligencia de inicio
- `02derechos.json` → Derechos del investigado
- `03letradogratis.json` → Asistencia jurídica
- `04manifestacion.json` → Manifestación investigado
- `05inmovilizacion.json` → Acta inmovilización
- `citacionjuicio.json` → Citación ordinaria
- `citacionjuiciorapido.json` → Citación rápida

### 4. **Renderizado de Documentos**
Procesa plantillas JSON:
- Resuelve `[[placeholder]]` → datos reales
- Construye body completo respetando estructura
- Soporta JSON anidado (artículos, subapartados, opciones)
- Inserta marcador especial `JUICIO_BOXES_MARKER` para cajas CPCL

### 5. **Gestión de Bluetooth**
Coordina impresión con SDK Zebra:
- Abre conexión compartida (una sola vez)
- Envía documentos secuencialmente (con delays)
- Cierra conexión garantizando impresión completa

---

## 📚 API Pública

### Métodos para Botones Individuales (UI)

```kotlin
// Imprime diligencia de derechos (impresora individual)
fun imprimirDerechos(context: Context, mac: String?)

// Imprime citación para juicio rápido
fun imprimirCitacionJuicioRapido(context: Context, mac: String?)

// Imprime citación para juicio ordinario
fun imprimirCitacionJuicio(context: Context, mac: String?)

// Imprime manifestación del investigado no detenido
fun imprimirManifestacion(context: Context, mac: String?)
```

**Características**:
- Cada uno crea su propia coroutine en IO dispatcher
- No bloquean la UI
- Muestran Toast si hay error
- `mac=null` → no hace nada (silencioso)

### Método para Lotes (Atestado Completo)

```kotlin
fun imprimirAtestadoCompleto(
    context: Context,
    mac: String,
    sigs: PrintSignatures? = null,
    onProgress: (index: Int, total: Int, docName: String) -> Unit,
    onFinished: () -> Unit,
    onError: (String) -> Unit
)
```

**Flujo**:
1. Abre UNA SOLA conexión BT (emparejamiento una vez)
2. Imprime 6 documentos secuencialmente:
   - Diligencia de inicio
   - Derechos del investigado
   - Asistencia jurídica gratuita
   - Manifestación
   - Citación (rápida u ordinaria)
   - Acta de inmovilización
3. Espera 15 segundos antes de cerrar conexión
4. Reporta progreso y errores via callbacks

---

## 🔧 Métodos Internos (Suspend)

### Impresión Individual Asincrónica

```kotlin
suspend fun imprimirInicioSuspend(
    context: Context,
    mac: String,
    sigs: PrintSignatures? = null,
    sharedConn: Connection? = null
)

suspend fun imprimirDerechosSuspend(...)
suspend fun imprimirCitacionJuicioRapidoSuspend(...)
suspend fun imprimirCitacionJuicioSuspend(...)
suspend fun imprimirManifestacionSuspend(...)
suspend fun imprimirLetradoGratisSuspend(...)
suspend fun imprimirInmovilizacionSuspend(...)
```

**Parámetro especial `sharedConn`**:
- Si es null → abre conexión nueva (impresión individual)
- Si es Connection → reutiliza conexión compartida (impresión en lote)

---

## 🛠️ Métodos Helper

### Construcción de Strings

| Método | Ejemplo | Uso |
|--------|---------|-----|
| `buildNombreCompleto(p)` | "Juan García López" | Nombres investigado |
| `buildLugar(o)` | "N-I, PK 23.5, Alcalá" | Ubicación delito |
| `buildDatosJuzgado(j)` | "Juzgado de Paz, C/ Mayor 10, Madrid" | Información juzgado |
| `buildInicioBody(...)` | [Body completo atestado inicio] | Construcción compleja |

### Conversión Booleana

```kotlin
manifestacionSiNo(value: Boolean?, fieldName: String): String
// true → "SI"
// false → "NO"
// null → "NO" (con log warning)
```

### Búsqueda en JSON

```kotlin
findJsonOptionById(array: JSONArray?, id: Any): JSONObject?
// Busca opción por ID en arrays JSON
// Usado para subopciones dinámicas
```

---

## 📊 Constantes Configurables

```kotlin
DELAY_BETWEEN_DOCS_MS = 5000L     // Delay entre documentos (ms)
FINAL_DRAIN_BEFORE_CLOSE_MS = 15000L  // Espera antes cerrar BT
JUICIO_BOXES_MARKER = "\u0000JUICIO_BOXES\u0000"  // Marcador CPCL
MANIFESTACION_REQUIRED_MSG = "Debe completar..."  // Validación
```

---

## 🔄 Flujo de Impresión Individual

```
Usuario toca "Imprimir Derechos"
  ↓
imprimirDerechos(context, mac)
  ├─ Lee datos desde Storage managers
  ├─ Construye mapa de placeholders
  └─ printDiligencia(context, mac, "docs/02derechos.json", ...)
    ├─ Crea coroutine en IO dispatcher
    └─ printDiligenciaSuspend()
      ├─ Lee JSON desde assets
      ├─ Resuelve [[placeholder]] → valores
      ├─ Construye body completo
      └─ BluetoothPrinterUtils.printDocumentSuspend()
        ├─ Convierte a CPCL
        ├─ Abre conexión Bluetooth
        ├─ Envía datos a impresora
        └─ Cierra conexión
```

---

## 🔄 Flujo de Impresión en Lote

```
Usuario toca "Imprimir Atestado Completo"
  ↓
imprimirAtestadoCompleto(context, mac, sigs, ...)
  ├─ openSharedBtConnection(mac)
  │  └─ Abre UNA conexión compartida
  ├─ Para cada documento en [inicio, derechos, letrado, ..., inmovilizacion]:
  │  ├─ imprimirXSuspend(context, mac, sigs, sharedConn)
  │  │  └─ Reutiliza conexión compartida
  │  └─ delay(5 segundos)
  ├─ delay(15 segundos)  // Espera final para impresión
  └─ closeSharedBtConnection(sharedConn)
     └─ Cierra conexión
```

---

## 🎯 Validaciones

### Manifestación Obligatoria

```kotlin
validateManifestacionForPrint(data: ManifestacionData)
// Si campos críticos son null:
// - Log warning
// - Permite continuar (imprime "NO" por defecto)
// - No lanza excepción
```

---

## 🔗 Dependencias Clave

| Clase | Responsabilidad |
|-------|-----------------|
| `BluetoothPrinterUtils` | Conversión JSON → CPCL, envío Bluetooth |
| `OcurrenciaDelitStorage` | Persistencia de datos del delito |
| `PersonaInvestigadaStorage` | Persistencia de datos investigado |
| `JuzgadoAtestadoStorage` | Persistencia datos juzgado |
| `ActuantesStorage` | Persistencia instructor/secretario |
| `PrintSignatures` | Datos de firmas capturadas |

---

## 📝 Secciones JSON Soportadas

```json
{
  "documento": {
    "titulo": "...",
    "cuerpo": { "descripcion": "..." },
    "qr": { "dato": "..." },
    "articulos": [...],
    "momento_informacion_derechos": { "opciones": [...] },
    "hechos_investigacion": { "puntos": [...] },
    "derechos_articulo_520": { "derechos": [...] },
    "manifestacion_investigado": { "opciones": [...] },
    "opciones_investigado": { "opciones": [...] },
    "normativa_aplicable": { "normas": [...] },
    "responsabilidades_penales": { "articulos": [...] },
    "secciones": [{ "titulo": "...", "opciones": [...] }],
    "enterado": { "titulo": "...", "texto": "..." }
  }
}
```

---

## ✅ Características Implementadas

- ✅ 4 diligencias públicas (derechos, citación rápida, citación ordinaria, manifestación)
- ✅ 6 versiones suspend para impresión en lote
- ✅ Construcción dinámica de placeholders desde Storage
- ✅ Renderizado de JSON anidado (artículos, subapartados)
- ✅ Soporte para QR dinámico
- ✅ Conexión Bluetooth compartida (evita emparejamientos múltiples)
- ✅ Delays configurables entre documentos
- ✅ Validaciones con fallos suaves (no lanzan excepciones)
- ✅ Logging detallado en cada etapa
- ✅ Callbacks de progreso para UI
- ✅ Manejo robusto de null/valores faltantes

---

## 📚 Documentación Agregada

- **Líneas de KDoc**: ~150 líneas
- **Secciones documentadas**: 25+
- **Ejemplos incluidos**: 15+
- **Links @see**: 10+
- **Constantes documentadas**: 6
- **Métodos documentados**: 12+

---

## 🚀 Próximos Pasos (Recomendaciones)

1. Documentar `BluetoothPrinterUtils` (conversión CPCL)
2. Documentar Data classes (Storage, PrintSignatures)
3. Agregar diagramas de flujo UML
4. Crear ejemplos de uso en README
5. Documentar manejo de errores y reintentos


