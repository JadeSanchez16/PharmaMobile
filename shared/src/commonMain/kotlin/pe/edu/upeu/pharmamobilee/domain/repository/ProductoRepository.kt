package pe.edu.upeu.pharmamobilee.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobilee.domain.model.Producto

interface ProductoRepository {
    /** La pantalla observa la fuente local y no una respuesta HTTP puntual. */
    fun observar(): Flow<List<Producto>>
    /** Sube lo pendiente y solo después descarga el catálogo remoto. */
    suspend fun sincronizar(): Result<Unit>
    suspend fun listar(): Result<List<Producto>>
    suspend fun obtener(id: Long): Result<Producto>
    suspend fun registrar(producto: Producto): Result<Producto>
    suspend fun actualizar(producto: Producto): Result<Producto>
    suspend fun eliminar(id: Long): Result<Unit>

    /** Instante local de la última sincronización completa, si existe. */
    fun ultimaSincronizacion(): Long? = null
}
