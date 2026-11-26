package es.dam.sintocar

import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.Line
import javax.sound.sampled.TargetDataLine

/**
 * Diagnóstico de audio del ordenador: qué micrófonos ve Java y qué formatos abre cada uno.
 * Se lanza con `SinTocar.exe --diagnostico` y escribe ~/sintocar-diagnostico.txt
 * (el .exe no tiene consola, así que el resultado va a un archivo).
 */
fun diagnosticoAudio(): File {
    val informe = StringBuilder()
    informe.appendLine("Java ${System.getProperty("java.version")} en ${System.getProperty("java.home")}")
    val formatos = listOf(16000f, 48000f, 44100f).flatMap { hz -> listOf(1, 2).map { AudioFormat(hz, 16, it, true, false) } }
    for (mi in AudioSystem.getMixerInfo()) {
        val mixer = AudioSystem.getMixer(mi)
        val captura = mixer.isLineSupported(Line.Info(TargetDataLine::class.java))
        informe.appendLine("\n[${mi.name}] (${mixer.javaClass.simpleName}) captura=$captura")
        if (!captura) continue
        mixer.targetLineInfo.forEach { info ->
            (info as? DataLine.Info)?.formats?.take(12)?.forEach { informe.appendLine("   admite: $it") }
        }
        for (f in formatos) {
            val info = DataLine.Info(TargetDataLine::class.java, f)
            val resultado = runCatching {
                val l = mixer.getLine(info) as TargetDataLine
                l.open(f); l.close(); "OK"
            }.getOrElse { "${it.javaClass.simpleName}: ${it.message}" }
            informe.appendLine("   ${f.sampleRate.toInt()} Hz ${f.channels} canal(es): soportado=${mixer.isLineSupported(info)} → $resultado")
        }
    }
    informe.appendLine("\n[Vosk]")
    informe.appendLine(runCatching {
        org.vosk.Recognizer(org.vosk.Model(File(System.getProperty("user.home"), ".vosk/vosk-model-small-es-0.42").path), 16000f)
            .use { "modelo y reconocedor OK" }
    }.getOrElse { it.stackTraceToString().lines().take(12).joinToString("\n") })
    return File(System.getProperty("user.home"), "sintocar-diagnostico.txt").apply { writeText(informe.toString()) }
}
