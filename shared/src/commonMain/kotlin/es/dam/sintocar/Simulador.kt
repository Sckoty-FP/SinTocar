package es.dam.sintocar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Los 7 gestos que reconoce el modelo, más "None" (ninguno). */
val GESTOS = listOf("None", "Thumb_Up", "Thumb_Down", "Open_Palm", "Closed_Fist", "Victory", "Pointing_Up", "ILoveYou")

/**
 * Simulador de gestos para cuando no hay cámara: se elige el gesto con botones
 * y se manda 15 veces por segundo, igual que haría la cámara con cada imagen.
 */
@Composable
fun SimuladorGestos(alDetectar: (Gesto) -> Unit) {
    var elegido by remember { mutableStateOf("None") }
    Column {
        for (fila in GESTOS.chunked(2)) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (g in fila) FilterChip(selected = g == elegido, onClick = { elegido = g }, modifier = Modifier.heightIn(min = BOTON_MINIMO), label = { Text(enCastellano(g)) })
        }
    }
    Repetir { alDetectar(Gesto(elegido, if (elegido == "None") 0f else 0.95f)) }
}

/**
 * Cámara de cara con interruptor «Sin cámara» (si no hay permiso de cámara, empieza ya simulando). Sin cámara, deslizadores para la sonrisa
 * y los ojos (0 a 100 %), mandados 15 veces por segundo.
 */
@Composable
fun FuenteCara(simularAlPrincipio: Boolean = false, alDetectar: (Cara) -> Unit) {
    var sinCamara by remember { mutableStateOf(simularAlPrincipio) }
    InterruptorSinCamara(sinCamara) { sinCamara = it }
    if (sinCamara) {
        var sonrisa by remember { mutableFloatStateOf(0f) }
        var ojos by remember { mutableFloatStateOf(1f) }
        Text("Sonrisa: ${(sonrisa * 100).toInt()} %")
        Slider(value = sonrisa, onValueChange = { sonrisa = it })
        Text("Ojos abiertos: ${(ojos * 100).toInt()} %")
        Slider(value = ojos, onValueChange = { ojos = it })
        Repetir { alDetectar(Cara(true, sonrisa, ojos, ojos)) }
    } else {
        CamaraCara(Modifier.fillMaxWidth().aspectRatio(3f / 4f), alDetectar)
    }
}

@Composable
private fun InterruptorSinCamara(activo: Boolean, alCambiar: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Switch(checked = activo, onCheckedChange = alCambiar, modifier = Modifier.botonAccesible())
        Text("Sin cámara: simular")
    }
}

/** Ejecuta [accion] cada 66 ms (unas 15 veces por segundo) mientras esté en pantalla. */
@Composable
private fun Repetir(accion: () -> Unit) {
    val ultima by rememberUpdatedState(accion)
    LaunchedEffect(Unit) {
        while (true) {
            ultima()
            delay(66)
        }
    }
}
