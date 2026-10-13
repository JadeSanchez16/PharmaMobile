package pe.edu.upeu.pharmamobilee.presentation.producto

import kotlin.test.Test
import kotlin.test.assertEquals
import pe.edu.upeu.pharmamobilee.domain.model.Producto

class ProductoUiTest {

    @Test
    fun formateaPrecioEnSolesAlMapearAPresentacion() {
        val productoUi = Producto(
            id = 1,
            nombre = "Paracetamol 500 mg",
            precio = 4.5,
            stock = 120
        ).toUi()

        assertEquals("S/ 4.50", productoUi.precio.replace('\u00A0', ' '))
    }

    @Test
    fun ocultaEtiquetaDeSesionEnNombreYCategoria() {
        val producto = Producto(
            id = 1,
            nombre = "Paracetamol 500 mg - Sesion 7",
            precio = 4.5,
            categoria = "Analgesicos Sesión 7"
        ).toUi()

        assertEquals("Paracetamol 500 mg", producto.nombre)
        assertEquals("Analgesicos", producto.categoria)
    }

    @Test
    fun conservaTextosSinEtiquetaDeSesion() {
        assertEquals("Naproxeno 550 mg", "Naproxeno 550 mg".sinEtiquetaDeSesion())
        assertEquals("Cuidado personal", "Cuidado personal".sinEtiquetaDeSesion())
    }
}
