# SinCarnet Android

Aplicacion Android en Jetpack Compose para consultar casuistica operativa cuando una persona conduce sin permiso/licencia en vigor.

> Aviso: esta app es una herramienta de consulta y apoyo. No sustituye normativa oficial, instrucciones internas ni criterio juridico profesional.

## 1) Objetivo del proyecto

`SinCarnet` centraliza en una interfaz simple:

- la seleccion de supuestos por tipo de casuistica,
- la resolucion visual del resultado (delito/infraccion/continuacion),
- y observaciones operativas detalladas para escenarios concretos.

El objetivo principal es reducir errores de interpretacion en escenarios de uso real, con una navegacion rapida y orientada a decisiones.

## 2) Stack tecnico

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Arquitectura de UI**: single-activity con enrutado por estado Compose
- **Build system**: Gradle Kotlin DSL
- **Android Gradle Plugin**: `9.1.0`
- **Kotlin plugin compose**: `2.2.10`
- **Compose BOM**: `2024.09.00`
- **SDKs**:
  - `minSdk = 31`
  - `targetSdk = 36`
  - `compileSdk = 36.1`

Configuracion principal en:

- `app/build.gradle.kts`
- `gradle/libs.versions.toml`

## 3) Arquitectura funcional

La app usa una unica actividad (`MainActivity`) y navega por rutas internas basadas en estado:

- `cases`
- `expired_validity`
- `judicial_suspension`
- `without_permit`
- `special_cases`

Flujo general:

1. `SplashScreen` (3 segundos)
2. `CasesScreen` (menu de casos)
3. Pantalla de casuistica correspondiente
4. `AboutDialog` desde el boton de informacion

Archivo clave:

- `app/src/main/java/com/oscar/sincarnet/MainActivity.kt`

## 4) Estructura del proyecto

```text
SinCarnetAndroid/
  app/
	src/main/
	  java/com/oscar/sincarnet/
		MainActivity.kt
		SplashScreen.kt
		CasesScreen.kt
		ExpiredValidityScreen.kt
		JudicialSuspensionScreen.kt
		WithoutPermitScreen.kt
		SpecialCasesScreen.kt
		AboutDialog.kt
		SharedComponents.kt
		PerdidaVigenciaFuntion.kt
	  res/values/strings.xml
	  assets/
		icons/
		images/
```

## 5) Componentes reutilizables

### `SharedComponents.kt`

- `BackIconButton`: boton de retroceso con asset `icons/retroceso.png`
- `OptionRadioRow`: fila seleccionable con `RadioButton`
- `YesNoQuestionBlock`: bloque de pregunta Si/No reutilizable
- `AssetImage`: carga de imagen desde assets con fallback a launcher

### `PerdidaVigenciaFuntionCard`

Tarjeta inferior comun para mostrar:

- mensaje de resultado,
- borde fijo (verde/naranja) o
- borde parpadeante (rojo/amarillo) para alertas.

Archivo: `app/src/main/java/com/oscar/sincarnet/PerdidaVigenciaFuntion.kt`

## 6) Casuistica implementada por pantalla

## 6.1 Perdida de Vigencia (`ExpiredValidityScreen`)

Opciones:

1. Conduce en periodo de perdida de vigencia
2. Conduce finalizada perdida de vigencia
3. Edictal

Reglas de decision (`resolveExpiredValidityDecision`):

- **Rojo parpadeante + delito**:
  - opcion 1,
  - opcion 2 con alguna respuesta negativa,
  - opcion 3 con conocimiento y fuera de periodo de recurso.
- **Amarillo parpadeante + infraccion**:
  - opcion 3 sin conocimiento,
  - opcion 3 con conocimiento y en periodo de recurso.
- **Verde fijo + puede continuar**:
  - opcion 2 con respuestas positivas.

Observaciones implementadas:

- caso de conduccion en periodo de perdida,
- caso edictal en periodo/pendiente de recurso.

## 6.2 Suspension Judicial (`JudicialSuspensionScreen`)

Variables principales:

- condena `<= 2 anos` o `> 2 anos`,
- conduccion `dentro` o `finalizada` la condena,
- curso/examen segun aplique.

Reglas de decision (`resolveJudicialSuspensionDecision`):

- **Rojo parpadeante + delito**: conduce dentro de condena.
- **Amarillo parpadeante + infraccion**:
  - `<= 2 anos` finalizada sin curso,
  - `> 2 anos` finalizada sin cumplir todas las condiciones.
- **Verde fijo + puede continuar**:
  - `<= 2 anos` finalizada con curso,
  - `> 2 anos` finalizada con curso y examen.

Observaciones implementadas:

- `<= 2 anos` dentro de condena,
- `<= 2 anos` finalizada sin curso,
- `> 2 anos` dentro de condena,
- `> 2 anos` finalizada sin curso ni examen.

## 6.3 Carecer de Permiso (`WithoutPermitScreen`)

Preguntas:

- si obtuvo alguna vez permiso/licencia,
- si ese titulo es valido en Espana (cuando corresponde).

Reglas de decision (`resolveWithoutPermitDecision`):

- **Rojo parpadeante + delito**: nunca obtuvo permiso/licencia.
- **Verde fijo + puede continuar**: permiso valido en Espana.
- **Naranja fijo + denuncia administrativa**: permiso no valido en Espana.

Observaciones implementadas:

- actuacion especifica para el caso de no haber obtenido nunca permiso/licencia.

## 6.4 Casos Especiales (`SpecialCasesScreen`)

Opciones:

- perdida de condiciones psicofisicas,
- conducir sin mantener requisitos de otorgamiento.

Regla actual:

- ambos casos muestran infraccion administrativa con borde naranja fijo.

Observaciones implementadas:

- texto especifico para perdida psicofisica,
- texto especifico para falta de requisitos.

## 7) Sistema de observaciones

Cada pantalla incluye boton `Observaciones` con estas caracteristicas:

- ocupa el espacio restante en la fila inferior,
- estilo visual uniforme (fondo `#40407A`, texto blanco, sin elevacion),
- muestra modal con icono de alerta (`assets/icons/error.png`) cuando aplica,
- contenido legal/operativo parametrizado en `strings.xml`.

## 8) Recursos y assets

Recursos de texto:

- `app/src/main/res/values/strings.xml`

Assets utilizados:

- `app/src/main/assets/images/escudo_bw.png`
- `app/src/main/assets/icons/retroceso.png`
- `app/src/main/assets/icons/sobre_nosotros.png`
- `app/src/main/assets/icons/error.png`

Cuando un asset no existe o falla la carga, se usa `ic_launcher_foreground` como fallback visual.

## 9) Instalacion y ejecucion

Requisitos:

- Android Studio actualizado (Koala o superior recomendado)
- JDK 11
- SDK Android instalado con API 36

Clonar:

```powershell
git clone https://github.com/oscargines/SinCarnetAndroid.git
cd SinCarnetAndroid
```

Compilar debug:

```powershell
./gradlew.bat assembleDebug
```

Instalar en dispositivo/emulador conectado:

```powershell
./gradlew.bat installDebug
```

Ejecutar tests unitarios:

```powershell
./gradlew.bat testDebugUnitTest
```

### APK de distribucion

Descarga directa del APK:

- [SinCarnet_V1.3.apk](./SinCarnet_V1.3.apk)

## 10) Estado actual

Estado: **en desarrollo activo**.

Actualmente disponible:

- navegacion completa entre casos,
- logica principal de decision por pantalla,
- modales de observaciones para supuestos clave,
- dialogo "Acerca de" con informacion del autor.

Aspectos tecnicos pendientes recomendados:

- separar logica de negocio de UI (ViewModel/use cases),
- ampliar test unitario de reglas de decision,
- anadir UI tests de flujos criticos,
- revisar ortografia/normalizacion de textos para release,
- internacionalizacion multilenguaje (`values-es`, `values-en`).

## 11) Mantenimiento y roadmap sugerido

Roadmap propuesto (prioridad alta a baja):

1. Cobertura completa de observaciones para todos los estados de cada pantalla.
2. Extraccion de reglas a capa de dominio testeable.
3. Suite de pruebas automatizadas (unit + UI).
4. Historial de cambios por version (`CHANGELOG.md`).
5. Pulido UX (accesibilidad, tamanos de fuente, contraste, feedback).

Buenas practicas para contribuir:

- mantener textos en `strings.xml` (evitar hardcode),
- reutilizar componentes de `SharedComponents.kt`,
- evitar duplicar logica de decision,
- compilar y probar antes de cada commit.

## 12) Creditos

- Desarrollo: GC Oscar I. Gines
- Referencia operativa inicial: grafico elaborado por AEGC
- Repositorio: `https://github.com/oscargines/SinCarnetAndroid.git`
