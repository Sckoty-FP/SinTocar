package es.dam.sintocar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EstabilizadorTest {
    /** Pasa una secuencia de gestos y devuelve cuáles se confirmaron. */
    private fun confirmados(imagenes: Int, vararg gestos: String): List<String> {
        val e = Estabilizador(imagenes)
        return gestos.filter { e.confirmar(it) }
    }

    @Test fun cincoSeguidasConfirman() = assertEquals(listOf("Thumb_Up"), confirmados(5, *Array(5) { "Thumb_Up" }))
    @Test fun cuatroNoBastan() = assertEquals(emptyList(), confirmados(5, *Array(4) { "Thumb_Up" }))
    @Test fun mantenerNoRepite() = assertEquals(listOf("Thumb_Up"), confirmados(5, *Array(30) { "Thumb_Up" }))
    @Test fun soltarYVolverRepite() = assertEquals(
        listOf("Thumb_Up", "Thumb_Up"),
        confirmados(5, *Array(5) { "Thumb_Up" }, "None", *Array(5) { "Thumb_Up" }),
    )
    @Test fun unFalloReinicia() = assertEquals(emptyList(), confirmados(5, "Thumb_Up", "Thumb_Up", "Victory", "Thumb_Up", "Thumb_Up"))
    @Test fun ningunoNoSeConfirma() = assertEquals(emptyList(), confirmados(5, *Array(10) { "None" }))
    @Test fun progreso() {
        val e = Estabilizador(5)
        repeat(2) { e.confirmar("Victory") }
        assertEquals(0.4f, e.progreso, 0.001f)
        e.confirmar("None")
        assertEquals(0f, e.progreso)
    }
}

class NombresTest {
    @Test fun pulgar() = assertEquals("👍 Pulgar arriba", enCastellano("Thumb_Up"))
    @Test fun desconocido() = assertEquals("Sin gesto", enCastellano("None"))
    @Test fun todosTienenNombre() = assertTrue(GESTOS.drop(1).none { enCastellano(it) == "Sin gesto" })
    @Test fun ningunoSinNombre() = assertFalse(enCastellano("Victory") == "Sin gesto")
}
