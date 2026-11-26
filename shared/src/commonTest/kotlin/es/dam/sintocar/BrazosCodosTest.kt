package es.dam.sintocar

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** A 2 o 3 m en vertical las muñecas se salen de la imagen con los brazos en cruz: entonces se miran los codos. */
class BrazosCodosTest {
    private fun sinMunecas(rodilla: Float, brazos: Float) =
        Postura(posturaManiqui(rodilla, brazos).puntos - Parte.MUNECA_IZQ - Parte.MUNECA_DER)

    @Test fun enCruzSinMunecasSeDetectaPorLosCodos() = assertTrue(brazosEnCruz(sinMunecas(175f, 90f)))
    @Test fun brazosAbajoSinMunecasNo() = assertFalse(brazosEnCruz(sinMunecas(175f, 0f)))
    @Test fun aMediasSinMunecasNo() = assertFalse(brazosEnCruz(sinMunecas(175f, 45f)))
    @Test fun sinCodosNiMunecasNo() =
        assertFalse(brazosEnCruz(Postura(sinMunecas(175f, 90f).puntos - Parte.CODO_IZQ - Parte.CODO_DER)))
}
