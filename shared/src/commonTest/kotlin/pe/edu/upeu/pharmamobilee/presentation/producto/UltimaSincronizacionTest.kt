package pe.edu.upeu.pharmamobilee.presentation.producto

import kotlin.test.Test
import kotlin.test.assertEquals

class UltimaSincronizacionTest {

    @Test
    fun nuncaSincronizadoSeExpresaSinFechaVacia() {
        assertEquals(
            "Aún no se ha sincronizado el catálogo",
            textoUltimaSincronizacion(null, ahoraMillis = 5_000L)
        )
    }

    @Test
    fun datosConMasDeUnDiaMuestranAntiguedadNatural() {
        val ahora = 2 * 24 * 60 * 60 * 1_000L
        assertEquals("Actualizado hace 2 días", textoUltimaSincronizacion(0L, ahora))
    }
}
