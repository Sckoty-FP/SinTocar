package es.dam.sintocar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Variantes del componente «Acción mantenida» de Figma. */
enum class EstadoAccion(val color: Color) {
    INACTIVA(Color(0xFF9E9E9E)),      // no se detecta nada
    MANTENIENDO(Color(0xFFFF7A3D)),   // se detecta: la barra se llena
    HECHA(Color(0xFF26A69A)),         // acción ejecutada
}

/**
 * Componente «Acción mantenida» de Figma: le dice al usuario qué gesto se ve y cuánto falta
 * para que cuente. Propiedades → parámetros: Estado → [estado], Texto → [texto], Progreso → [progreso].
 */
@Composable
fun AccionMantenida(estado: EstadoAccion, texto: String, progreso: Float) {
    Column(
        Modifier.fillMaxWidth().background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(texto, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = estado.color)
        Box(Modifier.fillMaxWidth().height(10.dp).background(Color(0xFFE0E0E0), RoundedCornerShape(5.dp))) {
            val lleno = if (estado == EstadoAccion.HECHA) 1f else progreso.coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth(lleno).height(10.dp).background(estado.color, RoundedCornerShape(5.dp)))
        }
    }
}
