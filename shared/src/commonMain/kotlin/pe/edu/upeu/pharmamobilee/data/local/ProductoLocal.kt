package pe.edu.upeu.pharmamobilee.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobilee.db.PharmaDb
import pe.edu.upeu.pharmamobilee.domain.model.OrigenProducto
import pe.edu.upeu.pharmamobilee.domain.model.Producto

/** Contrato local para que el repositorio conserve la fuente de verdad en SQLDelight. */
interface FuenteProductosLocal {
    fun observar(): Flow<List<Producto>>
    fun pendientes(): List<Producto>
    fun guardarPendiente(producto: Producto): Producto
    fun guardarSincronizado(producto: Producto)
    fun confirmarPendiente(idLocal: Long, productoServidor: Producto)
    fun eliminar(id: Long)
    fun reemplazarSincronizados(productos: List<Producto>)
}

/**
 * Capa local aislada de Ktor y de los DTO. Solo traduce las filas SQLDelight al
 * modelo de dominio y protege las escrituras compuestas con transacciones.
 */
class ProductoLocal(base: PharmaDb) : FuenteProductosLocal {
    private val consultas = base.productoQueries

    override fun observar(): Flow<List<Producto>> =
        consultas.listarTodos(::filaADominio)
            .asFlow()
            .mapToList(Dispatchers.Default)

    override fun pendientes(): List<Producto> =
        consultas.listarPendientes(::filaADominio).executeAsList()

    override fun guardarPendiente(producto: Producto): Producto {
        val menor = consultas.siguienteIdLocal().executeAsOne()
        val local = producto.copy(id = minOf(menor, 0L) - 1L, origen = OrigenProducto.LOCAL)
        insertar(local, pendiente = 1L)
        return local
    }

    override fun guardarSincronizado(producto: Producto) {
        insertar(producto.copy(origen = OrigenProducto.REMOTO), pendiente = 0L)
    }

    override fun confirmarPendiente(idLocal: Long, productoServidor: Producto) {
        consultas.transaction {
            consultas.borrarPorId(idLocal)
            insertar(productoServidor.copy(origen = OrigenProducto.REMOTO), pendiente = 0L)
        }
    }

    override fun eliminar(id: Long) {
        consultas.borrarPorId(id)
    }

    override fun reemplazarSincronizados(productos: List<Producto>) {
        consultas.transaction {
            consultas.borrarSincronizados()
            productos.forEach { insertar(it.copy(origen = OrigenProducto.REMOTO), pendiente = 0L) }
        }
    }

    private fun insertar(producto: Producto, pendiente: Long) {
        consultas.insertar(
            id = producto.id,
            nombre = producto.nombre,
            precio = producto.precio,
            stock = producto.stock.toLong(),
            pendiente = pendiente,
            activo = if (producto.activo) 1L else 0L,
            categoria_id = producto.categoriaId,
            categoria_nombre = producto.categoria,
            stock_disponible = if (producto.stockDisponible) 1L else 0L
        )
    }
}

private fun filaADominio(
    id: Long,
    nombre: String,
    precio: Double,
    stock: Long,
    pendiente: Long,
    activo: Long,
    categoriaId: Long?,
    categoriaNombre: String,
    stockDisponible: Long
): Producto = Producto(
    id = id,
    nombre = nombre,
    precio = precio,
    stock = stock.toInt(),
    activo = activo == 1L,
    categoria = categoriaNombre,
    categoriaId = categoriaId,
    stockDisponible = stockDisponible == 1L,
    origen = if (pendiente == 1L) OrigenProducto.LOCAL else OrigenProducto.REMOTO
)
