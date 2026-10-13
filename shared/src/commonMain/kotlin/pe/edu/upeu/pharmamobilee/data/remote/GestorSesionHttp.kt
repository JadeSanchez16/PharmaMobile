package pe.edu.upeu.pharmamobilee.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import pe.edu.upeu.pharmamobilee.data.local.AlmacenSeguro

class GestorSesionHttp(
    private val almacen: AlmacenSeguro
) {
    private var cliente: HttpClient? = null
    private val _sesionesInvalidas = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sesionesInvalidas = _sesionesInvalidas.asSharedFlow()

    fun vincular(cliente: HttpClient) {
        this.cliente = cliente
    }

    suspend fun tokenActualizado() {
        cliente?.authProvider<BearerAuthProvider>()?.clearToken()
    }

    suspend fun invalidarSesion() {
        almacen.borrarToken()
        cliente?.authProvider<BearerAuthProvider>()?.clearToken()
        _sesionesInvalidas.emit(Unit)
    }
}
