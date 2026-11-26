package es.dam.sintocar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccesibilidadTest {
    @Test fun elContadorSeLeeComoFrase() {
        assertEquals("3 de 10 repeticiones", descripcionContador(3, 10))
    }

    @Test fun elIndicadorDeVozNoLeeEmojisNiFlechas() {
        val d = descripcionIndicador(EstadoIndicador.RECONOCIDO, "pausa", Accion.PAUSA, null, false, "pausa")
        assertEquals("Orden reconocida: pausa, Pausa", d)
        assertFalse(d.contains("→") || d.contains("✔"))
    }

    @Test fun elIndicadorDiceElEstadoConTexto() {
        assertEquals("Te escucho", descripcionIndicador(EstadoIndicador.ESCUCHANDO, null, null, null, false, ""))
        assertEquals("Micrófono en espera", descripcionIndicador(EstadoIndicador.INACTIVO, null, null, null, false, ""))
        assertEquals("Hablando", descripcionIndicador(EstadoIndicador.INACTIVO, null, null, null, true, ""))
        assertTrue(descripcionIndicador(EstadoIndicador.NO_ENTENDIDO, null, null, null, false, "pausa").startsWith("No te he entendido"))
    }

    @Test fun elErrorSeAnadeALaDescripcion() {
        assertTrue(descripcionIndicador(EstadoIndicador.INACTIVO, null, null, "sin micro", false, "").endsWith("Error: sin micro"))
    }

    @Test fun cadaFaseDeRATieneNombreEnTexto() {
        assertTrue(FaseRA.entries.all { it.nombre.isNotBlank() })
    }
}
