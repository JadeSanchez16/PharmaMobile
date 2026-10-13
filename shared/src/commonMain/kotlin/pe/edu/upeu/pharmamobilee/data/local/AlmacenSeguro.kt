package pe.edu.upeu.pharmamobilee.data.local

interface AlmacenSeguro {
    fun guardarToken(token: String)
    fun leerToken(): String?
    fun borrarToken()
}

class AlmacenSeguroVolatil : AlmacenSeguro {
    private var token: String? = null

    override fun guardarToken(token: String) {
        this.token = token
    }

    override fun leerToken(): String? = token

    override fun borrarToken() {
        token = null
    }
}
