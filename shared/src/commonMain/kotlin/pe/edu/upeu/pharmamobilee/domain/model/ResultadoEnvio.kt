package pe.edu.upeu.pharmamobilee.domain.model

import pe.edu.upeu.pharmamobilee.domain.error.ErrorApi

/** Resultado de un intento de envío de una fila pendiente. */
sealed interface ResultadoEnvio {
    data class Exito(val idServidor: Long) : ResultadoEnvio
    data class Transitorio(val motivo: String) : ResultadoEnvio
    /** Conserva el tipo de error para que la interfaz pueda asociarlo al campo correcto. */
    data class Permanente(
        val motivo: String,
        val error: ErrorApi
    ) : ResultadoEnvio
}
