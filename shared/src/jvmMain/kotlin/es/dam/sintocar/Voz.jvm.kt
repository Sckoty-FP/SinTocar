package es.dam.sintocar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

/** Dónde deja Gradle el modelo de español de Vosk (lo descarga la primera vez que compilas desktopApp). */
private val CARPETA_MODELO = File(System.getProperty("user.home"), ".vosk/vosk-model-small-es-0.42")

/** El modelo tarda un par de segundos en cargarse: se carga una sola vez. */
private val modelo: Model by lazy {
    LibVosk.setLogLevel(LogLevel.WARNINGS)
    Model(CARPETA_MODELO.path)
}

/** Vosk devuelve JSON como {"text" : "añade pan"}. Esto saca el texto. */
private fun textoDe(json: String): String =
    Regex("\"text\"\\s*:\\s*\"([^\"]*)\"").find(json)?.groupValues?.get(1) ?: ""

/** Reconocedor de escritorio: Vosk + el micrófono del ordenador. No necesita Internet. */
private class ReconocedorEscritorio : Reconocedor {
    override var estado by mutableStateOf(EstadoVoz.PARADO)
    override var error by mutableStateOf<String?>(null)
    @Volatile var seguir = true   // false al salir de la pantalla: el hilo se para
    /** Cada escucha tiene un número. parar() lo sube: la escucha vieja ve que ya no es la actual y se calla. */
    @Volatile private var turno = 0

    override fun parar() {
        turno++
        estado = EstadoVoz.PARADO
    }

    override fun escuchar(alTerminar: (String) -> Unit) {
        val miTurno = ++turno
        error = null
        // No basta con que exista la carpeta: puede estar vacía (descarga cortada o el bug de Gradle del Tema 5).
        if (!File(CARPETA_MODELO, "conf/model.conf").exists()) {
            error = "Falta el modelo de voz en ${CARPETA_MODELO.path} (la carpeta está vacía o incompleta). " +
                "Bórrala y compila desktopApp con Internet una vez."
            return
        }
        estado = EstadoVoz.ESCUCHANDO
        // El micrófono se lee en otro hilo para que la ventana no se congele.
        thread(isDaemon = true) {
            var frase = ""
            try {
                // El micrófono elegido en «Dispositivos», en el primer formato que admita (16/48/44,1 kHz).
                val (micro, formato) = DispositivosEscritorio.abrirMicrofono()
                // try/finally: si Vosk falla, el micrófono se cierra igual. Si no, quedaba abierto y cada
                // reintento fallaba con «not supported», tapando la causa real del error.
                try { Recognizer(modelo, formato.sampleRate).use { rec ->      // Vosk remuestrea a lo que pide su modelo
                    val trozo = ByteArray(8192)
                    val limite = System.currentTimeMillis() + 8000          // como mucho 8 segundos
                    while (seguir && turno == miTurno && System.currentTimeMillis() < limite) {
                        val n = aMono(trozo, micro.read(trozo, 0, trozo.size), formato.channels)
                        if (rec.acceptWaveForm(trozo, n)) {                 // true = ha detectado un silencio
                            frase = textoDe(rec.result)
                            if (frase.isNotEmpty()) break                   // había frase antes del silencio: listo
                        }
                    }
                    if (frase.isEmpty()) frase = textoDe(rec.finalResult)   // lo que haya oído hasta el límite
                } } finally { micro.stop(); micro.close() }
            } catch (e: Exception) {
                // Sin micrófono, lo usa otra aplicación, o Vosk no pudo arrancar.
                SwingUtilities.invokeLater { estado = EstadoVoz.PARADO; error = "No puedo escuchar: ${e.message}" }
                return@thread
            }
            // Volver al hilo de la ventana para tocar el estado de la pantalla.
            SwingUtilities.invokeLater {
                if (turno != miTurno) return@invokeLater      // la pararon mientras escuchaba: no se entrega
                estado = EstadoVoz.PARADO
                if (seguir) alTerminar(frase)
            }
        }
    }
}

@Composable
actual fun rememberReconocedor(): Reconocedor {
    val r = remember { ReconocedorEscritorio() }
    DisposableEffect(Unit) { onDispose { r.seguir = false } }
    return r
}

/** La app habla con la voz del sistema operativo. */
@Composable
actual fun rememberHablante(): Hablante = remember {
    object : Hablante {
        override fun decir(texto: String, alTerminar: () -> Unit) {
            val so = System.getProperty("os.name").lowercase()
            val orden = when {
                so.contains("win") -> listOf(
                    "powershell", "-Command",
                    "Add-Type -AssemblyName System.Speech; " +
                        "(New-Object System.Speech.Synthesis.SpeechSynthesizer).Speak('${texto.replace("'", "")}')",
                )
                so.contains("mac") -> listOf("say", "-v", "Monica", texto)
                else -> listOf("espeak-ng", "-v", "es", texto)          // Linux: sudo apt install espeak-ng
            }
            // Se espera en otro hilo a que acabe de hablar y se avisa en el hilo de la ventana.
            thread(isDaemon = true) {
                runCatching { ProcessBuilder(orden).start().waitFor() }
                SwingUtilities.invokeLater(alTerminar)
            }
        }
    }
}
