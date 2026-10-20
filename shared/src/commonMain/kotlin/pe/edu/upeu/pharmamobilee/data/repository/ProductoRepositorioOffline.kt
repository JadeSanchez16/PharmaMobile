package pe.edu.upeu.pharmamobilee.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import pe.edu.upeu.pharmamobilee.data.local.FuenteProductosLocal
import pe.edu.upeu.pharmamobilee.data.mapper.toDomain
import pe.edu.upeu.pharmamobilee.data.mapper.toRequest
import pe.edu.upeu.pharmamobilee.data.remote.ProductoApi
import pe.edu.upeu.pharmamobilee.data.remote.ejecutarLlamada
import pe.edu.upeu.pharmamobilee.domain.model.Producto
import pe.edu.upeu.pharmamobilee.domain.model.OrigenProducto
import pe.edu.upeu.pharmamobilee.domain.repository.ProductoRepository

/**
 * Repositorio offline-first: SQLDelight es la fuente de verdad y PharmaSoft
 * solamente reconcilia sus datos. La subida siempre precede a la descarga.
 */
class ProductoRepositorioOffline(
    private val local: FuenteProductosLocal,
    private val remoto: ProductoApi,
    private val categoriaPorDefecto: Long
) : ProductoRepository {

    override fun observar(): Flow<List<Producto>> = local.observar()

    override suspend fun listar(): Result<List<Producto>> = runCatching {
        local.observar().first()
    }

    override suspend fun obtener(id: Long): Result<Producto> =
        listar().mapCatching { productos -> productos.first { it.id == id } }

    override suspend fun registrar(producto: Producto): Result<Producto> {
        val guardado = local.guardarPendiente(producto)
        // La escritura local ya ocurrió. Un fallo de red no revierte ni oculta el producto.
        val sincronizacion = sincronizar()
        return if (sincronizacion.isSuccess) {
            Result.success(producto.copy(origen = OrigenProducto.REMOTO))
        } else {
            Result.success(guardado)
        }
    }

    override suspend fun actualizar(producto: Producto): Result<Producto> = ejecutarLlamada {
        remoto.actualizar(producto.id, producto.toRequest(categoriaPorDefecto)).toDomain()
    }.onSuccess(local::guardarSincronizado)

    override suspend fun eliminar(id: Long): Result<Unit> = ejecutarLlamada {
        remoto.eliminar(id)
    }.onSuccess { local.eliminar(id) }

    override suspend fun sincronizar(): Result<Unit> {
        subirPendientes().getOrElse { return Result.failure(it) }
        return ejecutarLlamada {
            remoto.listarTodas().map { it.toDomain() }
        }.map { productos -> local.reemplazarSincronizados(productos) }
    }

    private suspend fun subirPendientes(): Result<Unit> {
        local.pendientes().forEach { pendiente ->
            val respuesta = ejecutarLlamada {
                remoto.crear(pendiente.toRequest(categoriaPorDefecto)).toDomain()
            }
            val remotoConfirmado = respuesta.getOrElse { return Result.failure(it) }
            local.confirmarPendiente(pendiente.id, remotoConfirmado)
        }
        return Result.success(Unit)
    }
}

