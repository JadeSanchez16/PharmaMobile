package pe.edu.upeu.pharmamobilee.data.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobilee.data.remote.dto.ProductoRequestDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductoApiCrudTest {

    @Test
    fun listarUsaGetConPaginacionYDeserializaElEnvoltorio() = runTest {
        val engine = MockEngine { solicitud ->
            assertEquals(HttpMethod.Get, solicitud.method)
            assertEquals("/api/v1/productos", solicitud.url.encodedPath)
            assertEquals("2", solicitud.url.parameters["pagina"])
            assertEquals("15", solicitud.url.parameters["tamanio"])
            respond(
                content = paginatedResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.toString()
                )
            )
        }
        val api = ProductoApi(
            crearHttpClient(engine, "http://localhost/api/v1/")
        )

        val pagina = api.listar(pagina = 2, tamanio = 15)

        assertEquals(2, pagina.pagina)
        assertEquals(15, pagina.tamanio)
        assertEquals(31L, pagina.totalElementos)
        assertEquals(3, pagina.totalPaginas)
        assertTrue(pagina.ultima)
        assertEquals("Producto", pagina.contenido.single().nombre)
    }

    @Test
    fun obtenerUsaGetConElId() = runTest {
        val api = apiQueVerifica(HttpMethod.Get, "/api/v1/productos/7")
        assertEquals(7L, api.obtener(7).id)
    }

    @Test
    fun crearUsaPostYDeserializa201() = runTest {
        val api = apiQueVerifica(
            method = HttpMethod.Post,
            path = "/api/v1/productos",
            status = HttpStatusCode.Created
        )
        assertEquals(7L, api.crear(request).id)
    }

    @Test
    fun actualizarUsaPutConElId() = runTest {
        val api = apiQueVerifica(HttpMethod.Put, "/api/v1/productos/7")
        assertEquals(7L, api.actualizar(7, request).id)
    }

    @Test
    fun eliminarUsaDeleteYSobre204NoLeeBody() = runTest {
        val engine = MockEngine { solicitud ->
            assertEquals(HttpMethod.Delete, solicitud.method)
            assertEquals("/api/v1/productos/7", solicitud.url.encodedPath)
            respond(content = "", status = HttpStatusCode.NoContent)
        }
        val api = ProductoApi(
            crearHttpClient(engine, "http://localhost/api/v1/")
        )

        api.eliminar(7)
    }

    private fun apiQueVerifica(
        method: HttpMethod,
        path: String,
        status: HttpStatusCode = HttpStatusCode.OK
    ): ProductoApi {
        val engine = MockEngine { request ->
            assertEquals(method, request.method)
            assertEquals(path, request.url.encodedPath)
            respond(
                content = response,
                status = status,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.toString()
                )
            )
        }
        return ProductoApi(
            crearHttpClient(engine, "http://localhost/api/v1/")
        )
    }

    private companion object {
        val request = ProductoRequestDto(
            nombre = "Producto",
            precio = 4.5,
            stock = 10,
            estado = true,
            categoriaId = 1
        )

        val response = """
            {
              "id": 7,
              "nombre": "Producto",
              "precio": 4.5,
              "stock": 10,
              "estado": true,
              "categoriaId": 1,
              "categoriaNombre": "Pruebas"
            }
        """.trimIndent()

        val paginatedResponse = """
            {
              "contenido": [$response],
              "pagina": 2,
              "tamanio": 15,
              "totalElementos": 31,
              "totalPaginas": 3,
              "ultima": true
            }
        """.trimIndent()
    }
}
