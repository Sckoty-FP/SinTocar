package es.dam.sintocar

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

// Qué constante de ML Kit corresponde a cada parte.
private val PARTES = mapOf(
    Parte.HOMBRO_IZQ to PoseLandmark.LEFT_SHOULDER, Parte.HOMBRO_DER to PoseLandmark.RIGHT_SHOULDER,
    Parte.CODO_IZQ to PoseLandmark.LEFT_ELBOW, Parte.CODO_DER to PoseLandmark.RIGHT_ELBOW,
    Parte.MUNECA_IZQ to PoseLandmark.LEFT_WRIST, Parte.MUNECA_DER to PoseLandmark.RIGHT_WRIST,
    Parte.CADERA_IZQ to PoseLandmark.LEFT_HIP, Parte.CADERA_DER to PoseLandmark.RIGHT_HIP,
    Parte.RODILLA_IZQ to PoseLandmark.LEFT_KNEE, Parte.RODILLA_DER to PoseLandmark.RIGHT_KNEE,
    Parte.TOBILLO_IZQ to PoseLandmark.LEFT_ANKLE, Parte.TOBILLO_DER to PoseLandmark.RIGHT_ANKLE,
)

private class AnalizadorPostura {
    private val detector = PoseDetection.getClient(
        PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build()
    )

    fun analizar(imagen: Bitmap): Postura {
        val pose = Tasks.await(detector.process(InputImage.fromBitmap(imagen, 0)))
        val puntos = PARTES.mapNotNull { (parte, tipo) ->
            pose.getPoseLandmark(tipo)?.let {
                parte to Punto(it.position.x / imagen.width, it.position.y / imagen.height, it.inFrameLikelihood)
            }
        }.toMap()
        return Postura(puntos)
    }

    fun cerrar() = detector.close()
}

/** La misma imagen pasa por la postura (ML Kit) y por la mano (MediaPipe). */
private class AnalizadorCuerpo(context: Context) : Analizador<Lectura> {
    private val postura = AnalizadorPostura()
    // Si el modelo de gestos no se puede abrir, la postura sigue funcionando sin gestos.
    private val mano = try { AnalizadorGestos(context) } catch (e: Exception) { null }

    override fun analizar(imagen: Bitmap, instanteMs: Long) =
        Lectura(postura.analizar(imagen), mano?.analizar(imagen, instanteMs))

    override fun cerrar() { postura.cerrar(); mano?.cerrar() }
}

@Composable
actual fun CamaraPostura(modifier: Modifier, alDetectar: (Lectura) -> Unit) =
    CamaraAnalizada(modifier, { AnalizadorCuerpo(it) }, alDetectar)
