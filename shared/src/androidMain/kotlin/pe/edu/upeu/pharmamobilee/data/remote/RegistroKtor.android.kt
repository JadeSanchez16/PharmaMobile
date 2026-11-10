package pe.edu.upeu.pharmamobilee.data.remote

import android.util.Log

/** Logcat conserva un tag estable y la configuración Ktor sanea Authorization antes de llegar aquí. */
actual fun registrarKtor(mensaje: String) {
    // android.util.Log no está implementado en las pruebas JVM Android Host.
    // En el emulador la primera rama emite el tag auténtico para Logcat.
    runCatching { Log.i("PharmaMobile-Ktor", mensaje) }
        .getOrElse { println("PharmaMobile-Ktor: $mensaje") }
}
