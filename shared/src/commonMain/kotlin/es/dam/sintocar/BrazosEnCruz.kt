package es.dam.sintocar

import kotlin.math.abs

/**
 * ¿Están los dos brazos estirados a la altura de los hombros?
 * Se miran las muñecas. Si no se ven (a 2 o 3 m en vertical se salen de la imagen), se miran los codos.
 */
fun brazosEnCruz(p: Postura): Boolean {
    val hi = p.visible(Parte.HOMBRO_IZQ) ?: return false
    val hd = p.visible(Parte.HOMBRO_DER) ?: return false
    val hombros = abs(hi.x - hd.x)
    fun horizontales(i: Punto, d: Punto, separacion: Float) =
        abs(i.y - hi.y) < 0.08f && abs(d.y - hd.y) < 0.08f && abs(i.x - d.x) > hombros * separacion
    val mi = p.visible(Parte.MUNECA_IZQ)
    val md = p.visible(Parte.MUNECA_DER)
    if (mi != null && md != null) return horizontales(mi, md, 2.5f)   // muñecas mucho más abiertas que los hombros
    val ci = p.visible(Parte.CODO_IZQ) ?: return false
    val cd = p.visible(Parte.CODO_DER) ?: return false
    return horizontales(ci, cd, 1.8f)                                 // los codos están más cerca, así que se pide menos separación
}

/** Cuenta cuánto tiempo seguido se mantiene una postura. Avisa una vez al llegar a [duracionMs]. */
class PosturaMantenida(private val duracionMs: Long = 2000) {
    private var desde: Long? = null
    private var avisado = false
    var progreso = 0f
        private set

    /** Devuelve true solo en el instante en que se completan los 2 segundos. */
    fun actualizar(seCumple: Boolean, ahoraMs: Long): Boolean {
        if (!seCumple) { desde = null; avisado = false; progreso = 0f; return false }
        val inicio = desde ?: ahoraMs.also { desde = it }
        progreso = ((ahoraMs - inicio).toFloat() / duracionMs).coerceAtMost(1f)
        if (progreso >= 1f && !avisado) { avisado = true; return true }
        return false
    }
}
