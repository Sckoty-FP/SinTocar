package es.dam.sintocar

import androidx.compose.runtime.Composable

/** Los tres momentos del micrófono. La pantalla los enseña para que el usuario sepa si le están oyendo. */
enum class EstadoVoz { PARADO, ESCUCHANDO, PROCESANDO }

/**
 * Convierte voz en texto. Cada plataforma lo hace a su manera:
 *   Android    → el reconocedor de voz del móvil (SpeechRecognizer).
 *   Escritorio → Vosk, un reconocedor que funciona sin Internet.
 * La pantalla solo usa esto; no sabe en qué plataforma está. (Base: Tema 5.)
 */
interface Reconocedor {
    /** En qué momento está. Es estado de Compose: si cambia, la pantalla se repinta sola. */
    val estado: EstadoVoz
    /** Si algo ha ido mal (sin permiso, sin micrófono…), aquí está el motivo. Si no, null. */
    val error: String?
    /** Escucha UNA frase. Al terminar llama a [alTerminar] con lo que ha entendido ("" si no ha oído nada). */
    fun escuchar(alTerminar: (frase: String) -> Unit)
    /** Corta la escucha en curso sin entregar nada (p. ej. porque la app va a hablar). */
    fun parar()
}

/** Convierte texto en voz: la app contesta hablando. */
interface Hablante {
    /** Dice [texto] y llama a [alTerminar] (en el hilo de la interfaz) cuando acaba. */
    fun decir(texto: String, alTerminar: () -> Unit = {})
}

/** Cada plataforma da su propio Reconocedor (ficheros Voz.android.kt y Voz.jvm.kt). */
@Composable
expect fun rememberReconocedor(): Reconocedor

@Composable
expect fun rememberHablante(): Hablante
