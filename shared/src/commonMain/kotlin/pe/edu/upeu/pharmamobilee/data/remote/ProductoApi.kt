package pe.edu.upeu.pharmamobilee.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import pe.edu.upeu.pharmamobilee.data.remote.dto.PaginaResponseDto
import pe.edu.upeu.pharmamobilee.data.remote.dto.ProductoRequestDto
import pe.edu.upeu.pharmamobilee.data.remote.dto.ProductoResponseDto

class ProductoApi(
    private val client: HttpClient
) {
    suspend fun listar(
        pagina: Int = 0,
        tamanio: Int = 20
    ): PaginaResponseDto<ProductoResponseDto> =
        client.get("productos") {
            parameter("pagina", pagina)
            parameter("tamanio", tamanio)
        }.body()

    /** Obtiene todas las páginas para que la sincronización no descarte productos remotos. */
    suspend fun listarTodas(tamanio: Int = 100): List<ProductoResponseDto> {
        var paginaActual = 0
        val productos = mutableListOf<ProductoResponseDto>()
        do {
            val pagina = listar(pagina = paginaActual, tamanio = tamanio)
            productos += pagina.contenido
            paginaActual++
        } while (!pagina.ultima)
        return productos
    }

    suspend fun obtener(id: Long): ProductoResponseDto =
        client.get("productos/$id").body()

    suspend fun crear(request: ProductoRequestDto): ProductoResponseDto =
        client.post("productos") {
            setBody(request)
        }.body()

    suspend fun actualizar(
        id: Long,
        request: ProductoRequestDto
    ): ProductoResponseDto =
        client.put("productos/$id") {
            setBody(request)
        }.body()

    suspend fun eliminar(id: Long) {
        client.delete("productos/$id")
    }
}
