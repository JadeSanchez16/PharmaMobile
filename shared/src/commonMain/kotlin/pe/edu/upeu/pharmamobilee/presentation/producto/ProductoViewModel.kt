package pe.edu.upeu.pharmamobilee.presentation.producto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import pe.edu.upeu.pharmamobilee.domain.error.ErrorApi
import pe.edu.upeu.pharmamobilee.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobilee.domain.error.mensajeUsuario
import pe.edu.upeu.pharmamobilee.domain.model.Producto
import pe.edu.upeu.pharmamobilee.domain.platform.Compartidor
import pe.edu.upeu.pharmamobilee.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobilee.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobilee.domain.usecase.comoTextoParaCompartir
import pe.edu.upeu.pharmamobilee.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobilee.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobilee.domain.usecase.ProductoRegistroException
import pe.edu.upeu.pharmamobilee.domain.usecase.RegistrarProductoUseCase

class ProductoViewModel(
    private val listarProductosUseCase: ListarProductosUseCase,
    private val registrarProductoUseCase: RegistrarProductoUseCase,
    private val actualizarProductoUseCase: ActualizarProductoUseCase,
    private val eliminarProductoUseCase: EliminarProductoUseCase,
    private val compartidor: Compartidor,
    private val productoRepository: ProductoRepository? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState = _uiState.asStateFlow()
    private var trabajoListado: Job? = null
    private var trabajoOperacion: Job? = null
    private var trabajoSincronizacion: Job? = null
    private var recargaPendiente = false

    init {
        if (productoRepository == null) {
            cargarProductos()
        } else {
            observarProductosLocales()
            sincronizar()
        }
    }

    fun actualizarNombre(nombre: String) {
        _uiState.update {
            it.copy(nombre = nombre, nombreError = null, mensajeExito = null)
        }
    }

    fun actualizarPrecio(precio: String) {
        _uiState.update {
            it.copy(precio = precio, precioError = null, mensajeExito = null)
        }
    }

    fun actualizarStock(stock: String) {
        _uiState.update {
            it.copy(stock = stock, stockError = null, mensajeExito = null)
        }
    }

    fun actualizarActivo(activo: Boolean) {
        _uiState.update { it.copy(activo = activo, mensajeExito = null) }
    }

    fun guardarProducto() {
        if (_uiState.value.operacion is ProductoOperacion.EnCurso) return

        val estado = _uiState.value
        val editando = estado.productoEnEdicionId != null
        val tipo = if (editando) {
            ProductoOperacion.Tipo.Actualizar
        } else {
            ProductoOperacion.Tipo.Crear
        }
        // Reservar la operación antes de lanzar la corrutina evita dos pulsaciones en cola.
        limpiarMensajes()
        _uiState.update { it.copy(operacion = ProductoOperacion.EnCurso(tipo)) }
        trabajoOperacion = viewModelScope.launch {
            val resultado = estado.productoEnEdicionId?.let { id ->
                actualizarProductoUseCase(
                    id = id,
                    nombre = estado.nombre,
                    precio = estado.precio,
                    stock = estado.stock,
                    activo = estado.activo,
                    categoriaId = estado.categoriaEnEdicionId
                )
            } ?: registrarProductoUseCase(
                nombre = estado.nombre,
                precio = estado.precio,
                stock = estado.stock,
                activo = estado.activo
            )

            val producto = resultado.getOrElse { error ->
                manejarFalloOperacion(error)
                return@launch
            }

            limpiarFormulario()
            val mensaje = if (editando) {
                "Producto \"${producto.nombre}\" actualizado correctamente"
            } else if (producto.id < 0) {
                "Producto guardado localmente. Pendiente de sincronizar."
            } else {
                "Producto \"${producto.nombre}\" registrado correctamente"
            }
            if (productoRepository == null) recargarTrasOperacion(mensaje) else {
                _uiState.update { it.copy(operacion = ProductoOperacion.Inactiva, mensajeExito = mensaje) }
            }
        }
    }

    fun eliminarProducto(id: Long) {
        if (_uiState.value.operacion is ProductoOperacion.EnCurso) return

        limpiarMensajes()
        _uiState.update {
            it.copy(
                operacion = ProductoOperacion.EnCurso(ProductoOperacion.Tipo.Eliminar, id)
            )
        }
        trabajoOperacion = viewModelScope.launch {
            eliminarProductoUseCase(id).getOrElse { error ->
                manejarFalloOperacion(error)
                return@launch
            }

            if (_uiState.value.productoEnEdicionId == id) {
                limpiarFormulario()
            }
            if (productoRepository == null) recargarTrasOperacion("Producto eliminado correctamente") else {
                _uiState.update { it.copy(operacion = ProductoOperacion.Inactiva, mensajeExito = "Producto eliminado correctamente") }
            }
        }
    }

    fun editarProducto(producto: Producto) {
        if (_uiState.value.operacion is ProductoOperacion.EnCurso) return
        _uiState.update {
            it.copy(
                nombre = producto.nombre,
                precio = producto.precio.toString(),
                stock = producto.stock.toString(),
                activo = producto.activo,
                nombreError = null,
                precioError = null,
                stockError = null,
                mensajeExito = null,
                operacion = ProductoOperacion.Inactiva,
                productoEnEdicionId = producto.id,
                categoriaEnEdicionId = producto.categoriaId
            )
        }
    }

    fun cancelarEdicion() {
        if (_uiState.value.operacion is ProductoOperacion.EnCurso) return
        limpiarFormulario()
    }

    fun compartir(producto: Producto) {
        compartidor.compartir(producto.comoTextoParaCompartir())
    }

    fun cargarProductos() {
        if (productoRepository != null) {
            sincronizar()
            return
        }
        trabajoListado?.cancel()
        trabajoListado = viewModelScope.launch {
            cargarListado(mostrarCarga = true)
        }
    }

    fun cancelarSolicitudesAlSalir() {
        if (trabajoListado?.isActive == true || trabajoOperacion?.isActive == true || trabajoSincronizacion?.isActive == true) {
            // El servidor puede haber recibido la mutación: reconciliar al volver.
            recargaPendiente = true
            trabajoListado?.cancel()
            trabajoOperacion?.cancel()
            trabajoSincronizacion?.cancel()
            _uiState.update { it.copy(operacion = ProductoOperacion.Inactiva) }
        }
    }

    fun reanudarSolicitudesAlEntrar() {
        if (recargaPendiente) {
            recargaPendiente = false
            cargarProductos()
        }
    }

    /** Acción explícita: la pantalla sigue leyendo de SQLDelight incluso si falla la red. */
    fun sincronizar() {
        val repositorio = productoRepository ?: run {
            cargarProductos()
            return
        }
        if (trabajoSincronizacion?.isActive == true) return
        trabajoSincronizacion = viewModelScope.launch {
            _uiState.update { it.copy(sincronizando = true, avisoRed = null) }
            repositorio.sincronizar().onFailure { error ->
                _uiState.update {
                    it.copy(
                        avisoRed = "Sin conexión: mostrando datos guardados",
                        operacion = if (it.operacion is ProductoOperacion.EnCurso) it.operacion else ProductoOperacion.Inactiva
                    )
                }
            }
            _uiState.update { it.copy(sincronizando = false) }
        }
    }

    private fun observarProductosLocales() {
        val repositorio = checkNotNull(productoRepository)
        trabajoListado = viewModelScope.launch {
            repositorio.observar().collect { productos ->
                _uiState.update {
                    it.copy(
                        productos = productos.map(Producto::toUi),
                        fase = if (productos.isEmpty()) ProductoFase.SinProductos else ProductoFase.ConProductos
                    )
                }
            }
        }
    }

    private suspend fun cargarListado(
        mostrarCarga: Boolean
    ): Result<List<Producto>> {
        if (mostrarCarga) {
            _uiState.update { it.copy(fase = ProductoFase.Cargando) }
        }

        val resultado = listarProductosUseCase()
        (resultado.exceptionOrNull() as? CancellationException)?.let { throw it }
        resultado.onSuccess { productos ->
            _uiState.update {
                it.copy(
                    productos = productos.map(Producto::toUi),
                    fase = if (productos.isEmpty()) {
                        ProductoFase.SinProductos
                    } else {
                        ProductoFase.ConProductos
                    }
                )
            }
        }.onFailure { error ->
            if (mostrarCarga) {
                _uiState.update {
                    it.copy(
                        productos = emptyList(),
                        fase = ProductoFase.Error(mensajeDe(error))
                    )
                }
            }
        }
        return resultado
    }

    private suspend fun recargarTrasOperacion(mensaje: String) {
        cargarListado(mostrarCarga = false).fold(
            onSuccess = {
                _uiState.update {
                    it.copy(
                        operacion = ProductoOperacion.Inactiva,
                        mensajeExito = mensaje
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        operacion = ProductoOperacion.Fallida(
                            "La operación se completó, pero no se pudo actualizar la lista: ${mensajeDe(error)}"
                        )
                    )
                }
            }
        )
    }

    private fun manejarFalloOperacion(fallo: Throwable) {
        if (fallo is CancellationException) throw fallo
        when (fallo) {
            is ProductoRegistroException -> _uiState.update {
                it.copy(
                    operacion = ProductoOperacion.Inactiva,
                    nombreError = fallo.nombreError,
                    precioError = fallo.precioError,
                    stockError = fallo.stockError
                )
            }

            is ErrorApiException -> when (val error = fallo.error) {
                is ErrorApi.Validacion -> _uiState.update {
                    it.copy(
                        operacion = ProductoOperacion.Inactiva,
                        nombreError = error.porCampo["nombre"],
                        precioError = error.porCampo["precio"],
                        stockError = error.porCampo["stock"]
                    )
                }

                else -> _uiState.update {
                    it.copy(
                        operacion = ProductoOperacion.Fallida(
                            error.mensajeUsuario()
                        )
                    )
                }
            }

            else -> _uiState.update {
                it.copy(
                    operacion = ProductoOperacion.Fallida(mensajeDe(fallo))
                )
            }
        }
    }

    private fun mensajeDe(error: Throwable): String =
        (error as? ErrorApiException)?.error.mensajeUsuario()

    private fun limpiarMensajes() {
        _uiState.update {
            it.copy(
                nombreError = null,
                precioError = null,
                stockError = null,
                mensajeExito = null,
                operacion = ProductoOperacion.Inactiva
            )
        }
    }

    private fun limpiarFormulario() {
        _uiState.update {
            it.copy(
                nombre = "",
                precio = "",
                stock = "",
                activo = true,
                nombreError = null,
                precioError = null,
                stockError = null,
                productoEnEdicionId = null,
                categoriaEnEdicionId = null
            )
        }
    }
}
