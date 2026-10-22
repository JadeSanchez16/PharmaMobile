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
import pe.edu.upeu.pharmamobilee.data.local.FuenteProductosLocal
import pe.edu.upeu.pharmamobilee.data.remote.GestorSesionHttp
import pe.edu.upeu.pharmamobilee.domain.model.Credenciales
import pe.edu.upeu.pharmamobilee.domain.model.ErrorSesion
import pe.edu.upeu.pharmamobilee.domain.model.Sesion
import pe.edu.upeu.pharmamobilee.domain.model.Producto
import pe.edu.upeu.pharmamobilee.domain.repository.AuthRepository
import java.io.File

/** Prueba contra el backend real, instalada en un APK de pruebas separado.
 * Requiere iniciar sesión como ALMACENERO desde la aplicación antes de ejecutarla.
 * No recibe credenciales, no cambia permisos y no modifica datos del servidor.
 */
class Sesion403Instrumentation : Instrumentation() {
    private var escenario: String? = null
    private var usuarioPrueba: String? = null
    private var clavePrueba: String? = null

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        escenario = arguments?.getString("escenario")
        usuarioPrueba = arguments?.getString("usuario")
        clavePrueba = arguments?.getString("clave")
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
            if (escenario == "aa11-e05-preparar") {
                // Se ejecuta con el Pixel en modo avión. El valor no es un JWT real y
                // nunca se registra: sirve para demostrar que el perfil local permite
                // abrir el catálogo mientras no haya red. Al reconectar, PharmaSoft
                // lo rechaza y el flujo 401 existente limpia la sesión.
                almacen.guardarToken("token-invalido-controlado-aa11")
                almacen.guardarPerfilSesion(
                    Sesion(
                        token = "token-invalido-controlado-aa11",
                        usuario = "AA11_OFFLINE",
                        nombreCompleto = "Escenario offline controlado",
                        roles = setOf("ADMIN"),
                        permisos = setOf("CATALOGO_ESCRIBIR")
                    )
                )
                val local = koin.get<FuenteProductosLocal>()
                local.guardarSincronizado(
                    Producto(
                        id = 11_021,
                        nombre = "AA11_CATÁLOGO_LOCAL",
                        precio = 1.50,
                        stock = 16,
                        categoria = "Analgésicos",
                        categoriaId = 1L
                    )
                )
                val activity = startActivitySync(
                    Intent(targetContext, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                runOnMainSync { activity.recreate() }
                Log.i("AA11-E05", "Perfil local preparado; token omitido del registro")
                resultado.putString("stream", "PASS: perfil local preparado para E05; token omitido.\n")
                finish(Activity.RESULT_OK, resultado)
                return
            }
            if (escenario == "e05") {
                val usuario = checkNotNull(usuarioPrueba) { "Falta usuario de desarrollo" }
                val clave = checkNotNull(clavePrueba) { "Falta clave de desarrollo" }
                val texto = verificarPrefijoBearerMinusculaReal(almacen, auth, usuario, clave)
                resultado.putString("stream", "$texto\n")
                finish(Activity.RESULT_OK, resultado)
                return
            }
            if (escenario == "logout") {
                val texto = verificarLogoutReal(almacen, auth, cliente)
                resultado.putString("stream", "$texto\n")
                finish(Activity.RESULT_OK, resultado)
                return
            }
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
