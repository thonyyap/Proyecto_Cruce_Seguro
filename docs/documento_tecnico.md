# Documento técnico resumido — Taller 2

## 1. Nombre del proyecto

**Cruce Seguro Accesible**

## 2. Problema

Las personas con discapacidad visual pueden necesitar información adicional al aproximarse a un cruce peatonal. El prototipo estudia cómo combinar sensores de un teléfono Android para advertir una aproximación, sin pretender reemplazar ayudas de movilidad ni infraestructura vial accesible.

## 3. Objetivo

Desarrollar una aplicación Android nativa que detecte simultáneamente que el usuario camina, se encuentra dentro de una zona de 30 m de un cruce registrado y se está acercando. Si las condiciones se mantienen 1.5 s, la aplicación genera una alerta física y sonora. Un cooldown de 20 s evita repeticiones.

## 4. Flujo entrada–procesamiento–decisión–acción

### Entrada física

- **Acelerómetro:** mide la aceleración en tres ejes.
- **Giroscopio:** mide la velocidad angular y valida actividad reciente de los sensores.
- **GPS:** entrega latitud, longitud y precisión.

### Procesamiento

- La magnitud del acelerómetro y su variación respecto a la gravedad se acumulan en una ventana para estimar caminata.
- Android calcula la distancia geodésica desde la ubicación actual hasta el cruce.
- Una ventana de cuatro distancias determina si existe una reducción neta suficiente, tolerando ruido pequeño.

### Decisión

El motor puro recibe datos ya procesados. La condición lógica es:

```text
caminando AND sensores recientes AND distancia <= 30 m
AND acercándose AND ubicación válida
```

Debe mantenerse 1,500 ms. Después de una alerta se aplica un cooldown de 20,000 ms. El motor no depende de clases Android, lo que permite pruebas unitarias deterministas.

### Acción física

- Nivel preventivo (hasta 30 m): vibración suave y voz informativa.
- Nivel cercano (hasta 15 m): vibración media y voz de acercamiento.
- Nivel crítico (hasta 8 m): vibración fuerte, voz urgente, pantalla de alto contraste y flash opcional.

## 5. Arquitectura

| Capa | Responsabilidad |
|---|---|
| `capture` | Lectura de SensorManager y Fused Location Provider |
| `processing` | Detección de caminata y tendencia de distancia |
| `decision` | Clasificación y máquina temporal de alertas |
| `actuation` | Vibración, TTS y linterna |
| `controller` | Combinación de flujos y estado de aplicación |
| `ui` | Jetpack Compose y accesibilidad |

Esta separación garantiza que la lógica de decisión no lea sensores directamente.

## 6. Modos

### Simulado

La distancia se controla con un slider de 0 a 50 m o con los botones Auto-acercar, Alejar y Parar. Acelerómetro y giroscopio conservan lecturas físicas reales.

### Real

La app usa GPS. El botón Crear cruce aquí guarda en memoria la ubicación actual como centro temporal. Una precisión peor que 25 m suspende las decisiones para reducir falsos positivos.

## 7. Accesibilidad

La interfaz usa alto contraste, textos grandes, controles táctiles amplios, indicadores textuales y semántica para TalkBack. La alerta crítica se anuncia como región activa y ocupa la pantalla con colores negro y amarillo.

## 8. Seguridad y tolerancia a fallos

El prototipo muestra una advertencia de uso académico. Si faltan sensores, permisos, GPS, TTS, vibración, cámara o flash, la app conserva los canales disponibles y evita cierres inesperados. No guarda ubicaciones permanentemente ni usa servicios externos.

## 9. Pruebas

Las pruebas JUnit comprueban:

- límites exactos de 30, 15 y 8 m;
- rechazo de distancias nulas, negativas o no finitas;
- reposo, caminata repetida y movimiento aislado;
- acercamiento, alejamiento y ruido de GPS;
- obligación de todas las condiciones;
- persistencia exacta de 1.5 s;
- reinicio al romper condiciones;
- cooldown de 20 s y reinicio explícito.

Además, Android Lint valida el proyecto y Gradle ensambla APK de debug y release.

## 10. Limitaciones y trabajo futuro

- Los umbrales requieren calibración con distintos teléfonos y estilos de marcha.
- Un cruce creado en esta versión solo permanece durante la sesión.
- La precisión GPS urbana puede degradarse entre edificios.
- No existe validación de tráfico, semáforos ni dirección real de la calle.
- Una versión futura podría usar cruces verificados localmente y pruebas de campo supervisadas, después de evaluación ética y de seguridad.

## 11. Conclusión

El proyecto demuestra un sistema ciberfísico pequeño: recibe señales físicas, las procesa, combina condiciones temporales y produce acciones físicas accesibles. La separación por capas permite explicar, probar y modificar cada parte de manera independiente.
