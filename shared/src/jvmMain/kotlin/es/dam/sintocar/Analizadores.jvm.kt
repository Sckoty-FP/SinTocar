package es.dam.sintocar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.dnn.Dnn
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin

// ======================= Gestos en escritorio =======================
// Dos modelos de MediaPipe (versión ONNX de OpenCV Zoo):
//   1) detector de palma: dónde está la mano y hacia dónde apunta;
//   2) puntos de la mano: los 21 puntos, con la mano recortada y puesta de pie.
// El gesto se decide después con reglas sencillas sobre los 21 puntos (ver gestoPorReglas).

/** Centros de las 2016 «anclas» del detector de palma: 24×24 casillas × 2 y 12×12 × 6. */
private val ANCLAS: List<Pair<Float, Float>> = buildList {
    for ((casillas, porCasilla) in listOf(24 to 2, 12 to 6))
        for (y in 0 until casillas) for (x in 0 until casillas) repeat(porCasilla) {
            add((x + 0.5f) / casillas to (y + 0.5f) / casillas)
        }
}

internal class AnalizadorGestosEscritorio : AnalizadorEscritorio<Gesto> {
    private val palma = Dnn.readNet(modelo("palm_detection_mediapipe_2023feb.onnx"))
    private val mano = Dnn.readNet(modelo("handpose_estimation_mediapipe_2023feb.onnx"))

    override fun analizar(imagen: Mat): Gesto {
        // 1) Detector de palma sobre la imagen hecha cuadrada a 192 × 192.
        val c = Cuadrada(imagen, 192)
        palma.setInput(c.entrada)
        val salidas = ArrayList<Mat>()
        palma.forward(salidas, palma.unconnectedOutLayersNames)
        val cajas = numeros(salidas[0])                       // 2016 × 18
        val notas = numeros(salidas[1])                       // 2016
        val i = notas.indices.maxBy { notas[it] }
        if (sigmoide(notas[i]) < 0.5f) return Gesto("None", 0f)
        val (ax, ay) = ANCLAS[i]
        fun aPixel(u: Float, v: Float) = Point(
            ((u / 192 + ax) * c.ladoOriginal - c.izquierda).toDouble(),
            ((v / 192 + ay) * c.ladoOriginal - c.arriba).toDouble(),
        )
        val centro = aPixel(cajas[i * 18], cajas[i * 18 + 1])
        val tamano = maxOf(cajas[i * 18 + 2], cajas[i * 18 + 3]) / 192 * c.ladoOriginal
        val muneca = aPixel(cajas[i * 18 + 4], cajas[i * 18 + 5])        // punto 0 de la palma
        val dedoMedio = aPixel(cajas[i * 18 + 8], cajas[i * 18 + 9])     // punto 2 de la palma

        // 2) Giro que pone la mano de pie, y centro de la mano (0,4 palmas hacia los dedos).
        var ang = PI / 2 - atan2(-(dedoMedio.y - muneca.y), dedoMedio.x - muneca.x)
        ang -= 2 * PI * floor((ang + PI) / (2 * PI))
        val dy = -0.4 * tamano
        val cx = centro.x - dy * sin(ang)
        val cy = centro.y + dy * cos(ang)
        val lado = tamano * 3.0                                          // recorte: 3 veces la palma
        val giro = Imgproc.getRotationMatrix2D(Point(cx, cy), Math.toDegrees(ang), 224 / lado)
        giro.put(0, 2, giro.get(0, 2)[0] + 112 - cx)
        giro.put(1, 2, giro.get(1, 2)[0] + 112 - cy)
        val recorte = Mat()
        Imgproc.warpAffine(imagen, recorte, giro, Size(224.0, 224.0), Imgproc.INTER_LINEAR, 0, Scalar(0.0, 0.0, 0.0))

        // 3) Los 21 puntos de la mano.
        mano.setInput(aEntrada(recorte, 224))
        val s = ArrayList<Mat>()
        mano.forward(s, mano.unconnectedOutLayersNames)
        val seguridad = numeros(s[1])[0]                                 // ¿hay una mano en el recorte? (0 a 1)
        if (seguridad < 0.8f) return Gesto("None", 0f)
        val p = numeros(s[0])                                            // 21 × (x, y, z) en el recorte
        val inversa = Mat()
        Imgproc.invertAffineTransform(giro, inversa)
        val m = DoubleArray(6) { inversa.get(it / 3, it % 3)[0] }
        val puntos = List(21) { k ->
            val u = p[k * 3].toDouble(); val v = p[k * 3 + 1].toDouble()
            Point(m[0] * u + m[1] * v + m[2], m[3] * u + m[4] * v + m[5])  // de vuelta a la imagen
        }
        val nombre = gestoPorReglas(puntos)
        return Gesto(nombre, if (nombre == "None") 0f else seguridad)
    }
}

/**
 * Decide el gesto a partir de los 21 puntos (0 = muñeca, 4 = punta del pulgar, 8 = punta del índice…).
 * Un dedo está estirado si su punta está claramente más lejos de la muñeca que su segunda articulación.
 * Las distancias no cambian al girar la mano, así que funciona en cualquier orientación.
 */
private fun gestoPorReglas(p: List<Point>): String {
    fun d(a: Int, b: Int) = hypot(p[a].x - p[b].x, p[a].y - p[b].y)
    val estirados = listOf(8 to 6, 12 to 10, 16 to 14, 20 to 18).map { (punta, nudillo) -> d(0, punta) > d(0, nudillo) * 1.15 }
    val pulgar = d(4, 9) > d(3, 9) * 1.1 && d(4, 0) > d(2, 0)
    val margen = d(0, 9) * 0.2
    return when {
        estirados.all { it } && pulgar -> "Open_Palm"
        estirados.none { it } && pulgar && p[4].y < p[2].y - margen -> "Thumb_Up"      // y crece hacia abajo
        estirados.none { it } && pulgar && p[4].y > p[2].y + margen -> "Thumb_Down"
        estirados.none { it } -> "Closed_Fist"
        estirados == listOf(true, true, false, false) -> "Victory"
        estirados == listOf(true, false, false, false) -> "Pointing_Up"
        estirados == listOf(true, false, false, true) && pulgar -> "ILoveYou"
        else -> "None"
    }
}

// ======================= Cara y sonrisa en escritorio =======================
// Clasificadores Haar de OpenCV: uno encuentra la cara y otro la sonrisa en la mitad de abajo.

private class AnalizadorCaraEscritorio : AnalizadorEscritorio<Cara> {
    private val caras = CascadeClassifier(modelo("haarcascade_frontalface_default.xml"))
    private val sonrisas = CascadeClassifier(modelo("haarcascade_smile.xml"))

    override fun analizar(imagen: Mat): Cara {
        val gris = Mat()
        Imgproc.cvtColor(imagen, gris, Imgproc.COLOR_BGR2GRAY)
        Imgproc.equalizeHist(gris, gris)
        val encontradas = MatOfRect()
        caras.detectMultiScale(gris, encontradas, 1.1, 5, 0, Size(80.0, 80.0), Size())
        val cara = encontradas.toArray().maxByOrNull { it.area() } ?: return Cara(false, 0f, 0f, 0f)
        val boca = Mat(gris, Rect(cara.x, cara.y + cara.height / 2, cara.width, cara.height / 2))
        // «Probabilidad» aproximada: cuanto más exigente es la búsqueda y aun así la encuentra, más clara es.
        var sonrisa = 0f
        for ((vecinos, valor) in listOf(40 to 1f, 25 to 0.85f, 15 to 0.6f, 8 to 0.3f)) {
            val r = MatOfRect()
            sonrisas.detectMultiScale(boca, r, 1.3, vecinos, 0, Size(cara.width / 4.0, cara.height / 10.0), Size())
            if (!r.empty()) { sonrisa = valor; break }
        }
        // Este método no mide los ojos: se dan por abiertos.
        return Cara(true, sonrisa, 1f, 1f)
    }
}

@Composable
actual fun CamaraCara(modifier: Modifier, alDetectar: (Cara) -> Unit) =
    CamaraWeb(modifier, numeroCamara = DispositivosEscritorio.camara, crearAnalizador = { AnalizadorCaraEscritorio() }, alResultado = alDetectar)
