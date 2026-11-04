package pe.edu.upeu.pharmamobilee.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics
import pe.edu.upeu.pharmamobilee.domain.port.Telemetria

/** Implementación Firebase que solo conserva contexto técnico, nunca PII ni credenciales. */
class TelemetriaFirebase : Telemetria {
    override fun registrarError(error: Throwable, contexto: Map<String, String>) {
        aplicarContexto(contextoTecnicoSeguro(contexto))
        Firebase.crashlytics.recordException(
            IllegalStateException("Fallo técnico: ${error::class.simpleName ?: "desconocido"}")
        )
    }

    override fun registrarEvento(nombre: String, datos: Map<String, String>) {
        val evento = nombre.seguroParaTelemetria()
        aplicarContexto(contextoTecnicoSeguro(datos))
        Firebase.crashlytics.setCustomKey("evento", evento)
        Firebase.crashlytics.log("evento=$evento")
    }

    private fun aplicarContexto(contexto: Map<String, String>) {
        contexto.forEach { (clave, valor) -> Firebase.crashlytics.setCustomKey(clave, valor) }
    }
}

private val clavesSensibles = setOf(
    "token", "authorization", "jwt", "password", "contrasena", "correo",
    "email", "usuario", "nombre", "dni", "telefono", "direccion"
)

internal fun contextoTecnicoSeguro(contexto: Map<String, String>): Map<String, String> =
    contexto.mapNotNull { (clave, valor) ->
        val claveNormalizada = clave.trim().lowercase()
        if (clavesSensibles.any(claveNormalizada::contains)) null
        else claveNormalizada.take(40) to valor.seguroParaTelemetria()
    }.toMap()

private fun String.seguroParaTelemetria(): String =
    replace('\n', ' ').replace('\r', ' ').trim().take(120)
