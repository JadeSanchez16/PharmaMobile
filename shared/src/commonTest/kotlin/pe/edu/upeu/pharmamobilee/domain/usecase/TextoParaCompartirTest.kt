package pe.edu.upeu.pharmamobilee.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import pe.edu.upeu.pharmamobilee.domain.model.Producto

class TextoParaCompartirTest {
    @Test
    fun textoIncluyeNombrePrecioFormateadoYStock() {
        val producto = Producto(
            id = 1L,
            nombre = "Paracetamol 500 mg",
            precio = 4.5,
            stock = 120
        )

        val texto = producto.comoTextoParaCompartir()

        assertEquals(
            "Paracetamol 500 mg — S/ 4.50 · Stock: 120",
            texto.replace('\u00A0', ' ')
        )
    }
}
