package pe.edu.upeu.pharmamobilee

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutenticacionSesion10ArquitecturaTest {
    @Test
    fun dominioDeSesionNoImportaKtorAndroidNiUIKit() {
        val raiz = directorioShared()
        val archivos = listOf(
            "src/commonMain/kotlin/pe/edu/upeu/pharmamobilee/domain/model/Sesion.kt",
            "src/commonMain/kotlin/pe/edu/upeu/pharmamobilee/domain/model/ErrorSesion.kt",
            "src/commonMain/kotlin/pe/edu/upeu/pharmamobilee/domain/repository/AuthRepository.kt"
        ).map(raiz::resolve)
        val importsProhibidos = archivos.flatMap { archivo ->
            archivo.readLines().filter {
                it.startsWith("import io.ktor") ||
                    it.startsWith("import android.") ||
                    it.startsWith("import platform.")
            }
        }
        assertTrue(archivos.all(File::isFile))
        assertTrue(importsProhibidos.isEmpty(), importsProhibidos.joinToString())
    }

    @Test
    fun almacenamientoUsaKeystoreYKeychainSinPreferenciasPlanas() {
        val raiz = directorioShared()
        val android = raiz.resolve(
            "src/androidMain/kotlin/pe/edu/upeu/pharmamobilee/data/local/AlmacenSeguroAndroid.kt"
        ).readText()
        val ios = raiz.resolve(
            "src/iosMain/kotlin/pe/edu/upeu/pharmamobilee/data/local/AlmacenSeguroIos.kt"
        ).readText()

        assertTrue(android.contains("AndroidKeyStore"))
        assertTrue(android.contains("AES/GCM/NoPadding"))
        assertTrue(android.contains("KeyGenParameterSpec"))
        assertFalse(android.contains("putString(CLAVE_TOKEN, token)"))
        assertTrue(ios.contains("SecItemAdd"))
        assertTrue(ios.contains("SecItemCopyMatching"))
        assertTrue(ios.contains("SecItemDelete"))
        assertTrue(ios.contains("kSecClassGenericPassword"))
    }

    @Test
    fun clienteCentralizaBearerYSaneaAuthorization() {
        val fuente = directorioShared().resolve(
            "src/commonMain/kotlin/pe/edu/upeu/pharmamobilee/data/remote/HttpClientFactory.kt"
        ).readText()
        assertTrue(fuente.contains("install(Auth)"))
        assertTrue(fuente.contains("loadTokens"))
        assertTrue(fuente.contains("sendWithoutRequest"))
        assertTrue(fuente.contains("sanitizeHeader"))
        assertTrue(fuente.contains("HttpHeaders.Authorization"))
        assertFalse(fuente.contains("header(HttpHeaders.Authorization"))
    }

    private fun directorioShared(): File {
        val actual = File(requireNotNull(System.getProperty("user.dir")))
        return listOf(actual, actual.resolve("shared"))
            .firstOrNull { it.resolve("src/commonMain").isDirectory }
            ?: error("No se encontró el módulo shared")
    }
}
