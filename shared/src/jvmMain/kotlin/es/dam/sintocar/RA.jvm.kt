package es.dam.sintocar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import org.opencv.calib3d.Calib3d
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.MatOfPoint3f
import org.opencv.core.Point
import org.opencv.core.Point3
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.ArucoDetector
import org.opencv.objdetect.Objdetect
import kotlin.math.sqrt

// Escritorio: realidad aumentada con un MARCADOR impreso (ArUco, de OpenCV) en lugar de ARCore.
// El marcador hace de «suelo encontrado»: dónde está y cómo está girado se calcula a partir de sus 4 esquinas.

/** Lado real del marcador impreso, en metros (imprime marcador_7.png a 15 cm de lado). */
private const val LADO_MARCADOR = 0.15

actual val comoBuscar = "Enseña el marcador impreso a la cámara"
actual val comoColocar = "Haz clic en la imagen: ahí queda la esterilla"

@Composable
actual fun compatibilidadRA(): String =
    "✅ Escritorio: RA con marcador impreso (OpenCV ArUco). Imprime marcador_7.png a 15 cm de lado."

@Composable
actual fun EscenaRA(modifier: Modifier, permitirColocar: Boolean, quitar: Int, colocar: Int, alCambiar: (EstadoRA) -> Unit) {
    var colocado by remember { mutableStateOf(false) }
    val avisar by rememberUpdatedState(alCambiar)
    LaunchedEffect(quitar) { colocado = false }
    LaunchedEffect(colocar) { if (colocar > 0 && permitirColocar) colocado = true }   // «colocar» por voz o botón

    // Sin cámara: una imagen de prueba con el marcador en el suelo, a la distancia del deslizador.
    var sinCamara by remember { mutableStateOf(false) }
    var distancia by remember { mutableFloatStateOf(2.5f) }
    val d by rememberUpdatedState(distancia)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Switch(checked = sinCamara, onCheckedChange = { sinCamara = it })
        Text("Sin cámara: imagen de prueba con el marcador")
    }
    if (sinCamara) {
        Text("Marcador a ${"%.1f".format(distancia).replace('.', ',')} m de la cámara")
        Slider(value = distancia, onValueChange = { distancia = it }, valueRange = 1f..4f)
    }

    // Hacer clic en la imagen = tocar para colocar.
    val conClic = if (permitirColocar) modifier.clickable { colocado = true } else modifier
    CamaraWeb(
        conClic,
        espejo = false,
        crearAnalizador = { AnalizadorMarcador { colocado } },
        alResultado = { avisar(it) },
        imagenDePrueba = if (sinCamara) ({ escenaDePrueba(d.toDouble()) }) else null,
    )
}

/** El marcador 7 del diccionario 4×4 (el mismo que marcador_7.png), con su borde blanco. */
private val marcadorConBorde: Mat by lazy {
    val marcador = Mat()
    Objdetect.generateImageMarker(Objdetect.getPredefinedDictionary(Objdetect.DICT_4X4_50), 7, 200, marcador)
    Imgproc.cvtColor(marcador, marcador, Imgproc.COLOR_GRAY2BGR)
    val conBorde = Mat()
    Core.copyMakeBorder(marcador, conBorde, 40, 40, 40, 40, Core.BORDER_CONSTANT, Scalar(255.0, 255.0, 255.0))
    conBorde
}

/**
 * Imagen de prueba de 480 × 640 (vertical 3:4): fondo gris y el marcador de 15 cm con su centro a
 * [distanciaM] metros delante de la cámara, inclinado 50° hacia atrás (como apoyado en el suelo o en una mesa).
 * Se dibuja con la misma cámara «ideal» que usa AnalizadorMarcador (distancia focal = ancho de la imagen),
 * así que la distancia que mide el analizador coincide con la del deslizador (± 7 %).
 */
internal fun escenaDePrueba(distanciaM: Double): Mat {
    val ancho = 480; val alto = 640
    val imagen = Mat(alto, ancho, CvType.CV_8UC3, Scalar(120.0, 130.0, 140.0))
    val f = ancho.toDouble()
    val camara = Mat(3, 3, CvType.CV_64F).apply { put(0, 0, f, 0.0, ancho / 2.0, 0.0, f, alto / 2.0, 0.0, 0.0, 1.0) }

    // Las 4 esquinas del marcador CON borde (el borde mide 40/200 del lado) en metros, delante de la cámara.
    val lado = LADO_MARCADOR * (1 + 2 * 40.0 / 200)
    val a = Math.toRadians(50.0)
    val (vy, vz) = Math.cos(a) to -Math.sin(a)        // dirección «hacia abajo» del marcador inclinado
    fun esquina(u: Double, v: Double) = Point3(u * lado, 0.1 + v * lado * vy, distanciaM + v * lado * vz)
    val enEscena = MatOfPoint3f(esquina(-0.5, -0.5), esquina(0.5, -0.5), esquina(0.5, 0.5), esquina(-0.5, 0.5))
    val esquinas = MatOfPoint2f()
    Calib3d.projectPoints(enEscena, Mat.zeros(3, 1, CvType.CV_64F), Mat.zeros(3, 1, CvType.CV_64F), camara, MatOfDouble(), esquinas)

    // Se reduce el marcador al tamaño aproximado que tendrá (si no, al deformarlo se ve con «dientes»
    // y el detector lo confunde) y se pega en esas 4 esquinas, en perspectiva.
    val objetivo = maxOf((f * lado / distanciaM * 1.5).toInt(), 42)
    val pequeno = Mat()
    Imgproc.resize(marcadorConBorde, pequeno, Size(objetivo.toDouble(), objetivo.toDouble()), 0.0, 0.0, Imgproc.INTER_AREA)
    val s = objetivo.toDouble()
    val h = Imgproc.getPerspectiveTransform(MatOfPoint2f(Point(0.0, 0.0), Point(s, 0.0), Point(s, s), Point(0.0, s)), esquinas)
    val pegado = Mat()
    Imgproc.warpPerspective(pequeno, pegado, h, imagen.size())
    val mascara = Mat()
    Imgproc.warpPerspective(Mat(pequeno.size(), CvType.CV_8UC1, Scalar(255.0)), mascara, h, imagen.size())
    pegado.copyTo(imagen, mascara)
    return imagen
}

private class AnalizadorMarcador(private val colocado: () -> Boolean) : AnalizadorEscritorio<EstadoRA> {
    private val detector = ArucoDetector(Objdetect.getPredefinedDictionary(Objdetect.DICT_4X4_50))

    override fun analizar(imagen: Mat): EstadoRA {
        val esquinas = ArrayList<Mat>()
        val ids = Mat()
        detector.detectMarkers(imagen, esquinas, ids)
        if (esquinas.isEmpty()) return EstadoRA()                  // sin marcador = aún no hay «suelo»
        val c = MatOfPoint2f(esquinas[0].reshape(2, 4))           // 4 esquinas en la imagen

        // Contorno verde del marcador: el equivalente a la rejilla del suelo de ARCore.
        Imgproc.polylines(imagen, listOf(MatOfPoint(*c.toArray())), true, Scalar(0.0, 200.0, 0.0), 3)
        if (!colocado()) return EstadoRA(sueloEncontrado = true)

        // Esterilla de 60 × 180 cm (4 × 12 lados de marcador), delante del marcador, en perspectiva.
        val marcador = MatOfPoint2f(Point(0.0, 0.0), Point(1.0, 0.0), Point(1.0, 1.0), Point(0.0, 1.0))
        val h = Imgproc.getPerspectiveTransform(marcador, c)
        val esterilla = MatOfPoint2f(Point(-1.5, -11.0), Point(2.5, -11.0), Point(2.5, 1.0), Point(-1.5, 1.0))
        val enImagen = MatOfPoint2f()
        Core.perspectiveTransform(esterilla, enImagen, h)
        val capa = imagen.clone()
        Imgproc.fillPoly(capa, listOf(MatOfPoint(*enImagen.toArray())), Scalar(198.0, 214.0, 61.0))
        Core.addWeighted(capa, 0.5, imagen, 0.5, 0.0, imagen)        // semitransparente sobre la imagen

        // Distancia cámara-marcador con solvePnP. La cámara web no está calibrada: se toma como
        // distancia focal el ancho de la imagen, una aproximación habitual (error de ±20 %).
        val f = imagen.width().toDouble()
        val camara = Mat(3, 3, CvType.CV_64F).apply {
            put(0, 0, f, 0.0, imagen.width() / 2.0, 0.0, f, imagen.height() / 2.0, 0.0, 0.0, 1.0)
        }
        val real = MatOfPoint3f(
            Point3(0.0, 0.0, 0.0), Point3(LADO_MARCADOR, 0.0, 0.0),
            Point3(LADO_MARCADOR, LADO_MARCADOR, 0.0), Point3(0.0, LADO_MARCADOR, 0.0),
        )
        val giro = Mat(); val posicion = Mat()
        Calib3d.solvePnP(real, c, camara, MatOfDouble(), giro, posicion)
        val t = DoubleArray(3) { posicion.get(it, 0)[0] }
        return EstadoRA(sueloEncontrado = true, colocado = true, distanciaM = sqrt(t[0] * t[0] + t[1] * t[1] + t[2] * t[2]).toFloat())
    }
}
