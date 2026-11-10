package pe.edu.upeu.pharmamobilee

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InfoDispositivoActividadAutonoma09Test {

    @Test
    fun infoDispositivoConservaPaqueteFirmaYApisNativasDistintas() {
        val shared = directorioShared()
        val comun = shared.resolve(
            "src/commonMain/kotlin/pe/edu/upeu/pharmamobilee/platform/InfoDispositivo.kt"
        ).readText()
        val android = shared.resolve(
            "src/androidMain/kotlin/pe/edu/upeu/pharmamobilee/platform/InfoDispositivo.android.kt"
        ).readText()
        val ios = shared.resolve(
            "src/iosMain/kotlin/pe/edu/upeu/pharmamobilee/platform/InfoDispositivo.ios.kt"
        ).readText()

        listOf(comun, android, ios).forEach { fuente ->
            assertTrue(fuente.contains("package pe.edu.upeu.pharmamobilee.platform"))
            assertTrue(fuente.contains("InfoDispositivo"))
            assertTrue(fuente.contains("val sistema: String"))
            assertTrue(fuente.contains("val version: String"))
        }
        assertTrue(comun.contains("expect class InfoDispositivo()"))
        assertTrue(android.contains("actual class InfoDispositivo actual constructor()"))
        assertTrue(android.contains("Build.VERSION.RELEASE"))
        assertTrue(ios.contains("actual class InfoDispositivo actual constructor()"))
        assertTrue(ios.contains("UIDevice.currentDevice.systemName()"))
        assertTrue(ios.contains("UIDevice.currentDevice.systemVersion"))
    }

    @Test
    fun infoDispositivoEstaConectadoALaPantallaAcerca() {
        val shared = directorioShared()
        val pantalla = shared.resolve(
            "src/commonMain/kotlin/pe/edu/upeu/pharmamobilee/presentation/acerca/AcercaScreen.kt"
        ).readText()
        val navegacion = shared.resolve(
            "src/commonMain/kotlin/pe/edu/upeu/pharmamobilee/navigation/Screen.kt"
        ).readText()
        val app = shared.resolve(
            "src/commonMain/kotlin/pe/edu/upeu/pharmamobilee/App.kt"
        ).readText()

        assertTrue(pantalla.contains("InfoDispositivo()"))
        assertTrue(pantalla.contains("dispositivo.sistema"))
        assertTrue(pantalla.contains("dispositivo.version"))
        assertTrue(navegacion.contains("data object Acerca"))
        assertTrue(app.contains("Screen.Acerca"))
        assertTrue(app.contains("AcercaScreen()"))
    }

    @Test
    fun commonMainNoContieneImportacionesNativas() {
        val commonMain = directorioShared().resolve("src/commonMain/kotlin")
        val importacionesProhibidas = commonMain.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { archivo ->
                archivo.readLines().asSequence()
                    .filter { it.startsWith("import android.") || it.startsWith("import platform.") }
                    .map { "${archivo.relativeTo(commonMain)}: $it" }
            }
            .toList()

        assertFalse(importacionesProhibidas.isNotEmpty(), importacionesProhibidas.joinToString())
    }

    private fun directorioShared(): File {
        val actual = File(requireNotNull(System.getProperty("user.dir")))
        val candidatos = listOf(actual, actual.resolve("shared"))
        return candidatos.firstOrNull { it.resolve("src/commonMain").isDirectory }
            ?: error("No se encontró el módulo shared desde ${actual.absolutePath}")
    }
}
