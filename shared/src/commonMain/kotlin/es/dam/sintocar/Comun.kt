package es.dam.sintocar

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** true en el ordenador. Se usa para enseñar la orden escrita, que solo existe en escritorio. */
expect val esEscritorio: Boolean

/** Elegir cámara y micrófono. En el ordenador puede haber varios; en el móvil no pinta nada. */
@Composable
expect fun SelectorDispositivos()

/** Variables de color de Figma. Fondo oscuro y texto blanco para que se lea bien a 2 o 3 metros. */
object Colores {
    val fondo = Color(0xFF101418)
    val superficie = Color(0xFF1C2228)
    val texto = Color.White
    val textoSuave = Color(0xFFB0BEC5)
    val correcto = Color(0xFF26A69A)
    val aviso = Color(0xFFFFB300)
    val error = Color(0xFFE53935)
    /** Rojo más claro para TEXTO: el de arriba no llega al contraste AA (4,4:1) sobre el fondo oscuro. */
    val errorTexto = Color(0xFFEF5350)
    val primario = Color(0xFF42A5F5)
}

/** Tamaños de Figma (1 px = 1 sp): principal 48 o más, apoyo 20 o más. */
object Tamanos {
    val contador = 96.sp
    val principal = 48.sp
    val titulo = 32.sp
    val apoyo = 22.sp
}

@Composable
fun TemaSinTocar(contenido: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(
        background = Colores.fondo, surface = Colores.superficie,
        primary = Colores.primario, onPrimary = Color.Black, onBackground = Colores.texto, onSurface = Colores.texto,
    ),
    content = contenido,
)

/** Botón grande, fácil de pulsar. Es la alternativa táctil de cada orden. */
@Composable
fun BotonGrande(texto: String, modifier: Modifier = Modifier, color: Color = Colores.primario, alPulsar: () -> Unit) =
    Button(
        onClick = alPulsar,
        modifier = modifier.defaultMinSize(minHeight = 64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.Black),
    ) { Text(texto, fontSize = Tamanos.apoyo, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp)) }

/** 83 000 ms → «01:23». */
fun formatoTiempo(ms: Long): String {
    val s = ms / 1000
    return "${(s / 60).toString().padStart(2, '0')}:${(s % 60).toString().padStart(2, '0')}"
}

fun Origen.texto() = when (this) {
    Origen.VOZ -> "voz"
    Origen.POSTURA -> "postura"
    Origen.TOQUE -> "toque"
}

fun Accion.texto() = when (this) {
    Accion.EMPEZAR -> "Empezar / seguir"
    Accion.PAUSA -> "Pausa"
    Accion.SIGUIENTE -> "Siguiente"
    Accion.TERMINAR -> "Terminar"
    Accion.AYUDA -> "Ayuda"
    Accion.FAVORITO -> "Favorito"
}
