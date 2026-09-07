# Lectura NFC del DNIe

## Alcance

La lectura del DNI electrónico se ejecuta únicamente en Android. El flujo usa el
`ReaderMode` de `NfcAdapter` para detectar el documento y `jmulticard-2.0` para
establecer PACE mediante `IsoDep` y extraer los grupos de datos del DNIe.

## Flujo de lectura

1. El usuario introduce el CAN de seis dígitos. El campo utiliza teclado numérico
   y la validación solo acepta enteros.
2. `MainActivity` activa `ReaderMode` con NFC-A/NFC-B y omite la comprobación NDEF.
3. El callback registra el `Tag` detectado en `NfcTagRepository` junto con su
   timestamp.
4. `NfcReadingHelper` acepta únicamente un tag detectado después del inicio del
   intento y mantiene `ReaderMode` activo mientras se ejecuta la lectura.
5. `NfcDniReader` crea `MrtdKeyStoreImpl`, ejecuta `engineLoad` y obtiene DG1 y
   DG13.
6. Al terminar, se limpia el tag almacenado y se desactiva `ReaderMode`. Si la
   conexión falla, el reintento exige detectar el documento de nuevo.

## Dependencias y compatibilidad

La combinación actual es deliberada:

| Dependencia | Uso |
|---|---|
| `jmulticard-2.0.jar` | PACE, transporte APDU y acceso a `MrtdKeyStoreImpl` |
| `bcprov-jdk15on:1.50` | Primitivas y clases ASN.1 usadas por jMulticard |
| `bcpkix-jdk15on:1.50` | CMS/PKIX usados durante la validación criptográfica |
| `dniedroid-release.jar` | Tipos y decodificación de grupos del DNIe |

`jmulticard-2.0` fue compilado contra la API antigua de Bouncy Castle y referencia
`org.bouncycastle.asn1.DERObjectIdentifier`. Esta clase no está disponible en las
versiones modernas usadas anteriormente, por lo que la aplicación detectaba el
tag correctamente pero fallaba antes de iniciar PACE con `NoClassDefFoundError`.
La comprobación de runtime valida explícitamente esa clase antes de comenzar la
sesión.

Los JAR locales de Bouncy Castle para Android se excluyen porque contienen
SpongyCastle reempaquetado (`org.spongycastle`) y no proporcionan las clases
`org.bouncycastle` que necesita jMulticard. No se deben sustituir estas versiones
sin validar la compatibilidad binaria de toda la cadena NFC.

## Datos extraídos

- DG1: nombre, apellidos, documento, fecha de nacimiento, nacionalidad y sexo.
- DG13: filiación, lugar de nacimiento y domicilio.
- `residenceAddress` se forma como:

  `actualAddress + actualPopulation + actualProvince`

  Los componentes vacíos se omiten y se separan con un espacio. Población y
  provincia también se conservan en `residencePopulation` y `residenceProvince`.

## Diagnóstico

Los tags de log son `MainActivityNfc`, `NfcReadingHelper` y `NfcDniReader`.
Incluyen UID, tecnologías, timestamp de detección, hilo, presencia de `IsoDep`,
estado de conexión, tamaño máximo de transacción, duración y cadena de causas.
El CAN nunca se escribe en los logs.

Para capturar una sesión limpia:

```powershell
adb logcat -c
adb logcat -s MainActivityNfc NfcReadingHelper NfcDniReader
```

Interpretación básica:

- `isoDepPresent=false`: el tag no expone la tecnología necesaria.
- `NoClassDefFoundError`: problema de empaquetado o versión de dependencias.
- `IOException`/`TagLostException` durante `engineLoad`: pérdida física del
  documento o de la sesión NFC.
- `CryptoCardException`: CAN incorrecto, documento bloqueado o rechazo de tarjeta.

## Fuentes principales

- `app/src/main/java/com/oscar/sincarnet/MainActivity.kt`
- `presentation/src/androidMain/kotlin/com/oscar/sincarnet/NfcReadingHelper.kt`
- `data/src/androidMain/kotlin/com/oscar/sincarnet/data/datasource/nfc/NfcDniReader.kt`
- `data/src/androidMain/kotlin/com/oscar/sincarnet/data/datasource/nfc/NfcTagRepository.kt`
- `data/build.gradle.kts`
