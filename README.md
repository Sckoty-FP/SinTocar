# Sin Tocar

App para entrenar en casa sin tocar el dispositivo. Cuenta sentadillas y zancadas con la cámara y obedece a la voz y a la postura. Funciona en el móvil Android y en el ordenador con el mismo código común (`commonMain`) (Kotlin Multiplatform + Compose Multiplatform).

Práctica de Desarrollo de Interfaces (2.º DAM).

## 1. Guía de uso

### Pantallas
1. **Inicio.** Concede la cámara y el micrófono en las tarjetas de permiso. Cada una explica para qué sirve. Si rechazas alguno, la app sigue funcionando con los botones. Después elige la rutina, o pulsa «Zona RA» para colocar antes la esterilla.
2. **Zona RA.** Coloca una esterilla virtual en el suelo con realidad aumentada y te dice a qué distancia estás. Es opcional: sin RA se puede entrenar igual. Abajo hay un botón para empezar el entrenamiento.
3. **Transición.** Antes de entrenar, un aviso explica que la app cambia a la cámara frontal para verte mientras entrenas. Se pulsa «Vale» para seguir.
4. **Entrenamiento.** Pon el móvil (o colócate frente al ordenador) a 2 o 3 m, para que se te vea de la cabeza a los pies. El marcador de arriba se pone verde cuando el encuadre es correcto. Empieza diciendo «empezar» o pulsando ▶. Al llegar al objetivo, la app lo anuncia en pantalla y en voz alta, y a los 3 s pasa al siguiente ejercicio.
5. **Resumen.** Muestra las repeticiones por ejercicio, el tiempo y las últimas órdenes con su origen. Sonríe 0,8 s o pulsa ☆ para guardar la rutina como favorita.

### Rutinas

| Rutina | Ejercicios (repeticiones) |
|--------|---------------------------|
| Suave | Sentadillas 8 · Zancadas 8 · Sentadillas 8 |
| Intensa | Sentadillas 15 · Zancadas 16 · Sentadillas 20 |

### Órdenes admitidas

| Acción | Voz | Gesto | Postura | Cara | Toque |
|--------|-----|-------|---------|------|-------|
| Empezar / seguir | «empezar», «sigue» | — | Brazos en cruz 2 s (en pausa) | — | Botón |
| Pausa | «pausa», «para» | Palma abierta | Brazos en cruz 2 s | — | Botón |
| Siguiente ejercicio | «siguiente» | Pulgar arriba | — | — | Botón |
| Terminar | «terminar» | — | — | — | Botón (con confirmación) |
| Ayuda | «ayuda» | — | — | — | Botón |
| Favorito (Resumen) | — | — | — | Sonrisa 0,8 s | Botón |
| Colocar / quitar (Zona RA) | «colocar», «coloca», «pon» / «quitar», «quita», «borrar» | — | — | — | Toque en el suelo / botón |

- La misma acción repetida en menos de 1 s, venga de donde venga, se ignora. El Resumen lo muestra como «ignorada».
- Si la app oye algo que no es una orden, dice «No te he entendido» y la pantalla sugiere una orden válida.
- **Sin encuadre correcto o en pausa no se cuenta**, y la pantalla dice por qué.
- **Cómo hacer la zancada:** ponte un poco de lado a la cámara (unos 45°). Da un paso largo adelante, baja hasta que las dos rodillas queden más o menos en ángulo recto y vuelve. Eso cuenta una; la siguiente, con la otra pierna. Si no bajas lo suficiente, la pantalla dice «Baja más» en naranja.
- **Brazos en cruz:** estira los brazos a la altura de los hombros y mantén la postura 2 s. La barra amarilla se llena. Si las muñecas se salen de la imagen, la app usa los codos.

### Cómo hacer los gestos y la sonrisa
- **Gestos de mano (palma abierta y pulgar arriba):** pon la mano a 40 o 60 cm de la cámara, con la palma hacia ella y con buena luz. Mantén el gesto un momento: el cuadro «Acción mantenida» muestra cuánto falta y pone «¡Hecho!» cuando cuenta.
- **Sonrisa:** en el Resumen, mira a la cámara y sonríe sin moverte durante 0,8 s. Si no lo detecta, usa el botón ☆.

### Cómo usar la Zona RA
- **Móvil:** necesita «Servicios de Google Play para RA» (ARCore). Mueve el móvil despacio apuntando al suelo hasta que salga la rejilla y toca para colocar la esterilla. También vale decir «colocar» y «quitar».
- **Ordenador:** imprime `docs/marcador_7.png` con el cuadrado a 15 cm de lado y enséñalo a la cámara en el suelo. Si no tienes impresora, activa «Sin cámara» y usa la imagen de prueba con el marcador.
- Si la RA falla, el entrenamiento funciona igual, sin la esterilla.

### Accesibilidad
Los estados se ven con color y con texto (nunca solo color). El indicador de voz, el contador y los avisos son regiones vivas, así que un lector de pantalla los anuncia al cambiar. Todos los botones miden 56 dp o más.

### Elegir cámara y micrófono (ordenador)
En **Dispositivos** (en Inicio y al final de Entrenamiento) eliges la cámara («Cámara 0, 1, 2…») y el micrófono. Si tienes una cámara virtual (OBS, por ejemplo), suele ser la 0: prueba otro número hasta que aparezcas tú. La elección se recuerda entre aperturas.

### Respaldos sin hardware (no sustituyen a la cámara y al micrófono reales)
- **Simulador de gestos y cara**: botones para elegir el gesto de la mano y deslizadores para la sonrisa, que mandan los datos como si vinieran de la cámara.
- **Maniquí**: el interruptor «Sin cámara: usar el maniquí» mueve un cuerpo virtual con dos deslizadores (rodilla y brazos).
- **Orden escrita** (solo en el ordenador): una frase escrita sigue el mismo camino que una hablada.

## 2. Guía de instalación

Requisitos: Android Studio o IntelliJ IDEA con el plugin de Kotlin Multiplatform, JDK 17 o superior, y un móvil Android 8.0 (API 26) o superior con la depuración USB activada.

1. *File → Open* → esta carpeta. Espera a que Gradle sincronice.
2. **Ordenador:** panel Gradle → `desktopApp → Tasks → compose desktop → run`, o bien `./gradlew :desktopApp:run`.
   La primera compilación descarga con Internet los modelos que quedan fuera del repositorio:
   - Voz (Vosk, español, unos 40 MB) → `~/.vosk/vosk-model-small-es-0.42`
   - Postura, mano y cara (ONNX de OpenCV Zoo y Haar de OpenCV) → `~/.di-modelos/`
3. **Móvil:** ejecuta la configuración **androidApp ▶** con el móvil conectado. El reconocimiento de voz usa el del sistema (Google), que puede necesitar conexión según el móvil.
4. **Tests:** `./gradlew :shared:jvmTest`.

### Generar los ejecutables
- **APK:** `./gradlew :androidApp:assembleDebug` → `androidApp/build/outputs/apk/debug/`.
- **.exe de Windows:** `./gradlew :desktopApp:createDistributable -PjdkEmpaquetado=<ruta de un JDK 25 con jpackage>` → `desktopApp/build/compose/binaries/main/app/SinTocar/`. No saques `SinTocar.exe` de su carpeta (al lado van `app\` y `runtime\`). El JDK de empaquetado tiene que ser **de la misma versión** con la que compila Gradle: si es más antiguo, el .exe se cierra sin mostrar ningún error.
- El .exe usa los modelos de `~/.vosk` y `~/.di-modelos`. En otro PC hay que compilar `desktopApp` una vez con Internet para que se descarguen.

### Si algo falla en el ordenador
`SinTocar.exe --diagnostico` no abre ninguna ventana: escribe `~/sintocar-diagnostico.txt` con los micrófonos que ve Java, los formatos que abre cada uno y si el modelo de Vosk carga. Si la carpeta `~/.vosk/vosk-model-small-es-0.42` está vacía, bórrala y vuelve a compilar `desktopApp`.

## 3. Guía técnica

### Arquitectura

```
Voz (Reconocedor)  ─┐
Postura (cámara)   ─┼─► Orden(accion, origen) ─► ControlEntrenamiento ─► estado ─► pantallas
Toque (botones)    ─┘                                   │
                                                        └─► anunciar(frase) ─► Hablante (voz)
```

- **`ControlEntrenamiento`** (commonMain) es el único punto de entrada de las órdenes. Es Kotlin puro con el reloj inyectado, así que se prueba sin dispositivo: debounce de 1 s, avance a los 3 s, no contar sin encuadre ni en pausa, tiempo sin pausas e historial con el origen.
- **`Ordenes.kt`** contiene el mapa de voz (`interpretar`, que compara palabras enteras para que «siguiente» no se confunda con «sigue») y **todas** las frases que dice la app (`Frases`). Un test comprueba que ninguna frase contiene una orden, para que la app no se obedezca a sí misma (requisito 5).
- **`EscuchaContinua`** convierte el reconocedor de una sola frase del Tema 5 en escucha continua, y la corta mientras la app habla.
- **`ContadorEjercicio`** generaliza el contador del Tema 6: dos umbrales con histéresis por ejercicio, más la detección de movimiento incompleto («Baja más»).
- **`expect`/`actual`**: cada plataforma da su cámara con postura, su reconocedor, su voz, sus permisos y el selector de dispositivos (que en Android no muestra nada).
- **`Dispositivos.jvm.kt`** abre el micrófono siempre a través de un mezclador concreto, probando 16, 48 y 44,1 kHz en mono o estéreo (`aMono` pasa el estéreo a mono). Vosk recibe la frecuencia real y remuestrea.

| Pieza | Android (`androidMain`) | Ordenador (`jvmMain`) |
|-------|-------------------------|-----------------------|
| Postura | CameraX + ML Kit Pose | OpenCV + MediaPipe Pose (ONNX) |
| Voz → texto | `SpeechRecognizer` | Vosk (sin conexión) |
| Texto → voz | `TextToSpeech` | Voz del sistema (System.Speech en Windows) |
| Permisos | Diálogo del sistema | Los gestiona el sistema operativo |

### Estructura

```
shared/src/commonMain   Lógica y pantallas comunes (todo lo que se ve)
shared/src/androidMain  Cámara, voz y permisos de Android
shared/src/jvmMain      Webcam, Vosk y voz de escritorio
shared/src/commonTest   Tests de la lógica (se ejecutan en la JVM)
androidApp              Arranque Android (MainActivity)
desktopApp              Arranque de escritorio (main.kt) y descarga de modelos
```

### Privacidad
La imagen y el audio se procesan en el dispositivo. No se guardan ni se envían a ningún servidor, con una excepción: el reconocedor de voz de Google en Android puede usar su servicio en línea según la configuración del móvil.

## Licencias de modelos 3D y de IA/visión

| Modelo | Uso en la app | Origen | Licencia |
|--------|---------------|--------|----------|
| `esterilla.glb` (modelo 3D) | Esterilla de la Zona RA en Android | Generado con el script `generar_esterilla.py` (NuiLab). Es material de clase del Tema 8 | Viene del material del profesor y no indica licencia |
| Postura MediaPipe, ONNX (`pose_estimation_mediapipe_2023mar.onnx`) | Postura en el ordenador | OpenCV Zoo: https://github.com/opencv/opencv_zoo | Apache 2.0 |
| Palma y mano, ONNX (`palm_detection_mediapipe_2023feb.onnx`, `handpose_estimation_mediapipe_2023feb.onnx`) | Gestos en el ordenador | OpenCV Zoo: https://github.com/opencv/opencv_zoo | Apache 2.0 |
| Haar de OpenCV (`haarcascade_frontalface_default.xml`, `haarcascade_smile.xml`) | Cara y sonrisa en el ordenador | OpenCV: https://github.com/opencv/opencv/tree/4.x/data/haarcascades | Licencia de OpenCV (los ficheros llevan su aviso dentro) |
| `gesture_recognizer.task` | Gestos de mano en Android | MediaPipe (Google): https://ai.google.dev/edge/mediapipe/solutions/vision/gesture_recognizer | Apache 2.0 |
| Vosk `vosk-model-small-es-0.42` | Voz a texto en el ordenador | Alpha Cephei: https://alphacephei.com/vosk/models | Apache 2.0 |
| ML Kit (postura y cara) | Postura y sonrisa en Android | Google: https://developers.google.com/ml-kit | Ver la página de origen |
| ARCore / SceneView | RA en Android | Google y SceneView | Ver la página de origen |

Los modelos de la tabla que se descargan al compilar no están en el repositorio.

## Autoría
Trabajo individual: **Elias Jesus Blanco Tovar**.

Parte del código sale de los ejemplos de los Temas 4 a 8 de clase.
