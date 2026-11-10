package pe.edu.upeu.pharmamobilee.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobilee.data.local.ProductoLocal
import pe.edu.upeu.pharmamobilee.data.remote.ProductoApi
import pe.edu.upeu.pharmamobilee.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobilee.domain.error.ErrorApi
import pe.edu.upeu.pharmamobilee.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobilee.domain.model.Producto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SincronizacionS13Test {
    @Test
    fun falloTransitorioSumaIntentoYNoMarcaFallida() = runTest {
        val local = ProductoLocal(baseEnMemoria())
        val pendiente = local.guardarPendiente(producto("Sin red"))
        val api = api { request ->
            when (request.method) {
                HttpMethod.Post -> respondError(HttpStatusCode.ServiceUnavailable)
                HttpMethod.Get -> respond(paginaVacia(), HttpStatusCode.OK, jsonHeaders())
                else -> error("Solicitud no esperada")
            }
        }

        val resultado = ProductoRepositorioOffline(local, api, 1L).sincronizar()

        val fila = local.observar().first().single { it.id == pendiente.id }
        assertTrue(resultado.isSuccess)
        assertEquals(1, fila.intentos)
        assertTrue(fila.ultimoError?.isNotBlank() == true)
        assertEquals(1L, local.observarPendientes(ProductoRepositorioOffline.MAX_INTENTOS).first())
        assertEquals(0L, local.observarFallidas(ProductoRepositorioOffline.MAX_INTENTOS).first())
    }

    @Test
    fun falloPermanenteMarcaFilaYNoLaReintenta() = runTest {
        val local = ProductoLocal(baseEnMemoria())
        val pendiente = local.guardarPendiente(producto("Duplicado"))
        val api = api { request ->
            when (request.method) {
                HttpMethod.Post -> respondError(HttpStatusCode.Conflict)
                HttpMethod.Get -> respond(paginaVacia(), HttpStatusCode.OK, jsonHeaders())
                else -> error("Solicitud no esperada")
            }
        }

        val resultado = ProductoRepositorioOffline(local, api, 1L).sincronizar()

        val fila = local.observar().first().single { it.id == pendiente.id }
        assertTrue(resultado.exceptionOrNull() is ErrorApiException)
        assertEquals(ProductoRepositorioOffline.MAX_INTENTOS, fila.intentos)
        assertTrue(fila.ultimoError?.isNotBlank() == true)
        assertEquals(0L, local.observarPendientes(ProductoRepositorioOffline.MAX_INTENTOS).first())
        assertEquals(1L, local.observarFallidas(ProductoRepositorioOffline.MAX_INTENTOS).first())
    }

    @Test
    fun validacionDelServidorPermaneceEnColaYSePropagaAlFormulario() = runTest {
        val local = ProductoLocal(baseEnMemoria())
        val api = api { request ->
            when (request.method) {
                HttpMethod.Post -> respond(
                    """{"message":"Existen errores de validación","validationErrors":{"precio":"El precio debe ser mayor o igual a 0.01"}}""",
                    HttpStatusCode.BadRequest,
                    jsonHeaders()
                )
                HttpMethod.Get -> respond(paginaVacia(), HttpStatusCode.OK, jsonHeaders())
                else -> error("Solicitud no esperada")
            }
        }

        val resultado = ProductoRepositorioOffline(local, api, 1L).registrar(producto("Precio cero").copy(precio = 0.0))

        val error = resultado.exceptionOrNull() as? ErrorApiException
        val validacion = error?.error as? ErrorApi.Validacion
        assertNotNull(validacion)
        assertEquals("El precio debe ser mayor o igual a 0.01", validacion.porCampo["precio"])
        assertEquals(1L, local.observarFallidas(ProductoRepositorioOffline.MAX_INTENTOS).first())
    }

    @Test
    fun esperaCrecienteTieneMaximoDeDieciseisSegundos() {
        assertEquals(1_000L, ProductoRepositorioOffline.esperaCreciente(0))
        assertEquals(2_000L, ProductoRepositorioOffline.esperaCreciente(1))
        assertEquals(16_000L, ProductoRepositorioOffline.esperaCreciente(4))
        assertEquals(16_000L, ProductoRepositorioOffline.esperaCreciente(99))
    }

    @Test
    fun subePendientesAntesDeDescargarElCatalogo() = runTest {
        val local = ProductoLocal(baseEnMemoria())
        val pendiente = local.guardarPendiente(producto("Orden de sincronizacion"))
        val orden = mutableListOf<HttpMethod>()
        val api = api { request ->
            orden += request.method
            when (request.method) {
                HttpMethod.Post -> respond(productoRespuesta(71, pendiente.nombre), HttpStatusCode.OK, jsonHeaders())
                HttpMethod.Get -> respond(paginaCon(71, pendiente.nombre), HttpStatusCode.OK, jsonHeaders())
                else -> error("Solicitud no esperada")
            }
        }

        ProductoRepositorioOffline(local, api, 1L).sincronizar().getOrThrow()

        assertEquals(listOf(HttpMethod.Post, HttpMethod.Get), orden)
        assertEquals(0L, local.observarPendientes(ProductoRepositorioOffline.MAX_INTENTOS).first())
    }

    @Test
    fun conflictoConMismoNombreSeConciliaSinCrearDuplicado() = runTest {
        val local = ProductoLocal(baseEnMemoria())
        val pendiente = local.guardarPendiente(producto("Producto ya recibido"))
        val api = api { request ->
            when (request.method) {
                HttpMethod.Post -> respondError(HttpStatusCode.Conflict)
                HttpMethod.Get -> respond(paginaCon(88, pendiente.nombre), HttpStatusCode.OK, jsonHeaders())
                else -> error("Solicitud no esperada")
            }
        }

        ProductoRepositorioOffline(local, api, 1L).sincronizar().getOrThrow()

        val productos = local.observar().first()
        assertEquals(listOf(88L), productos.map { it.id })
        assertEquals(0L, local.observarPendientes(ProductoRepositorioOffline.MAX_INTENTOS).first())
        assertEquals(0L, local.observarFallidas(ProductoRepositorioOffline.MAX_INTENTOS).first())
    }

    private fun api(responder: io.ktor.client.engine.mock.MockRequestHandler) =
        ProductoApi(crearHttpClient(MockEngine(responder), "http://localhost/api/v1/"))

    private fun producto(nombre: String) = Producto(
        id = 0L, nombre = nombre, precio = 10.0, stock = 5, categoriaId = 1L, categoria = "Pruebas"
    )

    private fun paginaVacia() = """{"contenido":[],"pagina":0,"tamanio":100,"totalElementos":0,"totalPaginas":0,"ultima":true}"""
    private fun paginaCon(id: Long, nombre: String) = """{"contenido":[${productoRespuesta(id, nombre)}],"pagina":0,"tamanio":100,"totalElementos":1,"totalPaginas":1,"ultima":true}"""
    private fun productoRespuesta(id: Long, nombre: String) = """{"id":$id,"nombre":"$nombre","precio":10.0,"stock":5,"estado":true,"categoriaId":1,"categoriaNombre":"Pruebas"}"""
    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
}
