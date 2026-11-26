package es.dam.sintocar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import java.util.prefs.Preferences
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.Line
import javax.sound.sampled.TargetDataLine

/**
 * Qué cámara y qué micrófono usa el ordenador. En un PC suele haber varios (webcam, cámara virtual de OBS,
 * micro del portátil, auriculares…) y el «por defecto» de Windows no siempre es el bueno.
 * La elección se guarda con Preferences para no repetirla cada vez que se abre.
 */
internal object DispositivosEscritorio {
    private val prefs = Preferences.userRoot().node("es/dam/sintocar")

    var camara by mutableIntStateOf(prefs.getInt("camara", 0))
        private set
    /** Nombre del mezclador de Java Sound; null = el micrófono por defecto del sistema. */
    var microfono by mutableStateOf(prefs.get("microfono", null))
        private set

    fun elegirCamara(n: Int) { camara = n; prefs.putInt("camara", n) }
    fun elegirMicrofono(nombre: String?) {
        microfono = nombre
        if (nombre == null) prefs.remove("microfono") else prefs.put("microfono", nombre)
    }

    /** Los dispositivos que pueden grabar (los que admiten una TargetDataLine). */
    fun microfonos(): List<String> = AudioSystem.getMixerInfo()
        .filter { AudioSystem.getMixer(it).isLineSupported(Line.Info(TargetDataLine::class.java)) }
        .map { it.name }

    /**
     * Abre el micrófono elegido en el primer formato que admita. Vosk prefiere 16 kHz mono, pero muchos
     * micrófonos solo dan 44,1 o 48 kHz, o estéreo: Vosk acepta otra frecuencia si se le dice cuál es,
     * y el estéreo se pasa a mono con [aMono].
     */
    fun abrirMicrofono(): Pair<TargetDataLine, AudioFormat> {
        // Siempre a través de un mezclador concreto: AudioSystem.getTargetDataLine() (el «por defecto» del Tema 5)
        // falló dentro del .exe empaquetado. Sin elección: todos los de captura, el primario de Windows primero.
        val nombres = microfono?.let { listOf(it) } ?: microfonos()
        val mezcladores = AudioSystem.getMixerInfo().filter { it.name in nombres }.map(AudioSystem::getMixer)
        val formatos = listOf(16000f, 48000f, 44100f).flatMap { hz -> listOf(1, 2).map { AudioFormat(hz, 16, it, true, false) } }
        var ultimoError: Exception? = null
        for (mixer in mezcladores) for (f in formatos) {
            val info = DataLine.Info(TargetDataLine::class.java, f)
            if (!mixer.isLineSupported(info)) continue
            try {
                val linea = mixer.getLine(info) as TargetDataLine
                linea.open(f); linea.start()
                return linea to f
            } catch (e: Exception) { ultimoError = e }
        }
        error("el micrófono «${microfono ?: "por defecto"}» no admite ningún formato de 16 bits" +
            (ultimoError?.message?.let { " ($it)" } ?: "") + ". Elige otro en «Dispositivos».")
    }
}

/** Audio de 16 bits little-endian: si es estéreo, se promedian los dos canales en uno. Devuelve los bytes válidos. */
internal fun aMono(datos: ByteArray, n: Int, canales: Int): Int {
    if (canales == 1) return n
    var salida = 0
    var i = 0
    while (i + 3 < n) {
        val izq = (datos[i].toInt() and 0xFF) or (datos[i + 1].toInt() shl 8)
        val der = (datos[i + 2].toInt() and 0xFF) or (datos[i + 3].toInt() shl 8)
        val media = (izq.toShort() + der.toShort()) / 2
        datos[salida] = (media and 0xFF).toByte(); datos[salida + 1] = (media shr 8).toByte()
        salida += 2; i += 4
    }
    return salida
}

@Composable
actual fun SelectorDispositivos() {
    val microfonos = remember { DispositivosEscritorio.microfonos() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Dispositivos", fontSize = Tamanos.apoyo, color = Colores.texto)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Desplegable("📷 Cámara ${DispositivosEscritorio.camara}", (0..4).map { "Cámara $it" }) {
                DispositivosEscritorio.elegirCamara(it)
            }
            Desplegable("🎤 ${DispositivosEscritorio.microfono ?: "Micrófono por defecto"}",
                listOf("Micrófono por defecto") + microfonos) {
                DispositivosEscritorio.elegirMicrofono(if (it == 0) null else microfonos[it - 1])
            }
        }
    }
}

@Composable
private fun Desplegable(texto: String, opciones: List<String>, alElegir: (Int) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { abierto = true }) { Text(texto, fontSize = Tamanos.apoyo, maxLines = 1) }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEachIndexed { i, o ->
                DropdownMenuItem(text = { Text(o) }, onClick = { abierto = false; alElegir(i) })
            }
        }
    }
}
