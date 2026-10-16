package pe.edu.upeu.pharmamobilee.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobilee.data.local.AlmacenSeguroVolatil
import pe.edu.upeu.pharmamobilee.data.remote.AuthApi
import pe.edu.upeu.pharmamobilee.data.remote.GestorSesionHttp
import pe.edu.upeu.pharmamobilee.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobilee.domain.model.Credenciales
import pe.edu.upeu.pharmamobilee.domain.model.ErrorSesion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthRepositorioRestTest {
    @Test
    fun loginGuardaTokenYElClienteLoAdjuntaAutomaticamente() = runTest {
        val almacen = AlmacenSeguroVolatil()
        val gestor = GestorSesionHttp(almacen)
        var cabeceraLogin: String? = "sin ejecutar"
        var cabeceraProducto: String? = null
        val motor = MockEngine { solicitud ->
            when (solicitud.url.encodedPath) {
                "/api/v1/auth/login" -> {
                    cabeceraLogin = solicitud.headers[HttpHeaders.Authorization]
                    respond(
                        content = LOGIN_OK,
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders
                    )
                }
                "/api/v1/productos" -> {
                    cabeceraProducto = solicitud.headers[HttpHeaders.Authorization]
                    respond("{}", HttpStatusCode.OK, jsonHeaders)
                }
                else -> error("Ruta inesperada: ${solicitud.url}")
            }
        }
        val cliente = crearHttpClient(
            engine = motor,
            urlBase = "http://localhost/api/v1/",
            almacenSeguro = almacen,
            gestorSesionHttp = gestor,
            esDepuracion = false
        )
        val repository = AuthRepositorioRest(AuthApi(cliente), almacen, gestor)

        val sesion = repository.iniciarSesion(Credenciales("admin", "secreto"))
        cliente.get("productos")

        assertNull(cabeceraLogin)
        assertEquals("token-prueba", almacen.leerToken())
        assertEquals("Bearer token-prueba", cabeceraProducto)
        assertTrue(sesion.puede("CATALOGO_ESCRIBIR"))
    }

    @Test
    fun sesionGuardadaValidaTokenContraAuthMe() = runTest {
        val almacen = AlmacenSeguroVolatil().apply { guardarToken("token-persistido") }
        val gestor = GestorSesionHttp(almacen)
        val motor = MockEngine { solicitud ->
            assertEquals("Bearer token-persistido", solicitud.headers[HttpHeaders.Authorization])
            respond(ME_OK, HttpStatusCode.OK, jsonHeaders)
        }
        val cliente = crearHttpClient(
            motor,
            "http://localhost/api/v1/",
            almacenSeguro = almacen,
            gestorSesionHttp = gestor,
            esDepuracion = false
        )
        val repository = AuthRepositorioRest(AuthApi(cliente), almacen, gestor)

        val sesion = repository.sesionGuardada()

        assertEquals("admin", sesion?.usuario)
        assertEquals(setOf("ADMIN"), sesion?.roles)
    }

    @Test
    fun credencialesInvalidasSeTraducenSinGuardarToken() = runTest {
        val almacen = AlmacenSeguroVolatil()
        val gestor = GestorSesionHttp(almacen)
        val cliente = crearHttpClient(
            MockEngine {
                respondError(
                    HttpStatusCode.Unauthorized,
                    """{"message":"Credenciales inválidas"}""",
                    jsonHeaders
                )
            },
            "http://localhost/api/v1/",
            almacenSeguro = almacen,
            gestorSesionHttp = gestor,
            esDepuracion = false
        )
        val repository = AuthRepositorioRest(AuthApi(cliente), almacen, gestor)

        assertFailsWith<ErrorSesion.CredencialesInvalidas> {
            repository.iniciarSesion(Credenciales("admin", "incorrecta"))
        }
        assertNull(almacen.leerToken())
    }

    @Test
    fun respuesta401ProtegidaBorraTokenY403LoConserva() = runTest {
        suspend fun ejecutar(estado: HttpStatusCode): String? {
            val almacen = AlmacenSeguroVolatil().apply { guardarToken("token-vigente") }
            val gestor = GestorSesionHttp(almacen)
            val cliente = crearHttpClient(
                MockEngine { respondError(estado, "{}", jsonHeaders) },
                "http://localhost/api/v1/",
                almacenSeguro = almacen,
                gestorSesionHttp = gestor,
                esDepuracion = false
            )
            runCatching { cliente.get("productos") }
            if (estado == HttpStatusCode.Forbidden) {
                assertEquals(ErrorSesion.SinPermiso, gestor.sinPermiso.value)
                gestor.descartarSinPermiso()
                assertNull(gestor.sinPermiso.value)
                assertEquals("token-vigente", almacen.leerToken())
                gestor.notificarSinPermiso()
                gestor.tokenActualizado()
                assertNull(gestor.sinPermiso.value)
            } else {
                assertNull(gestor.sinPermiso.value)
            }
            cliente.close()
            return almacen.leerToken()
        }

        assertNull(ejecutar(HttpStatusCode.Unauthorized))
        assertEquals("token-vigente", ejecutar(HttpStatusCode.Forbidden))
    }

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        const val LOGIN_OK = """
            {
              "token":"token-prueba",
              "tipo":"Bearer",
              "expiraEnSegundos":3600,
              "username":"admin",
              "nombreCompleto":"Administrador",
              "roles":["ADMIN"],
              "permisos":["CATALOGO_LEER","CATALOGO_ESCRIBIR"]
            }
        """
        const val ME_OK = """
            {
              "id":1,
              "username":"admin",
              "empleadoId":1,
              "empleadoNombre":"Administrador",
              "cargo":"Administrador",
              "roles":["ADMIN"],
              "permisos":["CATALOGO_LEER","CATALOGO_ESCRIBIR"],
              "estado":true,
              "fechaCreacion":"2026-10-13T10:00:00"
            }
        """
    }
}
