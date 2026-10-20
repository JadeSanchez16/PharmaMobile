package pe.edu.upeu.pharmamobilee.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import pe.edu.upeu.pharmamobilee.db.PharmaDb

class CreadorDeDriverAndroid(
    private val contexto: Context
) : CreadorDeDriver {
    override fun crear(): SqlDriver = AndroidSqliteDriver(
        schema = PharmaDb.Schema,
        context = contexto,
        name = "pharma.db"
    )
}
