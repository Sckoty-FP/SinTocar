package es.dam.sintocar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Pantalla Zona RA (Tema 8): se coloca una esterilla virtual en el suelo y se mide a qué distancia está.
 * Usa la cámara TRASERA. «colocar» y «quitar» (voz o botones) entran por [control], igual que el resto de órdenes.
 * Esta pantalla suelta la cámara al salir de la composición. La Sesión no enseña el Entrenamiento
 * (cámara frontal) hasta pasar por la transición, así nunca hay dos cámaras abiertas a la vez.
 */
@Composable
fun PantallaZonaRA(
    control: ControlEntrenamiento,
    escucha: EscuchaContinua,
    ahora: Long,
    alEmpezar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var estado by remember { mutableStateOf(EstadoRA()) }

    // ARCore puede tardar unos segundos en contestar si hay RA: mientras dice «Comprobando» se vuelve a preguntar cada segundo.
    var repaso by remember { mutableIntStateOf(0) }
    val compatibilidad = compatibilidadRA().also { repaso }
    LaunchedEffect(compatibilidad, repaso) { if (compatibilidad.startsWith("…")) { delay(1000); repaso++ } }
    val hayRA = compatibilidad.startsWith("✅")

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Zona RA", fontSize = Tamanos.principal, fontWeight = FontWeight.Bold, color = Colores.texto)
        Text(compatibilidad, fontSize = Tamanos.apoyo, color = Colores.textoSuave)

        if (hayRA) {
            ConPermiso(Recurso.CAMARA, "Para ver el suelo con la cámara trasera y colocar la esterilla. La imagen no se guarda ni se envía.") {
                val fase = faseRA(estado)
                val d = estado.distanciaM
                if (fase == FaseRA.COLOCADO && d != null) InstruccionRA(fase, "${metrosConComa(d)} m · ${consejo(d)}")
                else InstruccionRA(fase, textoInstruccion(fase, estado))
                EscenaRA(
                    Modifier.fillMaxWidth().height(420.dp),
                    permitirColocar = true,
                    quitar = control.peticionesQuitar,
                    colocar = control.peticionesColocar,
                ) { estado = it }
                Text("Di «colocar» o «quitar», o usa los botones.", fontSize = Tamanos.apoyo, color = Colores.textoSuave)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BotonGrande("Colocar en el centro", Modifier.weight(1f)) { control.ordenar(Orden(Accion.COLOCAR, Origen.TOQUE)) }
                    BotonGrande("Quitar esterilla", Modifier.weight(1f), Colores.aviso) { control.ordenar(Orden(Accion.QUITAR, Origen.TOQUE)) }
                }
            }
        } else {
            Text("Sin RA no pasa nada: puedes entrenar igualmente, solo que sin la esterilla virtual.",
                fontSize = Tamanos.apoyo, color = Colores.texto,
                modifier = Modifier.fillMaxWidth().background(Colores.superficie, RoundedCornerShape(12.dp)).padding(16.dp))
        }

        IndicadorVoz(escucha.indicador(ahora), escucha.ultimaFrase, escucha.ultimaAccion, escucha.error,
            escucha.hablando, "\"colocar\"")
        BotonGrande("Empezar entrenamiento", Modifier.fillMaxWidth(), Colores.correcto, alEmpezar)
    }
}

const val TEXTO_TRANSICION_CAMARA =
    "Cambio a la cámara frontal para verte mientras entrenas. Deja el móvil en el suelo, donde está la esterilla."

/**
 * Transición entre la Zona RA (cámara trasera) y el Entrenamiento (cámara frontal). Mientras se enseña,
 * la escena de RA ya no está en pantalla, así que su cámara ya está suelta cuando se abre la frontal.
 */
@Composable
fun PantallaTransicionCamara(alAceptar: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("📷", fontSize = Tamanos.principal)
        Text(TEXTO_TRANSICION_CAMARA, fontSize = Tamanos.titulo, color = Colores.texto, fontWeight = FontWeight.Bold)
        BotonGrande("Vale", Modifier.fillMaxWidth(), Colores.primario, alAceptar)
    }
}
