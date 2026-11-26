plugins {
    // AGP 9 ya incluye Kotlin: no hace falta el plugin org.jetbrains.kotlin.android.
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "es.dam.sintocar"
    compileSdk = 36

    defaultConfig {
        applicationId = "es.dam.sintocar"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
}


// El modelo de gestos de MediaPipe (unos 8 MB) se descarga la primera vez que se compila.
val descargarModeloGestos = tasks.register("descargarModeloGestos") {
    val url = "https://storage.googleapis.com/mediapipe-models/gesture_recognizer/gesture_recognizer/float16/1/gesture_recognizer.task"
    val destino = layout.projectDirectory.file("src/main/assets/gesture_recognizer.task").asFile
    doLast {
        if (!destino.exists() || destino.length() == 0L) {
            destino.parentFile.mkdirs()
            // Se descarga a .part y se renombra al acabar: una descarga cortada no deja un modelo roto.
            val parcial = File(destino.parentFile, destino.name + ".part")
            uri(url).toURL().openStream().use { entrada ->
                parcial.outputStream().use { salida -> entrada.copyTo(salida) }
            }
            destino.delete()
            check(parcial.renameTo(destino)) { "No se pudo guardar ${destino.path}" }
        }
    }
}
tasks.named("preBuild") { dependsOn(descargarModeloGestos) }
