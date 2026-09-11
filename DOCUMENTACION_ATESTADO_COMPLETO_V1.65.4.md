# Documentacion tecnica del atestado completo v1.65.4

## Alcance

La version 1.65.4 incorpora el flujo de generacion del atestado completo y su paquete documental final.

## Flujo funcional

1. El usuario debe generar el PDF de documentos escaneados o marcar que no existen documentos.
2. El sistema muestra la advertencia legal sobre la denuncia del articulo 1.1, opcion 5A, del Reglamento General de Conductores.
3. Se solicita la Jefatura Provincial de Trafico y el numero de boletin.
4. El numero de boletin acepta unicamente digitos y requiere al menos 12 cifras.
5. Se registra si el atestado se enviara por LEXNET o mediante otro modo de envio.
6. Se registran los antecedentes y las bases de datos seleccionadas.

## Orden de documentos

La generacion reducida desde la pantalla de firmas contiene:

```text
01inicio
02derechos
04manifestacion
03letradogratis
citacionjuicio / citacionjuiciorapido
05inmovilizacion
```

La generacion completa añade:

```text
Portada
Resumen
Atestado completo
PortadaAnexo
Documentos escaneados
```

Dentro del atestado completo se incorporan ademas `09denunciasformuladas`, `10antecedentes` y `11envio`.

## Politica de firmas

- Las diligencias `09`, `10` y `11` no muestran la firma del investigado.
- La plantilla `13letradogratis.json` se reserva para impresion Zebra y no se incluye en PDF ni ODT.
- Las diligencias ordinarias conservan la politica de firmas existente.

## Persistencia

`AtestadoInicioStorage` conserva:

- Jefatura provincial.
- Numero de boletin.
- Antecedentes y bases consultadas.
- Sistema de envio y modo alternativo.
- Rutas de los PDFs del atestado, escaneado y atestado completo.

## Generacion PDF

El PDF se crea con `android.graphics.pdf.PdfDocument`, `Canvas`, `Paint` y `RectF`.

La portada y el resumen se dibujan mediante layouts especificos en:

```text
data/src/androidMain/kotlin/com/oscar/sincarnet/data/pdf/CompleteAtestadoFrontMatterPdfGenerator.kt
```

La union de los documentos se realiza mediante `PdfRenderer` en:

```text
data/src/androidMain/kotlin/com/oscar/sincarnet/data/pdf/CompleteAtestadoPdfMerger.kt
```

## Distribucion

La APK de distribucion se genera en:

```text
app/build/outputs/apk/release/app-release.apk
```
