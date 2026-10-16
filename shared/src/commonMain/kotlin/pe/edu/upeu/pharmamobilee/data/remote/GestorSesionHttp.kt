package pe.edu.upeu.pharmamobilee.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.edu.upeu.pharmamobilee.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobilee.domain.model.ErrorSesion

class GestorSesionHttp(
    private val almacen: AlmacenSeguro
) {
    private var cliente: HttpClient? = null
    private val _sesionesInvalidas = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sesionesInvalidas = _sesionesInvalidas.asSharedFlow()
    private val _sinPermiso = MutableStateFlow<ErrorSesion.SinPermiso?>(null)
    val sinPermiso = _sinPermiso.asStateFlow()

    fun notificarSinPermiso() {
        _sinPermiso.value = ErrorSesion.SinPermiso
    }

    fun descartarSinPermiso() {
        _sinPermiso.value = null
    }

    fun vincular(cliente: HttpClient) {
        this.cliente = cliente
    }

    suspend fun tokenActualizado() {
        descartarSinPermiso()
        cliente?.authProvider<BearerAuthProvider>()?.clearToken()
    }

    suspend fun invalidarSesion() {
        descartarSinPermiso()
        almacen.borrarToken()
        cliente?.authProvider<BearerAuthProvider>()?.clearToken()
        _sesionesInvalidas.emit(Unit)
    }
}
