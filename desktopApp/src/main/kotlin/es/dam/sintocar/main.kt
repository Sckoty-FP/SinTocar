package es.dam.sintocar

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

// Arranque de escritorio: una ventana con la misma interfaz común que el móvil (App() de commonMain).
// Con «--diagnostico» no abre ventana: escribe ~/sintocar-diagnostico.txt con los micrófonos que ve.
fun main(args: Array<String>) {
    if ("--diagnostico" in args) { diagnosticoAudio(); return }
    ventana()
}

private fun ventana() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Sin Tocar",
        // Horizontal y grande: el ordenador se mira desde 2 o 3 metros.
        state = rememberWindowState(width = 1280.dp, height = 820.dp),
    ) {
        App()
    }
}
