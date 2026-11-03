package pe.edu.upeu.pharmamobilee.domain.permiso

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GestorPermisosTest {
    @Test
    fun gestorFalsoPermiteProbarLosTresEstadosSinDispositivo() = runTest {
        val falso = GestorPermisosFalso(EstadoPermiso.Denegado)
        assertEquals(EstadoPermiso.Denegado, falso.estado(Permiso.NOTIFICACIONES))

        falso.resultadoSolicitud = EstadoPermiso.Concedido
        assertEquals(EstadoPermiso.Concedido, falso.solicitar(Permiso.NOTIFICACIONES))

        falso.resultadoSolicitud = EstadoPermiso.DenegadoParaSiempre
        assertEquals(EstadoPermiso.DenegadoParaSiempre, falso.solicitar(Permiso.NOTIFICACIONES))
    }

    private class GestorPermisosFalso(
        var resultadoSolicitud: EstadoPermiso
    ) : GestorPermisos {
        override suspend fun estado(permiso: Permiso): EstadoPermiso = resultadoSolicitud
        override suspend fun solicitar(permiso: Permiso): EstadoPermiso = resultadoSolicitud
        override fun abrirAjustes() = Unit
    }
}
