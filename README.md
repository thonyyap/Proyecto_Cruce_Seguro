# Cruce Seguro Accesible

Prototipo académico Android que advierte a una persona con discapacidad visual cuando se aproxima caminando a un cruce peatonal registrado. Combina acelerómetro, giroscopio y GPS antes de emitir alertas mediante vibración, voz en español y una pantalla crítica de alto contraste.

> **Aviso:** es una demostración académica. No reemplaza el bastón, perro guía, semáforo accesible ni la comprobación personal del tránsito.

## Tecnología

La implementación es **Android nativa con Kotlin y Jetpack Compose**. Durante la definición del proyecto se eligió Android nativo porque Flutter no era obligatorio para el Taller 2. No utiliza Firebase, Google Maps, backend, login ni servicios pagos.

Requisitos principales:

- Android Studio Quail o compatible.
- Android SDK 37 y Build Tools 36.0.0.
- JDK 17 o el JBR incluido con Android Studio.
- Dispositivo Android 8.0 (API 26) o superior. Se recomienda un teléfono físico para probar sensores, GPS, vibración, TTS y flash.

## Flujo del sistema

```text
ENTRADA FÍSICA
  acelerómetro + giroscopio + GPS
        ↓
PROCESAMIENTO
  caminata + distancia + tendencia de acercamiento
        ↓
DECISIÓN
  caminando ∧ dentro de 30 m ∧ acercándose ∧ datos válidos
  durante 1.5 s, con cooldown de 20 s
        ↓
ACCIÓN FÍSICA
  vibración + voz en español + pantalla crítica + flash opcional
```

## Arquitectura

El código está separado por responsabilidad:

- `capture`: acceso a acelerómetro, giroscopio y ubicación.
- `processing`: detectores puros de caminata y acercamiento.
- `decision`: niveles de proximidad y motor temporal; nunca lee sensores directamente.
- `actuation`: vibración, Text-to-Speech y flash tolerante a fallos.
- `controller`: coordinación de señales, modos y estado de interfaz.
- `ui`: pantalla Compose accesible y tema de alto contraste.
- `model`: modelos inmutables compartidos.

El cruce real vive solo en memoria y se elimina al cerrar el proceso de la aplicación.

## Sensores y decisión

### Acelerómetro

Se calcula la magnitud del vector de aceleración y la variación respecto a la gravedad. Una ventana de ocho muestras exige movimiento repetido, reduciendo activaciones por un único golpe o movimiento aislado.

### Giroscopio

Sus tres ejes aparecen en debug. Una muestra reciente del giroscopio es obligatoria para considerar válidos los sensores de movimiento. Sigue siendo real incluso en el modo simulado.

### GPS

El modo real obtiene actualizaciones de alta precisión. La decisión se suspende si no hay permiso, el GPS está desactivado, no existe un cruce o la precisión es peor que 25 m.

### Acercamiento

Una ventana de cuatro distancias exige una disminución neta mínima de 1.5 m y al menos dos pasos decrecientes. Esto tolera pequeñas oscilaciones del GPS.

### Persistencia y cooldown

Todas las condiciones deben mantenerse durante 1,500 ms. Cualquier condición falsa reinicia el temporizador. Tras alertar se bloquean nuevas alertas durante 20,000 ms y después se exige una nueva persistencia completa.

## Niveles de alerta

| Distancia | Nivel | Respuesta |
|---:|---|---|
| 15–30 m | Preventivo | Dos pulsos suaves y aviso de cruce próximo |
| 8–15 m | Cercano | Tres pulsos medios y aviso de acercamiento |
| 0–8 m | Crítico | Vibración fuerte, voz urgente, pantalla negra/amarilla y flash opcional |

Si TTS, vibración o flash fallan, los otros canales continúan funcionando y la app no se cierra.

## Modo simulado

1. Seleccione **Simulado**.
2. Ajuste la distancia entre 0 y 50 m con el slider.
3. Use **Auto-acercar**, **Alejar** o **Parar**.
4. Camine con el teléfono para que el acelerómetro y el giroscopio reales satisfagan las condiciones.

Únicamente se simula la distancia. Los sensores de movimiento no se falsifican.

## Modo real

1. Conceda ubicación precisa y active el GPS.
2. Espere una precisión aceptable (25 m o mejor).
3. Sitúese en el centro de un cruce de prueba seguro y pulse **Crear cruce aquí**.
4. Aléjese y después aproxímese caminando para probar la alerta.
5. El botón **Habilitar flash opcional** permite autorizar la cámara. Negar este permiso no afecta las alertas principales.

Nunca haga pruebas dentro de tráfico activo. Utilice un entorno controlado y otra persona como supervisora.

## Accesibilidad

- Tema oscuro de alto contraste con amarillo de seguridad.
- Textos de 18 sp o más y controles de al menos 56 dp.
- Estados expresados con texto, no solo con color.
- Descripciones semánticas para TalkBack.
- Región activa asertiva para la alerta crítica.
- Distancia y condiciones principales visibles antes de los datos de depuración.

## Abrir y ejecutar

1. Abra Android Studio.
2. Seleccione **Open** y elija esta carpeta.
3. Espere la sincronización de Gradle.
4. Conecte un dispositivo con depuración USB o cree un emulador.
5. Ejecute la configuración `app`.

Desde PowerShell, si Java no está en `PATH`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

## Pruebas y calidad

```powershell
.\gradlew.bat ktlintFormat
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

Las pruebas unitarias cubren umbrales de proximidad, entradas inválidas, caminata, ruido de distancia, tendencia, conjunción de condiciones, persistencia, reinicio y cooldown.

## Generar APK

APK de depuración instalable:

```powershell
.\gradlew.bat assembleDebug
```

Resultado: `app/build/outputs/apk/debug/app-debug.apk`.

APK release sin firma de distribución:

```powershell
.\gradlew.bat assembleRelease
```

Resultado: `app/build/outputs/apk/release/app-release-unsigned.apk`. Para publicar o distribuir formalmente, configure una clave propia y firme el APK desde Android Studio con **Build > Generate Signed App Bundle or APK**.

## Permisos

- `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION`: distancia real.
- `VIBRATE`: patrones hápticos.
- `CAMERA`: control opcional del flash.

No se solicita ubicación en segundo plano ni se envían datos a servidores.
