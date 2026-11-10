package pe.edu.upeu.pharmamobilee.data.remote

/** Punto de registro nativo para que las solicitudes Ktor sean visibles en cada plataforma. */
expect fun registrarKtor(mensaje: String)
