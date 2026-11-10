package pe.edu.upeu.pharmamobilee.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobilee.db.PharmaDb
import pe.edu.upeu.pharmamobilee.domain.model.OrigenProducto
import pe.edu.upeu.pharmamobilee.domain.model.Producto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Verifica el esquema SQLDelight generado usando una base SQLite aislada en memoria. */
class ProductoLocalSqlDelightTest {

    @Test
    fun guardaPendienteNegativoYLoReemplazaPorElIdDelServidor() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PharmaDb.Schema.create(driver)
        val local = ProductoLocal(PharmaDb(driver))

        val pendiente = local.guardarPendiente(producto("Producto temporal"))
        assertTrue(pendiente.id < 0)
        assertEquals(OrigenProducto.LOCAL, local.pendientes().single().origen)

        local.confirmarPendiente(pendiente.id, producto("Producto temporal", id = 700L))

        val catalogo = local.observar().first()
        assertEquals(700L, catalogo.single().id)
        assertTrue(local.pendientes().isEmpty())
        driver.close()
    }

    private fun producto(nombre: String, id: Long = 0L) = Producto(
        id = id,
        nombre = nombre,
        precio = 8.5,
        stock = 4,
        categoriaId = 1L,
        categoria = "Pruebas"
    )
}
