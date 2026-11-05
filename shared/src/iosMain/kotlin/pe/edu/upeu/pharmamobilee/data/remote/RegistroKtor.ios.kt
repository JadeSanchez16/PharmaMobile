package pe.edu.upeu.pharmamobilee.data.remote

/** Implementación mínima para que el registro de Ktor también exista en iOS. */
actual fun registrarKtor(mensaje: String) {
    println("PharmaMobile-Ktor: $mensaje")
}
