package es.dam.sintocar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Pantalla Inicio: los permisos con la Tarjeta de permiso (cada uno explicando para qué) y la elección de rutina.
 * Si se rechaza un permiso la app sigue funcionando con los botones (Tema 1, privacidad y permisos).
 */
@Composable
fun PantallaInicio(modifier: Modifier = Modifier, alZonaRA: (Rutina) -> Unit = {}, alElegir: (Rutina) -> Unit) {
    val camara = rememberPermiso(Recurso.CAMARA)
    val micro = rememberPermiso(Recurso.MICROFONO)
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Sin Tocar", fontSize = Tamanos.principal, fontWeight = FontWeight.Bold, color = Colores.texto)
        Text("Entrena en casa sin tocar la pantalla: te cuento las repeticiones y te obedezco con la voz y los brazos.",
            fontSize = Tamanos.apoyo, color = Colores.textoSuave)

        TarjetaPermiso(camara.estado, Recurso.CAMARA.nombre,
            "Para verte y contar sentadillas y zancadas. La imagen se analiza en el dispositivo y no se guarda ni se envía.",
            camara.pedir, camara.abrirAjustes)
        TarjetaPermiso(micro.estado, Recurso.MICROFONO.nombre,
            "Para obedecer tus órdenes habladas. El audio se procesa sin guardarse. Sin él, usa los botones o los brazos.",
            micro.pedir, micro.abrirAjustes)

        SelectorDispositivos()

        Text("Elige tu rutina", fontSize = Tamanos.titulo, fontWeight = FontWeight.Bold, color = Colores.texto)
        Rutina.entries.forEach { r -> TarjetaRutina(r) { alElegir(r) } }

        // Zona RA: antes de entrenar se coloca la esterilla. Es opcional: se puede ir directo a una rutina.
        var rutinaRA by remember { mutableStateOf(Rutina.SUAVE) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Rutina después:", fontSize = Tamanos.apoyo, color = Colores.textoSuave)
            Rutina.entries.forEach { r ->
                Text(r.nombre, fontSize = Tamanos.apoyo, fontWeight = FontWeight.Bold,
                    color = if (r == rutinaRA) Color.Black else Colores.texto,
                    modifier = Modifier.background(if (r == rutinaRA) Colores.primario else Colores.superficie, RoundedCornerShape(12.dp))
                        .clickable { rutinaRA = r }.padding(horizontal = 16.dp, vertical = 10.dp))
            }
        }
        BotonGrande("📐 Zona RA: coloca la esterilla", Modifier.fillMaxWidth(), Colores.correcto) { alZonaRA(rutinaRA) }
    }
}

@Composable
private fun TarjetaRutina(rutina: Rutina, alPulsar: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Colores.superficie, RoundedCornerShape(16.dp))
            .clickable(onClick = alPulsar).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(rutina.nombre, fontSize = Tamanos.titulo, fontWeight = FontWeight.Bold, color = Colores.primario)
        Text(rutina.series.joinToString(" · ") { "${it.ejercicio.nombre} ${it.objetivo}" },
            fontSize = Tamanos.apoyo, color = Colores.texto)
    }
}
