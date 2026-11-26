package es.dam.sintocar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.time.TimeSource

/**
 * Pantalla Resumen: repeticiones por ejercicio, tiempo, últimas órdenes con su origen,
 * y guardar como favorito (sonriendo a la cámara frontal o con el botón; por voz ya no).
 */
@Composable
fun PantallaResumen(
    c: ControlEntrenamiento,
    escucha: EscuchaContinua,
    ahora: Long,
    modifier: Modifier = Modifier,
    alVolver: () -> Unit,
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Resumen", fontSize = Tamanos.principal, fontWeight = FontWeight.Bold, color = Colores.texto)
        Text("Rutina ${c.rutina.nombre} · ⏱ ${formatoTiempo(c.tiempoMs())}", fontSize = Tamanos.titulo, color = Colores.texto)

        Bloque {
            c.totalPorEjercicio().forEach { (ejercicio, total) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(ejercicio.nombre, fontSize = Tamanos.titulo, color = Colores.texto)
                    Text("$total", fontSize = Tamanos.titulo, fontWeight = FontWeight.Bold, color = Colores.correcto)
                }
            }
        }

        Text("Últimas órdenes", fontSize = Tamanos.apoyo, fontWeight = FontWeight.Bold, color = Colores.texto)
        Bloque {
            val ultimas = c.historial.takeLast(8).reversed()
            if (ultimas.isEmpty()) Text("Ninguna", fontSize = Tamanos.apoyo, color = Colores.textoSuave)
            ultimas.forEach { r ->
                val nota = when (r.resultado) {
                    Resultado.APLICADA -> ""
                    Resultado.REPETIDA -> " · ignorada (repetida en menos de 1 s)"
                    Resultado.NO_DISPONIBLE -> " · no disponible en ese momento"
                }
                Text("${formatoTiempo(r.ms)}  ${r.orden.accion.texto()} · ${r.orden.origen.texto()}$nota",
                    fontSize = Tamanos.apoyo,
                    color = if (r.resultado == Resultado.APLICADA) Colores.texto else Colores.textoSuave)
            }
        }

        if (c.favorito) Text("★ Guardada en favoritos", fontSize = Tamanos.titulo, fontWeight = FontWeight.Bold, color = Colores.aviso)
        else {
            BotonGrande("☆ Guardar como favorito", Modifier.fillMaxWidth(), color = Colores.aviso) {
                c.ordenar(Orden(Accion.FAVORITO, Origen.TOQUE))
            }
            SonrisaFavorito(c)
        }

        IndicadorVoz(escucha.indicador(ahora), escucha.ultimaFrase, escucha.ultimaAccion, escucha.error,
            escucha.hablando, pista = pistaVoz(c.fase))
        if (esEscritorio) OrdenEscrita(escucha) { c.ordenar(Orden(it, Origen.VOZ)) }

        BotonGrande("Volver al inicio", Modifier.fillMaxWidth(), alPulsar = alVolver)
    }
}

@Composable
private fun Bloque(contenido: @Composable () -> Unit) = Column(
    Modifier.fillMaxWidth().background(Colores.superficie, RoundedCornerShape(16.dp)).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) { contenido() }

/**
 * Sonreír a la cámara frontal 0,8 s guarda la rutina como favorito (origen CARA).
 * Sin permiso de cámara, o con el interruptor, se simula con un deslizador.
 */
@Composable
private fun SonrisaFavorito(c: ControlEntrenamiento) {
    val permiso = rememberPermiso(Recurso.CAMARA)
    val marca = remember { TimeSource.Monotonic.markNow() }
    val detector = remember { DetectorSonrisa({ marca.elapsedNow().inWholeMilliseconds }) }
    var progreso by remember { mutableFloatStateOf(0f) }
    var hecho by remember { mutableStateOf(false) }

    val estado = when {
        hecho -> EstadoAccion.HECHA
        progreso > 0f -> EstadoAccion.MANTENIENDO
        else -> EstadoAccion.INACTIVA
    }
    AccionMantenida(
        estado,
        when (estado) {
            EstadoAccion.HECHA -> "★ ¡Guardada!"
            EstadoAccion.MANTENIENDO -> "Sigue sonriendo…"
            EstadoAccion.INACTIVA -> if (detector.esperandoSoltar) "Baja la sonrisa para repetir" else "😊 Sonríe 0,8 s para guardar en favoritos"
        },
        progreso,
    )
    FuenteCara(simularAlPrincipio = permiso.estado != EstadoPermiso.CONCEDIDO) { cara ->
        if (detector.actualizar(if (cara.hayCara) cara.sonrisa else 0f)) {
            c.ordenar(Orden(Accion.FAVORITO, Origen.CARA))
            hecho = true
        }
        progreso = detector.progreso
    }
}
