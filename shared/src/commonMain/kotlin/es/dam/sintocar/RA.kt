package es.dam.sintocar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Lo que la escena de RA cuenta a la pantalla. */
class EstadoRA(
    val sueloEncontrado: Boolean = false,
    val colocado: Boolean = false,
    val distanciaM: Float? = null,   // del móvil a la esterilla, en metros
    val aviso: String? = null,       // poca luz, movimiento brusco…
)

/** Qué tiene que hacer el usuario para que se encuentre el suelo. Cambia entre móvil y escritorio. */
expect val comoBuscar: String

/** Qué tiene que hacer el usuario para colocar el objeto: tocar la pantalla o hacer clic. */
expect val comoColocar: String

/** "Compatible", "No compatible" o "Hay que instalar los servicios de RA". */
@Composable
expect fun compatibilidadRA(): String

/** Dónde se lanza el rayo para colocar: el último toque, o el centro si la orden vino por voz o por botón. */
fun puntoDeColocacion(ultimoToque: Pair<Float, Float>?, ancho: Int, alto: Int): Pair<Float, Float> =
    ultimoToque ?: (ancho / 2f to alto / 2f)

/**
 * Escena de RA a pantalla completa.
 * @param permitirColocar si es true, se puede colocar la esterilla.
 * @param quitar cada vez que este número cambia, se quita la esterilla.
 * @param colocar cada vez que este número cambia (y [permitirColocar]) se coloca la esterilla. Es la ÚNICA
 *   forma de colocar: así toda orden pasa por ControlEntrenamiento. Se coloca en el último punto tocado,
 *   o en el centro (móvil) o sobre el marcador (PC) si la orden vino por voz o por botón.
 * @param alTocar la escena avisa de un toque sobre ella. Devuelve true si la orden se aplicó
 *   (el control la puede rechazar: sin suelo, o repetida en menos de 1 s).
 */
@Composable
expect fun EscenaRA(modifier: Modifier, permitirColocar: Boolean, quitar: Int, colocar: Int, alTocar: () -> Boolean, alCambiar: (EstadoRA) -> Unit)
