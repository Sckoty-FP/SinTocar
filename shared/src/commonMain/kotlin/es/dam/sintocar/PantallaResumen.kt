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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Pantalla Resumen: repeticiones por ejercicio, tiempo, últimas órdenes con su origen,
 * y guardar como favorito (por voz «favorito» o con el botón).
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
        else BotonGrande("☆ Guardar como favorito", Modifier.fillMaxWidth(), color = Colores.aviso) {
            c.ordenar(Orden(Accion.FAVORITO, Origen.TOQUE))
        }
        Text("O di «favorito»", fontSize = Tamanos.apoyo, color = Colores.textoSuave)

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
