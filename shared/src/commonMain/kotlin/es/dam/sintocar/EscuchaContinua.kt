package es.dam.sintocar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/** Variantes del componente «Indicador de voz» de Figma (propiedad estado). */
enum class EstadoIndicador { INACTIVO, ESCUCHANDO, RECONOCIDO, NO_ENTENDIDO }

/**
 * El Reconocedor del Tema 5 solo escucha UNA frase. Esto lo hace continuo: al acabar una frase vuelve a escuchar,
 * menos mientras la app habla (para no oírse a sí misma).
 */
class EscuchaContinua(
    private val reconocedor: Reconocedor,
    private val hablante: Hablante,
    private val reloj: () -> Long,
) {
    var hablando by mutableStateOf(false); private set
    var ultimaFrase by mutableStateOf<String?>(null); private set
    var ultimaAccion by mutableStateOf<Accion?>(null); private set
    private var momentoResultado by mutableLongStateOf(-10_000L)

    internal var esperando = false
    internal var desde = 0L

    val estadoVoz get() = reconocedor.estado
    val error get() = reconocedor.error

    /** La app habla: primero se corta la escucha, y se reanuda cuando termina de hablar. */
    fun decir(texto: String, alTerminar: () -> Unit = {}) {
        reconocedor.parar(); esperando = false
        hablando = true
        hablante.decir(texto) { hablando = false; alTerminar() }
    }

    /**
     * Llega una frase (del micrófono o del campo de orden escrita).
     * Si es una orden se pasa a [alOrden]; si no, «No te he entendido» (sin repetir ninguna orden en voz alta).
     */
    fun recibir(frase: String, alOrden: (Accion) -> Unit) {
        if (frase.isBlank()) return                     // silencio: no es un error
        ultimaFrase = frase
        momentoResultado = reloj()
        val accion = interpretar(frase)
        ultimaAccion = accion
        if (accion == null) decir(Frases.NO_ENTENDIDO) else alOrden(accion)
    }

    /** La variante del indicador ahora mismo. El resultado se enseña 1,5 s y luego vuelve a escuchar. */
    fun indicador(ahora: Long): EstadoIndicador = when {
        ahora - momentoResultado < 1500 -> if (ultimaAccion != null) EstadoIndicador.RECONOCIDO else EstadoIndicador.NO_ENTENDIDO
        reconocedor.estado != EstadoVoz.PARADO -> EstadoIndicador.ESCUCHANDO
        else -> EstadoIndicador.INACTIVO
    }
}

/**
 * Crea la escucha continua y la mantiene viva mientras [activa] sea true.
 * Cada orden reconocida se entrega a [alOrden].
 */
@Composable
fun rememberEscuchaContinua(activa: Boolean, reloj: () -> Long, alOrden: (Accion) -> Unit): EscuchaContinua {
    val reconocedor = rememberReconocedor()
    val hablante = rememberHablante()
    val escucha = remember { EscuchaContinua(reconocedor, hablante, reloj) }
    val orden by rememberUpdatedState(alOrden)

    LaunchedEffect(activa) {
        if (!activa) { reconocedor.parar(); escucha.esperando = false; return@LaunchedEffect }
        while (true) {
            val ahora = reloj()
            // Una escucha que falló (sin red, micro ocupado…) o que se quedó colgada: se reintenta.
            if (escucha.esperando && (reconocedor.error != null || ahora - escucha.desde > 12_000)) {
                escucha.esperando = false
                delay(1500)
            }
            if (!escucha.hablando && !escucha.esperando && reconocedor.estado == EstadoVoz.PARADO) {
                escucha.esperando = true
                escucha.desde = reloj()
                reconocedor.escuchar { frase ->
                    escucha.esperando = false
                    escucha.recibir(frase) { orden(it) }
                }
            }
            delay(150)
        }
    }
    return escucha
}
