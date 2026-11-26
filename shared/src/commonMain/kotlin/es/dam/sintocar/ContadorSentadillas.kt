package es.dam.sintocar

/**
 * Cuenta sentadillas con dos umbrales:
 *   ARRIBA --(rodilla < 100°)--> ABAJO --(rodilla > 160°)--> ARRIBA y suma 1
 * Entre 100° y 160° no cambia nada, así el temblor del modelo no cuenta repeticiones falsas.
 */
class ContadorSentadillas {
    var repeticiones = 0
        private set
    var abajo = false
        private set

    fun nuevoAngulo(grados: Float) {
        if (!abajo && grados < 100f) abajo = true
        else if (abajo && grados > 160f) { abajo = false; repeticiones++ }
    }
}
