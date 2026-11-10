package pe.edu.upeu.pharmamobilee.domain.port

/**
 * Puerto del dominio para reportar eventos técnicos sin acoplar casos de uso
 * ni pantallas al SDK de Firebase.
 */
interface Telemetria {
    fun registrarError(error: Throwable, contexto: Map<String, String> = emptyMap())
    fun registrarEvento(nombre: String, datos: Map<String, String> = emptyMap())
}
