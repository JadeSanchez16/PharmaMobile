package pe.edu.upeu.pharmamobilee.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import pe.edu.upeu.pharmamobilee.data.local.FuenteProductosLocal
import pe.edu.upeu.pharmamobilee.data.mapper.toDomain
import pe.edu.upeu.pharmamobilee.data.mapper.toRequest
import pe.edu.upeu.pharmamobilee.data.remote.ProductoApi
import pe.edu.upeu.pharmamobilee.data.remote.ejecutarLlamada
import pe.edu.upeu.pharmamobilee.domain.error.ErrorApi
import pe.edu.upeu.pharmamobilee.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobilee.domain.error.mensajeUsuario
import pe.edu.upeu.pharmamobilee.domain.model.Producto
import pe.edu.upeu.pharmamobilee.domain.model.OrigenProducto
import pe.edu.upeu.pharmamobilee.domain.model.ResultadoEnvio
import pe.edu.upeu.pharmamobilee.domain.repository.ProductoRepository
import kotlin.time.Clock

/**
 * Repositorio offline-first: SQLDelight es la fuente de verdad y PharmaSoft
 * solamente reconcilia sus datos. La subida siempre precede a la descarga.
 */
class ProductoRepositorioOffline(
    private val local: FuenteProductosLocal,
    private val remoto: ProductoApi,
    private val categoriaPorDefecto: Long
) : ProductoRepository {

    companion object {
        const val MAX_INTENTOS = 5

        /** 1 s, 2 s, 4 s, 8 s y 16 s; se limita para no crecer sin control. */
        fun esperaCreciente(intentosPrevios: Int): Long =
            1_000L shl intentosPrevios.coerceIn(0, 4)
    }

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
        val validacionServidor = sincronizacion.exceptionOrNull() as? ErrorApiException
        if (validacionServidor?.error is ErrorApi.Validacion) {
            // La fila se conserva marcada para revisión, y el formulario recibe
            // los mensajes exactos del backend en sus campos correspondientes.
            return Result.failure(validacionServidor)
        }
        return if (sincronizacion.isSuccess) {
            Result.success(producto.copy(origen = OrigenProducto.REMOTO))
        } else {
            // La sincronización puede haber registrado el primer fallo y
            // actualizado intentos/ultimoError. Se devuelve la fila vigente.
            Result.success(local.pendientes().firstOrNull { it.id == guardado.id } ?: guardado)
        }
    }

    override suspend fun actualizar(producto: Producto): Result<Producto> = ejecutarLlamada {
        remoto.actualizar(producto.id, producto.toRequest(categoriaPorDefecto)).toDomain()
    }.onSuccess(local::guardarSincronizado)

    override suspend fun eliminar(id: Long): Result<Unit> {
        // Un producto creado sin red aún no existe en PharmaSoft. Eliminarlo
        // debe retirar solamente su fila pendiente de la fuente local.
        if (id < 0) {
            local.eliminar(id)
            return Result.success(Unit)
        }

        return ejecutarLlamada {
            remoto.eliminar(id)
        }.onSuccess { local.eliminar(id) }
    }

    override suspend fun sincronizar(): Result<Unit> {
        val falloPermanente = subirPendientes().exceptionOrNull()
        val descarga = ejecutarLlamada {
            remoto.listarTodas().map { it.toDomain() }
        }.map { productos ->
            local.reemplazarSincronizados(productos)
            local.guardarUltimaSincronizacion(Clock.System.now().toEpochMilliseconds())
        }
        // Aunque una fila sea inválida, el catálogo remoto se actualiza. Se devuelve
        // el fallo tipado para que el formulario explique qué debe corregirse.
        return if (falloPermanente != null) Result.failure(falloPermanente) else descarga
    }

    override fun ultimaSincronizacion(): Long? = local.ultimaSincronizacion()

    override fun observarPendientes(): Flow<Int> =
        local.observarPendientes(MAX_INTENTOS).map(Long::toInt)

    override fun observarFallidas(): Flow<Int> =
        local.observarFallidas(MAX_INTENTOS).map(Long::toInt)

    private suspend fun subirPendientes(): Result<Unit> = runCatching {
        var falloPermanente: ErrorApiException? = null
        local.pendientesPorEnviar(MAX_INTENTOS).forEach { pendiente ->
            when (val resultado = enviarPendiente(pendiente)) {
                is ResultadoEnvio.Exito -> {
                    local.confirmarPendiente(
                        pendiente.id,
                        pendiente.copy(
                            id = resultado.idServidor,
                            origen = OrigenProducto.REMOTO,
                            intentos = 0,
                            ultimoError = null
                        )
                    )
                }

                is ResultadoEnvio.Permanente -> {
                    local.marcarFallida(pendiente.id, resultado.motivo, MAX_INTENTOS)
                    if (falloPermanente == null) {
                        falloPermanente = ErrorApiException(resultado.error)
                    }
                }

                is ResultadoEnvio.Transitorio -> {
                    // Crear no es idempotente: no se repite automáticamente dentro
                    // de esta misma sincronización. La espera limita la cola y el
                    // siguiente intento se produce solo con una acción explícita.
                    delay(esperaCreciente(pendiente.intentos))
                    local.sumarIntento(pendiente.id, resultado.motivo)
                }
            }
        }
        falloPermanente?.let { throw it }
    }

    private suspend fun enviarPendiente(pendiente: Producto): ResultadoEnvio {
        val respuesta = ejecutarLlamada {
            remoto.crear(pendiente.toRequest(categoriaPorDefecto)).toDomain()
        }
        respuesta.onSuccess { return ResultadoEnvio.Exito(it.id) }

        val error = checkNotNull(respuesta.exceptionOrNull())
        // Si el servidor informa conflicto, se consulta una vez el catálogo para
        // reconciliar un POST cuyo resultado se perdió, sin crear un duplicado.
        if ((error as? ErrorApiException)?.error is ErrorApi.Conflicto) {
            val existente = ejecutarLlamada {
                remoto.listarTodas().map { it.toDomain() }
            }.getOrNull()?.firstOrNull { remoto ->
                remoto.nombre.trim().equals(pendiente.nombre.trim(), ignoreCase = true)
            }
            if (existente != null) return ResultadoEnvio.Exito(existente.id)
        }
        return error.aResultadoEnvio()
    }
}

private fun Throwable.aResultadoEnvio(): ResultadoEnvio = when (
    val api = (this as? ErrorApiException)?.error
) {
    ErrorApi.SinConexion,
    ErrorApi.TiempoAgotado,
    ErrorApi.Servidor -> ResultadoEnvio.Transitorio(api.mensajeUsuario())

    is ErrorApi.Validacion,
    ErrorApi.NoEncontrado,
    is ErrorApi.Conflicto,
    ErrorApi.SinPermiso,
    ErrorApi.SesionExpirada -> ResultadoEnvio.Permanente(api.mensajeUsuario(), api)

    null -> ResultadoEnvio.Transitorio("No se pudo completar el envío. Inténtalo nuevamente.")
}

