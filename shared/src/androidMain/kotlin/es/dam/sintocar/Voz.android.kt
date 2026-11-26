package es.dam.sintocar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.Locale

/** Reconocedor de Android. La primera vez que se pulsa pide el permiso de micrófono. */
private class ReconocedorAndroid(private val context: Context) : Reconocedor {
    override var estado by mutableStateOf(EstadoVoz.PARADO)
    override var error by mutableStateOf<String?>(null)

    /** El reconocedor del sistema. Es null si el móvil no tiene ninguno (p. ej. emulador sin Google Play). */
    val sistema: SpeechRecognizer? =
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null

    /** Lo pone rememberReconocedor: abre el diálogo «¿Permitir grabar audio?». */
    var pedirPermiso: () -> Unit = {}
    /** Si hubo que pedir permiso, aquí se guarda a quién avisar cuando lo conceda. */
    var pendiente: ((String) -> Unit)? = null

    override fun escuchar(alTerminar: (String) -> Unit) {
        error = null
        if (sistema == null) {
            error = "Este dispositivo no tiene reconocimiento de voz. En el emulador usa una imagen con Google Play."
            return
        }
        val tienePermiso = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (!tienePermiso) {
            pendiente = alTerminar   // se escuchará en cuanto el usuario diga «Permitir»
            pedirPermiso()
            return
        }

        // 1. Qué hacer en cada momento de la escucha.
        sistema.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { estado = EstadoVoz.ESCUCHANDO }
            override fun onEndOfSpeech() { estado = EstadoVoz.PROCESANDO }
            override fun onResults(results: Bundle?) {
                estado = EstadoVoz.PARADO
                val frases = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                alTerminar(frases?.firstOrNull() ?: "")   // la primera es la más probable
            }
            override fun onError(codigo: Int) {
                estado = EstadoVoz.PARADO
                when (codigo) {
                    // No ha oído nada o nada reconocible: no es un fallo, se entrega "".
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> alTerminar("")
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> error = "Sin conexión a Internet"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> error = "Falta el permiso de micrófono"
                    else -> error = "El reconocedor ha fallado (código $codigo)"
                }
            }
            // Momentos que este ejemplo no usa, pero que la interfaz obliga a escribir.
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        // 2. Qué queremos que escuche: habla libre, en español de España.
        val peticion = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
        }

        // 3. A escuchar.
        sistema.startListening(peticion)
    }

    override fun parar() {
        sistema?.cancel()          // cancel() no llama a onResults ni a onError: no se entrega nada
        estado = EstadoVoz.PARADO
    }
}

@Composable
actual fun rememberReconocedor(): Reconocedor {
    val context = LocalContext.current
    val reconocedor = remember { ReconocedorAndroid(context) }

    // Diálogo del permiso. Al contestar: si dice que sí, se escucha lo que estaba pendiente.
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        val avisar = reconocedor.pendiente
        reconocedor.pendiente = null
        if (concedido && avisar != null) reconocedor.escuchar(avisar)
        else reconocedor.error = "Sin permiso de micrófono no puedo escucharte. Puedes darlo en Ajustes › Aplicaciones."
    }
    reconocedor.pedirPermiso = { lanzador.launch(Manifest.permission.RECORD_AUDIO) }

    // Al salir de la pantalla se suelta el micrófono.
    DisposableEffect(Unit) { onDispose { reconocedor.sistema?.destroy() } }
    return reconocedor
}

@Composable
actual fun rememberHablante(): Hablante {
    val context = LocalContext.current
    val tts = remember {
        var motor: TextToSpeech? = null
        // Cuando el motor de voz está listo, se le pone el idioma español.
        TextToSpeech(context) { ok ->
            if (ok == TextToSpeech.SUCCESS) motor?.language = Locale.forLanguageTag("es-ES")
        }.also { motor = it }
    }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }
    return remember {
        object : Hablante {
            private val principal = Handler(Looper.getMainLooper())
            private val pendientes = mutableMapOf<String, () -> Unit>()
            private var siguienteId = 0

            init {
                // El motor avisa desde otro hilo: se vuelve al hilo de la interfaz con el Handler.
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) = avisar(id)
                    @Deprecated("Obligatorio en API antiguas") override fun onError(id: String?) = avisar(id)
                    override fun onStop(id: String?, interrupted: Boolean) = avisar(id)
                })
            }

            private fun avisar(id: String?) {
                val f = synchronized(pendientes) { pendientes.remove(id) } ?: return
                principal.post(f)
            }

            // QUEUE_FLUSH: si estaba diciendo otra cosa, la corta y dice esta.
            override fun decir(texto: String, alTerminar: () -> Unit) {
                val id = "frase${siguienteId++}"
                synchronized(pendientes) { pendientes[id] = alTerminar }
                if (tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS) avisar(id)
            }
        }
    }
}
