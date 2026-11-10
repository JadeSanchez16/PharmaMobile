package pe.edu.upeu.pharmamobilee.data.local

import pe.edu.upeu.pharmamobilee.domain.model.Sesion

interface AlmacenSeguro {
    fun guardarToken(token: String)
    fun leerToken(): String?
    fun borrarToken()

    /**
     * Conserva el perfil ya autenticado junto al token cifrado. Esto permite abrir el
     * catálogo local cuando no hay red; la validez del token se vuelve a verificar en
     * PharmaSoft en la próxima comunicación. Las implementaciones antiguas pueden
     * mantener estos métodos vacíos durante la migración.
     */
    fun guardarPerfilSesion(sesion: Sesion) = Unit
    fun leerPerfilSesion(): Sesion? = null
}

class AlmacenSeguroVolatil : AlmacenSeguro {
    private var token: String? = null
    private var perfil: Sesion? = null

    override fun guardarToken(token: String) {
        this.token = token
    }

    override fun leerToken(): String? = token

    override fun borrarToken() {
        token = null
        perfil = null
    }

    override fun guardarPerfilSesion(sesion: Sesion) {
        perfil = sesion
    }

    override fun leerPerfilSesion(): Sesion? = perfil
}
