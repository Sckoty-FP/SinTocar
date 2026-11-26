package es.dam.sintocar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Variantes del componente «Instrucción de RA» de Figma: una por fase de la experiencia. */
enum class FaseRA(val icono: String, val fondo: Color) {
    BUSCANDO("📱", Color(0xFFFFB300)),   // aún no hay suelo: hay que mover el dispositivo
    LISTO("👆", Color(0xFFFF7A3D)),      // hay suelo: ya se puede colocar
    COLOCADO("✅", Color(0xFF26A69A)),   // el objeto está en su sitio
    PROBLEMA("⚠", Color(0xFFE53935)),   // poca luz, movimiento brusco…
}

/** En qué fase está la experiencia de RA. El aviso de un problema tiene prioridad sobre todo lo demás. */
fun faseRA(estado: EstadoRA): FaseRA = when {
    estado.aviso != null -> FaseRA.PROBLEMA
    estado.colocado -> FaseRA.COLOCADO
    estado.sueloEncontrado -> FaseRA.LISTO
    else -> FaseRA.BUSCANDO
}

/**
 * Componente «Instrucción de RA» de Figma: una franja con icono y una instrucción corta
 * que dice al usuario QUÉ HACER en cada fase. Propiedades → parámetros: Fase → [fase], Texto → [texto].
 */
@Composable
fun InstruccionRA(fase: FaseRA, texto: String) {
    Row(
        Modifier.fillMaxWidth().background(fase.fondo, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(fase.icono, fontSize = 28.sp)
        Text(texto, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
