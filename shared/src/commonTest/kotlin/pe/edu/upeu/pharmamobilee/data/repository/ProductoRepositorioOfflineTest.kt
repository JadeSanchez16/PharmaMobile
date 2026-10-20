package pe.edu.upeu.pharmamobilee.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import pe.edu.upeu.pharmamobilee.data.local.FuenteProductosLocal
import pe.edu.upeu.pharmamobilee.data.remote.ProductoApi
import pe.edu.upeu.pharmamobilee.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobilee.domain.model.OrigenProducto
import pe.edu.upeu.pharmamobilee.domain.model.Producto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Pruebas comunes del flujo offline-first; no requieren Android ni un servidor real. */
class ProductoRepositorioOfflineTest {

    @Test
    fun registrarSinConexionConservaProductoPendienteConIdNegativo() = runTest {
        val local = FuenteLocalFalsa()
        val api = ProductoApi(crearHttpClient(MockEngine { throw IOException("Sin conexión controlada") }, URL_BASE))
        val repositorio = ProductoRepositorioOffline(local, api, categoriaPorDefecto = 1L)

        val creado = repositorio.registrar(producto("Producto sin red")).getOrThrow()

        assertTrue(creado.id < 0)
        assertEquals(OrigenProducto.LOCAL, creado.origen)
        assertEquals(listOf(creado), local.pendientes())
    }

    @Test
    fun observarEmiteElCatalogoGuardadoSinEsperarUnaRespuestaHttp() = runTest {
        val local = FuenteLocalFalsa(listOf(producto("Catálogo local", id = 33L)))
        val api = ProductoApi(crearHttpClient(MockEngine { error("No debe llamar a red") }, URL_BASE))
        val repositorio = ProductoRepositorioOffline(local, api, categoriaPorDefecto = 1L)

        val catalogo = repositorio.observar().first()

        assertEquals(1, catalogo.size)
        assertEquals("Catálogo local", catalogo.single().nombre)
        assertEquals(OrigenProducto.REMOTO, catalogo.single().origen)
    }

    @Test
    fun sincronizarPublicaPendientesAntesDeDescargarCatalogoRemoto() = runTest {
        val local = FuenteLocalFalsa(listOf(producto("Pendiente", id = -1L, origen = OrigenProducto.LOCAL)))
        val orden = mutableListOf<HttpMethod>()
        val api = ProductoApi(
            crearHttpClient(
                MockEngine { request ->
                    orden += request.method
                    when (request.method) {
                        HttpMethod.Post -> respond(productoJson(501L, "Pendiente"), HttpStatusCode.Created, jsonHeaders())
                        HttpMethod.Get -> respond(paginaJson(501L, "Pendiente"), HttpStatusCode.OK, jsonHeaders())
                        else -> error("Solicitud no esperada: ${request.method}")
                    }
                },
                URL_BASE
            )
        )
        val repositorio = ProductoRepositorioOffline(local, api, categoriaPorDefecto = 1L)

        repositorio.sincronizar().getOrThrow()

        assertEquals(listOf(HttpMethod.Post, HttpMethod.Get), orden)
        assertTrue(local.pendientes().isEmpty())
        assertFalse(local.observar().first().single().id < 0)
        assertEquals(501L, local.observar().first().single().id)
    }

    private class FuenteLocalFalsa(inicial: List<Producto> = emptyList()) : FuenteProductosLocal {
        private val estado = MutableStateFlow(inicial)

        override fun observar(): Flow<List<Producto>> = estado

        override fun pendientes(): List<Producto> = estado.value.filter { it.origen == OrigenProducto.LOCAL }

        override fun guardarPendiente(producto: Producto): Producto {
            val id = (estado.value.minOfOrNull { it.id } ?: 0L).coerceAtMost(0L) - 1L
            return producto.copy(id = id, origen = OrigenProducto.LOCAL).also { nuevo ->
                estado.value = estado.value + nuevo
            }
        }

        override fun guardarSincronizado(producto: Producto) {
            reemplazar(producto.copy(origen = OrigenProducto.REMOTO))
        }

        override fun confirmarPendiente(idLocal: Long, productoServidor: Producto) {
            estado.value = estado.value.filterNot { it.id == idLocal } + productoServidor.copy(origen = OrigenProducto.REMOTO)
        }

        override fun eliminar(id: Long) {
            estado.value = estado.value.filterNot { it.id == id }
        }

        override fun reemplazarSincronizados(productos: List<Producto>) {
            val pendientes = estado.value.filter { it.origen == OrigenProducto.LOCAL }
            estado.value = pendientes + productos.map { it.copy(origen = OrigenProducto.REMOTO) }
        }

        private fun reemplazar(producto: Producto) {
            estado.value = estado.value.filterNot { it.id == producto.id } + producto
        }
    }

    private fun producto(
        nombre: String,
        id: Long = 0L,
        origen: OrigenProducto = OrigenProducto.REMOTO
    ) = Producto(
        id = id,
        nombre = nombre,
        precio = 12.5,
        stock = 8,
        categoriaId = 1L,
        categoria = "Analgesicos",
        origen = origen
    )

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun productoJson(id: Long, nombre: String) = """
        {"id":$id,"nombre":"$nombre","precio":12.5,"stock":8,"estado":true,
         "categoriaId":1,"categoriaNombre":"Analgesicos"}
    """.trimIndent()

    private fun paginaJson(id: Long, nombre: String) = """
        {"contenido":[${productoJson(id, nombre)}],"pagina":0,"tamanio":100,
         "totalElementos":1,"totalPaginas":1,"ultima":true}
    """.trimIndent()

    private companion object {
        const val URL_BASE = "http://localhost/api/v1/"
    }
}
