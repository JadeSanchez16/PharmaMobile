package pe.edu.upeu.pharmamobilee

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import pe.edu.upeu.pharmamobilee.domain.model.Producto
import pe.edu.upeu.pharmamobilee.domain.usecase.comoTextoParaCompartir
import pe.edu.upeu.pharmamobilee.presentation.producto.toUi

class CapacidadesNativasSesion09Test {

    @Test
    fun formatoAndroidYTextoComunCumplenElPatronOficial() {
        val producto = Producto(
            id = 1,
            nombre = "Paracetamol 500 mg",
            precio = 4.5,
            stock = 120
        )

        assertEquals("S/ 4.50", producto.toUi().precio.normalizarEspacioMonetario())
        assertEquals(
            "Paracetamol 500 mg — S/ 4.50 · Stock: 120",
            producto.comoTextoParaCompartir().normalizarEspacioMonetario()
        )
    }

    @Test
    fun expectYActualConservanPaqueteNombreYFirma() {
        val shared = directorioShared()
        val rutas = listOf(
            "src/commonMain/kotlin/pe/edu/upeu/pharmamobilee/platform/Formato.kt",
            "src/androidMain/kotlin/pe/edu/upeu/pharmamobilee/platform/Formato.android.kt",
            "src/iosMain/kotlin/pe/edu/upeu/pharmamobilee/platform/Formato.ios.kt"
        )

        val fuentes = rutas.map { shared.resolve(it).readText() }
        fuentes.forEach { fuente ->
            assertTrue(fuente.contains("package pe.edu.upeu.pharmamobilee.platform"))
            assertTrue(fuente.contains("fun formatearSoles(valor: Double): String"))
        }
        assertTrue(fuentes[0].contains("expect fun formatearSoles"))
        assertTrue(fuentes[1].contains("actual fun formatearSoles"))
        assertTrue(fuentes[2].contains("actual fun formatearSoles"))
    }

    @Test
    fun platformModulesRegistranElCompartidorDeCadaPlataforma() {
        val shared = directorioShared()
        val android = shared.resolve(
            "src/androidMain/kotlin/pe/edu/upeu/pharmamobilee/di/PlatformModule.android.kt"
        ).readText()
        val ios = shared.resolve(
            "src/iosMain/kotlin/pe/edu/upeu/pharmamobilee/di/PlatformModule.ios.kt"
        ).readText()

        assertTrue(android.contains("single<Compartidor> { CompartidorAndroid(androidContext()) }"))
        assertTrue(ios.contains("single<Compartidor> { CompartidorIos() }"))
    }

    @Test
    fun presentationNoImportaAndroidNiUIKit() {
        val presentation = directorioShared().resolve(
            "src/commonMain/kotlin/pe/edu/upeu/pharmamobilee/presentation"
        )
        val importacionesProhibidas = presentation.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { archivo ->
                archivo.readLines().asSequence()
                    .filter { it.startsWith("import android.") || it.startsWith("import platform.UIKit") }
                    .map { "${archivo.relativeTo(presentation)}: $it" }
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

    private fun String.normalizarEspacioMonetario(): String = replace('\u00A0', ' ')
}
