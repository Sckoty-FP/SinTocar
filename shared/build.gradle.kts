plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    // Destino Android (AGP 9).
    androidLibrary {
        namespace = "es.dam.sintocar.shared"
        compileSdk = 36
        minSdk = 26
    }
    // Destino escritorio (Windows, Linux y macOS).
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.camerax.camera2)
            implementation(libs.camerax.lifecycle)
            implementation(libs.camerax.view)
            implementation(libs.mlkit.pose)
            implementation(libs.mlkit.face)                 // sonrisa en el Resumen (Tema 7)
            implementation(libs.mediapipe.tasks.vision)     // gestos de la mano (Tema 7)
            implementation(libs.sceneview.ar)               // realidad aumentada: SceneView + ARCore (Tema 8)
        }
        jvmMain.dependencies {
            implementation(libs.opencv)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.vosk)                       // voz sin Internet en el ordenador (Tema 5)
        }
    }
}
