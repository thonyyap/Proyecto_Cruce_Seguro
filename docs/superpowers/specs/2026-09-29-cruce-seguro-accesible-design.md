# Diseño: Cruce Seguro Accesible para Android

## Propósito

Crear un prototipo académico Android nativo que ayude a personas con discapacidad visual a identificar la aproximación caminando a un cruce peatonal registrado. La aplicación demuestra de forma observable el flujo **entrada física → procesamiento → decisión → acción física**.

La aplicación es una ayuda experimental y no sustituye bastones, perros guía, semáforos accesibles ni la verificación personal del tránsito.

## Alcance

- Android nativo con Kotlin y Jetpack Compose.
- Sin Firebase, mapas, backend, cuentas, login ni servicios pagos.
- El cruce creado en modo real se conserva solo mientras vive el proceso de la aplicación.
- Orientación principal vertical y una sola pantalla desplazable.
- Interfaz y mensajes de voz en español.

## Arquitectura

La solución separa cinco etapas:

1. **Captura:** `SensorService` publica acelerómetro y giroscopio; `LocationService` publica ubicación y precisión GPS.
2. **Procesamiento:** componentes puros calculan variación del acelerómetro, estado de caminata, distancia y tendencia de acercamiento.
3. **Decisión:** `DecisionEngine` recibe únicamente valores procesados. No conoce APIs de Android ni lee sensores.
4. **Actuación:** `AlertService` coordina vibración, TTS, estado visual crítico y flash opcional.
5. **Interfaz:** `MainViewModel` expone un estado inmutable a una pantalla Jetpack Compose.

Las dependencias se construyen localmente, sin framework de inyección, para mantener el prototipo sencillo.

## Entradas físicas y procesamiento

### Acelerómetro

Se calcula la magnitud del vector y su desviación respecto a la gravedad. Una ventana corta suavizada determina movimiento compatible con caminata y evita que una muestra aislada cambie el estado. El umbral será una constante configurable y la UI mostrará la variación actual.

### Giroscopio y orientación

El giroscopio permanece real en ambos modos. Sus valores se muestran en debug y participan en la validez de la señal de movimiento: datos recientes confirman que la captura física está activa. No se simula su lectura.

### GPS

En modo real se solicitan permisos de ubicación y se reciben actualizaciones con precisión. La distancia al cruce se calcula con la API geodésica de Android. `Crear cruce aquí` copia la ubicación válida actual como centro temporal.

La falta de permiso, ubicación desactivada o ausencia de cruce impide alertar y aparece como estado accesible en pantalla. Una precisión mayor de 25 m se considera insuficiente para una decisión de seguridad y no activa alertas.

### Acercamiento

Una ventana temporal de distancias determina acercamiento cuando la tendencia neta disminuye por un margen mínimo, tolerando ruido del GPS. En modo simulado se usa la misma lógica con la distancia del slider o el movimiento automático.

## Modos

### Simulado

- Slider de 0 a 50 m.
- `Auto-acercar` reduce gradualmente la distancia.
- `Alejar` aumenta gradualmente la distancia.
- `Parar` detiene el cambio automático.
- Solo la distancia es simulada; acelerómetro y giroscopio siguen siendo lecturas físicas reales.

### Real

- Usa la ubicación y precisión del GPS.
- `Crear cruce aquí` registra temporalmente la ubicación actual.
- Si aún no existe un cruce, la distancia aparece como no disponible y no se alerta.

Cambiar de modo reinicia la tendencia de distancia y cualquier temporizador de condiciones para no mezclar muestras.

## Decisión

El motor recibe una instantánea con:

- caminando;
- sensores de movimiento recientes;
- distancia válida;
- dentro de la zona, definida como distancia menor o igual a 30 m;
- acercándose;
- precisión GPS aceptable en modo real;
- instante monotónico actual.

La condición candidata es la conjunción de todas las condiciones aplicables. Debe permanecer verdadera durante al menos **1.5 segundos** de forma continua. Si alguna condición se vuelve falsa, el temporizador se reinicia.

Después de una alerta se aplica un cooldown global de **20 segundos**. Finalizado el cooldown, debe existir nuevamente una condición sostenida completa antes de emitir otra alerta. Esto evita una alerta inmediata al expirar el cooldown.

## Niveles de proximidad y actuación

| Nivel | Distancia | Voz | Vibración | Visual |
|---|---:|---|---|---|
| Preventivo | ≤30 m y >15 m | “Cruce peatonal próximo, continúe con precaución.” | Dos pulsos suaves | Estado amarillo |
| Cercano | ≤15 m y >8 m | “Se está acercando al cruce peatonal.” | Tres pulsos medios | Estado naranja |
| Crítico | ≤8 m | “Atención. Cruce peatonal inmediato. Deténgase y verifique antes de cruzar.” | Patrón fuerte y repetido | Pantalla negra/amarilla de alto contraste |

La vibración usa amplitud cuando el dispositivo la admite y un patrón compatible en versiones anteriores. TTS se configura en español y sus fallos no cierran la aplicación. El flash se intenta solo para la alerta crítica; falta de cámara, permiso, hardware o error se ignoran de forma segura después de actualizar el estado de disponibilidad.

## Interfaz y accesibilidad

La pantalla muestra:

- selector claro de modo simulado/real;
- distancia destacada al cruce;
- nivel de proximidad;
- estado de GPS y precisión;
- estado de acelerómetro y giroscopio;
- indicadores `Caminando`, `Cerca del cruce` y `Acercándose`;
- controles de simulación o botón `Crear cruce aquí` según el modo;
- sección de debug expandible con variación del acelerómetro, ejes del giroscopio, distancia, tendencia y precisión GPS.

Se usan contraste alto, tipografía grande, objetivos táctiles de al menos 48 dp, etiquetas no dependientes solo del color y semántica de Compose para TalkBack. Los cambios críticos se anuncian como región activa.

## Permisos y plataforma

El manifiesto declara ubicación precisa, vibración, cámara y flash. La ubicación se solicita en tiempo de ejecución. La cámara es opcional: negar su permiso no bloquea el flujo principal. La aplicación no solicita ubicación en segundo plano y funciona únicamente en primer plano.

## Manejo de errores

- Sensores no disponibles: estado visible, sin decisión de alerta.
- GPS o permiso no disponibles: orientación accesible para corregirlo, sin cierre inesperado.
- Precisión insuficiente: se muestra el dato y se suspende la decisión real.
- TTS, vibración o flash no disponibles: se conservan los demás canales de alerta.
- Ciclo de vida: suscripciones y TTS se liberan al destruir el controlador correspondiente.

## Pruebas y aceptación

Las pruebas unitarias cubren:

- clasificación exacta de 30 m, 15 m y 8 m;
- conjunción de caminata, zona y acercamiento;
- reinicio al romper una condición antes de 1.5 s;
- alerta al mantener condiciones durante 1.5 s;
- bloqueo durante 20 s y nueva persistencia tras el cooldown;
- tendencia de acercamiento con ruido pequeño;
- ausencia de alerta con precisión GPS inválida;
- reinicio al cambiar de modo.

La entrega se verifica con formateo Kotlin, pruebas unitarias, análisis de Android Lint y ensamblado de un APK de release. Si el SDK o Gradle no están disponibles en el equipo, los archivos se preservarán y se documentará exactamente qué verificaciones no pudieron ejecutarse.

## Entregables

- Proyecto Gradle compatible con Android Studio.
- Código Kotlin organizado en `capture`, `processing`, `decision`, `actuation`, `controller` y `ui`.
- Pruebas unitarias de procesamiento y decisión.
- `README.md` con arquitectura, instalación, ambos modos y generación de APK.
- `docs/documento_tecnico.md` resumido para el informe del Taller 2.
