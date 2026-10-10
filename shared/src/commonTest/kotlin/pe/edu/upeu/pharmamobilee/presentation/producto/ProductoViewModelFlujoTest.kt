package pe.edu.upeu.pharmamobilee.presentation.producto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobilee.domain.model.Producto
import pe.edu.upeu.pharmamobilee.domain.platform.Compartidor
import pe.edu.upeu.pharmamobilee.domain.error.ErrorApi
import pe.edu.upeu.pharmamobilee.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobilee.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobilee.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobilee.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobilee.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobilee.domain.usecase.RegistrarProductoUseCase
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProductoViewModelFlujoTest {

    @Test
    fun repositorioVacioProduceFaseSinProductos() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repositorio = ProductoRepositoryFalso(productos = emptyList())
            val viewModel = crearViewModel(repositorio)

            assertIs<ProductoFase.Cargando>(viewModel.uiState.value.fase)
            advanceUntilIdle()

            assertIs<ProductoFase.SinProductos>(viewModel.uiState.value.fase)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun repositorioConProductosProduceFaseConProductos() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val productos = listOf(
                Producto(id = 1L, nombre = "Paracetamol", precio = 3.5, stock = 20),
                Producto(id = 2L, nombre = "Ibuprofeno", precio = 5.0, stock = 8),
                Producto(id = 3L, nombre = "Alcohol", precio = 7.0, stock = 15)
            )
            val repositorio = ProductoRepositoryFalso(productos = productos)
            val viewModel = crearViewModel(repositorio)

            assertIs<ProductoFase.Cargando>(viewModel.uiState.value.fase)
            advanceUntilIdle()

            assertIs<ProductoFase.ConProductos>(viewModel.uiState.value.fase)
            assertEquals(productos, viewModel.uiState.value.productos.map { it.producto })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun repositorioQueLanzaExcepcionProduceFaseError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repositorio = ProductoRepositoryFalso(
                errorAlListar = ErrorApiException(ErrorApi.SinConexion)
            )
            val viewModel = crearViewModel(repositorio)

            advanceUntilIdle()

            val fase = assertIs<ProductoFase.Error>(viewModel.uiState.value.fase)
            assertEquals(
                "No se pudo conectar con el servicio. Verifica tu conexión a Internet.",
                fase.mensaje
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun registrarConPrecioCeroMuestraErrorYNoLlamaRepositorio() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repositorio = ProductoRepositoryFalso()
            val viewModel = crearViewModel(repositorio)
            advanceUntilIdle()

            viewModel.actualizarNombre("Vitamina C")
            viewModel.actualizarPrecio("0")
            viewModel.actualizarStock("12")
            viewModel.guardarProducto()
            advanceUntilIdle()

            assertEquals("El precio debe ser mayor a 0", viewModel.uiState.value.precioError)
            assertEquals(0, repositorio.registros)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun editarProductoActualizaElRegistroSinDuplicarlo() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val producto = Producto(id = 7L, nombre = "Alcohol", precio = 7.0, stock = 15)
            val repositorio = ProductoRepositoryFalso(productos = listOf(producto))
            val viewModel = crearViewModel(repositorio)
            advanceUntilIdle()

            viewModel.editarProducto(producto)
            viewModel.actualizarPrecio("8.50")
            viewModel.guardarProducto()
            advanceUntilIdle()

            assertEquals(0, repositorio.registros)
            assertEquals(1, repositorio.actualizaciones)
            assertEquals(8.50, viewModel.uiState.value.productos.single().producto.precio)
            assertEquals(null, viewModel.uiState.value.productoEnEdicionId)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun error400DelServidorSeDistribuyeEnLosCamposDelFormulario() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val errores = mapOf(
                "nombre" to "El nombre debe tener entre 3 y 150 caracteres",
                "precio" to "El precio debe ser mayor o igual a 0.01",
                "stock" to "El stock no puede ser negativo"
            )
            val repositorio = ProductoRepositoryFalso(
                errorAlRegistrar = ErrorApiException(
                    ErrorApi.Validacion(errores)
                )
            )
            val viewModel = crearViewModel(repositorio)
            advanceUntilIdle()

            viewModel.actualizarNombre("AB")
            viewModel.actualizarPrecio("3.50")
            viewModel.actualizarStock("10")
            viewModel.guardarProducto()
            advanceUntilIdle()

            assertEquals(errores["nombre"], viewModel.uiState.value.nombreError)
            assertEquals(errores["precio"], viewModel.uiState.value.precioError)
            assertEquals(errores["stock"], viewModel.uiState.value.stockError)
            assertIs<ProductoFase.SinProductos>(viewModel.uiState.value.fase)
            assertIs<ProductoOperacion.Inactiva>(viewModel.uiState.value.operacion)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun eliminarRecargaLaListaYFinalizaLaOperacion() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val producto = Producto(id = 5L, nombre = "Temporal", precio = 2.0)
            val permitirEliminacion = CompletableDeferred<Unit>()
            val repositorio = ProductoRepositoryFalso(
                productos = listOf(producto),
                permitirEliminacion = permitirEliminacion
            )
            val viewModel = crearViewModel(repositorio)
            advanceUntilIdle()

            viewModel.eliminarProducto(producto.id)
            // Intentos en la misma vuelta, antes de que se ejecute la corrutina.
            viewModel.eliminarProducto(producto.id)
            viewModel.guardarProducto()
            runCurrent()

            assertEquals(
                ProductoOperacion.EnCurso(ProductoOperacion.Tipo.Eliminar, producto.id),
                viewModel.uiState.value.operacion
            )
            assertIs<ProductoFase.ConProductos>(viewModel.uiState.value.fase)
            assertEquals(producto, viewModel.uiState.value.productos.single().producto)
            viewModel.eliminarProducto(producto.id)
            viewModel.editarProducto(producto)
            viewModel.cancelarEdicion()
            runCurrent()
            assertEquals(1, repositorio.solicitudesEliminacion)
            assertEquals(0, repositorio.registros)
            assertEquals(null, viewModel.uiState.value.productoEnEdicionId)

            permitirEliminacion.complete(Unit)
            advanceUntilIdle()

            assertIs<ProductoFase.SinProductos>(viewModel.uiState.value.fase)
            assertIs<ProductoOperacion.Inactiva>(viewModel.uiState.value.operacion)
            assertEquals("Producto eliminado correctamente", viewModel.uiState.value.mensajeExito)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun soloElBotonCorrespondienteSeDeshabilitaSegunLaOperacion() {
        val inactiva = ProductoOperacion.Inactiva
        val fallida = ProductoOperacion.Fallida("Sin conexión")
        for (operacion in listOf(inactiva, fallida)) {
            assertFalse(operacion.guardando)
            assertFalse(operacion.eliminando(5L))
        }
        for (tipo in listOf(ProductoOperacion.Tipo.Crear, ProductoOperacion.Tipo.Actualizar)) {
            val guardando = ProductoOperacion.EnCurso(tipo)
            assertTrue(guardando.guardando)
            assertFalse(guardando.eliminando(5L))
        }
        val eliminando = ProductoOperacion.EnCurso(ProductoOperacion.Tipo.Eliminar, 5L)
        assertFalse(eliminando.guardando)
        assertTrue(eliminando.eliminando(5L))
        assertFalse(eliminando.eliminando(6L))
    }

    @Test
    fun guardarConservaListadoYRechazaMutacionesSimultaneas() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            for (editando in listOf(false, true)) {
                val producto = Producto(id = 5L, nombre = "Alcohol", precio = 2.0, stock = 10)
                val permitirGuardado = CompletableDeferred<Unit>()
                val repositorio = ProductoRepositoryFalso(
                    productos = listOf(producto), permitirGuardado = permitirGuardado
                )
                val viewModel = crearViewModel(repositorio)
                advanceUntilIdle()
                if (editando) viewModel.editarProducto(producto)
                viewModel.actualizarNombre("Alcohol actualizado")
                viewModel.actualizarPrecio("3.00")
                viewModel.actualizarStock("10")

                viewModel.guardarProducto()
                viewModel.guardarProducto()
                viewModel.eliminarProducto(producto.id)
                viewModel.editarProducto(producto)
                viewModel.cancelarEdicion()
                runCurrent()

                assertIs<ProductoFase.ConProductos>(viewModel.uiState.value.fase)
                assertEquals(producto, viewModel.uiState.value.productos.single().producto)
                val operacion = assertIs<ProductoOperacion.EnCurso>(viewModel.uiState.value.operacion)
                assertEquals(
                    if (editando) ProductoOperacion.Tipo.Actualizar else ProductoOperacion.Tipo.Crear,
                    operacion.tipo
                )
                assertTrue(operacion.guardando)
                assertFalse(operacion.eliminando(producto.id))
                assertEquals(if (editando) producto.id else null, viewModel.uiState.value.productoEnEdicionId)
                assertEquals(1, repositorio.registros + repositorio.actualizaciones)
                assertEquals(0, repositorio.solicitudesEliminacion)

                permitirGuardado.complete(Unit)
                advanceUntilIdle()
                assertIs<ProductoOperacion.Inactiva>(viewModel.uiState.value.operacion)
                assertIs<ProductoFase.ConProductos>(viewModel.uiState.value.fase)
                assertTrue(viewModel.uiState.value.productos.any { it.nombre == "Alcohol actualizado" })
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun salirCancelaGuardadoSinErrorNiActualizacionTardia() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val permitirGuardado = CompletableDeferred<Unit>()
            val producto = Producto(id = 5L, nombre = "Alcohol", precio = 2.0, stock = 10)
            val repositorio = ProductoRepositoryFalso(listOf(producto), permitirGuardado = permitirGuardado)
            val viewModel = crearViewModel(repositorio)
            advanceUntilIdle()
            viewModel.actualizarNombre("Temporal cancelado")
            viewModel.actualizarPrecio("3.00")
            viewModel.actualizarStock("10")
            viewModel.guardarProducto()
            runCurrent()
            assertIs<ProductoOperacion.EnCurso>(viewModel.uiState.value.operacion)
            viewModel.cancelarSolicitudesAlSalir()
            runCurrent()
            val estadoAlSalir = viewModel.uiState.value
            assertIs<ProductoOperacion.Inactiva>(estadoAlSalir.operacion)
            assertIs<ProductoFase.ConProductos>(estadoAlSalir.fase)
            assertEquals(null, estadoAlSalir.mensajeExito)
            permitirGuardado.complete(Unit)
            advanceUntilIdle()
            assertEquals(estadoAlSalir, viewModel.uiState.value)
            viewModel.reanudarSolicitudesAlEntrar()
            advanceUntilIdle()
            assertEquals(listOf(producto), viewModel.uiState.value.productos.map { it.producto })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun compartirDelegaTextoComunAlCompartidor() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val producto = Producto(
                id = 9L,
                nombre = "Naproxeno 550 mg",
                precio = 8.5,
                stock = 14
            )
            val compartidor = CompartidorFalso()
            val viewModel = crearViewModel(
                productoRepository = ProductoRepositoryFalso(listOf(producto)),
                compartidor = compartidor
            )
            advanceUntilIdle()

            viewModel.compartir(producto)

            assertContains(compartidor.ultimoTexto.orEmpty(), "Naproxeno 550 mg")
            assertContains(compartidor.ultimoTexto.orEmpty(), "S/")
            assertContains(compartidor.ultimoTexto.orEmpty(), "Stock: 14")
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun crearViewModel(
        productoRepository: ProductoRepository,
        compartidor: Compartidor = CompartidorFalso()
    ): ProductoViewModel {
        return ProductoViewModel(
            listarProductosUseCase = ListarProductosUseCase(productoRepository),
            registrarProductoUseCase = RegistrarProductoUseCase(productoRepository),
            actualizarProductoUseCase = ActualizarProductoUseCase(productoRepository),
            eliminarProductoUseCase = EliminarProductoUseCase(productoRepository),
            compartidor = compartidor
        )
    }

    private class CompartidorFalso : Compartidor {
        var ultimoTexto: String? = null
            private set

        override fun compartir(texto: String) {
            ultimoTexto = texto
        }
    }

    private class ProductoRepositoryFalso(
        productos: List<Producto> = emptyList(),
        private val errorAlListar: Throwable? = null,
        private val errorAlRegistrar: Throwable? = null,
        private val permitirEliminacion: CompletableDeferred<Unit>? = null,
        private val permitirGuardado: CompletableDeferred<Unit>? = null
    ) : ProductoRepository {
        private val productosGuardados = productos.toMutableList()
        var registros: Int = 0
            private set
        var actualizaciones: Int = 0
            private set
        var solicitudesEliminacion: Int = 0
            private set

        override suspend fun registrar(producto: Producto): Result<Producto> = runCatching {
            errorAlRegistrar?.let { throw it }
            registros++
            permitirGuardado?.await()
            producto.copy(id = 100L + registros).also(productosGuardados::add)
        }

        override suspend fun actualizar(producto: Producto): Result<Producto> = runCatching {
            actualizaciones++
            permitirGuardado?.await()
            val indice = productosGuardados.indexOfFirst { it.id == producto.id }
            check(indice >= 0)
            productosGuardados[indice] = producto
            producto
        }

        override suspend fun listar(): Result<List<Producto>> = runCatching {
            errorAlListar?.let { throw it }
            productosGuardados.toList()
        }

        override suspend fun obtener(id: Long): Result<Producto> = runCatching {
            productosGuardados.first { it.id == id }
        }

        override suspend fun eliminar(id: Long): Result<Unit> = runCatching {
            solicitudesEliminacion++
            permitirEliminacion?.await()
            check(productosGuardados.removeAll { it.id == id })
        }
    }
}
