package pe.edu.upeu.pharmamobilee.data.repository

import pe.edu.upeu.pharmamobilee.db.PharmaDb

/**
 * Cada destino de pruebas aporta una base SQLDelight aislada.
 * La suite queda en commonTest y nunca comparte estado entre casos.
 */
expect fun baseEnMemoria(): PharmaDb
