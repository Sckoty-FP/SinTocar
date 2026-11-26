package es.dam.sintocar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Pantalla Entrenamiento: cámara con esqueleto y Marcador de encuadre, contador grande, ejercicio y objetivo,
 * tiempo, Indicador de voz, barra de la postura de pausa, botones y ayuda.
 * Las órdenes de los botones y de la postura van a [control] con su origen; las de voz llegan por la Sesión.
 */
@Composable
fun PantallaEntrenamiento(
    control: ControlEntrenamiento,
    escucha: EscuchaContinua,
    ahora: Long,
    reloj: () -> Long,
    modifier: Modifier = Modifier,
) {
    val permisoCamara = rememberPermiso(Recurso.CAMARA)
    var usarManiqui by remember { mutableStateOf(permisoCamara.estado != EstadoPermiso.CONCEDIDO) }
    var postura by remember { mutableStateOf<Postura?>(null) }
    val cruz = remember { PosturaMantenida(2000) }
    var progresoCruz by remember { mutableFloatStateOf(0f) }
    // Un contador nuevo en cada serie, para no arrastrar una bajada a medias del ejercicio anterior.
    val contador = remember(control.indice) { ContadorEjercicio.para(control.serieActual.ejercicio) }
    var incompletaEn by remember { mutableLongStateOf(-10_000L) }
    // Gestos de la mano: el mismo fotograma que la postura. El lector los convierte en órdenes.
    val lector = remember { LectorGestos(5) }
    var gestoViendo by remember { mutableStateOf("None") }
    var progresoGesto by remember { mutableFloatStateOf(0f) }
    var gestoHechoEn by remember { mutableLongStateOf(-10_000L) }
    var gestosDisponibles by remember { mutableStateOf(true) }   // false si falta el modelo de la mano
    var enCruzAhora by remember { mutableStateOf(false) }

    // Cada imagen de la cámara (o del maniquí) pasa por aquí.
    val procesar by rememberUpdatedState { p: Postura ->
        postura = p
        control.actualizarEncuadre(encuadre(p) == Encuadre.CORRECTO)
        val enCruz = brazosEnCruz(p)
        enCruzAhora = enCruz
        // 1) La postura de pausa funciona aunque esté en pausa: es la que la quita.
        if (cruz.actualizar(enCruz, reloj())) control.accionBrazosEnCruz?.let { control.ordenar(Orden(it, Origen.POSTURA)) }
        progresoCruz = cruz.progreso
        // 2) Solo se cuenta con datos fiables (requisito 4). Con los brazos en cruz tampoco: es una orden.
        if (control.conteo == Conteo.CONTANDO && !enCruz) {
            anguloRodillaMasDoblada(p)?.let { grados ->
                when (contador.nuevoAngulo(grados)) {
                    Movimiento.REPETICION -> control.repeticion()
                    Movimiento.INCOMPLETA -> incompletaEn = reloj()
                    Movimiento.NADA -> {}
                }
            }
        }
    }
    // Cada gesto de la mano (de la cámara o del simulador) entra aquí.
    val procesarGesto by rememberUpdatedState { g: Gesto ->
        // Brazos en cruz y palma abierta se pisan: mientras se cruzan los brazos, la mano no manda.
        lector.leer(g, ignorar = ignorarGestos(enCruzAhora, cruz.progreso))?.let { orden ->
            control.ordenar(orden)
            gestoHechoEn = reloj()
        }
        gestoViendo = lector.viendo
        progresoGesto = lector.progreso
    }
    val incompleta = ahora - incompletaEn < 1500
    val colorEsqueleto = when {
        control.conteo != Conteo.CONTANDO -> Colores.textoSuave
        incompleta -> Colores.aviso
        else -> Colores.correcto
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val horizontal = maxWidth > maxHeight
        val altoCamara = maxHeight * 0.45f
        val camara = @Composable { m: Modifier ->
            CamaraConEsqueleto(postura, colorEsqueleto, conCamara = !usarManiqui && permisoCamara.estado == EstadoPermiso.CONCEDIDO,
                modifier = m) { lectura ->
                gestosDisponibles = lectura.gestosDisponibles
                procesar(lectura.postura)
                lectura.gesto?.let { procesarGesto(it) }   // null = esta imagen no se ha mirado con la mano
            }
        }
        val panel = @Composable { m: Modifier ->
            val estadoGesto = when {
                ahora - gestoHechoEn < 1000 -> EstadoAccion.HECHA
                progresoGesto > 0f -> EstadoAccion.MANTENIENDO
                else -> EstadoAccion.INACTIVA
            }
            Panel(control, escucha, ahora, contador.abajo, incompleta, progresoCruz, usarManiqui,
                gestoViendo, estadoGesto, progresoGesto, gestosDisponibles,
                alCambiarManiqui = { usarManiqui = it; if (it) gestosDisponibles = true }, alManiqui = { procesar(it) },
                alGestoSimulado = { procesarGesto(it) }, modifier = m)
        }
        if (horizontal) Row(Modifier.fillMaxSize()) {
            camara(Modifier.fillMaxHeight().aspectRatio(3f / 4f))
            panel(Modifier.weight(1f).fillMaxHeight())
        } else Column(Modifier.fillMaxSize()) {
            camara(Modifier.fillMaxWidth().height(altoCamara))
            panel(Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Panel(
    c: ControlEntrenamiento,
    escucha: EscuchaContinua,
    ahora: Long,
    abajo: Boolean,
    incompleta: Boolean,
    progresoCruz: Float,
    usarManiqui: Boolean,
    gestoViendo: String,
    estadoGesto: EstadoAccion,
    progresoGesto: Float,
    gestosDisponibles: Boolean,
    alCambiarManiqui: (Boolean) -> Unit,
    alManiqui: (Postura) -> Unit,
    alGestoSimulado: (Gesto) -> Unit,
    modifier: Modifier,
) {
    var confirmarTerminar by remember { mutableStateOf(false) }
    fun toque(a: Accion) { c.ordenar(Orden(a, Origen.TOQUE)) }

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Ejercicio, serie y tiempo.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(c.serieActual.ejercicio.nombre, fontSize = Tamanos.titulo, fontWeight = FontWeight.Bold, color = Colores.texto)
            Text("⏱ ${formatoTiempo(c.tiempoMs())}", fontSize = Tamanos.titulo, color = Colores.texto)
        }
        Text("Serie ${c.indice + 1} de ${c.rutina.series.size} · objetivo ${c.serieActual.objetivo}",
            fontSize = Tamanos.apoyo, color = Colores.textoSuave)

        // Contador grande: se lee a 3 metros.
        // Región viva: el lector de pantalla dice el número cada vez que cambia.
        Text("${c.repeticionesActuales} / ${c.serieActual.objetivo}",
            fontSize = Tamanos.contador, fontWeight = FontWeight.Black, color = Colores.texto,
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = descripcionContador(c.repeticionesActuales, c.serieActual.objetivo)
            })

        // Por qué cuenta o no cuenta (requisito 4): una sola instrucción, la más importante.
        val mensaje = textoConteo(c.conteo, c.ultimoAnuncio, incompleta, abajo)
        val color = colorConteo(c.conteo, incompleta)
        Text(mensaje, fontSize = Tamanos.titulo, fontWeight = FontWeight.Bold, color = color,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })

        // Ayuda siempre visible, según el momento (descubribilidad).
        Text(pistaCorta(c.fase), fontSize = Tamanos.apoyo, color = Colores.texto)

        IndicadorVoz(escucha.indicador(ahora), escucha.ultimaFrase, escucha.ultimaAccion, escucha.error,
            escucha.hablando, pista = pistaVoz(c.fase))

        // Barra de la postura de pausa: cuánto falta de los 2 segundos con los brazos en cruz.
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Brazos en cruz 2 s → ${when (c.accionBrazosEnCruz) { Accion.PAUSA -> "pausa"; Accion.EMPEZAR -> "seguir"; else -> "nada ahora" }}",
                fontSize = Tamanos.apoyo, color = Colores.textoSuave)
            LinearProgressIndicator(progress = { progresoCruz }, modifier = Modifier.fillMaxWidth().height(12.dp),
                color = Colores.aviso)
        }

        // Gesto de la mano: qué se ve y cuánto falta para que cuente (componente «Acción mantenida» de Figma).
        val accionGesto = LectorGestos.accionDe(gestoViendo)
        AccionMantenida(
            estadoGesto,
            when {
                estadoGesto == EstadoAccion.HECHA -> "¡Hecho!"
                accionGesto == Accion.PAUSA -> "${enCastellano(gestoViendo)} → pausa"
                accionGesto == Accion.SIGUIENTE -> "${enCastellano(gestoViendo)} → siguiente"
                !gestosDisponibles -> "Gestos no disponibles"
                else -> "Palma ✋ = pausa · pulgar 👍 = siguiente"
            },
            progresoGesto,
        )

        // Alternativa táctil de cada orden.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (c.fase == Fase.PREPARADO || c.fase == Fase.PAUSA)
                BotonGrande(if (c.fase == Fase.PREPARADO) "▶ Empezar" else "▶ Seguir", color = Colores.correcto) { toque(Accion.EMPEZAR) }
            else BotonGrande("⏸ Pausa", color = Colores.aviso) { toque(Accion.PAUSA) }
            BotonGrande("⏭ Siguiente") { toque(Accion.SIGUIENTE) }
            BotonGrande("■ Terminar", color = Colores.error) { confirmarTerminar = true }
            BotonGrande("? Ayuda", color = Colores.textoSuave) { toque(Accion.AYUDA) }
        }

        if (c.ayudaVisible) AyudaOrdenes()

        if (esEscritorio) OrdenEscrita(escucha) { c.ordenar(Orden(it, Origen.VOZ)) }
        SelectorDispositivos()

        // Respaldo sin cámara (Tema 6): no sustituye a la cámara real en el vídeo.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(checked = usarManiqui, onCheckedChange = alCambiarManiqui, modifier = Modifier.botonAccesible())
            Text("Sin cámara: usar el maniquí", fontSize = Tamanos.apoyo, color = Colores.textoSuave)
        }
        if (usarManiqui) {
            Maniqui(alManiqui)
            SimuladorGestos(alGestoSimulado)
        }
    }

    // «Terminar» no se puede deshacer: con el botón se confirma (tolerancia al error).
    if (confirmarTerminar) AlertDialog(
        onDismissRequest = { confirmarTerminar = false },
        title = { Text("¿Terminar el entrenamiento?", fontSize = Tamanos.apoyo) },
        text = { Text("Irás al resumen y no podrás seguir con esta rutina.", fontSize = Tamanos.apoyo) },
        confirmButton = { TextButton(onClick = { confirmarTerminar = false; toque(Accion.TERMINAR) }, modifier = Modifier.botonAccesible()) { Text("Sí, terminar", fontSize = Tamanos.apoyo) } },
        dismissButton = { TextButton(onClick = { confirmarTerminar = false }, modifier = Modifier.botonAccesible()) { Text("No, seguir", fontSize = Tamanos.apoyo) } },
    )
}

/**
 * Qué dice la franja de estado (es una región viva: el lector de pantalla la lee en voz alta por el altavoz).
 * Por eso NINGUNA palabra de MAPA_VOZ puede salir aquí: ni «pausa» ni «para». Lo comprueba un test.
 */
fun textoConteo(conteo: Conteo, anuncio: String?, incompleta: Boolean, abajo: Boolean) = when (conteo) {
    Conteo.SIN_EMPEZAR -> "Preparado"
    Conteo.EN_PAUSA -> "⏸ Detenido · no cuento"
    Conteo.SIN_ENCUADRE -> "No cuento: aléjate hasta que te vea entero"
    Conteo.ANUNCIANDO -> anuncio.orEmpty()
    Conteo.TERMINADO -> ""
    Conteo.CONTANDO -> when {
        incompleta -> "Baja más"
        abajo -> "⬆ Bien, ahora sube"
        else -> "⬇ Baja"
    }
}

private fun colorConteo(conteo: Conteo, incompleta: Boolean) = when (conteo) {
    Conteo.SIN_EMPEZAR -> Colores.textoSuave
    Conteo.EN_PAUSA -> Colores.aviso
    Conteo.SIN_ENCUADRE -> Colores.error
    Conteo.ANUNCIANDO -> Colores.correcto
    Conteo.TERMINADO -> Colores.texto
    Conteo.CONTANDO -> if (incompleta) Colores.aviso else Colores.correcto
}

/** La pista de cada momento, en pantalla. */
private fun pistaCorta(f: Fase) = when (f) {
    Fase.PREPARADO -> "Di «empezar» o pulsa el botón"
    Fase.EN_MARCHA -> "Di «pausa», cruza los brazos 2 s o enseña la palma · «siguiente» o pulgar arriba · «ayuda»"
    Fase.PAUSA -> "Di «sigue» o cruza los brazos 2 s"
    Fase.OBJETIVO_CUMPLIDO -> "Paso al siguiente en 3 s · di «pausa» para esperar"
    Fase.TERMINADO -> ""
}

/** La orden que se sugiere cuando no se entiende lo que ha dicho. */
fun pistaVoz(f: Fase) = when (f) {
    Fase.PREPARADO, Fase.PAUSA -> "«empezar»"
    Fase.EN_MARCHA, Fase.OBJETIVO_CUMPLIDO -> "«pausa»"
    Fase.TERMINADO -> "«ayuda»"
}

/** El mapa de órdenes completo (requisito 2), visible con «ayuda». */
@Composable
fun AyudaOrdenes() {
    Column(
        Modifier.fillMaxWidth().background(Colores.superficie, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Qué puedes hacer", fontSize = Tamanos.apoyo, fontWeight = FontWeight.Bold, color = Colores.texto)
        listOf(
            "Empezar" to "«empezar» · botón",
            "Seguir (en pausa)" to "«sigue» · brazos en cruz 2 s · botón",
            "Pausa" to "«pausa», «para» · brazos en cruz 2 s · ✋ palma abierta · botón",
            "Siguiente ejercicio" to "«siguiente» · 👍 pulgar arriba · botón",
            "Terminar" to "«terminar» · botón (con confirmación)",
            "Ayuda" to "«ayuda» · botón",
            "Colocar / quitar la esterilla (Zona RA)" to "«colocar», «quitar» · tocar la escena · botones",
            "Favorito (en el resumen)" to "😊 sonrisa 0,8 s · botón",
        ).forEach { (accion, como) ->
            Text("$accion: $como", fontSize = Tamanos.apoyo, color = Colores.texto)
        }
    }
}

/** Respaldo de escritorio sin micrófono: la frase escrita sigue el mismo camino que la hablada. */
@Composable
fun OrdenEscrita(escucha: EscuchaContinua, alOrden: (Accion) -> Unit) {
    var texto by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(texto, { texto = it }, label = { Text("Orden escrita") }, singleLine = true, modifier = Modifier.weight(1f))
        BotonGrande("Enviar") { escucha.recibir(texto, alOrden); texto = "" }
    }
}

/** Lo que lee el lector de pantalla del contador: «3 de 10 repeticiones», en vez de «3 barra 10». */
fun descripcionContador(hechas: Int, objetivo: Int) = "$hechas de $objetivo repeticiones"
