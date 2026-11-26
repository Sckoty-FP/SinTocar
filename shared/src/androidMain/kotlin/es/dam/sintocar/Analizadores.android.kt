package es.dam.sintocar

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.tasks.Tasks
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

// ---------- Gestos con MediaPipe ----------
internal class AnalizadorGestos(context: Context) : Analizador<Gesto> {
    // El modelo gesture_recognizer.task está en androidApp/src/main/assets (lo descarga Gradle).
    private val reconocedor = GestureRecognizer.createFromOptions(
        context,
        GestureRecognizer.GestureRecognizerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath("gesture_recognizer.task").build())
            .setRunningMode(RunningMode.VIDEO)   // vídeo: sigue la mano entre imágenes
            .setNumHands(1)
            .build()
    )
    private var ultimo = 0L

    override fun analizar(imagen: Bitmap, instanteMs: Long): Gesto {
        val t = maxOf(instanteMs, ultimo + 1).also { ultimo = it }   // el instante tiene que crecer siempre
        val resultado = reconocedor.recognizeForVideo(BitmapImageBuilder(imagen).build(), t)
        val mejor = resultado.gestures().firstOrNull()?.firstOrNull()
        return Gesto(mejor?.categoryName() ?: "None", mejor?.score() ?: 0f)
    }

    override fun cerrar() = reconocedor.close()
}

// ---------- Cara con ML Kit ----------
private class AnalizadorCara : Analizador<Cara> {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)   // sonrisa y ojos
            .build()
    )

    override fun analizar(imagen: Bitmap, instanteMs: Long): Cara {
        val cara = Tasks.await(detector.process(InputImage.fromBitmap(imagen, 0))).firstOrNull()
            ?: return Cara(false, 0f, 0f, 0f)
        return Cara(
            true,
            cara.smilingProbability ?: 0f,
            cara.leftEyeOpenProbability ?: 0f,
            cara.rightEyeOpenProbability ?: 0f,
        )
    }

    override fun cerrar() = detector.close()
}

@Composable
actual fun CamaraCara(modifier: Modifier, alDetectar: (Cara) -> Unit) =
    CamaraAnalizada(modifier, { AnalizadorCara() }, alDetectar)
