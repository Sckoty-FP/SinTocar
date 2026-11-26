# Informe de herramientas - Sin Tocar

## 1. Ficha de herramientas (Tema 2)

| Capacidad | Móvil (Android) | Ordenador (escritorio JVM) | Procesa en el dispositivo | Coste |
|-----------|-----------------|----------------------------|---------------------------|-------|
| Postura del cuerpo | ML Kit Pose Detection (Google), 33 puntos | MediaPipe Pose en ONNX, ejecutado con OpenCV DNN | Sí / Sí | Gratis |
| Imagen de la cámara | CameraX | OpenCV `VideoCapture` (webcam) | Sí / Sí | Gratis |
| Voz → texto | `SpeechRecognizer` del sistema (Google) | Vosk, modelo pequeño en español (~40 MB) | Depende del móvil / Sí, sin conexión | Gratis |
| Texto → voz | `TextToSpeech` del sistema | Voz de Windows (System.Speech) | Sí / Sí | Gratis |
| Gestos de la mano | MediaPipe Gesture Recognizer (`gesture_recognizer.task`), 8 gestos | Palma + 21 puntos de la mano en ONNX (OpenCV Zoo) y reglas por dedos | Sí / Sí | Gratis |
| Cara y sonrisa | ML Kit Face Detection (probabilidad de sonrisa) | Clasificadores Haar de OpenCV (cara y sonrisa) | Sí / Sí | Gratis |
| Realidad aumentada | ARCore + SceneView (planos del suelo y anclas) | Marcador ArUco impreso de 15 cm + `solvePnP` (OpenCV) | Sí / Sí | Gratis |

Alternativas consideradas:
- **MediaPipe Tasks en Android:** descartado. ML Kit ya da la postura con menos configuración, y es la que se usó en el Tema 6.
- **Servicio de voz en la nube (p. ej. Google Cloud Speech):** descartado. Enviaría la voz a un servidor (es un dato personal según el RGPD) y cuesta dinero.

## 2. Tabla de medidas (Temas 5 y 6)

Condiciones: en casa, con el dispositivo a unos 2,5 m. Probé con luz normal y con menos luz (sin llegar a oscuro), y en silencio y con la tele encendida a volumen bajo. Móvil Android y portátil con su webcam integrada.

Los tiempos están medidos a ojo con el reloj y con el historial de órdenes de la pantalla Resumen, que guarda la hora de cada orden. Lo que no pude medir lo pongo como «no medido» en vez de inventarlo.

### Voz
| Medida | Móvil | Ordenador |
|--------|-------|-----------|
| Órdenes obedecidas | 8/10 («pausa» y «sigue» alternadas) | 3/10 (10 órdenes distintas) |
| Tiempo desde que acabo de hablar hasta que reacciona | ~3 s | ~10 s |
| Tiempo mínimo entre dos órdenes seguidas (escucha → respuesta hablada → vuelve a escuchar) | 4 a 6 s | no medido |
| Palabras que no son órdenes y responde «No te he entendido» | 4/5 | no medido |
| Órdenes falsas en 1 minuto sin hablar | 0 (también con la tele encendida) | no medido |

En el móvil, las 2 órdenes que fallaron fueron porque hablé antes de que el micrófono volviera a escuchar. Mientras la app habla, la escucha se para a propósito, para que no se obedezca a sí misma (requisito 5). Con la tele a volumen bajo me seguía entendiendo.

### Postura
| Medida | Móvil | Ordenador |
|--------|-------|-----------|
| Sentadillas contadas de 10 | 10/10 | no medido |
| Zancadas contadas de 10 | 10/10 | 2/10 |
| Brazos en cruz detectados | 5/5 | 1 vez en varios intentos |
| Imágenes por segundo | no medido (la app no lo muestra) | no medido |
| Distancia mínima con encuadre correcto | 2,5 m | más de la que tengo en casa |
| Distancia máxima con encuadre correcto | 8 m o más | no medido |

En el móvil, cuando no bajaba del todo en la sentadilla, no la contaba y decía «Baja más». Si estaba demasiado cerca decía «Aléjate».

### Limitaciones que encontré
- **Una pierna tapada no se detecta.** A 8 m, detrás de un sofá que me tapaba una pierna, el marcador seguía diciendo «Te veo bien». Pasa porque el valor que usamos de ML Kit (`inFrameLikelihood`) dice si el punto está dentro de la imagen, no si se ve. ML Kit se inventa dónde está la articulación tapada.
- **Las zancadas son permisivas.** En el móvil contó también alguna zancada que no estaba del todo bien hecha. El umbral es bajo porque de lado la cámara ve peor la rodilla de atrás.
- **El pitido de Android.** En algunos móviles suena un pitido cada vez que la escucha vuelve a empezar. Lo hace el sistema, no la app.

### Gestos, cara y RA (Temas 7 y 8)
| Medida | Móvil | Ordenador |
|--------|-------|-----------|
| Palma abierta → pausa, de 10 | __/10 | __/10 |
| Pulgar arriba → siguiente, de 10 | __/10 | __/10 |
| Distancia máxima a la que reconoce la mano | __ m | __ m |
| Sonrisa → favorito, de 5 | __/5 | __/5 |
| Falsos gestos en 1 minuto de entrenamiento | __ | __ |
| Encuentra el suelo (tiempo aproximado) | __ s | no aplica (marcador) |
| Distancia que da la Zona RA frente a la real (medida con metro) | __ m / __ m | __ m / __ m |

## 3. Decisión justificada

**El dispositivo recomendado es el móvil.** Con ML Kit y `SpeechRecognizer` obedeció 8 de 10 órdenes, contó 10 de 10 sentadillas y zancadas y detectó 5 de 5 brazos en cruz, a 2,5 m y en condiciones normales de casa (poca luz, tele de fondo).

En el ordenador la app funciona con el mismo `commonMain`, pero las medidas son bastante peores: 3 de 10 órdenes, unos 10 s de espera y 2 de 10 zancadas. Las causas que creo más probables son:
- La webcam del portátil tiene menos ángulo y menos resolución que la cámara del móvil. Para verme entero tengo que alejarme mucho, y de lejos los puntos del cuerpo salen menos fiables.
- El modelo pequeño de Vosk acierta menos que el reconocedor de Google del móvil.
- El portátil hace la postura y la voz a la vez con el procesador, sin tarjeta gráfica, y eso lo hace más lento.

Aun así, mantengo Vosk en el ordenador porque funciona sin Internet y la voz no sale del equipo. La alternativa en la nube acertaría más, pero envía datos personales a un servidor y cuesta dinero. Para que el ordenador se pueda usar igualmente, la app tiene dos respaldos: el campo de **orden escrita** y el **maniquí** con deslizadores. Y todo se puede hacer también con los botones. Si se usa en el ordenador, recomiendo una webcam externa y una habitación con espacio para ponerse a 3 m o más.
