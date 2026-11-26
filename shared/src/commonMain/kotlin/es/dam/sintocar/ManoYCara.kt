package es.dam.sintocar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Lo que devuelve el reconocedor de gestos en cada imagen. */
class Gesto(
    val nombre: String,      // "Thumb_Up", "Open_Palm", "Closed_Fist", "Victory", "Pointing_Up", "Thumb_Down", "ILoveYou" o "None"
    val confianza: Float,    // 0 a 1
)

/** Nombre en castellano de cada gesto de MediaPipe. */
fun enCastellano(nombre: String) = when (nombre) {
    "Thumb_Up" -> "👍 Pulgar arriba"
    "Thumb_Down" -> "👎 Pulgar abajo"
    "Open_Palm" -> "✋ Palma abierta"
    "Closed_Fist" -> "✊ Puño cerrado"
    "Victory" -> "✌ Victoria"
    "Pointing_Up" -> "☝ Señalar arriba"
    "ILoveYou" -> "🤟 Te quiero"
    else -> "Sin gesto"
}

/** Lo que devuelve el detector de caras en cada imagen. */
class Cara(
    val hayCara: Boolean,
    val sonrisa: Float,      // probabilidad de 0 a 1
    val ojoIzquierdo: Float, // probabilidad de que esté abierto
    val ojoDerecho: Float,
)

/** Cámara frontal que mira la cara. En Android: ML Kit Face Detection. */
@Composable
expect fun CamaraCara(modifier: Modifier, alDetectar: (Cara) -> Unit)
