package es.dam.sintocar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContadorEjercicioTest {
    private fun eventos(ej: Ejercicio, vararg grados: Float): List<Movimiento> {
        val c = ContadorEjercicio.para(ej)
        return grados.map { c.nuevoAngulo(it) }.filter { it != Movimiento.NADA }
    }

    @Test fun sentadillaCompleta() =
        assertEquals(listOf(Movimiento.REPETICION), eventos(Ejercicio.SENTADILLAS, 175f, 90f, 175f))

    @Test fun bajarAMediasEsIncompleta() =
        assertEquals(listOf(Movimiento.INCOMPLETA), eventos(Ejercicio.SENTADILLAS, 175f, 125f, 175f))

    @Test fun temblarDePieNoEsNada() =
        assertEquals(emptyList(), eventos(Ejercicio.SENTADILLAS, 175f, 150f, 170f, 148f, 175f))

    @Test fun temblarAbajoCuentaUna() =
        assertEquals(listOf(Movimiento.REPETICION), eventos(Ejercicio.SENTADILLAS, 175f, 99f, 101f, 99f, 159f, 161f))

    @Test fun zancadaUsaUmbralMasSuave() {
        // En la zancada vista de frente la rodilla se ve menos doblada que en la sentadilla.
        assertEquals(listOf(Movimiento.REPETICION), eventos(Ejercicio.ZANCADAS, 175f, 108f, 170f))
        assertEquals(listOf(Movimiento.INCOMPLETA), eventos(Ejercicio.SENTADILLAS, 175f, 108f, 170f))
    }

    @Test fun abajoSeInforma() {
        val c = ContadorEjercicio.para(Ejercicio.SENTADILLAS)
        c.nuevoAngulo(90f)
        assertEquals(true, c.abajo)
    }
}

class AnguloEjercicioTest {
    @Test fun usaLaRodillaMasDoblada() {
        // Maniquí con las dos rodillas iguales: da el mismo ángulo que la izquierda.
        assertEquals(anguloRodilla(posturaManiqui(95f, 0f))!!, anguloRodillaMasDoblada(posturaManiqui(95f, 0f))!!, 0.01f)
    }

    @Test fun conUnaPiernaBastaParaZancadas() {
        val sinIzq = Postura(posturaManiqui(95f, 0f).puntos - Parte.TOBILLO_IZQ)
        assertEquals(95f, anguloRodillaMasDoblada(sinIzq)!!, 1f)
    }

    @Test fun sinPiernasNoHayAngulo() =
        assertNull(anguloRodillaMasDoblada(Postura(emptyMap())))
}
