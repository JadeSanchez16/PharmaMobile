package pe.edu.upeu.pharmamobilee.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.edu.upeu.pharmamobilee.data.productosSimulados
import pe.edu.upeu.pharmamobilee.domain.model.Producto
import pe.edu.upeu.pharmamobilee.domain.repository.ProductoRepository

class ProductoRepositorioEnMemoria(
    productosIniciales: List<Producto> = productosSimulados
) : ProductoRepository {
    private val productos = productosIniciales.toMutableList()
    private val observados = MutableStateFlow(productosIniciales)
    private var siguienteId = (productos.maxOfOrNull { it.id } ?: 0L) + 1L

    override suspend fun registrar(producto: Producto): Result<Producto> = runCatching {
        delay(400)
        val productoRegistrado = producto.copy(id = siguienteId++)
        productos.add(productoRegistrado)
        observados.value = productos.toList()
        productoRegistrado
    }

    override suspend fun actualizar(producto: Producto): Result<Producto> = runCatching {
        delay(400)
        val indice = productos.indexOfFirst { it.id == producto.id }
        require(indice >= 0) { "No existe el producto con id ${producto.id}" }
        productos[indice] = producto
        observados.value = productos.toList()
        producto
    }

    override suspend fun listar(): Result<List<Producto>> = runCatching {
        delay(400)
        productos.toList()
    }

    override suspend fun obtener(id: Long): Result<Producto> = runCatching {
        delay(200)
        productos.first { it.id == id }
    }

    override suspend fun eliminar(id: Long): Result<Unit> = runCatching {
        delay(200)
        val eliminado = productos.removeAll { it.id == id }
        require(eliminado) { "No existe el producto con id $id" }
        observados.value = productos.toList()
    }

    override fun observar(): Flow<List<Producto>> = observados.asStateFlow()

    override suspend fun sincronizar(): Result<Unit> = Result.success(Unit)
}
