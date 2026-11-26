package es.dam.sintocar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import nu.pattern.OpenCV
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.videoio.VideoCapture
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte

/** Un modelo que mira una imagen de la cámara web (formato BGR de OpenCV) y devuelve lo que ha encontrado. */
internal interface AnalizadorEscritorio<T> {
    fun analizar(imagen: Mat): T
}

/**
 * Cámara web del ordenador con OpenCV, para escritorio.
 * Hace lo mismo que CamaraAnalizada en Android: pone la imagen en espejo, la recorta a 3:4 en vertical,
 * se la pasa a un analizador (si hay) y entrega el resultado a la pantalla.
 * @param espejo true como la cámara frontal del móvil; false como la trasera (la RA del Tema 8,
 *   porque un marcador impreso visto en espejo ya no se reconoce).
 */
@Composable
internal fun <T> CamaraWeb(
    modifier: Modifier,
    numeroCamara: Int = 0,
    espejo: Boolean = true,
    crearAnalizador: (() -> AnalizadorEscritorio<T>)? = null,
    alResultado: (T) -> Unit = {},
    imagenDePrueba: (() -> Mat)? = null,   // si no es null, se usa en vez de la cámara (unas 15 por segundo)
) {
    var imagen by remember { mutableStateOf<ImageBitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val avisar by rememberUpdatedState(alResultado)

    val prueba by rememberUpdatedState(imagenDePrueba)
    LaunchedEffect(numeroCamara, imagenDePrueba == null) {
        withContext(Dispatchers.IO) {                     // la cámara y los modelos, fuera del hilo de la ventana
            OpenCV.loadLocally()                          // carga la librería nativa de OpenCV que viene en el .jar
            val camara = if (prueba == null) VideoCapture(numeroCamara) else null
            if (camara != null && !camara.isOpened) { error = "No se encuentra la cámara web $numeroCamara"; return@withContext }
            error = null
            val analizador = try { crearAnalizador?.invoke() } catch (e: Exception) { error = e.message; camara?.release(); return@withContext }
            val fotograma = Mat()
            // Lee una imagen: de la cámara o, sin cámara, la imagen de prueba.
            fun leer(): Boolean {
                val generar = prueba ?: return camara?.read(fotograma) ?: false
                generar().copyTo(fotograma)
                return true
            }
            try {
                while (isActive && leer()) {
                    if (camara == null) delay(66)
                    if (espejo) Core.flip(fotograma, fotograma, 1)   // espejo, como la cámara frontal del móvil
                    val ancho = minOf(fotograma.width(), fotograma.height() * 3 / 4)
                    val vertical = Mat(fotograma, Rect((fotograma.width() - ancho) / 2, 0, ancho, fotograma.height()))
                    val resultado = analizador?.analizar(vertical)
                    val bitmap = aBufferedImage(vertical).toComposeImageBitmap()
                    withContext(Dispatchers.Main) {
                        imagen = bitmap
                        if (resultado != null) avisar(resultado)
                    }
                }
            } finally {
                camara?.release()                         // al salir de la pantalla, se apaga la cámara
            }
        }
    }

    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        // fillMaxSize: sin él la imagen salía a su tamaño en píxeles (pequeña con webcams de 480p)
        // y el esqueleto, que ocupa todo el recuadro, no coincidía con el cuerpo.
        imagen?.let { Image(it, contentDescription = "Cámara", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds) }
        error?.let { Text(it, color = Color.White) }
    }
}

/** Convierte una imagen de OpenCV (BGR) en una imagen de Java que Compose sabe pintar. */
private fun aBufferedImage(mat: Mat): BufferedImage {
    val copia = mat.clone()                               // el recorte no es continuo en memoria: se copia
    val salida = BufferedImage(copia.width(), copia.height(), BufferedImage.TYPE_3BYTE_BGR)
    copia.get(0, 0, (salida.raster.dataBuffer as DataBufferByte).data)
    return salida
}
