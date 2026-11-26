package es.dam.sintocar

/**
 * Convierte la probabilidad de sonrisa (llega en cada imagen) en UNA orden de favorito.
 * Reglas, como el Paso 4 del Tema 7: sonrisa mayor de 0,8 durante 0,8 s seguidos, y para repetir
 * hay que bajar de 0,3 (histéresis: entre 0,3 y 0,8 no se cuenta ni se suelta).
 * @param reloj hora actual en ms; en los tests, uno que avanza a mano.
 */
class DetectorSonrisa(
    private val reloj: () -> Long,
    private val umbral: Float = 0.8f,
    private val duracionMs: Long = 800,
    private val soltar: Float = 0.3f,
) {
    private var desde: Long? = null

    /** True tras confirmar: hasta que la sonrisa baje de [soltar] no se vuelve a contar. */
    var esperandoSoltar = false
        private set

    /** De 0 a 1: cuánto falta para confirmar. Para la barra de «Acción mantenida». */
    var progreso = 0f
        private set

    /** Devuelve true solo en la imagen en que se completan los 0,8 s. */
    fun actualizar(sonrisa: Float): Boolean {
        val ahora = reloj()
        when {
            esperandoSoltar -> { if (sonrisa < soltar) esperandoSoltar = false }
            sonrisa > umbral -> {
                val inicio = desde ?: ahora.also { desde = it }
                progreso = ((ahora - inicio).toFloat() / duracionMs).coerceAtMost(1f)
                if (progreso >= 1f) { esperandoSoltar = true; desde = null; progreso = 0f; return true }
            }
            else -> { desde = null; progreso = 0f }
        }
        return false
    }
}
