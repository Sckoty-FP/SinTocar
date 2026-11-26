import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)          // Compose para el sistema en el que compilas
    implementation(libs.kotlinx.coroutines.swing)      // hilo principal de la ventana
}

compose.desktop {
    application {
        mainClass = "es.dam.sintocar.MainKt"
        // Para generar el .exe hace falta un JDK con jpackage DE LA MISMA VERSIÓN con la que compila Gradle (hoy 25).
        // Si es más viejo, el .exe se cierra en silencio (UnsupportedClassVersionError, sin consola que lo muestre).
        // Uso: ./gradlew :desktopApp:createDistributable -PjdkEmpaquetado=C:/Users/elias/.jdks/openjdk-25
        providers.gradleProperty("jdkEmpaquetado").orNull?.let { javaHome = it }
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "SinTocar"
            packageVersion = "1.0.0"
            // El .exe lleva su propio Java recortado. Vosk (JNA) y OpenCV necesitan módulos que el
            // recorte automático puede dejar fuera: se incluyen todos para no fallar en el PC de otro.
            includeAllModules = true
        }
    }
}

// Modelos de visión para escritorio (OpenCV Zoo y OpenCV). Se descargan una vez en ~/.di-modelos.
val descargarModelos = tasks.register("descargarModelos") {
    val carpeta = File(System.getProperty("user.home"), ".di-modelos")
    val archivos = mapOf(
        "pose_estimation_mediapipe_2023mar.onnx" to "https://media.githubusercontent.com/media/opencv/opencv_zoo/main/models/pose_estimation_mediapipe/pose_estimation_mediapipe_2023mar.onnx",
    )
    doLast {
        carpeta.mkdirs()
        for ((nombre, url) in archivos) {
            val destino = File(carpeta, nombre)
            if (!destino.exists() || destino.length() == 0L) {
                // Se descarga a un archivo .part y solo al terminar se renombra:
                // si la descarga se corta, no queda un modelo a medias que parezca bueno.
                val parcial = File(carpeta, "$nombre.part")
                uri(url).toURL().openStream().use { entrada -> parcial.outputStream().use { entrada.copyTo(it) } }
                destino.delete()
                check(parcial.renameTo(destino)) { "No se pudo guardar ${destino.path}" }
            }
        }
    }
}

// Modelo de reconocimiento de voz en español de Vosk (unos 40 MB). Se descarga una vez en ~/.vosk.
val descargarModeloVoz = tasks.register("descargarModeloVoz") {
    val carpeta = File(System.getProperty("user.home"), ".vosk")
    val destino = File(carpeta, "vosk-model-small-es-0.42")
    // OJO (bug del Tema 5): con outputs.dir(destino) Gradle crea la carpeta VACÍA antes de ejecutar la tarea,
    // así que «if (!destino.exists())» nunca era cierto y el modelo no se descargaba. Se mira un archivo del modelo.
    doLast {
        if (!File(destino, "conf/model.conf").exists()) {
            carpeta.mkdirs()
            val zip = File(carpeta, "modelo-es.zip")
            uri("https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip").toURL().openStream().use { entrada ->
                zip.outputStream().use { salida -> entrada.copyTo(salida) }
            }
            project.copy { from(project.zipTree(zip)); into(carpeta) }
            zip.delete()
        }
    }
}

tasks.named("compileKotlin") { dependsOn(descargarModelos, descargarModeloVoz) }
