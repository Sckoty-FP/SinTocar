package es.dam.sintocar

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlin.time.TimeSource

private data class Destino(val rutina: Rutina, val conZonaRA: Boolean)

private class Ref<T> { var valor: T? = null }

/** Las pantallas. La Zona RA va antes del Entrenamiento si se ha elegido; el Resumen sale cuando la fase es TERMINADO. */
@Composable
fun App() = TemaSinTocar {
    Surface(Modifier.fillMaxSize(), color = Colores.fondo) {
        // Lo elegido en Inicio: la rutina y si se pasa antes por la Zona RA.
        var destino by remember { mutableStateOf<Destino?>(null) }
        val elegido = destino
        if (elegido == null) PantallaInicio(Modifier.safeDrawingPadding(),
            alZonaRA = { destino = Destino(it, conZonaRA = true) }) { destino = Destino(it, conZonaRA = false) }
        else Sesion(elegido.rutina, elegido.conZonaRA, Modifier.safeDrawingPadding()) { destino = null }
    }
}

/** Una sesión de entrenamiento: crea el control, la escucha continua y el reloj, y los conecta. */
@Composable
private fun Sesion(rutina: Rutina, conZonaRA: Boolean, modifier: Modifier, alVolver: () -> Unit) {
    val inicio = remember { TimeSource.Monotonic.markNow() }
    val reloj: () -> Long = remember { { inicio.elapsedNow().inWholeMilliseconds } }
    val microfono = rememberPermiso(Recurso.MICROFONO)

    // La escucha necesita al control (para darle órdenes) y el control a la escucha (para hablar).
    // Para no depender uno del otro al crearlos se usa una referencia que se rellena después.
    val ref = remember { Ref<ControlEntrenamiento>() }
    val escucha = rememberEscuchaContinua(
        activa = microfono.estado == EstadoPermiso.CONCEDIDO,
        reloj = reloj,
    ) { accion -> ref.valor?.ordenar(Orden(accion, Origen.VOZ)) }
    val c = remember(rutina) { ControlEntrenamiento(rutina, reloj, anunciar = { escucha.decir(it) }, enZonaRA = conZonaRA).also { ref.valor = it } }
    // Entre la Zona RA y el Entrenamiento hay una transición: una sola cámara abierta a la vez.
    var transicion by remember { mutableStateOf(false) }

    // Reloj de la pantalla: avance automático a los 3 s y tiempo en marcha.
    var ahora by remember { mutableLongStateOf(0L) }
    LaunchedEffect(c) {
        while (true) { c.tic(); ahora = reloj(); delay(100) }
    }

    if (c.zonaRA && !transicion) PantallaZonaRA(c, escucha, ahora, { transicion = true }, modifier)
    else if (c.zonaRA) PantallaTransicionCamara({ transicion = false; c.salirZonaRA() }, modifier)
    else if (c.fase == Fase.TERMINADO) PantallaResumen(c, escucha, ahora, modifier, alVolver)
    else PantallaEntrenamiento(c, escucha, ahora, reloj, modifier)
}
