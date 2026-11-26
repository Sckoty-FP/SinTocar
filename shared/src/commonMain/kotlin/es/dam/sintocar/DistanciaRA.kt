package es.dam.sintocar

import kotlin.math.roundToInt

/** Texto de la instrucción en cada fase. */
fun textoInstruccion(fase: FaseRA, estado: EstadoRA) = when (fase) {
    FaseRA.PROBLEMA -> estado.aviso ?: "Hay un problema con la RA"
    FaseRA.BUSCANDO -> comoBuscar
    FaseRA.LISTO -> comoColocar
    FaseRA.COLOCADO -> "Esterilla colocada: muévete, no se mueve"
}

/** Consejo para que la cámara frontal vea el cuerpo entero al entrenar sobre la esterilla. */
fun consejo(metros: Float) = when {
    metros < 1.8f -> "Demasiado cerca"
    metros <= 3.2f -> "Distancia buena para entrenar"
    else -> "Demasiado lejos"
}

/** 2.345 → "2,3". Con una cifra decimal basta: la medida tiene un error de varios centímetros. */
fun metrosConComa(d: Float): String {
    val decimas = (d * 10).roundToInt()
    return "${decimas / 10},${decimas % 10}"
}
