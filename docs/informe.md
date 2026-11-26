# Informe de herramientas - Sin Tocar

> Borrador para convertir a PDF. **Las celdas «__» se rellenan midiendo con el dispositivo real a 2 o 3 m.** No inventes números: el profesor puede pedirte que lo repitas delante de él.

## 1. Ficha de herramientas (Tema 2)

| Capacidad | Móvil (Android) | Ordenador (escritorio JVM) | Procesa en el dispositivo | Coste |
|-----------|-----------------|----------------------------|---------------------------|-------|
| Postura del cuerpo | ML Kit Pose Detection (Google), 33 puntos | MediaPipe Pose en ONNX, ejecutado con OpenCV DNN | Sí / Sí | Gratis |
| Imagen de la cámara | CameraX | OpenCV `VideoCapture` (webcam) | Sí / Sí | Gratis |
| Voz → texto | `SpeechRecognizer` del sistema (Google) | Vosk, modelo pequeño en español (~40 MB) | Depende del móvil / Sí, sin conexión | Gratis |
| Texto → voz | `TextToSpeech` del sistema | Voz de Windows (System.Speech) | Sí / Sí | Gratis |

Alternativas consideradas:
- **MediaPipe Tasks en Android:** descartado. ML Kit ya da la postura con menos configuración, y es la que se usó en el Tema 6.
- **Servicio de voz en la nube (p. ej. Google Cloud Speech):** descartado. Enviaría la voz a un servidor (RGPD) y cuesta dinero.

## 2. Tabla de medidas (Temas 5 y 6)

Condiciones: distancia __ m, luz __, ruido de fondo __.

### Voz (10 órdenes de cada tipo)
| Medida | Móvil | Ordenador |
|--------|-------|-----------|
| Aciertos de 10 «pausa» | __/10 | __/10 |
| Aciertos de 10 «siguiente» | __/10 | __/10 |
| Latencia desde que acabas de hablar hasta que reacciona (media) | __ s | __ s |
| Falsas órdenes en 1 minuto de entrenamiento sin hablar | __ | __ |

### Postura (10 repeticiones de cada tipo)
| Medida | Móvil | Ordenador |
|--------|-------|-----------|
| Sentadillas contadas de 10 | __/10 | __/10 |
| Zancadas contadas de 10 | __/10 | __/10 |
| Brazos en cruz detectados de 5 | __/5 | __/5 |
| Imágenes por segundo aproximadas | __ fps | __ fps |
| Distancia máxima con encuadre correcto | __ m | __ m |

## 3. Decisión justificada

(Escríbela con los números de arriba. Ejemplo de estructura: «En el móvil se usa ML Kit + SpeechRecognizer porque … (__/10 aciertos, __ s). En el ordenador se usa Vosk porque funciona sin conexión y … aunque … Por eso la app …».)
