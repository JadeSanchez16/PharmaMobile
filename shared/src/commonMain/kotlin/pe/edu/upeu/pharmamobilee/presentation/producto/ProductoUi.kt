package pe.edu.upeu.pharmamobilee.presentation.producto

import pe.edu.upeu.pharmamobilee.domain.model.OrigenProducto
import pe.edu.upeu.pharmamobilee.domain.model.Producto
import pe.edu.upeu.pharmamobilee.platform.formatearSoles

data class ProductoUi(
    val producto: Producto,
    val precio: String
) {
    val id: Long get() = producto.id
    val nombre: String get() = producto.nombre.sinEtiquetaDeSesion()
    val stock: Int get() = producto.stock
    val activo: Boolean get() = producto.activo
    val categoria: String get() = producto.categoria.sinEtiquetaDeSesion()
    val stockDisponible: Boolean get() = producto.stockDisponible
    val requiereReposicion: Boolean get() = producto.requiereReposicion
    val origen: OrigenProducto get() = producto.origen
}

private val etiquetaDeSesion = Regex(
    pattern = """\s*[-–—]?\s*sesi[oó]n\s*7\b""",
    option = RegexOption.IGNORE_CASE
)

internal fun String.sinEtiquetaDeSesion(): String =
    replace(etiquetaDeSesion, " ")
        .replace(Regex("""\s{2,}"""), " ")
        .trim()

fun Producto.toUi(): ProductoUi = ProductoUi(
    producto = this,
    precio = formatearSoles(precio)
)
