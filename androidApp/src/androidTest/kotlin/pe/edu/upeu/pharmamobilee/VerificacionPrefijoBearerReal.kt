package pe.edu.upeu.pharmamobilee

import android.util.Log
import kotlinx.coroutines.runBlocking
import pe.edu.upeu.pharmamobilee.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobilee.domain.model.Credenciales
import pe.edu.upeu.pharmamobilee.domain.repository.AuthRepository
import java.net.HttpURLConnection
import java.net.URL

/** Ejecuta E05 en el emulador con un JWT de desarrollo solo en memoria y lo omite del log. */
internal fun verificarPrefijoBearerMinusculaReal(
    almacen: AlmacenSeguro,
    auth: AuthRepository,
    usuario: String,
    clave: String
): String = runBlocking {
    try {
        auth.iniciarSesion(Credenciales(usuario, clave))
        val token = checkNotNull(almacen.leerToken()) { "El login no guardó el token" }
        val conexion = URL("http://10.0.2.2:8080/api/v1/auth/me").openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = "GET"
            conexion.setRequestProperty("Authorization", "bearer $token")
            val estado = conexion.responseCode
            val cuerpo = conexion.errorStream.bufferedReader().use { it.readText() }
            check(estado == 401) { "E05 debía devolver HTTP 401" }
            check(cuerpo.contains("Debe iniciar sesión para acceder a este recurso")) {
                "E05 devolvió un message distinto"
            }
            Log.i(
                "AA10-E05",
                "GET /api/v1/auth/me; Authorization: bearer ***; HTTP 401; " +
                    "message: Debe iniciar sesión para acceder a este recurso"
            )
        } finally {
            conexion.disconnect()
        }
    } finally {
        auth.cerrarSesion()
    }
    "PASS E05: Android envió Authorization: bearer ***; PharmaSoft respondió HTTP 401 y message literal esperado."
}
