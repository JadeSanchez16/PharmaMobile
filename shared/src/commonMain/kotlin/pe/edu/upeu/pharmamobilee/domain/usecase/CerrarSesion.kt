package pe.edu.upeu.pharmamobilee.domain.usecase

import pe.edu.upeu.pharmamobilee.domain.repository.AuthRepository

class CerrarSesion(
    private val repository: AuthRepository
) {
    suspend operator fun invoke() = repository.cerrarSesion()
}
