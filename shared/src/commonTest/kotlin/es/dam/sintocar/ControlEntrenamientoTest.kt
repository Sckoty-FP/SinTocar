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
private fun gesto(a: Accion) = Orden(a, Origen.GESTO)
private fun cara(a: Accion) = Orden(a, Origen.CARA)

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

    @Test fun pasaAlSiguienteALosTresSegundosDeTerminarDeHablar() {
        val r = Reloj(); val c = control(reloj = r)
        c.ordenar(toque(Accion.EMPEZAR)); completar(c)
        r.avanzar(5000); c.tic()
        assertEquals(0, c.indice)                        // aún está hablando: los 3 s no han empezado
        c.anuncioTerminado()                             // la voz acaba de terminar la frase
        r.avanzar(2999); c.tic()
        assertEquals(0, c.indice)
        r.avanzar(1); c.tic()
        assertEquals(1, c.indice)
        assertEquals(Ejercicio.ZANCADAS, c.serieActual.ejercicio)
        assertEquals(Fase.EN_MARCHA, c.fase)
    }

    @Test fun siElTTSNoAvisaAvanzaIgualALosSieteSegundos() {
        val r = Reloj(); val c = control(reloj = r)
        c.ordenar(toque(Accion.EMPEZAR)); completar(c)
        r.avanzar(6999); c.tic()
        assertEquals(0, c.indice)
        r.avanzar(1); c.tic()
        assertEquals(1, c.indice)
    }

    @Test fun pausaJustoDespuesDeTerminarDeHablarCancelaElAvance() {
        val r = Reloj(); val c = control(reloj = r)
        c.ordenar(toque(Accion.EMPEZAR)); completar(c)
        r.avanzar(2000); c.anuncioTerminado()
        r.avanzar(500); assertEquals(Resultado.APLICADA, c.ordenar(voz(Accion.PAUSA)))
        r.avanzar(10_000); c.tic()
        assertEquals(0, c.indice)
        assertEquals(Fase.PAUSA, c.fase)
    }

    @Test fun elFinDeUnAnuncioQueNoEsElDelObjetivoNoHaceNada() {
        val r = Reloj(); val c = control(reloj = r)
        c.ordenar(toque(Accion.EMPEZAR))
        c.anuncioTerminado()                             // acaba «Vamos...»: no hay nada que avanzar
        r.avanzar(10_000); c.tic()
        assertEquals(0, c.indice)
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
        repeat(3) { completar(c); c.anuncioTerminado(); r.avanzar(3000); c.tic() }
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
        assertEquals(Resultado.NO_DISPONIBLE, c.ordenar(cara(Accion.FAVORITO)))
        c.ordenar(toque(Accion.TERMINAR)); r.avanzar(1000)
        assertEquals(Resultado.APLICADA, c.ordenar(cara(Accion.FAVORITO)))
        assertTrue(c.favorito)
        assertEquals(Origen.CARA, c.historial.last().orden.origen)
    }

    @Test fun pausaSinEmpezarNoDisponible() =
        assertEquals(Resultado.NO_DISPONIBLE, control().ordenar(toque(Accion.PAUSA)))

    @Test fun brazosEnCruzNoHacenNadaAntesDeEmpezar() = assertNull(control().accionBrazosEnCruz)

    @Test fun brazosEnCruzPausanEnMarchaYSiguenEnPausa() {
        val r = Reloj(); val c = control(reloj = r)
        c.ordenar(toque(Accion.EMPEZAR)); r.avanzar(1000)
        assertEquals(Accion.PAUSA, c.accionBrazosEnCruz)
        c.ordenar(postura(Accion.PAUSA)); r.avanzar(1000)
        assertEquals(Accion.EMPEZAR, c.accionBrazosEnCruz)
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
    }

    /** Mapa nuevo: «favorito» ya no se dice, se guarda con la sonrisa o con el botón. */
    @Test fun favoritoYaNoSeDicePorVoz() = assertNull(interpretar("favorito"))

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
            repeat(rutina.series.size) { repeat(c.serieActual.objetivo) { c.repeticion() }; c.anuncioTerminado(); r.avanzar(3000); c.tic() }
        }
        val todas = anuncios + Frases.todas()
        assertTrue(anuncios.isNotEmpty())
        assertTrue(anuncios.all { it in Frases.todas() }, "Todo lo que se dice sale de Frases")
        todas.forEach { assertNull(interpretar(it), "La app diría una orden: «$it»") }
    }
}

class GestoTest {
    @Test fun palmaAbiertaPausaEnMarcha() {
        val r = Reloj(); val c = control(reloj = r)
        c.ordenar(toque(Accion.EMPEZAR)); r.avanzar(1000)
        assertEquals(Resultado.APLICADA, c.ordenar(gesto(Accion.PAUSA)))
        assertEquals(Fase.PAUSA, c.fase)
        assertEquals(Origen.GESTO, c.historial.last().orden.origen)
    }

    @Test fun pulgarArribaPasaALaSiguienteSerie() {
        val c = control()
        c.ordenar(toque(Accion.EMPEZAR))
        assertEquals(Resultado.APLICADA, c.ordenar(gesto(Accion.SIGUIENTE)))
        assertEquals(1, c.indice)
    }

    @Test fun palmaSinEmpezarNoDisponible() {
        val c = control()
        assertEquals(Resultado.NO_DISPONIBLE, c.ordenar(gesto(Accion.PAUSA)))
        assertEquals(Fase.PREPARADO, c.fase)
        assertEquals(Origen.GESTO, c.historial.last().orden.origen)   // se anota igual, con su origen
    }

    @Test fun debounceEntreOrigenesDistintos() {
        val r = Reloj(); val c = control(reloj = r)
        c.ordenar(toque(Accion.EMPEZAR))
        c.ordenar(voz(Accion.SIGUIENTE))
        r.avanzar(500)
        assertEquals(Resultado.REPETIDA, c.ordenar(gesto(Accion.SIGUIENTE)))
        assertEquals(1, c.indice)                        // solo avanzó una vez
        r.avanzar(500)
        assertEquals(Resultado.APLICADA, c.ordenar(gesto(Accion.SIGUIENTE)))
        assertEquals(2, c.indice)
    }

    @Test fun gestoYPosturaNoSeBloqueanSiSonAccionesDistintas() {
        val r = Reloj(); val c = control(reloj = r)
        c.ordenar(toque(Accion.EMPEZAR))
        r.avanzar(100)
        assertEquals(Resultado.APLICADA, c.ordenar(gesto(Accion.SIGUIENTE)))
        assertEquals(Resultado.APLICADA, c.ordenar(postura(Accion.PAUSA)))
    }
}

/** Una sesión en la Zona RA. Por defecto ya hay suelo: sin suelo no se puede colocar. */
private fun controlRA(reloj: Reloj = Reloj(), suelo: Boolean = true) =
    ControlEntrenamiento(Rutina.SUAVE, reloj = { reloj.ms }, enZonaRA = true).also { it.suelo(suelo) }

class ZonaRATest {
    @Test fun colocarPorVozSeAplicaYSeAnotaConSuOrigen() {
        val c = controlRA()
        assertEquals(Resultado.APLICADA, c.ordenar(voz(Accion.COLOCAR)))
        assertEquals(1, c.peticionesColocar)
        assertEquals(Origen.VOZ, c.historial.last().orden.origen)
    }

    @Test fun colocarSinSueloNoSeAplicaYAvisa() {
        val r = Reloj(); val c = controlRA(r, suelo = false)
        assertEquals(Resultado.NO_DISPONIBLE, c.ordenar(voz(Accion.COLOCAR)))
        assertEquals(0, c.peticionesColocar)
        assertTrue(c.sinSuelo)
        assertEquals("No encuentro el suelo: apunta al suelo y repite la orden",
            textoInstruccion(FaseRA.BUSCANDO, EstadoRA(), c.sinSuelo))
        c.suelo(true)                                      // aparece el suelo: el aviso se va
        assertFalse(c.sinSuelo)
        r.avanzar(1000)
        assertEquals(Resultado.APLICADA, c.ordenar(voz(Accion.COLOCAR)))
        assertEquals(1, c.peticionesColocar)
    }

    @Test fun elToqueEnLaEscenaEsUnaOrdenDeColocarConOrigenToque() {
        val c = controlRA()
        assertEquals(Resultado.APLICADA, c.ordenar(toque(Accion.COLOCAR)))
        assertEquals(Origen.TOQUE, c.historial.last().orden.origen)
        assertEquals(1, c.peticionesColocar)
    }

    @Test fun elPuntoDeColocacionEsElUltimoToqueOElCentro() {
        assertEquals(100f to 200f, puntoDeColocacion(100f to 200f, 400, 800))
        assertEquals(200f to 400f, puntoDeColocacion(null, 400, 800))
    }

    @Test fun quitarPorToqueSeAplicaYSeAnotaConSuOrigen() {
        val c = controlRA()
        assertEquals(Resultado.APLICADA, c.ordenar(toque(Accion.QUITAR)))
        assertEquals(1, c.peticionesQuitar)
        assertEquals(Origen.TOQUE, c.historial.last().orden.origen)
    }

    @Test fun colocarYQuitarEnMenosDeUnSegundoSeIgnoranVengaDeDondeVenga() {
        val r = Reloj(); val c = controlRA(r)
        c.ordenar(voz(Accion.COLOCAR))
        r.avanzar(500)
        assertEquals(Resultado.REPETIDA, c.ordenar(toque(Accion.COLOCAR)))
        assertEquals(1, c.peticionesColocar)
        c.ordenar(voz(Accion.QUITAR))
        r.avanzar(999)
        assertEquals(Resultado.REPETIDA, c.ordenar(toque(Accion.QUITAR)))
        assertEquals(1, c.peticionesQuitar)
        r.avanzar(1)
        assertEquals(Resultado.APLICADA, c.ordenar(toque(Accion.QUITAR)))
        assertEquals(2, c.peticionesQuitar)
    }

    @Test fun fueraDeLaZonaRANoHacenNada() {
        val c = control()                                  // sesión normal, sin Zona RA
        assertEquals(Resultado.NO_DISPONIBLE, c.ordenar(voz(Accion.COLOCAR)))
        assertEquals(Resultado.NO_DISPONIBLE, c.ordenar(toque(Accion.QUITAR)))
        assertEquals(0, c.peticionesColocar)
        assertEquals(0, c.peticionesQuitar)
        assertEquals(Origen.VOZ, c.historial.first().orden.origen)   // se anotan igual
    }

    @Test fun alSalirDeLaZonaRADejanDeValer() {
        val r = Reloj(); val c = controlRA(r)
        c.salirZonaRA()
        r.avanzar(2000)
        assertEquals(Resultado.NO_DISPONIBLE, c.ordenar(voz(Accion.COLOCAR)))
    }

    @Test fun enLaZonaRANoSeEntrenaAunque() {
        val c = controlRA()
        assertEquals(Resultado.NO_DISPONIBLE, c.ordenar(voz(Accion.EMPEZAR)))
        assertEquals(Fase.PREPARADO, c.fase)
        assertEquals(Resultado.APLICADA, c.ordenar(voz(Accion.AYUDA)))   // la ayuda sí vale
    }

    @Test fun lasPalabrasDeVoz() {
        assertEquals(Accion.COLOCAR, interpretar("Colocar"))
        assertEquals(Accion.COLOCAR, interpretar("coloca la esterilla"))
        assertEquals(Accion.QUITAR, interpretar("quitar"))
        assertEquals(Accion.QUITAR, interpretar("Quita"))
    }

    @Test fun laTransicionDiceLoQueSeLePide() {
        assertEquals("Cambio a la cámara frontal para verte mientras entrenas. Deja el móvil en el suelo, donde está la esterilla.",
            TEXTO_TRANSICION_CAMARA)
    }
}
