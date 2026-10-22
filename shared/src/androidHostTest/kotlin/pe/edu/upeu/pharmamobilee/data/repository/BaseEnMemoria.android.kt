package pe.edu.upeu.pharmamobilee.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import pe.edu.upeu.pharmamobilee.db.PharmaDb

/** Base SQLDelight real, aislada y efímera para las pruebas comunes en Android host. */
actual fun baseEnMemoria(): PharmaDb {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    PharmaDb.Schema.create(driver)
    return PharmaDb(driver)
}
