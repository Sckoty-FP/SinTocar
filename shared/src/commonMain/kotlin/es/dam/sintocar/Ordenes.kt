package es.dam.sintocar

/** Lo que el usuario puede pedir. Cada acción es una fila del mapa de órdenes. */
enum class Accion { EMPEZAR, PAUSA, SIGUIENTE, TERMINAR, AYUDA, FAVORITO }

/** Por dónde llegó la orden. El Resumen lo enseña. */
enum class Origen { VOZ, POSTURA, TOQUE }

/** Una orden con su origen: lo único que entra en [ControlEntrenamiento]. */
data class Orden(val accion: Accion, val origen: Origen)

/** Columna «Voz» del mapa de órdenes: palabra → acción. */
val MAPA_VOZ: Map<String, Accion> = mapOf(
    "empezar" to Accion.EMPEZAR, "sigue" to Accion.EMPEZAR,
    "pausa" to Accion.PAUSA, "para" to Accion.PAUSA,
    "siguiente" to Accion.SIGUIENTE,
    "terminar" to Accion.TERMINAR,
    "ayuda" to Accion.AYUDA,
    "favorito" to Accion.FAVORITO,
)

/**
 * Convierte la frase reconocida en una acción, o null si no hay ninguna orden.
 * Compara PALABRAS enteras, no trozos: así «siguiente» no se confunde con «sigue».
 */
fun interpretar(frase: String): Accion? =
    normalizar(frase).split(Regex("[^a-zñ]+")).firstNotNullOfOrNull { MAPA_VOZ[it] }

/** Minúsculas y sin tildes: el reconocedor a veces devuelve «Pausa» o «pausá». */
private fun normalizar(s: String) = s.lowercase()
    .replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u').replace('ü', 'u')

/**
 * TODO lo que la app dice en voz alta. Requisito 5: ninguna frase puede contener una orden,
 * porque el micrófono la oiría y la app se obedecería a sí misma. Lo comprueba un test.
 * Ojo: «para» es una orden, así que no se puede decir «prepárate para…».
 */
object Frases {
    fun empezar(s: Serie) = "Vamos. ${s.ejercicio.nombre}: ${s.objetivo} repeticiones."
    const val EN_PAUSA = "Entrenamiento detenido. Te espero."   // NO «En pausa»: contiene la orden «pausa»
    fun objetivo(siguiente: Serie?) =
        if (siguiente == null) "¡Objetivo cumplido! Rutina completada."
        else "¡Objetivo cumplido! Ahora, ${siguiente.ejercicio.nombre.lowercase()}."
    fun cambio(s: Serie) = "Ahora, ${s.ejercicio.nombre.lowercase()}: ${s.objetivo} repeticiones."
    const val TERMINADO = "Entrenamiento completado. Buen trabajo."
    const val FAVORITO = "Rutina guardada en favoritos."
    /** Se dice cuando oye algo que no es una orden. La pista («Prueba: "pausa"») va SOLO en pantalla. */
    const val NO_ENTENDIDO = "No te he entendido."

    /** Todas las frases posibles, para el test. */
    fun todas(): List<String> {
        val series = Rutina.entries.flatMap { it.series }
        return series.map(::empezar) + series.map(::objetivo) + objetivo(null) + series.map(::cambio) +
            listOf(EN_PAUSA, TERMINADO, FAVORITO, NO_ENTENDIDO)
    }
}
