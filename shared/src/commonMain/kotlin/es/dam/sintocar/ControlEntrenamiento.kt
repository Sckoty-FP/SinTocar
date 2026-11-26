package es.dam.sintocar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Ejercicio(val nombre: String) { SENTADILLAS("Sentadillas"), ZANCADAS("Zancadas") }

data class Serie(val ejercicio: Ejercicio, val objetivo: Int)

enum class Rutina(val nombre: String, val series: List<Serie>) {
    SUAVE("Suave", listOf(Serie(Ejercicio.SENTADILLAS, 8), Serie(Ejercicio.ZANCADAS, 8), Serie(Ejercicio.SENTADILLAS, 8))),
    INTENSA("Intensa", listOf(Serie(Ejercicio.SENTADILLAS, 15), Serie(Ejercicio.ZANCADAS, 16), Serie(Ejercicio.SENTADILLAS, 20))),
}

/** En qué momento del entrenamiento estamos. */
enum class Fase { PREPARADO, EN_MARCHA, PAUSA, OBJETIVO_CUMPLIDO, TERMINADO }

/** Qué pasó con una orden. */
enum class Resultado { APLICADA, REPETIDA, NO_DISPONIBLE }

/** Si ahora mismo se cuenta, y si no, por qué (la pantalla lo dice). Requisito 4. */
enum class Conteo { CONTANDO, SIN_EMPEZAR, EN_PAUSA, SIN_ENCUADRE, ANUNCIANDO, TERMINADO }

data class OrdenRegistrada(val orden: Orden, val ms: Long, val resultado: Resultado)

/** Margen extra por si el TTS no avisa de que ha terminado. */
private const val MARGEN_VOZ_MS = 4000L

/**
 * El cerebro de la app. TODAS las órdenes (voz, postura, toque) entran por [ordenar].
 * No sabe nada de cámaras, micrófonos ni pantallas: por eso se puede testear con un reloj falso.
 *
 * @param reloj hora actual en ms. En la app, el reloj del sistema; en los tests, uno que avanza a mano.
 * @param anunciar se llama con cada frase que hay que decir en voz alta (y enseñar en pantalla).
 * @param enZonaRA true si la sesión empieza en la Zona RA (antes de entrenar). Se sale con [salirZonaRA].
 */
class ControlEntrenamiento(
    val rutina: Rutina,
    private val reloj: () -> Long,
    private val anunciar: (String) -> Unit = {},
    enZonaRA: Boolean = false,
) {
    var fase by mutableStateOf(Fase.PREPARADO); private set
    var indice by mutableStateOf(0); private set
    private val repeticiones = mutableStateListOf(*Array(rutina.series.size) { 0 })
    var encuadreOk by mutableStateOf(false); private set
    var ayudaVisible by mutableStateOf(false); private set
    var favorito by mutableStateOf(false); private set
    /**
     * true mientras se está en la Zona RA (antes de entrenar). En ese estado solo valen COLOCAR, QUITAR y AYUDA;
     * fuera de ella, COLOCAR y QUITAR no hacen nada (NO_DISPONIBLE).
     */
    var zonaRA by mutableStateOf(enZonaRA); private set
    /** Contadores que la escena de RA vigila: cada orden aplicada de colocar o quitar los sube en uno. */
    var peticionesColocar by mutableIntStateOf(0); private set
    var peticionesQuitar by mutableIntStateOf(0); private set
    /** true si hay suelo (o marcador) en la escena de RA. Sin suelo no se puede colocar. */
    var sueloEncontrado by mutableStateOf(false); private set
    /** true si han pedido colocar y no había suelo: la InstruccionRA dice qué hacer. */
    var sinSuelo by mutableStateOf(false); private set
    /** La última frase anunciada, para enseñarla en pantalla además de decirla. */
    var ultimoAnuncio by mutableStateOf<String?>(null); private set

    /** Todas las órdenes recibidas, aplicadas o no. */
    val historial = mutableStateListOf<OrdenRegistrada>()

    private val ultimaVez = mutableMapOf<Accion, Long>()
    private var avanzarEn: Long? = null
    private var esperandoFinDeVoz = false
    private var tiempoAcumulado = 0L
    private var enMarchaDesde: Long? = null

    val serieActual: Serie get() = rutina.series[indice]
    val repeticionesActuales: Int get() = repeticiones[indice]

    val conteo: Conteo
        get() = when (fase) {
            Fase.PREPARADO -> Conteo.SIN_EMPEZAR
            Fase.PAUSA -> Conteo.EN_PAUSA
            Fase.OBJETIVO_CUMPLIDO -> Conteo.ANUNCIANDO
            Fase.TERMINADO -> Conteo.TERMINADO
            Fase.EN_MARCHA -> if (encuadreOk) Conteo.CONTANDO else Conteo.SIN_ENCUADRE
        }

    /**
     * Lo que significa «brazos en cruz 2 s» ahora mismo: seguir si está en pausa, pausar si está en marcha,
     * y nada antes de empezar (mapa nuevo: para empezar hay que decirlo o pulsar).
     */
    val accionBrazosEnCruz: Accion?
        get() = when (fase) {
            Fase.PAUSA -> Accion.EMPEZAR
            Fase.EN_MARCHA, Fase.OBJETIVO_CUMPLIDO -> Accion.PAUSA
            Fase.PREPARADO, Fase.TERMINADO -> null
        }

    /** Punto único de entrada de órdenes: voz, postura, toque, gesto y cara (requisito 1). */
    fun ordenar(orden: Orden): Resultado {
        val ahora = reloj()
        val anterior = ultimaVez[orden.accion]
        val resultado = when {
            anterior != null && ahora - anterior < 1000 -> Resultado.REPETIDA
            aplicar(orden.accion, ahora) -> Resultado.APLICADA
            else -> Resultado.NO_DISPONIBLE
        }
        if (resultado != Resultado.REPETIDA) ultimaVez[orden.accion] = ahora
        historial += OrdenRegistrada(orden, ahora, resultado)
        return resultado
    }

    fun salirZonaRA() { zonaRA = false; ayudaVisible = false }

    /** La escena de RA avisa de si ve el suelo. Cuando aparece, el aviso de «sin suelo» sobra. */
    fun suelo(hay: Boolean) {
        sueloEncontrado = hay
        if (hay) sinSuelo = false
    }

    private fun aplicar(accion: Accion, ahora: Long): Boolean = when {
        accion == Accion.COLOCAR -> colocar()
        accion == Accion.QUITAR -> zonaRA.also { if (it) peticionesQuitar++ }
        // En la Zona RA aún no se entrena: solo la ayuda vale además de colocar y quitar.
        zonaRA && accion != Accion.AYUDA -> false
        else -> aplicarEntrenamiento(accion, ahora)
    }

    /** Solo se coloca con suelo: si no, no se anota como aplicada y se avisa. */
    private fun colocar(): Boolean {
        if (!zonaRA) return false
        if (!sueloEncontrado) { sinSuelo = true; return false }
        sinSuelo = false
        peticionesColocar++
        return true
    }

    private fun aplicarEntrenamiento(accion: Accion, ahora: Long): Boolean = when (accion) {
        Accion.EMPEZAR -> when (fase) {
            Fase.PREPARADO, Fase.PAUSA -> {
                val completa = repeticiones[indice] >= serieActual.objetivo
                ponerEnMarcha(ahora)
                if (completa) avanzar(ahora) else decir(Frases.empezar(serieActual))
                true
            }
            else -> false
        }
        Accion.PAUSA -> when (fase) {
            Fase.EN_MARCHA, Fase.OBJETIVO_CUMPLIDO -> {
                pararReloj(ahora); avanzarEn = null; esperandoFinDeVoz = false; fase = Fase.PAUSA; decir(Frases.EN_PAUSA); true
            }
            else -> false
        }
        Accion.SIGUIENTE -> when (fase) {
            Fase.TERMINADO -> false
            else -> { avanzar(ahora); true }
        }
        Accion.TERMINAR -> when (fase) {
            Fase.TERMINADO -> false
            else -> { terminar(ahora); true }
        }
        Accion.AYUDA -> { ayudaVisible = !ayudaVisible; true }
        Accion.FAVORITO -> when {
            fase == Fase.TERMINADO && !favorito -> { favorito = true; decir(Frases.FAVORITO); true }
            else -> false
        }
        Accion.COLOCAR, Accion.QUITAR -> false   // las trata aplicar() antes de llegar aquí
    }

    /** El contador de la cámara avisa de una repetición. Solo cuenta con datos fiables (requisito 4). */
    fun repeticion() {
        if (conteo != Conteo.CONTANDO) return
        repeticiones[indice]++
        if (repeticiones[indice] >= serieActual.objetivo) {
            fase = Fase.OBJETIVO_CUMPLIDO
            // Los 3 s empiezan cuando la voz TERMINA el anuncio (la escucha está parada mientras habla).
            // Si la voz no avisa, se avanza igual a los 3 s + 4 s de margen.
            avanzarEn = reloj() + 3000 + MARGEN_VOZ_MS
            esperandoFinDeVoz = true
            decir(Frases.objetivo(rutina.series.getOrNull(indice + 1)))
        }
    }

    /** La voz ha terminado de decir el anuncio de objetivo: desde ahora quedan 3 s para decir «pausa». */
    fun anuncioTerminado() {
        if (fase != Fase.OBJETIVO_CUMPLIDO || !esperandoFinDeVoz) return
        esperandoFinDeVoz = false
        avanzarEn = reloj() + 3000
    }

    fun actualizarEncuadre(ok: Boolean) { encuadreOk = ok }

    /** Llamar a menudo (p. ej. cada 100 ms): hace el avance automático a los 3 s (requisito 3). */
    fun tic() {
        val en = avanzarEn ?: return
        val ahora = reloj()
        if (fase == Fase.OBJETIVO_CUMPLIDO && ahora >= en) avanzar(ahora)
    }

    /** Tiempo entrenando de verdad (sin contar pausas ni antes de empezar). */
    fun tiempoMs(): Long = tiempoAcumulado + (enMarchaDesde?.let { reloj() - it } ?: 0)

    fun totalPorEjercicio(): Map<Ejercicio, Int> =
        rutina.series.indices.groupBy({ rutina.series[it].ejercicio }, { repeticiones[it] }).mapValues { it.value.sum() }

    private fun avanzar(ahora: Long) {
        avanzarEn = null; esperandoFinDeVoz = false
        if (indice + 1 >= rutina.series.size) { terminar(ahora); return }
        indice++
        if (fase == Fase.OBJETIVO_CUMPLIDO) fase = Fase.EN_MARCHA
        decir(Frases.cambio(serieActual))
    }

    private fun terminar(ahora: Long) {
        pararReloj(ahora); avanzarEn = null; esperandoFinDeVoz = false
        fase = Fase.TERMINADO
        decir(Frases.TERMINADO)
    }

    private fun ponerEnMarcha(ahora: Long) { fase = Fase.EN_MARCHA; enMarchaDesde = ahora }

    private fun pararReloj(ahora: Long) {
        enMarchaDesde?.let { tiempoAcumulado += ahora - it }
        enMarchaDesde = null
    }

    private fun decir(frase: String) { ultimoAnuncio = frase; anunciar(frase) }
}
