package es.dam.sintocar

/**
 * Convierte los gestos de la mano (llegan en cada imagen) en órdenes del entrenamiento.
 * Palma abierta → pausa, pulgar arriba → siguiente. Solo cuentan con confianza mayor de 0,6
 * y se confirman con el [Estabilizador]: 5 imágenes seguidas, y hay que soltar para repetir.
 */
class LectorGestos(imagenes: Int = 5) {
    private val estabilizador = Estabilizador(imagenes)

    /** El gesto que se está viendo ahora ("None" si no hay ninguno que sirva). */
    var viendo = "None"
        private set

    /** De 0 a 1: cuánto falta para confirmar. Solo avanza con los gestos que son una orden. */
    val progreso: Float get() = if (accionDe(viendo) == null) 0f else estabilizador.progreso

    /** Devuelve la orden solo en la imagen en que el gesto queda confirmado. */
    fun leer(g: Gesto, ignorar: Boolean = false): Orden? {
        // Ignorar = como si no hubiera mano: así no se acumula progreso y hay que soltar para volver a empezar.
        viendo = if (!ignorar && g.confianza > 0.6f) g.nombre else "None"
        val confirmado = estabilizador.confirmar(viendo)
        return if (confirmado) accionDe(viendo)?.let { Orden(it, Origen.GESTO) } else null
    }

    companion object {

        /** Columna «Gesto» del mapa de órdenes. */
        fun accionDe(gesto: String): Accion? = when (gesto) {
            "Open_Palm" -> Accion.PAUSA
            "Thumb_Up" -> Accion.SIGUIENTE
            else -> null
        }
    }
}

/** Con los brazos en cruz (o a medias de los 2 s) la mano no manda: los dos gestos se pisan. */
fun ignorarGestos(enCruz: Boolean, progresoCruz: Float) = enCruz || progresoCruz > 0f
