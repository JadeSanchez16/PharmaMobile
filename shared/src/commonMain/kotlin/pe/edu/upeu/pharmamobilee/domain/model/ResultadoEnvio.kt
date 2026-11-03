package pe.edu.upeu.pharmamobilee.domain.model

/** Resultado de un intento de envío de una fila pendiente. */
sealed interface ResultadoEnvio {
    data class Exito(val idServidor: Long) : ResultadoEnvio
    data class Transitorio(val motivo: String) : ResultadoEnvio
    data class Permanente(val motivo: String) : ResultadoEnvio
}
