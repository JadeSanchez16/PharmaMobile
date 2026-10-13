package pe.edu.upeu.pharmamobilee.domain.usecase

import pe.edu.upeu.pharmamobilee.domain.model.Sesion
import pe.edu.upeu.pharmamobilee.domain.repository.AuthRepository

class RecuperarSesion(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Sesion? = repository.sesionGuardada()
}
