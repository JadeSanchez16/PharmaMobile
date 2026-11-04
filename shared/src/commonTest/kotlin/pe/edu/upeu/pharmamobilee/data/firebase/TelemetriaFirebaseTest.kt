package pe.edu.upeu.pharmamobilee.data.firebase

import kotlin.test.Test
import kotlin.test.assertEquals

class TelemetriaFirebaseTest {
    @Test
    fun contextoTecnicoExcluyeTokensYDatosPersonales() {
        val seguro = contextoTecnicoSeguro(
            mapOf(
                "pantalla" to "inventario",
                "codigo_http" to "503",
                "authorization" to "Bearer secreto",
                "correo" to "persona@ejemplo.com"
            )
        )

        assertEquals(mapOf("pantalla" to "inventario", "codigo_http" to "503"), seguro)
    }
}
