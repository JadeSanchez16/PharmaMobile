package pe.edu.upeu.pharmamobilee.permiso

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import pe.edu.upeu.pharmamobilee.domain.permiso.EstadoPermiso
import pe.edu.upeu.pharmamobilee.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobilee.domain.permiso.Permiso

/** Implementación Android: recuerda solicitudes previas para distinguir denegación definitiva. */
class GestorPermisosAndroid(
    private val contexto: Context
) : GestorPermisos {
    private val preferencias: SharedPreferences =
        contexto.getSharedPreferences("permisos_pharmamobile", Context.MODE_PRIVATE)
    private var puente: PuentePermisosAndroid? = null

    fun adjuntar(actividad: ComponentActivity) {
        puente = PuentePermisosAndroid(actividad)
    }

    fun separar(actividad: ComponentActivity) {
        if (puente?.actividad === actividad) puente = null
    }

    override suspend fun estado(permiso: Permiso): EstadoPermiso {
        val nombre = nombreSistema(permiso) ?: return EstadoPermiso.Concedido
        if (ContextCompat.checkSelfPermission(contexto, nombre) == PackageManager.PERMISSION_GRANTED) {
            return EstadoPermiso.Concedido
        }
        val actividad = puente?.actividad ?: return EstadoPermiso.Denegado
        val yaSePidio = preferencias.getBoolean(clave(nombre), false)
        val procedeExplicar = ActivityCompat.shouldShowRequestPermissionRationale(actividad, nombre)
        return if (!yaSePidio || procedeExplicar) EstadoPermiso.Denegado else EstadoPermiso.DenegadoParaSiempre
    }

    override suspend fun solicitar(permiso: Permiso): EstadoPermiso {
        val nombre = nombreSistema(permiso) ?: return EstadoPermiso.Concedido
        val puenteActual = puente ?: return EstadoPermiso.Denegado
        preferencias.edit().putBoolean(clave(nombre), true).apply()
        if (puenteActual.pedir(nombre)) return EstadoPermiso.Concedido
        return estado(permiso)
    }

    override fun abrirAjustes() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", contexto.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        contexto.startActivity(intent)
    }

    private fun nombreSistema(permiso: Permiso): String? = when (permiso) {
        Permiso.NOTIFICACIONES -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else null
    }

    private fun clave(nombre: String) = "pedido_$nombre"
}

class PuentePermisosAndroid(val actividad: ComponentActivity) {
    private var pendiente: CompletableDeferred<Boolean>? = null
    private val lanzador = actividad.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> pendiente?.complete(concedido) }

    suspend fun pedir(nombre: String): Boolean {
        check(pendiente == null) { "Ya existe una solicitud de permiso en curso" }
        val espera = CompletableDeferred<Boolean>()
        pendiente = espera
        lanzador.launch(nombre)
        return try {
            espera.await()
        } finally {
            pendiente = null
        }
    }
}
