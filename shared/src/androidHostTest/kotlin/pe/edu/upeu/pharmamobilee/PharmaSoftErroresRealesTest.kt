package pe.edu.upeu.pharmamobilee

import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import pe.edu.upeu.pharmamobilee.data.remote.ProductoApi
import pe.edu.upeu.pharmamobilee.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobilee.data.repository.ProductoRepositoryImpl
import pe.edu.upeu.pharmamobilee.domain.error.ErrorApi
import pe.edu.upeu.pharmamobilee.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobilee.domain.model.Producto
import kotlin.test.assertEquals
import kotlin.test.assertIs
import java.time.OffsetDateTime

/** Integración optativa, sin altas ni modificaciones: requiere PharmaSoft local. */
class PharmaSoftErroresRealesTest {
    @Before fun requiereBackendExplicito() {
        assumeTrue("Activar PHARMASOFT_PRUEBAS_REALES=true", System.getenv("PHARMASOFT_PRUEBAS_REALES") == "true")
    }

    private fun comprobar(
        escenario: String,
        timeout: Long = 15_000,
        bloque: suspend (ProductoRepositoryImpl) -> Result<*>
    ): ErrorApiException = runBlocking {
        val client = crearHttpClient(OkHttp.create(), "http://localhost:8080/api/v1/", timeout)
        try {
            val inicio = OffsetDateTime.now()
            val fallo = assertIs<ErrorApiException>(bloque(ProductoRepositoryImpl(ProductoApi(client), 1L)).exceptionOrNull())
            println("ESCENARIO=$escenario | fecha=$inicio | plataforma=Android host JVM/Windows | backend=PharmaSoft real")
            println("HTTP=" + ((fallo.cause as? ClientRequestException)?.response?.status?.value ?: "sin respuesta HTTP"))
            println("ErrorApi=${fallo.error} | mensaje=${fallo.message} | fin=${OffsetDateTime.now()}")
            fallo
        } finally {
            client.close()
        }
    }

    @Test fun precioCeroServidor400Validacion() {
        val error = comprobar("E02 POST precio=0") {
            it.registrar(Producto(0L, "AA08_PRECIO_INVALIDO", 0.0, 1, categoriaId = 1L))
        }
        assertEquals(400, (error.cause as ClientRequestException).response.status.value)
        assertEquals("El precio debe ser mayor que cero", assertIs<ErrorApi.Validacion>(error.error).porCampo["precio"])
    }

    @Test fun actualizarInexistente404NoEncontrado() {
        val error = comprobar("E03 PUT /productos/999999") {
            it.actualizar(Producto(999999L, "AA08_INEXISTENTE", 3.0, 1, categoriaId = 1L))
        }
        assertEquals(404, (error.cause as ClientRequestException).response.status.value)
        assertIs<ErrorApi.NoEncontrado>(error.error)
    }

    @Test fun nombreDuplicado409Conflicto() = runBlocking {
        val client = crearHttpClient(OkHttp.create(), "http://localhost:8080/api/v1/")
        val existente = try { ProductoApi(client).obtener(41L) } finally { client.close() }
        // Registro temporal de la Guía 08; nunca se elimina un producto preexistente.
        assumeTrue(existente.nombre.startsWith("S08_QA_"))
        val error = comprobar("E05 POST nombre duplicado del temporal 41") {
            it.registrar(Producto(0L, existente.nombre, 3.0, 1, categoriaId = 1L))
        }
        assertEquals(409, (error.cause as ClientRequestException).response.status.value)
        assertEquals("Ya existe un producto con el nombre " + existente.nombre, assertIs<ErrorApi.Conflicto>(error.error).mensaje)
    }

    @Test fun timeoutReal1msTiempoAgotado() {
        val error = comprobar("E07 GET /productos timeout=1ms", timeout = 1L) { it.listar() }
        assertIs<ErrorApi.TiempoAgotado>(error.error)
    }
}
