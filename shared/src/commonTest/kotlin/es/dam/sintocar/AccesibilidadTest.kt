package es.dam.sintocar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccesibilidadTest {
    @Test fun elContadorSeLeeComoFrase() {
        assertEquals("3 de 10 repeticiones", descripcionContador(3, 10))
    }

    @Test fun elIndicadorDeVozNoLeeEmojisNiFlechas() {
        val d = descripcionIndicador(EstadoIndicador.RECONOCIDO, "pausa", Accion.PAUSA, null, false, "pausa")
        assertEquals("Orden reconocida", d)
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

    /**
     * Requisito 5 con el lector de pantalla: TalkBack lee las regiones vivas por el altavoz y el micrófono lo oye.
     * Ningún texto de una región viva puede contener una palabra de MAPA_VOZ.
     */
    @Test fun ningunaRegionVivaContieneUnaOrden() {
        val textos = mutableListOf<String>()
        val frases = MAPA_VOZ.keys.toList() + listOf("", "hola")
        val errores = listOf(null, "Sin conexión a Internet", "Falta el permiso de micrófono",
            "Este dispositivo no tiene reconocimiento de voz. En el emulador usa una imagen con Google Play.",
            "El reconocedor ha fallado (código 7)")
        val pistas = Fase.entries.map { pistaVoz(it) } + MAPA_VOZ.keys
        for (estado in EstadoIndicador.entries) for (f in frases) for (a in Accion.entries + listOf(null))
            for (e in errores) for (h in listOf(true, false)) for (p in pistas)
                textos += descripcionIndicador(estado, f, a, e, h, p)
        for (hechas in 0..25) for (objetivo in 1..25) textos += descripcionContador(hechas, objetivo)
        for (fase in FaseRA.entries) {
            val avisos = listOf(null, "Hay poca luz", "Mueve el móvil más despacio", "Apunta a una zona con más textura", "He perdido la referencia")
            for (aviso in avisos) for (sin in listOf(true, false))
                textos += descripcionInstruccionRA(fase, textoInstruccion(fase, EstadoRA(aviso = aviso), sin))
            for (m in listOf(0.5f, 2.5f, 5f)) textos += descripcionInstruccionRA(fase, "${metrosConComa(m)} m · ${consejo(m)}")
        }
        for (c in Conteo.entries) for (i in listOf(true, false)) for (ab in listOf(true, false))
            for (an in Frases.todas() + listOf(null)) textos += textoConteo(c, an, i, ab)
        assertTrue(textos.size > 100)
        textos.forEach { assertNull(interpretar(it), "Una región viva diría una orden: «$it»") }
    }
}
