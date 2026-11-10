package pe.edu.upeu.pharmamobilee.domain.model

sealed class ErrorSesion(
    override val message: String
) : Exception(message) {
    data object CredencialesInvalidas : ErrorSesion(
        "Usuario o contraseña incorrectos."
    )

    data object SesionExpirada : ErrorSesion(
        "La sesión expiró. Inicia sesión nuevamente."
    )

    data object SinPermiso : ErrorSesion(
        "No tienes permisos para realizar esta operación."
    )

    data object SinConexion : ErrorSesion(
        "No se pudo conectar con PharmaSoft. Verifica el servicio e inténtalo nuevamente."
    )

    data class Servidor(val detalle: String) : ErrorSesion(detalle)
}
