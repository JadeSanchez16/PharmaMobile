package pe.edu.upeu.pharmamobilee

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import pe.edu.upeu.pharmamobilee.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobilee.domain.repository.AuthRepository

/** Usa el mismo cliente Ktor de la app antes y después del logout real. */
internal fun verificarLogoutReal(
    almacen: AlmacenSeguro,
    auth: AuthRepository,
    cliente: HttpClient
): String {
    check(almacen.leerToken() != null) { "Falta sesión de desarrollo previa" }
    runBlocking {
        check(cliente.get("auth/me").status.value == 200)
        Log.i("AA10-Logout", "Antes: GET /auth/me HTTP 200; JWT omitido")

        auth.cerrarSesion()
        check(almacen.leerToken() == null)
        Log.i("AA10-Logout", "Logout: almacén vacío y clearToken invocado")

        val error = runCatching { cliente.get("auth/me") }.exceptionOrNull()
        check(error is ClientRequestException && error.response.status.value == 401) {
            "La petición del mismo cliente tras logout no recibió HTTP 401"
        }
        Log.i("AA10-Logout", "Después: mismo cliente GET /auth/me HTTP 401; revisar cabeceras Ktor saneadas")
    }
    return "PASS: mismo cliente Ktor GET /auth/me 200 antes; logout borra almacén; GET /auth/me 401 después."
}
