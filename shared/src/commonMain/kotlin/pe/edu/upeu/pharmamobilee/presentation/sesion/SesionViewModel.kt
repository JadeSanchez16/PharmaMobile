package pe.edu.upeu.pharmamobilee.presentation.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobilee.data.remote.GestorSesionHttp
import pe.edu.upeu.pharmamobilee.domain.model.ErrorSesion
import pe.edu.upeu.pharmamobilee.domain.usecase.CerrarSesion
import pe.edu.upeu.pharmamobilee.domain.usecase.IniciarSesion
import pe.edu.upeu.pharmamobilee.domain.usecase.RecuperarSesion

class SesionViewModel(
    private val iniciarSesion: IniciarSesion,
    private val cerrarSesion: CerrarSesion,
    private val recuperarSesion: RecuperarSesion,
    gestorSesionHttp: GestorSesionHttp
) : ViewModel() {
    private val _uiState = MutableStateFlow(SesionUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            gestorSesionHttp.sesionesInvalidas.collect {
                _uiState.update {
                    SesionUiState(
                        comprobandoSesion = false,
                        error = ErrorSesion.SesionExpirada.message
                    )
                }
            }
        }
        recuperar()
    }

    fun actualizarUsuario(valor: String) {
        _uiState.update {
            it.copy(usuario = valor, usuarioError = null, error = null)
        }
    }

    fun actualizarClave(valor: String) {
        _uiState.update {
            it.copy(clave = valor, claveError = null, error = null)
        }
    }

    fun ingresar() {
        if (_uiState.value.cargando) return
        val usuario = _uiState.value.usuario.trim()
        val clave = _uiState.value.clave
        val usuarioError = if (usuario.isBlank()) "Ingresa tu usuario." else null
        val claveError = if (clave.isBlank()) "Ingresa tu contraseña." else null
        if (usuarioError != null || claveError != null) {
            _uiState.update {
                it.copy(usuarioError = usuarioError, claveError = claveError)
            }
            return
        }

        _uiState.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val sesion = iniciarSesion(usuario, clave)
                _uiState.update {
                    SesionUiState(
                        usuario = sesion.usuario,
                        comprobandoSesion = false,
                        sesion = sesion
                    )
                }
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: ErrorSesion) {
                _uiState.update {
                    it.copy(cargando = false, clave = "", error = error.message)
                }
            }
        }
    }

    fun salir() {
        if (_uiState.value.cargando) return
        _uiState.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            cerrarSesion()
            _uiState.value = SesionUiState(comprobandoSesion = false)
        }
    }

    private fun recuperar() {
        viewModelScope.launch {
            try {
                val sesion = recuperarSesion()
                _uiState.update {
                    it.copy(
                        comprobandoSesion = false,
                        sesion = sesion,
                        usuario = sesion?.usuario.orEmpty()
                    )
                }
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: ErrorSesion) {
                _uiState.update {
                    SesionUiState(
                        comprobandoSesion = false,
                        error = error.message
                    )
                }
            }
        }
    }
}
