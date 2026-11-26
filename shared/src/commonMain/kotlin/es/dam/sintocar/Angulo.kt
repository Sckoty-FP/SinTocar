package es.dam.sintocar

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Ángulo en grados (0 a 180) en el punto [b], formado por a-b-c.
 * Rodilla: angulo(cadera, rodilla, tobillo) → unos 175° de pie y menos de 100° agachado.
 * x se multiplica por 3/4 porque la imagen es más alta que ancha (3:4).
 */
fun angulo(a: Punto, b: Punto, c: Punto): Float {
    val ux = (a.x - b.x) * 0.75f; val uy = a.y - b.y
    val vx = (c.x - b.x) * 0.75f; val vy = c.y - b.y
    val coseno = (ux * vx + uy * vy) / (sqrt(ux * ux + uy * uy) * sqrt(vx * vx + vy * vy))
    return (acos(coseno.coerceIn(-1f, 1f)) * 180 / PI).toFloat()
}

/** Ángulo de la rodilla izquierda, o null si no se ven la cadera, la rodilla y el tobillo. */
fun anguloRodilla(p: Postura): Float? {
    val cadera = p.visible(Parte.CADERA_IZQ) ?: return null
    val rodilla = p.visible(Parte.RODILLA_IZQ) ?: return null
    val tobillo = p.visible(Parte.TOBILLO_IZQ) ?: return null
    return angulo(cadera, rodilla, tobillo)
}
