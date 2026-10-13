package pe.edu.upeu.pharmamobilee.presentation.sesion

import pe.edu.upeu.pharmamobilee.domain.model.Sesion

data class SesionUiState(
    val usuario: String = "",
    val clave: String = "",
    val usuarioError: String? = null,
    val claveError: String? = null,
    val cargando: Boolean = false,
    val comprobandoSesion: Boolean = true,
    val error: String? = null,
    val sesion: Sesion? = null
)
