# Sin Tocar

App para entrenar en casa sin tocar el dispositivo. Cuenta sentadillas y zancadas con la cámara y obedece a la voz y a la postura. Funciona en el móvil Android y en el ordenador con el mismo código común (`commonMain`) (Kotlin Multiplatform + Compose Multiplatform).

Práctica del RA2 de Desarrollo de Interfaces (2.º DAM), criterios a, b, c y d.

## 1. Guía de uso

1. **Inicio.** Concede la cámara y el micrófono en las tarjetas de permiso. Cada una explica para qué sirve el permiso. Si rechazas alguno, la app sigue funcionando con los botones. Después elige la rutina.
2. **Entrenamiento.** Pon el móvil (o colócate frente al ordenador) a 2 o 3 m, para que se te vea de la cabeza a los pies. El marcador de arriba se pone verde cuando el encuadre es correcto. Empieza diciendo «empezar», cruzando los brazos 2 segundos o pulsando ▶.
3. Al llegar al objetivo, la app lo anuncia en pantalla y en voz alta, y a los 3 s pasa al siguiente ejercicio. Si dices «pausa» antes, espera.
4. **Resumen.** Muestra las repeticiones por ejercicio, el tiempo y las últimas órdenes con su origen. Di «favorito» o pulsa ☆ para guardar la rutina.

### Rutinas

| Rutina | Ejercicios (repeticiones) |
|--------|---------------------------|
| Suave | Sentadillas 8 · Zancadas 8 · Sentadillas 8 |
| Intensa | Sentadillas 15 · Zancadas 16 · Sentadillas 20 |

### Órdenes admitidas

| Acción | Voz | Postura | Toque |
|--------|-----|---------|-------|
| Empezar / seguir | «empezar», «sigue» | Brazos en cruz 2 s (al principio o en pausa) | Botón ▶ |
| Pausa | «pausa», «para» | Brazos en cruz 2 s (en marcha) | Botón ⏸ |
| Siguiente ejercicio | «siguiente» | No | Botón ⏭ |
| Terminar | «terminar» | No | Botón ■ con confirmación |
| Ayuda | «ayuda» | No | Botón ? |
| Favorito (Resumen) | «favorito» | No | Botón ☆ |

- La misma acción repetida en menos de 1 s, venga de donde venga, se ignora. El Resumen lo muestra como «ignorada».
- Si la app oye algo que no es una orden, dice «No te he entendido» y la pantalla sugiere una orden válida.
- **Sin encuadre correcto o en pausa no se cuenta**, y la pantalla dice por qué.
- **Cómo hacer la zancada:** ponte un poco de lado a la cámara (unos 45°). Da un paso largo adelante, baja hasta que las dos rodillas queden más o menos en ángulo recto y vuelve. Eso cuenta una; la siguiente, con la otra pierna. Si no bajas lo suficiente, la pantalla dice «Baja más» en naranja.
- **Brazos en cruz:** estira los brazos a la altura de los hombros y mantén la postura 2 s. La barra amarilla se llena. Si las muñecas se salen de la imagen, la app usa los codos.

### Elegir cámara y micrófono (ordenador)
En **Dispositivos** (en Inicio y al final de Entrenamiento) eliges la cámara («Cámara 0, 1, 2…») y el micrófono. Si tienes una cámara virtual (OBS, por ejemplo), suele ser la 0: prueba otro número hasta que aparezcas tú. La elección se recuerda entre aperturas.

### Respaldos sin hardware (no sustituyen a la cámara y al micrófono reales)
- **Maniquí**: el interruptor «Sin cámara: usar el maniquí» mueve un cuerpo virtual con dos deslizadores (rodilla y brazos).
- **Orden escrita** (solo en el ordenador): una frase escrita sigue el mismo camino que una hablada.

## 2. Guía de instalación

Requisitos: Android Studio o IntelliJ IDEA con el plugin de Kotlin Multiplatform, JDK 17 o superior, y un móvil Android 8.0 (API 26) o superior con la depuración USB activada.

1. *File → Open* → esta carpeta. Espera a que Gradle sincronice.
2. **Ordenador:** panel Gradle → `desktopApp → Tasks → compose desktop → run`, o bien `./gradlew :desktopApp:run`.
   La primera compilación descarga con Internet dos modelos que quedan fuera del repositorio:
   - Voz (Vosk, español, unos 40 MB) → `~/.vosk/vosk-model-small-es-0.42`
   - Postura (MediaPipe en ONNX) → `~/.di-modelos/`
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

## Autoría
Trabajo individual: **Elias Jesus Blanco Tovar** (diseño, implementación, pruebas, documentación y vídeo).

Parte del código sale de los ejemplos de los Temas 4, 5 y 6 de clase.
