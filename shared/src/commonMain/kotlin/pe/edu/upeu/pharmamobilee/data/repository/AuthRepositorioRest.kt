package pe.edu.upeu.pharmamobilee.data.repository

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import pe.edu.upeu.pharmamobilee.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobilee.data.remote.AuthApi
import pe.edu.upeu.pharmamobilee.data.remote.GestorSesionHttp
import pe.edu.upeu.pharmamobilee.data.remote.dto.ErrorResponseDto
import pe.edu.upeu.pharmamobilee.data.remote.dto.LoginRequestDto
import pe.edu.upeu.pharmamobilee.data.remote.dto.LoginResponseDto
import pe.edu.upeu.pharmamobilee.data.remote.dto.UsuarioActualDto
import pe.edu.upeu.pharmamobilee.domain.model.Credenciales
import pe.edu.upeu.pharmamobilee.domain.model.ErrorSesion
import pe.edu.upeu.pharmamobilee.domain.model.Sesion
import pe.edu.upeu.pharmamobilee.domain.repository.AuthRepository
import io.ktor.client.call.body

class AuthRepositorioRest(
    private val api: AuthApi,
    private val almacen: AlmacenSeguro,
    private val gestorSesionHttp: GestorSesionHttp
) : AuthRepository {
    override suspend fun iniciarSesion(credenciales: Credenciales): Sesion =
        ejecutarAutenticacion(esLogin = true) {
            val respuesta = api.login(
                LoginRequestDto(
                    username = credenciales.usuario,
                    password = credenciales.clave
                )
            )
            val sesion = respuesta.aSesion()
            almacen.guardarToken(respuesta.token)
            almacen.guardarPerfilSesion(sesion)
            gestorSesionHttp.tokenActualizado()
            sesion
        }

    override suspend fun sesionGuardada(): Sesion? {
        val token = almacen.leerToken() ?: return null
        return try {
            ejecutarAutenticacion(esLogin = false) {
                api.usuarioActual().aSesion(token).also(almacen::guardarPerfilSesion)
            }
        } catch (error: ErrorSesion.SinConexion) {
            // Sin red no invalida un JWT almacenado ni el catálogo local. La siguiente
            // operación protegida seguirá validándolo contra PharmaSoft.
            almacen.leerPerfilSesion()?.copy(token = token) ?: throw error
        }
    }

    override suspend fun cerrarSesion() {
        almacen.borrarToken()
        gestorSesionHttp.tokenActualizado()
    }

    private suspend fun <T> ejecutarAutenticacion(
        esLogin: Boolean,
        bloque: suspend () -> T
    ): T = try {
        bloque()
    } catch (cancelacion: CancellationException) {
        throw cancelacion
    } catch (error: ClientRequestException) {
        val mensaje = runCatching {
            error.response.body<ErrorResponseDto>().message
        }.getOrNull()
        throw when (error.response.status.value) {
            401 -> if (esLogin) {
                ErrorSesion.CredencialesInvalidas
            } else {
                almacen.borrarToken()
                ErrorSesion.SesionExpirada
            }
            403 -> ErrorSesion.SinPermiso
            else -> ErrorSesion.Servidor(
                mensaje ?: "PharmaSoft rechazó la solicitud."
            )
        }
    } catch (_: HttpRequestTimeoutException) {
        throw ErrorSesion.SinConexion
    } catch (_: SocketTimeoutException) {
        throw ErrorSesion.SinConexion
    } catch (_: IOException) {
        throw ErrorSesion.SinConexion
    } catch (_: JsonConvertException) {
        throw ErrorSesion.Servidor("PharmaSoft devolvió una respuesta no válida.")
    }
}

private fun LoginResponseDto.aSesion(): Sesion = Sesion(
    token = token,
    usuario = username,
    nombreCompleto = nombreCompleto,
    roles = roles.toSet(),
    permisos = permisos.toSet()
)

private fun UsuarioActualDto.aSesion(token: String): Sesion = Sesion(
    token = token,
    usuario = username,
    nombreCompleto = empleadoNombre,
    roles = roles.toSet(),
    permisos = permisos.toSet()
)
