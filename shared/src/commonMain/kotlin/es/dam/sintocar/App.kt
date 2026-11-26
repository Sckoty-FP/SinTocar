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

private class Ref<T> { var valor: T? = null }

/** Las tres pantallas. El Resumen sale cuando la fase de la sesión es TERMINADO. */
@Composable
fun App() = TemaSinTocar {
    Surface(Modifier.fillMaxSize(), color = Colores.fondo) {
        var rutina by remember { mutableStateOf<Rutina?>(null) }
        val elegida = rutina
        if (elegida == null) PantallaInicio(Modifier.safeDrawingPadding()) { rutina = it }
        else Sesion(elegida, Modifier.safeDrawingPadding()) { rutina = null }
    }
}

/** Una sesión de entrenamiento: crea el control, la escucha continua y el reloj, y los conecta. */
@Composable
private fun Sesion(rutina: Rutina, modifier: Modifier, alVolver: () -> Unit) {
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
    val c = remember(rutina) { ControlEntrenamiento(rutina, reloj, anunciar = { escucha.decir(it) }).also { ref.valor = it } }

    // Reloj de la pantalla: avance automático a los 3 s y tiempo en marcha.
    var ahora by remember { mutableLongStateOf(0L) }
    LaunchedEffect(c) {
        while (true) { c.tic(); ahora = reloj(); delay(100) }
    }

    if (c.fase == Fase.TERMINADO) PantallaResumen(c, escucha, ahora, modifier, alVolver)
    else PantallaEntrenamiento(c, escucha, ahora, reloj, modifier)
}
