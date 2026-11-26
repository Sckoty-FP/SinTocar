package es.dam.sintocar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberOnGestureListener
import kotlin.math.sqrt

actual val comoBuscar = "Mueve el móvil despacio apuntando al suelo"
actual val comoColocar = "Toca la rejilla: ahí queda la esterilla"

@Composable
actual fun compatibilidadRA(): String {
    val context = LocalContext.current
    return when (ArCoreApk.getInstance().checkAvailability(context)) {
        ArCoreApk.Availability.SUPPORTED_INSTALLED -> "✅ Compatible y listo"
        ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED,
        ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> "⚠ Compatible: falta instalar o actualizar «Servicios de Google Play para RA»"
        ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> "❌ Este móvil no es compatible con RA"
        else -> "… Comprobando (vuelve a entrar en unos segundos)"
    }
}

@Composable
actual fun EscenaRA(modifier: Modifier, permitirColocar: Boolean, quitar: Int, colocar: Int, alCambiar: (EstadoRA) -> Unit) {
    val motor = rememberEngine()
    val cargador = rememberModelLoader(motor)
    val modelo = rememberModelInstance(cargador, "models/esterilla.glb")   // null mientras carga
    val avisar by rememberUpdatedState(alCambiar)

    var ancla by remember { mutableStateOf<Anchor?>(null) }
    var suelo by remember { mutableStateOf(false) }
    var distancia by remember { mutableStateOf<Float?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }
    val ultimoFrame = remember { arrayOfNulls<Frame>(1) }
    var tamano by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(quitar) { ancla?.detach(); ancla = null; distancia = null }
    // «colocar» por voz o botón: rayo desde el centro de la pantalla. Si aún no hay suelo, no hace nada.
    LaunchedEffect(colocar) {
        if (colocar > 0 && permitirColocar && tamano != IntSize.Zero) {
            val impacto = ultimoFrame[0]?.hitTest(tamano.width / 2f, tamano.height / 2f)?.firstOrNull { it.trackable is Plane }
            impacto?.createAnchor()?.let { nueva -> ancla?.detach(); ancla = nueva }
        }
    }
    LaunchedEffect(suelo, ancla, distancia, aviso) { avisar(EstadoRA(suelo, ancla != null, distancia, aviso)) }

    ARSceneView(
        modifier = modifier.onSizeChanged { tamano = it },
        engine = motor,
        modelLoader = cargador,
        planeRenderer = ancla == null,                       // rejilla solo mientras no hay nada colocado
        sessionConfiguration = { _, config ->
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL   // solo suelos y mesas
        },
        onTrackingFailureChanged = { razon ->
            aviso = when (razon) {
                null, TrackingFailureReason.NONE -> null
                TrackingFailureReason.INSUFFICIENT_LIGHT -> "Hay poca luz"
                TrackingFailureReason.EXCESSIVE_MOTION -> "Mueve el móvil más despacio"
                TrackingFailureReason.INSUFFICIENT_FEATURES -> "Apunta a una zona con más textura"
                else -> "He perdido la referencia"
            }
        },
        onSessionUpdated = { _, frame ->
            ultimoFrame[0] = frame
            if (!suelo) suelo = frame.getUpdatedTrackables(Plane::class.java).any { it.trackingState == TrackingState.TRACKING }
            val a = ancla
            if (a != null) {
                val c = frame.camera.pose; val p = a.pose
                val dx = c.tx() - p.tx(); val dy = c.ty() - p.ty(); val dz = c.tz() - p.tz()
                val d = sqrt(dx * dx + dy * dy + dz * dz)
                if (distancia == null || kotlin.math.abs(d - distancia!!) > 0.05f) distancia = d
            }
        },
        onGestureListener = rememberOnGestureListener(
            onSingleTapConfirmed = { evento, _ ->
                if (permitirColocar) {
                    // Rayo desde el dedo: el primer plano que toca es donde se coloca.
                    val impacto = ultimoFrame[0]?.hitTest(evento)?.firstOrNull { it.trackable is Plane }
                    impacto?.createAnchor()?.let { nueva -> ancla?.detach(); ancla = nueva }
                }
            },
        ),
    ) {
        ancla?.let { a ->
            AnchorNode(anchor = a) {
                modelo?.let { ModelNode(modelInstance = it, scaleToUnits = 1.8f) }   // 1,8 m de largo
            }
        }
    }
}
