# 📋 ESTRUCTURA DE ELEMENTOS EN PDF - ATESTADO SIGNATURES

## ⚙️ CONSTANTES DE PÁGINA
- **Ancho (A4)**: 595 pt
- **Alto (A4)**: 842 pt
- **Margen superior**: 5.5 mm = 15.7 pt
- **Margen inferior**: 15 mm = 42.5 pt
- **Margen izquierdo**: 5 mm = 14.2 pt
- **Margen derecho**: 5 mm = 14.2 pt

## 📍 DIVISIONES DE COLUMNAS
- **Columna izquierda**: 0 - 175 mm (498 pt)
- **Columna derecha**: 175 mm - 210 mm (inicio en 498 pt)
- **División vertical izquierda** (línea guía): 20 mm desde LEFT
- **División vertical derecha** (línea guía): 15 mm desde RIGHT

---

## 🎯 ELEMENTOS DEL PDF

### ELEMENTO 1: Caja "ATESTADO NÚMERO:"
```
┌─────────────────────────────┐
│ ATESTADO NÚMERO:            │
└─────────────────────────────┘
```
- **Posición**: Parte superior, columna derecha (izquierda)
- **TOP**: 5.6 mm desde borde superior
- **LEFT**: Inicio columna derecha + 5 mm
- **Ancho**: 75 mm
- **Alto**: 6 mm
- **Contenido**: Texto "ATESTADO NÚMERO:" (tamaño 9pt)

---

### ELEMENTO 2: Caja "FOLIO Nº"
```
┌──────────────┐
│ FOLIO Nº     │
└──────────────┘
```
- **Posición**: Parte superior, columna derecha (derecha)
- **TOP**: 5.6 mm desde borde superior (ALINEADO CON ELEMENTO 1)
- **RIGHT**: 3 mm desde borde derecho
- **Ancho**: 25 mm
- **Alto**: 6 mm
- **Contenido**: Texto "FOLIO Nº" (tamaño 9pt)

---

### ELEMENTO 3: Líneas guía laterales (divisoras)
```
│                                    │
│  Línea izquierda                  │  Línea derecha
│  (20 mm desde LEFT)               │  (15 mm desde RIGHT)
│                                    │
```
- **LEFT línea izquierda**: 20 mm desde borde izquierdo
- **RIGHT línea derecha**: 15 mm desde borde derecho
- **TOP**: 5.6 mm
- **BOTTOM**: 15 mm desde borde inferior
- **Color**: Negro (trazo 1.2 pt)

---

### ELEMENTO 4: Escudo de España
```
    🇪🇸
```
- **Posición**: Esquina superior izquierda de columna izquierda
- **TOP**: 5.5 mm (margen superior)
- **LEFT**: 5 mm (margen izquierdo)
- **Ancho**: 8 mm
- **Alto**: Proporcional (autocalculado)
- **Imagen**: `images/EscEspana.png`

---

### ELEMENTO 5: Escudo Guardia Civil
```
        🛡️
```
- **Posición**: Esquina superior derecha de columna derecha
- **TOP**: 5.5 mm (margen superior)
- **RIGHT**: 1 mm desde borde derecho de columna derecha
- **Ancho**: 7 mm
- **Alto**: Proporcional (autocalculado)
- **Imagen**: `images/EscGuardiaCivil.png`

---

### ELEMENTO 6: Área de contenido principal
```
═════════════════════════════════════
    TÍTULO DE CITACIÓN
═════════════════════════════════════
Cuerpo de la citación con placeholders
reemplazados por datos reales...
```
- **TOP del título**: 12.5 mm
- **LEFT contenido**: Inicio columna derecha + 6 pt (margen interior)
- **RIGHT contenido**: Fin columna derecha - 6 pt (margen interior)
- **Fuente**: Calibri (Regular y Bold)
- **Tamaño título**: 14 pt (bold)
- **Tamaño texto**: 11 pt (regular)

---

### ELEMENTO 7: Sección de citación
Incluye:
1. Título de citación
2. Cuerpo principal (con reemplazo de placeholders)
3. Secciones múltiples (cada una con):
   - Título de sección
   - Contenido
   - Opciones (indentadas 8pt)
   - Items (indentados 8pt)
   - Información adicional
4. Cierre
5. Sección "Enterado" (título + texto)

---

### ELEMENTO 8: Caja de firma INSTRUCTOR (superior izquierda)
```
┌─────────────────────┐
│ Instructor          │
│                     │
│  [FIRMA O TEXTO]    │
│                     │
└─────────────────────┘
```
- **TOP**: Después del contenido principal (variable)
- **LEFT**: Inicio columna derecha
- **Ancho**: ~40-50 pt
- **Alto**: Calculado dinámicamente (~42% del espacio disponible)
- **RIGHT**: ~10pt desde elemento 9

---

### ELEMENTO 9: Caja de firma SECRETARIO (superior derecha)
```
┌─────────────────────┐
│ Secretario          │
│                     │
│  [FIRMA O TEXTO]    │
│                     │
└─────────────────────┘
```
- **TOP**: Misma que INSTRUCTOR (alineadas)
- **RIGHT**: ~5 mm desde borde derecho
- **Ancho**: ~40-50 pt
- **Alto**: Igual que INSTRUCTOR
- **LEFT**: ~10pt desde elemento 8

---

### ELEMENTO 10: Caja de firma INVESTIGADO (inferior centrada)
```
         ┌──────────────────┐
         │ Investigado      │
         │                  │
         │ [FIRMA O TEXTO]  │
         │                  │
         └──────────────────┘
```
- **TOP**: BOTTOM del INSTRUCTOR + 10 pt (gap)
- **LEFT**: Centrada horizontalmente en columna derecha
- **Ancho**: ~25% del ancho de columna derecha
- **Alto**: Calculado dinámicamente (~58% del espacio disponible)
- **BOTTOM**: 15 mm desde borde inferior

---

## 🔧 CONSTANTES CONFIGURABLES

| Nombre | Valor | Descripción |
|--------|-------|-------------|
| `SIDE_MARGIN_MM` | 5 mm | Margen lateral izquierdo/derecho |
| `TOP_MARGIN_MM` | 5.5 mm | Margen superior |
| `BOTTOM_MARGIN_MM` | 15 mm | Margen inferior |
| `LEFT_COL_MM` | 10 mm | Ancho columna izquierda inicial |
| `CENTER_COL_MM` | 175 mm | Inicio columna derecha |
| `ESC_ESPANA_WIDTH_MM` | 8 mm | Ancho escudo España |
| `ESC_GC_WIDTH_MM` | 7 mm | Ancho escudo Guardia Civil |
| `HEADER_HEIGHT_MM` | 22 mm | Altura zona de encabezado |
| `TITLE_TOP_MM` | 12.5 mm | TOP del título de citación |
| `TITLE_BODY_GAP_PT` | 18 pt | Espacio entre título y cuerpo |

---

## 🐛 CÓMO IDENTIFICAR PROBLEMAS

Si algo está **descuadrado**, sigue estos pasos:

1. **Identifica el elemento** en el PDF que se ve mal
2. **Encuentra su número** en la lista anterior
3. **Revisa las líneas de comentario** en `AtestadoPdfGenerator.kt`
4. **Modifica las constantes** asociadas:
   - Para movimiento horizontal: ajusta `LEFT` o `RIGHT` (en mm)
   - Para movimiento vertical: ajusta `TOP` o `BOTTOM` (en mm)
   - Para tamaño: ajusta `Ancho` o `Alto` (en mm)

### Ejemplo: Si el escudo de España está muy a la derecha
- Busca `ELEMENTO 4: Escudo de España`
- El valor `5 mm` en `// Distancia LEFT: 5 mm` es el problema
- Cámbialo a un valor menor para moverlo a la izquierda (ej: `3 mm`)

---

## 📏 CONVERSIONES ÚTILES

```
1 mm = 2.834645669 pt
1 pt = 0.352777778 mm

Ejemplos:
5 mm = 14.2 pt
10 mm = 28.3 pt
20 mm = 56.7 pt
```

---

## ✅ CHECKLIST DE VERIFICACIÓN

- [ ] Elemento 1 y 2 alineados en TOP
- [ ] Elemento 3 líneas verticales rectas
- [ ] Elemento 4 y 5 escudos sin solapamiento
- [ ] Elemento 6 título visible y centrado
- [ ] Elemento 7 texto envuelto correctamente
- [ ] Elemento 8 y 9 firmas alineadas horizontalmente
- [ ] Elemento 10 firma investigado centrada
- [ ] Todos los márgenes respetados

