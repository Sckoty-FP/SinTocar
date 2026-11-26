package es.dam.sintocar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LectorGestosTest {
    private fun gesto(nombre: String, confianza: Float = 0.95f) = Gesto(nombre, confianza)

    /** Manda [n] imágenes seguidas del mismo gesto y devuelve las órdenes que salieron. */
    private fun LectorGestos.mandar(n: Int, nombre: String, confianza: Float = 0.95f) =
        List(n) { leer(gesto(nombre, confianza)) }.filterNotNull()

    @Test fun palmaAbiertaDa5ImagenesLaPausa() =
        assertEquals(listOf(Orden(Accion.PAUSA, Origen.GESTO)), LectorGestos().mandar(5, "Open_Palm"))

    @Test fun pulgarArribaDaSiguiente() =
        assertEquals(listOf(Orden(Accion.SIGUIENTE, Origen.GESTO)), LectorGestos().mandar(5, "Thumb_Up"))

    @Test fun cuatroImagenesNoBastan() = assertEquals(emptyList(), LectorGestos().mandar(4, "Open_Palm"))

    @Test fun mantenerNoRepite() = assertEquals(1, LectorGestos().mandar(40, "Thumb_Up").size)

    @Test fun soltarYVolverRepite() {
        val l = LectorGestos()
        val primera = l.mandar(5, "Thumb_Up")
        l.mandar(1, "None", 0f)
        assertEquals(2, (primera + l.mandar(5, "Thumb_Up")).size)
    }

    @Test fun confianzaBajaNoCuenta() = assertEquals(emptyList(), LectorGestos().mandar(20, "Open_Palm", 0.6f))

    @Test fun otrosGestosNoSonOrdenes() {
        val l = LectorGestos()
        assertEquals(emptyList(), l.mandar(20, "Victory"))
        assertEquals(0f, l.progreso)                     // y la barra no se llena
    }

    @Test fun elProgresoSubeConLaPalma() {
        val l = LectorGestos()
        l.mandar(2, "Open_Palm")
        assertEquals(0.4f, l.progreso, 0.001f)
        assertEquals("Open_Palm", l.viendo)
        l.mandar(1, "None", 0f)
        assertEquals(0f, l.progreso)
    }

    @Test fun elMapaDeGestos() {
        assertEquals(Accion.PAUSA, LectorGestos.accionDe("Open_Palm"))
        assertEquals(Accion.SIGUIENTE, LectorGestos.accionDe("Thumb_Up"))
        assertNull(LectorGestos.accionDe("Closed_Fist"))
    }
}
