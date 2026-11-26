package es.dam.sintocar

import kotlin.test.Test
import kotlin.test.assertEquals

class AudioTest {
    private fun muestras(vararg v: Int) = ByteArray(v.size * 2).also { b ->
        v.forEachIndexed { i, x -> b[2 * i] = (x and 0xFF).toByte(); b[2 * i + 1] = (x shr 8).toByte() }
    }
    private fun leer(b: ByteArray, n: Int) = (0 until n / 2).map { ((b[2 * it].toInt() and 0xFF) or (b[2 * it + 1].toInt() shl 8)).toShort().toInt() }

    @Test fun monoNoCambia() {
        val b = muestras(100, -200)
        assertEquals(4, aMono(b, 4, 1))
        assertEquals(listOf(100, -200), leer(b, 4))
    }

    @Test fun estereoPromediaCadaPar() {
        val b = muestras(1000, 3000, -1000, -3000, 32767, 32767)
        val n = aMono(b, b.size, 2)
        assertEquals(6, n)
        assertEquals(listOf(2000, -2000, 32767), leer(b, n))
    }
}
