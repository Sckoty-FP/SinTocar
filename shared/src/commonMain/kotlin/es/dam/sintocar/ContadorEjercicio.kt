package es.dam.sintocar

/** Lo que pasa con cada ángulo nuevo de la rodilla. */
enum class Movimiento { NADA, REPETICION, INCOMPLETA }

/**
 * Es el ContadorSentadillas del Tema 6 con los umbrales como parámetros, para usarlo en los dos ejercicios:
 *   ARRIBA --(rodilla < [bajo])--> ABAJO --(rodilla > [alto])--> ARRIBA y REPETICION
 * Si baja de [casi] pero no llega a [bajo] y vuelve a subir → INCOMPLETA (la pantalla dice «Baja más»).
 * Entre umbrales no cambia nada: el temblor del modelo no cuenta repeticiones falsas.
 */
class ContadorEjercicio(private val bajo: Float, private val alto: Float, private val casi: Float) {
    var abajo = false
        private set
    private var minimo = 180f

    fun nuevoAngulo(grados: Float): Movimiento {
        if (!abajo) {
            if (grados < bajo) { abajo = true; return Movimiento.NADA }
            if (grados < minimo) minimo = grados
            if (grados > alto) {
                val incompleta = minimo < casi
                minimo = 180f
                if (incompleta) return Movimiento.INCOMPLETA
            }
            return Movimiento.NADA
        }
        if (grados > alto) { abajo = false; minimo = 180f; return Movimiento.REPETICION }
        return Movimiento.NADA
    }

    companion object {
        /** Umbrales de cada ejercicio. En la zancada la rodilla se ve menos doblada, por eso su umbral de «abajo» es más alto. */
        fun para(e: Ejercicio) = when (e) {
            Ejercicio.SENTADILLAS -> ContadorEjercicio(bajo = 100f, alto = 160f, casi = 140f)
            Ejercicio.ZANCADAS -> ContadorEjercicio(bajo = 115f, alto = 155f, casi = 145f)
        }
    }
}

/**
 * Ángulo de la rodilla más doblada de las dos (o de la única que se vea).
 * En la zancada la pierna de delante cambia en cada repetición, así que no vale mirar siempre la izquierda.
 */
fun anguloRodillaMasDoblada(p: Postura): Float? {
    fun lado(c: Parte, r: Parte, t: Parte): Float? {
        val cadera = p.visible(c) ?: return null
        val rodilla = p.visible(r) ?: return null
        val tobillo = p.visible(t) ?: return null
        return angulo(cadera, rodilla, tobillo)
    }
    return listOfNotNull(
        lado(Parte.CADERA_IZQ, Parte.RODILLA_IZQ, Parte.TOBILLO_IZQ),
        lado(Parte.CADERA_DER, Parte.RODILLA_DER, Parte.TOBILLO_DER),
    ).minOrNull()
}
