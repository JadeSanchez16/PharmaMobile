package pe.edu.upeu.pharmamobilee.data.local

import app.cash.sqldelight.db.SqlDriver

/** Abre la misma base SQLDelight mediante el driver específico de cada plataforma. */
interface CreadorDeDriver {
    fun crear(): SqlDriver
}
