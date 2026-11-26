package es.dam.sintocar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Reloj de mentira: el test decide qué hora es. */
private class Reloj(var ms: Long = 0) { fun avanzar(d: Long) { ms += d } }

private fun control(rutina: Rutina = Rutina.SUAVE, reloj: Reloj = Reloj()) =
    ControlEntrenamiento(rutina, reloj = { reloj.ms }).also { it.actualizarEncuadre(true) }

private fun toque(a: Accion) = Orden(a, Origen.TOQUE)
private fun voz(a: Accion) = Orden(a, Origen.VOZ)
private fun postura(a: Accion) = Orden(a, Origen.POSTURA)

class ArranqueTest {
    @Test fun empiezaPreparado() {
        val c = control()
        assertEquals(Fase.PREPARADO, c.fase)
        assertEquals(Ejercicio.SENTADILLAS, c.serieActual.ejercicio)
        assertEquals(8, c.serieActual.objetivo)
    }

    @Test fun empezarPoneEnMarcha() {
        val c = control()
        assertEquals(Resultado.APLICADA, c.ordenar(toque(Accion.EMPEZAR)))
        assertEquals(Fase.EN_MARCHA, c.fase)
    }

    @Test fun rutinasDelEnunciado() {
        assertEquals(listOf(8, 8, 8), Rutina.SUAVE.series.map { it.objetivo })
        assertEquals(listOf(15, 16, 20), Rutina.INTENSA.series.map { it.objetivo })
        assertEquals(listOf(Ejercicio.SENTADILLAS, Ejercicio.ZANCADAS, Ejercicio.SENTADILLAS), Rutina.INTENSA.series.map { it.ejercicio })
    }
}

class DebounceTest {
    @Test fun mismaAccionEnMenosDeUnSegundoSeIgnoraVengaDeDondeVenga() {
        val r = Reloj(); val c = control(reloj = r)
        c.ordenar(toque(Accion.AYUDA))
        r.avanzar(999)
        assertEquals(Resultado.REPETIDA, c.ordenar(voz(Accion.AYUDA)))
    }

    @Test fun mismaAccionPasadoUnSegundoSeAplica() {
        val r = Reloj(); val c = control(reloj = r)
        c.ordenar(toque(Accion.AYUDA))
        r.avanzar(1000)
        assertEquals(Resultado.APLICADA, c.ordenar(voz(Accion.AYUDA)))
    }

    @Test fun accionesDistintasNoSeBloquean() {
        val r = Reloj(); val c = control(reloj = r)
        c.ordenar(voz(Accion.EMPEZAR))
        r.avanzar(100)
        assertEquals(Resultado.APLICADA, c.ordenar(postura(Accion.PAUSA)))
        assertEquals(Fase.PAUSA, c.fase)
    }

    @Test fun vozYPosturaALaVezSoloCuentaUna() {
        val r = Reloj(); val c = control(reloj = r)
        c.ordenar(voz(Accion.EMPEZAR)); r.avanzar(1500)
        c.ordenar(voz(Accion.PAUSA)); r.avanzar(300)
        assertEquals(Resultado.REPETIDA, c.ordenar(postura(Accion.PAUSA)))
        assertEquals(1, c.historial.count { it.orden.accion == Accion.PAUSA && it.resultado == Resultado.APLICADA })
    }
}

class ConteoTest {
    @Test fun cuentaEnMarchaConEncuadre() {
        val c = control(); c.ordenar(toque(Accion.EMPEZAR))
        c.repeticion()
        assertEquals(1, c.repeticionesActuales)
        assertEquals(Conteo.CONTANDO, c.conteo)
    }

    @Test fun sinEncuadreNoCuentaYLoDice() {
        val c = control(); c.ordenar(toque(Accion.EMPEZAR))
        c.actualizarEncuadre(false)
        c.repeticion()
        assertEquals(0, c.repeticionesActuales)
        assertEquals(Conteo.SIN_ENCUADRE, c.conteo)
    }

    @Test fun enPausaNoCuentaYLoDice() {
        val r = Reloj(); val c = control(reloj = r)
        c.ordenar(toque(Accion.EMPEZAR)); r.avanzar(1000)
        c.ordenar(toque(Accion.PAUSA))
        c.repeticion()
        assertEquals(0, c.repeticionesActuales)
        assertEquals(Conteo.EN_PAUSA, c.conteo)
    }

    @Test fun antesDeEmpezarNoCuenta() {
        val c = control()
        c.repeticion()
        assertEquals(0, c.repeticionesActuales)
        assertEquals(Conteo.SIN_EMPEZAR, c.conteo)
    }
}

class AvanceAutomaticoTest {
    private fun completar(c: ControlEntrenamiento) = repeat(c.serieActual.objetivo) { c.repeticion() }

    @Test fun alLlegarAlObjetivoAnunciaYEspera() {
        val anuncios = mutableListOf<String>()
        val r = Reloj(); val c = ControlEntrenamiento(Rutina.SUAVE, { r.ms }, { anuncios += it }).also { it.actualizarEncuadre(true) }
        c.ordenar(toque(Accion.EMPEZAR))
        val antes = anuncios.size
        completar(c)
        assertEquals(Fase.OBJETIVO_CUMPLIDO, c.fase)
        assertEquals(antes + 1, anuncios.size)
        assertEquals(Frases.objetivo(Rutina.SUAVE.series[1]), anuncios.last())
        c.repeticion()                                   // mientras anuncia no se cuenta de más
        assertEquals(8, c.repeticionesActuales)
    }

    @Test fun pasaAlSiguienteALosTresSegundos() {
        val r = Reloj(); val c = control(reloj = r)
        c.ordenar(toque(Accion.EMPEZAR)); completar(c)
        r.avanzar(2999); c.tic()
        assertEquals(0, c.indice)
        r.avanzar(1); c.tic()
        assertEquals(1, c.indice)
        assertEquals(Ejercicio.ZANCADAS, c.serieActual.ejercicio)
        assertEquals(Fase.EN_MARCHA, c.fase)
    }

    @Test fun pausaCancelaElAvanceYSeguirLoHace() {
        val r = Reloj(); val c = control(reloj = r)
        c.ordenar(toque(Accion.EMPEZAR)); r.avanzar(1000); completar(c)
        r.avanzar(1000); c.ordenar(voz(Accion.PAUSA))
        r.avanzar(5000); c.tic()
        assertEquals(0, c.indice)
        assertEquals(Fase.PAUSA, c.fase)
        c.ordenar(voz(Accion.EMPEZAR))
        assertEquals(1, c.indice)
        assertEquals(Fase.EN_MARCHA, c.fase)
    }

    @Test fun laUltimaSerieTermina() {
        val r = Reloj(); val c = control(reloj = r)
        c.ordenar(toque(Accion.EMPEZAR))
        repeat(3) { completar(c); r.avanzar(3000); c.tic() }
        assertEquals(Fase.TERMINADO, c.fase)
        assertEquals(mapOf(Ejercicio.SENTADILLAS to 16, Ejercicio.ZANCADAS to 8), c.totalPorEjercicio())
    }
}

class OrdenesTest {
    @Test fun siguienteSaltaAunqueNoLlegue() {
        val c = control(); c.ordenar(toque(Accion.EMPEZAR))
        c.repeticion()
        c.ordenar(voz(Accion.SIGUIENTE))
        assertEquals(1, c.indice)
        assertEquals(Fase.EN_MARCHA, c.fase)
    }

    @Test fun terminarLlevaAlResumen() {
        val c = control(); c.ordenar(toque(Accion.EMPEZAR))
        c.ordenar(toque(Accion.TERMINAR))
        assertEquals(Fase.TERMINADO, c.fase)
    }

    @Test fun ayudaAlternaYNoCambiaLaFase() {
        val c = control()
        c.ordenar(voz(Accion.AYUDA))
        assertTrue(c.ayudaVisible)
        assertEquals(Fase.PREPARADO, c.fase)
    }

    @Test fun favoritoSoloEnElResumen() {
        val r = Reloj(); val c = control(reloj = r)
        assertEquals(Resultado.NO_DISPONIBLE, c.ordenar(voz(Accion.FAVORITO)))
        c.ordenar(toque(Accion.TERMINAR)); r.avanzar(1000)
        assertEquals(Resultado.APLICADA, c.ordenar(voz(Accion.FAVORITO)))
        assertTrue(c.favorito)
    }

    @Test fun pausaSinEmpezarNoDisponible() =
        assertEquals(Resultado.NO_DISPONIBLE, control().ordenar(toque(Accion.PAUSA)))

    @Test fun brazosEnCruzEmpiezaOPausaSegunLaFase() {
        val r = Reloj(); val c = control(reloj = r)
        assertEquals(Accion.EMPEZAR, c.accionBrazosEnCruz)
        c.ordenar(postura(Accion.EMPEZAR)); r.avanzar(1000)
        assertEquals(Accion.PAUSA, c.accionBrazosEnCruz)
        c.ordenar(toque(Accion.TERMINAR))
        assertNull(c.accionBrazosEnCruz)
    }

    @Test fun historialGuardaElOrigen() {
        val c = control()
        c.ordenar(postura(Accion.EMPEZAR))
        assertEquals(Origen.POSTURA, c.historial.last().orden.origen)
    }
}

class TiempoTest {
    @Test fun soloCuentaElTiempoEnMarcha() {
        val r = Reloj(); val c = control(reloj = r)
        r.avanzar(5000)                                  // antes de empezar no cuenta
        c.ordenar(toque(Accion.EMPEZAR)); r.avanzar(10_000)
        c.ordenar(toque(Accion.PAUSA)); r.avanzar(7000)  // en pausa no cuenta
        assertEquals(10_000, c.tiempoMs())
    }
}

class VozTest {
    @Test fun mapaDeOrdenes() {
        assertEquals(Accion.EMPEZAR, interpretar("empezar"))
        assertEquals(Accion.EMPEZAR, interpretar("Sigue"))
        assertEquals(Accion.PAUSA, interpretar("pausa"))
        assertEquals(Accion.PAUSA, interpretar("para ya"))
        assertEquals(Accion.SIGUIENTE, interpretar("siguiente"))
        assertEquals(Accion.TERMINAR, interpretar("terminar"))
        assertEquals(Accion.AYUDA, interpretar("ayuda"))
        assertEquals(Accion.FAVORITO, interpretar("favorito"))
    }

    @Test fun loQueNoEsOrdenDevuelveNull() {
        assertNull(interpretar("hola qué tal"))
        assertNull(interpretar(""))
    }

    @Test fun siguienteNoSeConfundeConSigue() = assertEquals(Accion.SIGUIENTE, interpretar("siguiente"))

    /** Requisito 5: la app no se obedece a sí misma. */
    @Test fun ningunaFraseDeLaAppEsUnaOrden() {
        val anuncios = mutableListOf<String>()
        for (rutina in Rutina.entries) {
            val r = Reloj()
            val c = ControlEntrenamiento(rutina, { r.ms }, { anuncios += it }).also { it.actualizarEncuadre(true) }
            c.ordenar(toque(Accion.EMPEZAR))
            repeat(rutina.series.size) { repeat(c.serieActual.objetivo) { c.repeticion() }; r.avanzar(3000); c.tic() }
        }
        val todas = anuncios + Frases.todas()
        assertTrue(anuncios.isNotEmpty())
        assertTrue(anuncios.all { it in Frases.todas() }, "Todo lo que se dice sale de Frases")
        todas.forEach { assertNull(interpretar(it), "La app diría una orden: «$it»") }
    }
}
