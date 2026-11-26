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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Variantes del componente «Instrucción de RA» de Figma: una por fase de la experiencia. */
enum class FaseRA(val icono: String, val fondo: Color, val nombre: String) {
    BUSCANDO("📱", Color(0xFFFFB300), "Buscando el suelo"),   // aún no hay suelo: hay que mover el dispositivo
    LISTO("👆", Color(0xFFFF7A3D), "Suelo encontrado"),     // hay suelo: ya se puede colocar
    COLOCADO("✅", Color(0xFF26A69A), "Colocado"),            // el objeto está en su sitio
    PROBLEMA("⚠", Color(0xFFE53935), "Problema"),            // poca luz, movimiento brusco…
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
        Modifier.fillMaxWidth().background(fase.fondo, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp)
            // Región viva: cada cambio de fase se anuncia. El estado va en el texto, no solo en el color.
            .clearAndSetSemantics {
                contentDescription = descripcionInstruccionRA(fase, texto)
                liveRegion = LiveRegionMode.Polite
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(fase.icono, fontSize = 28.sp)
        Text(texto, color = Color.Black,   // negro: el blanco sobre ámbar o verde no llega a AA
             fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

/** Lo que lee el lector de pantalla de la franja: nombre de la fase y la instrucción. */
fun descripcionInstruccionRA(fase: FaseRA, texto: String) = "${fase.nombre}. $texto"
