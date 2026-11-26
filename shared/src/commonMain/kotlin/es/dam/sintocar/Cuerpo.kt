package es.dam.sintocar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Las partes del cuerpo que usan los ejemplos. */
enum class Parte { HOMBRO_IZQ, HOMBRO_DER, CODO_IZQ, CODO_DER, MUNECA_IZQ, MUNECA_DER, CADERA_IZQ, CADERA_DER, RODILLA_IZQ, RODILLA_DER, TOBILLO_IZQ, TOBILLO_DER }

/** Un punto del cuerpo: x e y de 0 a 1 sobre la imagen, y la confianza del modelo (0 a 1). */
class Punto(val x: Float, val y: Float, val confianza: Float)

/** Lo que devuelve el modelo en cada imagen. Vacío si no hay nadie. */
class Postura(val puntos: Map<Parte, Punto>) {
    /** El punto solo si el modelo está bastante seguro de que se ve. */
    fun visible(parte: Parte): Punto? = puntos[parte]?.takeIf { it.confianza > 0.6f }
}

/**
 * Lo que sale de UNA imagen de la cámara frontal: la postura y, si esa imagen se ha mirado, el gesto de la mano.
 * [gesto] es null si no se ha analizado la mano (en el PC solo se mira 1 de cada 3 imágenes) o si falta el modelo.
 */
class Lectura(val postura: Postura, val gesto: Gesto?, val gestosDisponibles: Boolean = true)

/**
 * Cámara frontal que avisa con la lectura de cada imagen. La MISMA imagen pasa por la postura y por la mano
 * (una sola cámara). En Android: CameraX + ML Kit + MediaPipe.
 */
@Composable
expect fun CamaraPostura(modifier: Modifier, alDetectar: (Lectura) -> Unit)
