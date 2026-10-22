package pe.edu.upeu.pharmamobilee.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import pe.edu.upeu.pharmamobilee.data.local.ProductoLocal
import pe.edu.upeu.pharmamobilee.data.mapper.toDomain
import pe.edu.upeu.pharmamobilee.data.remote.ProductoApi
import pe.edu.upeu.pharmamobilee.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobilee.domain.model.OrigenProducto
import pe.edu.upeu.pharmamobilee.domain.model.Producto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Suite solicitada por la Actividad Autónoma 11. Cada caso crea su propia
 * base SQLDelight en memoria y usa MockEngine; por eso no requiere emulador,
 * red ni espera.
 */
class Actividad11OfflineFirstTest {

    @Test
    fun sincronizacionConRedGuardaCatalogoYElFlowEmiteListaCompleta() = runTest {
        val local = ProductoLocal(baseEnMemoria())
        val api = api { request ->
            assertEquals(HttpMethod.Get, request.method)
            respond(paginaJson(productoJson(11, "Remoto A"), productoJson(12, "Remoto B")), HttpStatusCode.OK, jsonHeaders())
        }

        ProductoRepositorioOffline(local, api, 1L).sincronizar().getOrThrow()

        val catalogo = local.observar().first()
        assertEquals(listOf(11L, 12L), catalogo.map(Producto::id))
        assertEquals(2, catalogo.size)
        assertTrue(local.ultimaSincronizacion() != null)
    }

    @Test
    fun registroSinRedGuardaPendienteYElFlowLoEmite() = runTest {
        val local = ProductoLocal(baseEnMemoria())
        val api = api { throw IOException("Sin conexión controlada") }

        val creado = ProductoRepositorioOffline(local, api, 1L).registrar(producto("Sin red")).getOrThrow()

        assertTrue(creado.id < 0)
        assertEquals(OrigenProducto.LOCAL, local.pendientes().single().origen)
        assertEquals("Sin red", local.observar().first().single().nombre)
    }

    @Test
    fun envioAlReconectarConfirmaLaFilaPendiente() = runTest {
        val local = ProductoLocal(baseEnMemoria())
        local.guardarPendiente(producto("Pendiente"))
        val api = api { request ->
            when (request.method) {
                HttpMethod.Post -> respond(productoJson(501, "Pendiente"), HttpStatusCode.Created, jsonHeaders())
                HttpMethod.Get -> respond(paginaJson(productoJson(501, "Pendiente")), HttpStatusCode.OK, jsonHeaders())
                else -> error("Método no esperado")
            }
        }

        ProductoRepositorioOffline(local, api, 1L).sincronizar().getOrThrow()

        assertTrue(local.pendientes().isEmpty())
        assertEquals(501L, local.observar().first().single().id)
    }

    @Test
    fun bajadaDelServidorNoBorraLoPendiente() = runTest {
        val local = ProductoLocal(baseEnMemoria())
        val creadoSinConexion = local.guardarPendiente(producto("Creado sin conexión"))
        val api = api { request ->
            assertEquals(HttpMethod.Get, request.method)
            respond(paginaJson(productoJson(800, "Remoto")), HttpStatusCode.OK, jsonHeaders())
        }

        val descargados = api.listarTodas().map { it.toDomain() }
        local.reemplazarSincronizados(descargados)

        val catalogo = local.observar().first()
        assertEquals(setOf(creadoSinConexion.id, 800L), catalogo.map(Producto::id).toSet())
        assertEquals("Creado sin conexión", local.pendientes().single().nombre)
    }

    @Test
    fun identificadoresLocalesYRemotosNoColisionan() = runTest {
        val local = ProductoLocal(baseEnMemoria())
        local.guardarPendiente(producto("Local"))
        val api = api { request ->
            when (request.method) {
                HttpMethod.Post -> respond(productoJson(701, "Local"), HttpStatusCode.Created, jsonHeaders())
                HttpMethod.Get -> respond(
                    paginaJson(productoJson(7, "Servidor existente"), productoJson(701, "Local")),
                    HttpStatusCode.OK,
                    jsonHeaders()
                )
                else -> error("Método no esperado")
            }
        }

        ProductoRepositorioOffline(local, api, 1L).sincronizar().getOrThrow()

        assertEquals(setOf(7L, 701L), local.observar().first().map(Producto::id).toSet())
        assertFalse(local.observar().first().any { it.id < 0 })
    }

    @Test
    fun sincronizarDosVecesNoDuplicaFilas() = runTest {
        val local = ProductoLocal(baseEnMemoria())
        val api = api { request ->
            assertEquals(HttpMethod.Get, request.method)
            respond(paginaJson(productoJson(90, "Único remoto")), HttpStatusCode.OK, jsonHeaders())
        }
        val repositorio = ProductoRepositorioOffline(local, api, 1L)

        repositorio.sincronizar().getOrThrow()
        repositorio.sincronizar().getOrThrow()

        val catalogo = local.observar().first()
        assertEquals(1, catalogo.size)
        assertEquals(90L, catalogo.single().id)
    }

    private fun api(responder: MockRequestHandler): ProductoApi =
        ProductoApi(crearHttpClient(MockEngine(responder), URL_BASE))

    private fun producto(nombre: String, id: Long = 0, origen: OrigenProducto = OrigenProducto.REMOTO) = Producto(
        id = id, nombre = nombre, precio = 12.5, stock = 8, categoriaId = 1L, categoria = "Pruebas", origen = origen
    )

    private fun productoJson(id: Long, nombre: String) = """{"id":$id,"nombre":"$nombre","precio":12.5,"stock":8,"estado":true,"categoriaId":1,"categoriaNombre":"Pruebas"}"""
    private fun paginaJson(vararg productos: String) = """{"contenido":[${productos.joinToString()}],"pagina":0,"tamanio":100,"totalElementos":${productos.size},"totalPaginas":1,"ultima":true}"""
    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private companion object { const val URL_BASE = "http://localhost/api/v1/" }
}
