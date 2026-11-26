package es.dam.sintocar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DetectorSonrisaTest {
    private var ms = 0L
    private fun detector() = DetectorSonrisa({ ms })

    /** Manda la misma sonrisa durante [duracion] ms, una imagen cada 66 ms, y cuenta cuántas veces confirma. */
    private fun DetectorSonrisa.sonreir(sonrisa: Float, duracion: Long): Int {
        var veces = 0
        val fin = ms + duracion
        while (ms <= fin) { if (actualizar(sonrisa)) veces++; ms += 66 }
        return veces
    }

    @Test fun sonrisaLargaConfirma() = assertEquals(1, detector().sonreir(0.95f, 1000))

    @Test fun sonrisaCortaNoConfirma() = assertEquals(0, detector().sonreir(0.95f, 600))

    @Test fun justoEnElUmbralNoCuenta() = assertEquals(0, detector().sonreir(0.8f, 3000))

    @Test fun soloConfirmaCuandoPasanLos800ms() {
        val d = detector()
        assertFalse(d.actualizar(0.9f))
        ms += 799
        assertFalse(d.actualizar(0.9f))
        ms += 1
        assertTrue(d.actualizar(0.9f))
    }

    @Test fun mantenerLaSonrisaNoRepite() = assertEquals(1, detector().sonreir(0.95f, 10_000))

    @Test fun bajarDe03PermiteRepetir() {
        val d = detector()
        var veces = d.sonreir(0.95f, 1000)
        d.sonreir(0.1f, 200)
        veces += d.sonreir(0.95f, 1000)
        assertEquals(2, veces)
    }

    @Test fun bajarSoloA05NoPermiteRepetir() {
        val d = detector()
        var veces = d.sonreir(0.95f, 1000)
        d.sonreir(0.5f, 300)                             // histéresis: entre 0,3 y 0,8 sigue sin soltar
        veces += d.sonreir(0.95f, 2000)
        assertEquals(1, veces)
    }

    @Test fun dejarDeSonreirAMitadReiniciaLaCuenta() {
        val d = detector()
        d.sonreir(0.95f, 500)
        d.sonreir(0.5f, 100)                             // se corta: vuelve a empezar
        assertEquals(0, d.sonreir(0.95f, 600))
    }

    @Test fun elProgresoSubeYSeReinicia() {
        val d = detector()
        d.actualizar(0.9f)
        ms += 400
        d.actualizar(0.9f)
        assertEquals(0.5f, d.progreso, 0.001f)
        d.actualizar(0.2f)
        assertEquals(0f, d.progreso)
    }
}
