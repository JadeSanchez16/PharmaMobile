package pe.edu.upeu.pharmamobilee.domain.usecase

import pe.edu.upeu.pharmamobilee.domain.model.Credenciales
import pe.edu.upeu.pharmamobilee.domain.model.Sesion
import pe.edu.upeu.pharmamobilee.domain.repository.AuthRepository

class IniciarSesion(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(usuario: String, clave: String): Sesion =
        repository.iniciarSesion(Credenciales(usuario.trim(), clave))
}
