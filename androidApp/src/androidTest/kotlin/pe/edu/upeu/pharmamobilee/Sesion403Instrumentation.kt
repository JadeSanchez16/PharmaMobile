package pe.edu.upeu.pharmamobilee

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext
import pe.edu.upeu.pharmamobilee.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobilee.data.remote.GestorSesionHttp
import pe.edu.upeu.pharmamobilee.domain.model.ErrorSesion
import pe.edu.upeu.pharmamobilee.domain.repository.AuthRepository
import java.io.File

/** Prueba contra el backend real, instalada en un APK de pruebas separado.
 * Requiere iniciar sesión como ALMACENERO desde la aplicación antes de ejecutarla.
 * No recibe credenciales, no cambia permisos y no modifica datos del servidor.
 */
class Sesion403Instrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        start()
    }

    override fun onStart() {
        val resultado = Bundle()
        try {
            waitForIdleSync()
            check(targetContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
            Log.i("S10-PruebaReal", "Inicio: APK debug; esperando inyección de la aplicación")
            repeat(100) {
                if (GlobalContext.getOrNull() == null) SystemClock.sleep(100)
            }
            val koin = GlobalContext.get()
            val almacen = koin.get<AlmacenSeguro>()
            val gestor = koin.get<GestorSesionHttp>()
            val auth = koin.get<AuthRepository>()
            val cliente = koin.get<HttpClient>()
            val tokenAntes = checkNotNull(almacen.leerToken()) { "Iniciar sesión como ALMACENERO primero" }
            Log.i("S10-PruebaReal", "Almacén seguro: token presente, valor omitido")
            runBlocking {
                check(auth.sesionGuardada()?.roles?.contains("ALMACENERO") == true)
            }
            startActivitySync(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            check(esperarTexto("Cerrar sesión")) { "No se recuperó la sesión en la interfaz" }
            runBlocking {
                val error = runCatching { cliente.get("clientes") }.exceptionOrNull()
                check(error is ClientRequestException && error.response.status.value == 403)
                check(gestor.sinPermiso.value == ErrorSesion.SinPermiso)
            }
            check(esperarTexto(ErrorSesion.SinPermiso.message)) { "El aviso 403 no es visible" }
            check(almacen.leerToken() == tokenAntes) { "El 403 modificó el token" }
            guardarCaptura("S10_403_Android.png")
            runBlocking {
                check(auth.sesionGuardada()?.roles?.contains("ALMACENERO") == true)
                check(cliente.get("productos").status.value == 200)
            }
            Log.i("S10-PruebaReal", "PASS: clientes=403; SinPermiso visible; token conservado; auth/me=200; productos=200")
            resultado.putString("stream", "PASS: 1 escenario real Android; HTTP 403, aviso visible, sesión conservada y productos HTTP 200.\n")
            finish(Activity.RESULT_OK, resultado)
        } catch (error: Throwable) {
            // Solo tipo y mensaje de las comprobaciones; nunca solicitudes ni tokens.
            Log.e("S10-PruebaReal", "FAIL: ${error::class.simpleName}" + if (error is IllegalStateException) " (${error.message})" else "")
            resultado.putString("stream", "FAIL: ${error::class.simpleName}; revisar Logcat saneado.\n")
            finish(Activity.RESULT_CANCELED, resultado)
        }
    }

    private fun esperarTexto(texto: String): Boolean {
        repeat(100) {
            if (contiene(uiAutomation.rootInActiveWindow, texto)) return true
            SystemClock.sleep(100)
        }
        return false
    }

    private fun contiene(nodo: AccessibilityNodeInfo?, texto: String): Boolean {
        if (nodo == null) return false
        if (nodo.text?.toString()?.contains(texto) == true || nodo.contentDescription?.toString()?.contains(texto) == true) return true
        return (0 until nodo.childCount).any { contiene(nodo.getChild(it), texto) }
    }

    private fun guardarCaptura(nombre: String) {
        val captura = checkNotNull(uiAutomation.takeScreenshot())
        File(targetContext.filesDir, nombre).outputStream().use {
            check(captura.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        captura.recycle()
    }
}
