# Cruce Seguro Accesible Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir una aplicación Android nativa que combine sensores reales y distancia real o simulada para advertir accesiblemente la aproximación caminando a un cruce.

**Architecture:** Servicios Android encapsulan captura y actuación; procesadores Kotlin puros transforman muestras; un motor puro decide con persistencia y cooldown; un `MainViewModel` coordina todo y expone `StateFlow` a Jetpack Compose. Las pruebas unitarias se concentran en procesamiento y decisión sin depender del framework Android.

**Tech Stack:** Kotlin, Gradle Kotlin DSL, Android SDK, Jetpack Compose Material 3, Lifecycle ViewModel/StateFlow, Google Play Services Location, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-09-29-cruce-seguro-accesible-design.md`

## Global Constraints

- Android nativo con Kotlin y Jetpack Compose; interfaz y TTS en español.
- Sin Firebase, Google Maps, backend, login ni servicios pagos.
- Solo primer plano; cruce temporal en memoria; orientación vertical.
- Condición conjunta sostenida 1.5 s y cooldown global de 20 s.
- Umbrales: preventivo ≤30 m, cercano ≤15 m, crítico ≤8 m.
- El modo simulado sustituye solo la distancia; acelerómetro y giroscopio siempre son reales.
- Ubicación real con precisión >25 m no puede producir alertas.
- Fallos de TTS, vibración o flash nunca deben cerrar la app.

## Review Focus

- Tiempo no monotónico o reinicio de sesión: `DecisionEngineTest` verificará que `reset()` borra persistencia y cooldown.
- Distancia inválida (`null`, negativa o no finita): `ProximityClassifierTest` y `ApproachDetectorTest` exigirán estado no alertable.
- Ruido de GPS sin tendencia neta suficiente: `ApproachDetectorTest` impedirá falsos acercamientos.
- Sensores sin muestras recientes: `DecisionEngineTest` impedirá alertas aunque las otras banderas sean verdaderas.
- Cambio de modo durante persistencia: `MainViewModel` invocará el reinicio de tendencia y decisión; se validará mediante la API pura de sesión.

---

### Task 1: Scaffolding Android y modelo de proximidad

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `app/build.gradle.kts`, `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/{strings,colors,themes}.xml`
- Create: `app/src/test/java/com/cruceseguro/decision/ProximityClassifierTest.kt`
- Create: `app/src/main/java/com/cruceseguro/decision/ProximityLevel.kt`

**Interfaces:**
- Produces: `enum class ProximityLevel`, `fun classifyProximity(distanceMeters: Double?): ProximityLevel`.

- [ ] Escribir pruebas con valores literales para `null`, negativos, no finitos, 30, 15 y 8 metros.
- [ ] Ejecutar `gradlew testDebugUnitTest --tests '*ProximityClassifierTest'` y confirmar fallo por símbolos inexistentes.
- [ ] Crear el proyecto y la implementación mínima de clasificación.
- [ ] Repetir la prueba y confirmar éxito.
- [ ] Commit: `build: scaffold native Android application`.

### Task 2: Procesamiento de movimiento y acercamiento

**Files:**
- Create: `app/src/test/java/com/cruceseguro/processing/WalkingDetectorTest.kt`
- Create: `app/src/test/java/com/cruceseguro/processing/ApproachDetectorTest.kt`
- Create: `app/src/main/java/com/cruceseguro/processing/WalkingDetector.kt`
- Create: `app/src/main/java/com/cruceseguro/processing/ApproachDetector.kt`

**Interfaces:**
- Produces: `WalkingSample`, `WalkingState`, `WalkingDetector.addSample(...)`, `ApproachDetector.addDistance(...)`, `ApproachDetector.reset()`.

- [ ] Escribir pruebas de reposo, caminata sostenida, muestra aislada, tendencia decreciente, ruido, alejamiento y entradas inválidas.
- [ ] Ejecutar las dos clases de prueba y confirmar fallos por APIs ausentes.
- [ ] Implementar ventanas acotadas y umbrales nombrados, sin dependencias Android.
- [ ] Ejecutar toda la suite y confirmar éxito.
- [ ] Commit: `feat: process walking and approach signals`.

### Task 3: Motor de decisión temporal

**Files:**
- Create: `app/src/test/java/com/cruceseguro/decision/DecisionEngineTest.kt`
- Create: `app/src/main/java/com/cruceseguro/decision/DecisionEngine.kt`

**Interfaces:**
- Consumes: `ProximityLevel`.
- Produces: `DecisionInput`, `AlertDecision`, `DecisionEngine.evaluate(input)`, `DecisionEngine.reset()`.

- [ ] Escribir pruebas para cada condición falsa, persistencia menor y exacta de 1,500 ms, ruptura intermedia, cooldown de 20,000 ms, nueva persistencia posterior y `reset()`.
- [ ] Ejecutar `DecisionEngineTest` y confirmar fallo por API ausente.
- [ ] Implementar una máquina de estados basada en milisegundos monotónicos recibidos en la entrada.
- [ ] Ejecutar toda la suite y confirmar éxito.
- [ ] Commit: `feat: add sustained crossing alert decisions`.

### Task 4: Captura y actuación Android

**Files:**
- Create: `app/src/main/java/com/cruceseguro/capture/SensorService.kt`
- Create: `app/src/main/java/com/cruceseguro/capture/LocationService.kt`
- Create: `app/src/main/java/com/cruceseguro/actuation/AlertService.kt`
- Create: `app/src/main/java/com/cruceseguro/model/AppModels.kt`

**Interfaces:**
- Produces: flujos `SensorSnapshot` y `LocationSnapshot`; `AlertService.alert(level)` y `AlertService.close()`.

- [ ] Definir modelos inmutables y contratos pequeños consumibles por el controlador.
- [ ] Implementar registro/liberación de sensores y ubicación con estados explícitos de disponibilidad y permisos.
- [ ] Implementar TTS español, patrones diferenciados de vibración y flash crítico dentro de manejo seguro de excepciones.
- [ ] Compilar `assembleDebug` para verificar contratos Android.
- [ ] Commit: `feat: capture device inputs and deliver alerts`.

### Task 5: Controlador y modos operativo/simulado

**Files:**
- Create: `app/src/main/java/com/cruceseguro/controller/MainUiState.kt`
- Create: `app/src/main/java/com/cruceseguro/controller/MainViewModel.kt`
- Create: `app/src/main/java/com/cruceseguro/controller/MainViewModelFactory.kt`

**Interfaces:**
- Consumes: servicios, detectores y motor de tareas anteriores.
- Produces: `StateFlow<MainUiState>` y acciones `setMode`, `setSimulatedDistance`, `startApproaching`, `startMovingAway`, `stopSimulation`, `createCrossingHere`.

- [ ] Implementar el bucle de simulación 0–50 m y garantizar que el cambio de modo reinicie tendencia/decisión.
- [ ] Combinar muestras reales, calcular distancia geodésica y construir `DecisionInput` con precisión y frescura.
- [ ] Disparar `AlertService` solo cuando el motor produzca una decisión y reflejar estado crítico en UI.
- [ ] Compilar y ejecutar toda la suite.
- [ ] Commit: `feat: coordinate real and simulated crossing modes`.

### Task 6: Interfaz accesible con Jetpack Compose

**Files:**
- Create: `app/src/main/java/com/cruceseguro/MainActivity.kt`
- Create: `app/src/main/java/com/cruceseguro/ui/CrossingScreen.kt`
- Create: `app/src/main/java/com/cruceseguro/ui/theme/{Color,Theme,Type}.kt`

**Interfaces:**
- Consumes: `MainUiState` y acciones del ViewModel.
- Produces: pantalla única accesible con solicitud de permisos en tiempo de ejecución.

- [ ] Crear la pantalla desplazable con distancia, GPS, sensores, tres condiciones, controles por modo y debug expandible.
- [ ] Añadir semántica TalkBack, textos ≥18 sp, controles ≥48 dp, etiquetas textuales y región crítica de alto contraste.
- [ ] Gestionar ubicación y cámara opcional mediante contratos de actividad sin bloquear la app si se deniegan.
- [ ] Ejecutar `lintDebug`, pruebas y `assembleDebug`.
- [ ] Commit: `feat: add accessible crossing status interface`.

### Task 7: Documentación, release y verificación final

**Files:**
- Create: `README.md`
- Create: `docs/documento_tecnico.md`
- Create: `.gitignore`
- Modify: `app/build.gradle.kts`, `app/proguard-rules.pro` si el build release lo requiere.

**Interfaces:**
- Produces: instrucciones reproducibles de apertura, permisos, uso de modos y APK.

- [ ] Documentar propósito, advertencia de seguridad, arquitectura, sensores, modos, instalación y comandos Gradle equivalentes al flujo Android nativo.
- [ ] Documentar el resumen académico del Taller 2 y el flujo físico completo.
- [ ] Ejecutar formateo Kotlin disponible, `testDebugUnitTest`, `lintDebug` y `assembleRelease`.
- [ ] Corregir cada fallo mediante un test regresivo cuando afecte comportamiento.
- [ ] Revisar requisito por requisito contra la especificación y registrar cualquier limitación ambiental verificable.
- [ ] Commit: `docs: complete Android project handoff`.
