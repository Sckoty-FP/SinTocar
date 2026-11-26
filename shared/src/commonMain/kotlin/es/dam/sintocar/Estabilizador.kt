package es.dam.sintocar

/**
 * Convierte un gesto que llega en cada imagen (15–30 veces por segundo) en UNA acción.
 * Reglas: el mismo gesto en [imagenes] imágenes seguidas, y para repetirlo hay que soltarlo antes.
 */
class Estabilizador(private val imagenes: Int = 5) {
    private var candidato = ""
    private var seguidas = 0
    private var yaUsado = false

    /** De 0 a 1: cuánto falta para confirmar el gesto actual. Para la barra de «Acción mantenida». */
    val progreso: Float get() = if (candidato == "None") 0f else (seguidas.toFloat() / imagenes).coerceAtMost(1f)

    /** Devuelve true solo cuando el gesto queda confirmado. */
    fun confirmar(gesto: String): Boolean {
        if (gesto != candidato) { candidato = gesto; seguidas = 0; yaUsado = false }
        seguidas++
        if (seguidas >= imagenes && !yaUsado && gesto != "None") { yaUsado = true; return true }
        return false
    }
}
