package es.dam.sintocar

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Componente «Indicador de voz» de Figma. Una variante por estado:
 *   INACTIVO      → micrófono gris
 *   ESCUCHANDO    → micrófono de color que late (ondas)
 *   RECONOCIDO    → texto del comando y marca verde 1,5 s
 *   NO_ENTENDIDO  → «No te he entendido. Prueba: "pausa"» (la pista solo se LEE, no se dice)
 */
@Composable
fun IndicadorVoz(estado: EstadoIndicador, frase: String?, accion: Accion?, error: String?, hablando: Boolean, pista: String) {
    val (color, texto) = when (estado) {
        EstadoIndicador.INACTIVO -> Colores.textoSuave to (if (hablando) "🔈 Hablando…" else "🎤 Micrófono en espera")
        EstadoIndicador.ESCUCHANDO -> Colores.errorTexto to "🎤 Te escucho…"
        EstadoIndicador.RECONOCIDO -> Colores.correcto to "✔ «${frase.orEmpty()}» → ${accion?.texto()}"
        EstadoIndicador.NO_ENTENDIDO -> Colores.aviso to "No te he entendido. Prueba: $pista"
    }
    val latido by rememberInfiniteTransition().animateFloat(
        initialValue = 1f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
    )
    Column(
        Modifier.background(Colores.superficie, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(20.dp)
                    .scale(if (estado == EstadoIndicador.ESCUCHANDO) latido else 1f)
                    .background(color, CircleShape)
            )
            Text(texto, color = if (estado == EstadoIndicador.INACTIVO) Colores.textoSuave else color,
                fontSize = Tamanos.apoyo, fontWeight = FontWeight.Bold)
        }
        if (error != null) Text(error, color = Colores.errorTexto, fontSize = Tamanos.apoyo)
    }
}
