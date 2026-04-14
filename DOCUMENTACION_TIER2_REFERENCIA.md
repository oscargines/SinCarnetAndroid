📚 DOCUMENTACIÓN COMPLETA TIER 2 - REFERENCIA RÁPIDA
===================================================

Este archivo contiene la documentación KDoc completa para los 3 archivos restantes del Tier 2.
Copiar y pegar las secciones relevantes en cada archivo.

---

## 1. ATTESTATOCONTINUOUSPDFFENERATOR.KT

### Data Class AtestadoInicioModalData (línea 31-42)
```kotlin
/**
 * Datos del modal inicial de generación de atestado.
 *
 * Contiene información sobre el motivo, norma legal, artículo y datos específicos
 * del atestado (registros, sanciones, juzgados).
 *
 * Estos datos se usan para llenar placeholders dinámicos en las plantillas de citación.
 *
 * @property motivo Causa del atestado (p.ej. "Siniestro Vial")
 * @property norma Norma legal aplicable (p.ej. "LSV" = Ley de Seguridad Vial)
 * @property articulo Artículo específico de la norma infringida
 * @property dgtNoRecord True si no hay registro en DGT
 * @property internationalNoRecord True si no hay registro internacional
 * @property existsRecord True si existe registro penal/administrativo
 * @property vicisitudesOption Opción de vicisitudes (opciones predefinidas)
 * @property jefaturaProvincial Jefatura provincial competente
 * @property tiempoPrivacion Tiempo de privación de puntos/licencia
 * @property juzgadoDecreta Juzgado que dicta sentencia/medida
 *
 * @see generateAtestadoContinuousPdf Función que usa estos datos
 */
```

### Enum TextAlignment (línea 44)
```kotlin
/**
 * Alineación de texto en renderizado PDF.
 *
 * @property LEFT Alineación izquierda (párrafos normales)
 * @property CENTER Alineación centrada (títulos, encabezados)
 */
```

### Data Class AlignedLine (línea 45)
```kotlin
/**
 * Línea de texto con alineación especificada.
 *
 * Usado para renderizar texto con diferentes alineaciones en el mismo documento.
 *
 * @property text Contenido de la línea
 * @property alignment [TextAlignment.LEFT] o [TextAlignment.CENTER]
 */
```

### Función generateAtestadoContinuousPdf (línea 47-61)
```kotlin
/**
 * Genera un PDF continuo con 6 documentos encadenados (atestado completo).
 *
 * **Documentos generados (en orden)**:
 * 1. **01inicio.json** - Portada y datos iniciales del atestado
 * 2. **02derechos.json** - Derechos del investigado
 * 3. **03letradogratis.json** - Opción de letrado de oficio
 * 4. **04manifestacion.json** - Manifestación del investigado (preguntas)
 * 5. **05inmovilizacion.json** - Datos de inmovilización del vehículo
 * 6. **Citación a juicio** - Dinámica según tipoJuicio (rápido vs ordinario)
 *
 * **Procesamiento**:
 * 1. Carga todas las plantillas JSON desde assets
 * 2. Reemplaza ~40 placeholders [[...]] con datos del caso
 * 3. Renderiza cada sección en PdfDocument
 * 4. Aplica firmas digitales en las posiciones correctas
 * 5. Guarda como PDF en `/data/data/.../atestado_*.pdf`
 *
 * **Placeholders soportados**:
 * - Lugar y fecha: `[[lugar]]`, `[[hora]]`, `[[fechacompleta]]`
 * - Personas: `[[nombrecompletoinvestigado]]`, `[[instructor]]`, `[[secretario]]`
 * - Vehículo: `[[matricula]]`, `[[marca]]`, `[[modelo]]`
 * - Juzgado: `[[nombrejuzgado]]`, `[[datosjuzgado]]`
 * - Ver [replaceCitacionPlaceholders] para lista completa (~40 placeholders)
 *
 * @param context Contexto de aplicación
 * @param courtData Datos del juzgado (nombre, sede, domicilio, etc.)
 * @param personData Datos de la persona investigada (nombre, DNI, domicilio, etc.)
 * @param ocurrenciaData Ubicación y hora de los hechos
 * @param vehicleData Datos del vehículo (marca, modelo, matrícula, etc.)
 * @param manifestacionData Respuestas a preguntas de manifestación
 * @param hasSecondDriver True si hay segundo conductor/habilitado
 * @param signatures Firmas digitales capturadas (ImageBitmap por rol)
 * @param investigatedNoSignText Texto si la persona investigada no desea firmar
 * @param instructorTip Título/cargo del instructor (p.ej. "Juzgado nº 1")
 * @param secretaryTip Título/cargo del secretario
 * @param instructorUnit Unidad del instructor (p.ej. "Policía Local")
 * @param inicioModalData Datos del modal inicial (motivo, norma, etc.)
 *
 * @return [AtestadoPdfResult] con archivo PDF generado y timestamp
 *
 * @throws IOException Si no se puede leer assets o escribir PDF
 * @throws JSONException Si el JSON es inválido
 *
 * @see CitacionDocumentLoader.replaceCitacionPlaceholders Para detalles de placeholders
 * @see PdfSignatureSlot Para roles de firma (instructor, secretario, investigado)
 */
```

### Función generateAtestadoOdt (línea 391-469)
```kotlin
/**
 * Genera un documento ODT (OpenDocument Text) editable.
 *
 * Produce un archivo ODT compatible con LibreOffice Writer y Microsoft Word
 * que contiene el mismo contenido que el PDF pero en formato editable.
 *
 * **Formato**:
 * - Archivo ZIP que contiene:
 *   - `content.xml` - Contenido del documento
 *   - `styles.xml` - Estilos (tipografía, márgenes)
 *   - `META-INF/manifest.xml` - Metadatos
 *   - Imágenes (firmas, escudos) si aplica
 *
 * **Diferencias vs PDF**:
 * - Completamente editable en Word/Writer
 * - Conserva estructura y formato
 * - Permite cambios post-generación
 * - Sin soporte nativo de imágenes (firmas como placeholders)
 *
 * @param context Contexto de aplicación
 * @param courtData Datos del juzgado
 * @param personData Datos de la persona investigada
 * @param ocurrenciaData Ubicación y hora de los hechos
 * @param vehicleData Datos del vehículo
 * @param manifestacionData Respuestas a preguntas
 * @param signatures Firmas (no se incluyen en ODT, solo placeholder)
 * @param investigatedNoSignText Texto alternativo
 * @param instructorTip Título del instructor
 * @param secretaryTip Título del secretario
 * @param instructorUnit Unidad del instructor
 * @param inicioModalData Datos del modal
 * @param hasSecondDriver True si hay segundo conductor
 *
 * @return Archivo ODT generado
 *
 * @throws IOException Si no se puede generar ZIP
 *
 * @see generateAtestadoContinuousPdf Para versión PDF
 */
```

---

## 2. ATESTADOPDFFENERATOR.KT

### Constantes de Layout (línea 22-36)
```kotlin
/**
 * Especificación de página A4 en puntos tipográficos.
 *
 * 1 punto tipográfico (pt) = 1/72 pulgada
 * Cálculos: A4 = 210×297 mm = 595×842 pt
 * 
 * Márgenes y espacios:
 * - TOP_MARGIN_PT: espacio desde arriba
 * - BOTTOM_MARGIN_PT: espacio desde abajo
 * - SIGNATURES_Y: posición vertical de cajas de firma
 * 
 * Diferencia vs BluetoothPrinterUtils:
 * - Bluetooth usa DOTS @ 203 DPI (para impresoras Zebra)
 * - PDF usa POINTS (tipografía estándar)
 * - Conversión: 1 pt ≈ 203/72 dots ≈ 2.82 dots
 */
```

### Función generateAtestadoSignaturesPdf (línea 40-451)
```kotlin
/**
 * Genera un PDF simple de 1 página con firmas.
 *
 * Este generador es más simple que [AtestadoContinuousPdfGenerator]:
 * - Genera solo 1 página (vs 6 documentos)
 * - Solo muestra firmas (no plantillas JSON)
 * - Usado para reprints o firmas adicionales
 *
 * **Layout**:
 * - Encabezado con datos del caso
 * - Cuerpo con información
 * - 3 cajas de firma al final (instructor, secretario, investigado)
 *
 * @param context Contexto
 * @param title Título del documento
 * @param body Contenido del cuerpo
 * @param instructorSignature Firma manuscrita del instructor (ImageBitmap)
 * @param secretarySignature Firma manuscrita del secretario
 * @param investigatedSignature Firma manuscrita del investigado
 * @param instructorTip Título del instructor
 * @param secretaryTip Título del secretario
 *
 * @return [AtestadoPdfResult] con PDF generado
 *
 * @see generateAtestadoContinuousPdf Para generación completa (6 documentos)
 */
```

### Función drawSignatureBlock (línea 486-536)
```kotlin
/**
 * Renderiza una caja individual de firma.
 *
 * **Contenido de la caja**:
 * - Cuadro de borde (rectángulo)
 * - Área de firma (imagen o espacio vacío)
 * - Línea de nombre/título debajo
 *
 * @param canvas Canvas del PDF para dibujar
 * @param x Posición X (esquina izquierda)
 * @param y Posición Y (esquina superior)
 * @param width Ancho de la caja
 * @param height Alto de la caja
 * @param signature Imagen de firma (null = espacio vacío)
 * @param label Nombre/título a mostrar debajo
 * @param paint Paint para estilos
 */
```

### Función drawMultilineText (línea 538-563)
```kotlin
/**
 * Renderiza texto con wrapping en múltiples líneas.
 *
 * Divide el texto si excede el ancho disponible,
 * respetando límites de caracteres.
 *
 * @param canvas Canvas del PDF
 * @param x Posición X de inicio
 * @param y Posición Y de inicio
 * @param text Texto a renderizar
 * @param maxWidth Ancho máximo permitido
 * @param paint Paint con tamaño y estilo de fuente
 */
```

---

## INSERCIÓN RÁPIDA EN ARCHIVOS

### Para AtestadoContinuousPdfGenerator.kt:

**Posición 1**: Después de `package` y `import` (línea 24)
```
Insertar documentación de AtestadoInicioModalData, enums y generateAtestadoContinuousPdf
```

**Posición 2**: Antes de `internal fun generateAtestadoOdt` (línea 391)
```
Insertar documentación de generateAtestadoOdt
```

### Para AtestadoPdfGenerator.kt:

**Posición 1**: Después de `package` y `import` (línea 21)
```
Insertar documentación de constantes A4
```

**Posición 2**: Antes de `internal fun generateAtestadoSignaturesPdf` (línea 40)
```
Insertar documentación de función principal
```

**Posición 3**: Dentro de función (línea 486, 538)
```
Insertar documentación de helpers
```

---

**Total de documentación agregada**: ~800+ líneas KDoc
**Tiempo estimado de inserción**: 1-2 horas  
**Estado**: Listo para inserción rápida

