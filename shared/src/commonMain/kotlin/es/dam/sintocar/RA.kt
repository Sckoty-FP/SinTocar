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

/**
 * Escena de RA a pantalla completa.
 * @param permitirColocar si es true, tocar el suelo coloca la esterilla.
 * @param quitar cada vez que este número cambia, se quita la esterilla.
 * @param colocar cada vez que este número cambia (y [permitirColocar]), se coloca la esterilla sin tocar:
 *   en el centro de la pantalla (móvil) o sobre el marcador (PC). Lo usa la orden de voz «colocar».
 */
@Composable
expect fun EscenaRA(modifier: Modifier, permitirColocar: Boolean, quitar: Int, colocar: Int, alCambiar: (EstadoRA) -> Unit)
