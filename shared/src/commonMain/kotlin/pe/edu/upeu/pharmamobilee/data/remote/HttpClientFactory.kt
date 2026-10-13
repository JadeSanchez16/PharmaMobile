package pe.edu.upeu.pharmamobilee.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pe.edu.upeu.pharmamobilee.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobilee.data.local.AlmacenSeguroVolatil

fun crearHttpClient(
    engine: HttpClientEngine,
    urlBase: String,
    tiempoEsperaSolicitudMillis: Long = 15_000,
    ignorarCamposDesconocidos: Boolean = true,
    almacenSeguro: AlmacenSeguro = AlmacenSeguroVolatil(),
    gestorSesionHttp: GestorSesionHttp = GestorSesionHttp(almacenSeguro),
    esDepuracion: Boolean = true
): HttpClient = HttpClient(engine) {
    expectSuccess = true

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = ignorarCamposDesconocidos
                isLenient = true
                encodeDefaults = true
            }
        )
    }

    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                println("PharmaMobile-Ktor: $message")
            }
        }
        level = if (esDepuracion) LogLevel.HEADERS else LogLevel.NONE
        sanitizeHeader { nombre ->
            nombre.equals(HttpHeaders.Authorization, ignoreCase = true)
        }
    }

    install(Auth) {
        bearer {
            loadTokens {
                almacenSeguro.leerToken()?.let { token ->
                    BearerTokens(token, "")
                }
            }
            sendWithoutRequest { peticion ->
                !peticion.url.encodedPath.endsWith("/auth/login")
            }
        }
    }

    install(HttpTimeout) {
        requestTimeoutMillis = tiempoEsperaSolicitudMillis
        connectTimeoutMillis = 10_000
    }

    defaultRequest {
        url(urlBase)
        contentType(ContentType.Application.Json)
    }

    HttpResponseValidator {
        validateResponse { respuesta ->
            val esLogin = respuesta.call.request.url.encodedPath.endsWith("/auth/login")
            if (respuesta.status == HttpStatusCode.Unauthorized && !esLogin) {
                gestorSesionHttp.invalidarSesion()
            }
        }
        handleResponseExceptionWithRequest { causa, peticion ->
            val respuesta = (causa as? ClientRequestException)?.response
            val esLogin = peticion.url.encodedPath.endsWith("/auth/login")
            if (respuesta?.status == HttpStatusCode.Unauthorized && !esLogin) {
                gestorSesionHttp.invalidarSesion()
            }
        }
    }
}.also(gestorSesionHttp::vincular)
