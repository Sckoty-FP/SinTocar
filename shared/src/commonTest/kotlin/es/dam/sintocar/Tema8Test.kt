package es.dam.sintocar

import kotlin.test.Test
import kotlin.test.assertEquals

class FaseTest {
    @Test fun alEmpezarBusca() = assertEquals(FaseRA.BUSCANDO, faseRA(EstadoRA()))
    @Test fun conSueloListo() = assertEquals(FaseRA.LISTO, faseRA(EstadoRA(sueloEncontrado = true)))
    @Test fun colocado() = assertEquals(FaseRA.COLOCADO, faseRA(EstadoRA(sueloEncontrado = true, colocado = true)))
    @Test fun elAvisoMandaSobreTodo() =
        assertEquals(FaseRA.PROBLEMA, faseRA(EstadoRA(sueloEncontrado = true, colocado = true, aviso = "Hay poca luz")))
}

class DistanciaTest {
    @Test fun cerca() = assertEquals("Demasiado cerca", consejo(1.2f))
    @Test fun buena() = assertEquals("Distancia buena: puedes entrenar", consejo(2.5f))
    @Test fun limiteBajo() = assertEquals("Distancia buena: puedes entrenar", consejo(1.8f))
    @Test fun limiteAlto() = assertEquals("Distancia buena: puedes entrenar", consejo(3.2f))
    @Test fun lejos() = assertEquals("Demasiado lejos", consejo(3.5f))
    @Test fun conComa() = assertEquals("2,3", metrosConComa(2.345f))
    @Test fun redondea() = assertEquals("2,4", metrosConComa(2.36f))
    @Test fun entero() = assertEquals("3,0", metrosConComa(3f))
}
