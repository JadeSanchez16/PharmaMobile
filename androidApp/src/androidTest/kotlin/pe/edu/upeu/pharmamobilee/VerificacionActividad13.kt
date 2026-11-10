package pe.edu.upeu.pharmamobilee

import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import pe.edu.upeu.pharmamobilee.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobilee.domain.model.Credenciales
import pe.edu.upeu.pharmamobilee.domain.model.Producto
import pe.edu.upeu.pharmamobilee.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobilee.domain.repository.ProductoRepository
import java.io.File

/** Recorre servicios y pantallas reales; no fabrica HTTP ni modifica permisos. */
internal fun verificarActividad13(i: Instrumentation, escenario: String, usuario: String?, clave: String?): String = runBlocking {
    val koin = GlobalContext.get()
    val auth = koin.get<AuthRepository>()
    val almacen = koin.get<AlmacenSeguro>()
    val repo = koin.get<ProductoRepository>()
    fun pantalla(nombre: String) {
        SystemClock.sleep(1800)
        val bitmap = checkNotNull(i.uiAutomation.takeScreenshot())
        File(i.targetContext.filesDir, "$nombre.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        bitmap.recycle()
    }
    fun abrir() {
        i.startActivitySync(Intent(i.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        SystemClock.sleep(5000)
    }
    fun nodo(n: AccessibilityNodeInfo?, texto: String): AccessibilityNodeInfo? {
        if (n == null) return null
        if (n.text?.toString() == texto || n.contentDescription?.toString() == texto) return n
        for (x in 0 until n.childCount) nodo(n.getChild(x), texto)?.let { return it }
        return null
    }
    fun pulsar(texto: String) {
        repeat(30) {
            if (nodo(i.uiAutomation.rootInActiveWindow,texto)==null && nodo(i.uiAutomation.rootInActiveWindow,"Inventario")==null) SystemClock.sleep(1000)
        }
        var n = nodo(i.uiAutomation.rootInActiveWindow,texto)
        if (n == null && texto == "Productos" && nodo(i.uiAutomation.rootInActiveWindow,"Inventario") != null) return
        check(n != null) { "No se encontró $texto" }
        while (n != null && !n.isClickable) n = n.parent
        check(n?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) { "No se pulsó $texto" }
        SystemClock.sleep(1800)
    }
    fun filtrar(nombre: String) {
        fun editable(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (n == null) return null
            if (n.isEditable) return n
            for (x in 0 until n.childCount) editable(n.getChild(x))?.let { return it }
            return null
        }
        val campo=checkNotNull(editable(i.uiAutomation.rootInActiveWindow))
        val args=Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,nombre) }
        check(campo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args))
        SystemClock.sleep(800)
    }
    when (escenario) {
        "aa13-login" -> {
            val s=auth.iniciarSesion(Credenciales(checkNotNull(usuario),checkNotNull(clave)))
            check(almacen.leerToken()!=null)
            abrir(); pantalla("AA13_02_login")
            check(repo.sincronizar().isSuccess)
            pulsar("Productos"); pantalla("AA13_03_catalogo")
            "PASS login; roles=${s.roles}; privilegios=${s.permisos}; filas=${repo.listar().getOrThrow().size}; ultima=${repo.ultimaSincronizacion()}"
        }
        "aa13-online" -> {
            val nombre="AA13_ONLINE_20261106"
            if (repo.listar().getOrThrow().none { it.nombre==nombre }) check(repo.registrar(Producto(0,nombre,1.5,16,categoriaId=1)).isSuccess)
            val filas=repo.listar().getOrThrow().filter { it.nombre==nombre }
            check(filas.size==1 && filas.single().id>0)
            abrir(); pulsar("Productos"); filtrar(nombre); pantalla("AA13_04_online")
            "PASS registro online; nombre=$nombre; id=${filas.single().id}"
        }
        "aa13-offline" -> {
            val nombre="AA13_OFFLINE_20261106B"
            if (repo.listar().getOrThrow().none { it.nombre==nombre }) repo.registrar(Producto(0,nombre,1.75,17,categoriaId=1)).getOrThrow()
            val filas=repo.listar().getOrThrow().filter { it.nombre==nombre }
            check(filas.size==1 && filas.single().id<0) { "La red aún responde; no se acredita offline" }
            abrir(); pulsar("Productos"); filtrar(nombre); pantalla("AA13_05_offline")
            "PASS registro offline; id=${filas.single().id}; pendientes=${repo.observarPendientes().first()}"
        }
        "aa13-sync" -> {
            val antes=repo.observarPendientes().first()
            check(repo.sincronizar().isSuccess)
            val filas=repo.listar().getOrThrow().filter { it.nombre=="AA13_OFFLINE_20261106B" }
            check(filas.size==1 && filas.single().id>0)
            abrir(); pulsar("Productos"); filtrar("AA13_OFFLINE_20261106B"); pantalla("AA13_06_sync")
            "PASS sync; antes=$antes; despues=${repo.observarPendientes().first()}; id=${filas.single().id}"
        }
        "aa13-logout" -> {
            val antes=repo.listar().getOrThrow().size
            auth.cerrarSesion(); check(almacen.leerToken()==null)
            val despues=repo.listar().getOrThrow().size
            abrir(); pantalla("AA13_10_logout")
            "PASS logout; token=null; filasAntes=$antes; filasDespues=$despues"
        }
        else -> error("Escenario desconocido")
    }
}
