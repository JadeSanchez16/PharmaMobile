package pe.edu.upeu.pharmamobilee.data.local

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import pe.edu.upeu.pharmamobilee.domain.model.Sesion

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class AlmacenSeguroIos : AlmacenSeguro {
    override fun guardarToken(token: String) {
        guardarTexto(token, CUENTA_TOKEN)
    }

    override fun leerToken(): String? = leerTexto(CUENTA_TOKEN)

    override fun guardarPerfilSesion(sesion: Sesion) {
        guardarTexto(
            listOf(
                sesion.usuario,
                sesion.nombreCompleto,
                sesion.roles.sorted().joinToString(","),
                sesion.permisos.sorted().joinToString(",")
            ).joinToString(SEPARADOR),
            CUENTA_PERFIL
        )
    }

    override fun leerPerfilSesion(): Sesion? {
        val token = leerToken() ?: return null
        val partes = leerTexto(CUENTA_PERFIL)?.split(SEPARADOR, limit = 4) ?: return null
        if (partes.size != 4) return null
        return Sesion(
            token = token,
            usuario = partes[0],
            nombreCompleto = partes[1],
            roles = partes[2].aConjunto(),
            permisos = partes[3].aConjunto()
        )
    }

    private fun guardarTexto(valor: String, cuenta: String) = memScoped {
        borrarTexto(cuenta)
        val bytes = valor.encodeToByteArray()
        val datos = bytes.usePinned { fijados ->
            CFDataCreate(
                allocator = null,
                bytes = fijados.addressOf(0).reinterpret(),
                length = bytes.size.toLong()
            )
        } ?: return@memScoped

        conConsultaBase(cuenta) { consulta ->
            CFDictionarySetValue(consulta, kSecValueData, datos)
            SecItemAdd(consulta, null)
        }
        CFRelease(datos)
    }

    private fun leerTexto(cuenta: String): String? = memScoped {
        conConsultaBase(cuenta) { consulta ->
            CFDictionarySetValue(consulta, kSecReturnData, kCFBooleanTrue)
            CFDictionarySetValue(consulta, kSecMatchLimit, kSecMatchLimitOne)
            val resultado = alloc<CFTypeRefVar>()
            val estado = SecItemCopyMatching(consulta, resultado.ptr)
            if (estado != errSecSuccess) return@conConsultaBase null
            val datos: CFDataRef = resultado.value?.reinterpret()
                ?: return@conConsultaBase null
            val tamanio = CFDataGetLength(datos).toInt()
            val puntero = CFDataGetBytePtr(datos) ?: return@conConsultaBase null
            try {
                puntero.readBytes(tamanio).decodeToString()
            } finally {
                CFRelease(datos)
            }
        }
    }

    override fun borrarToken() {
        borrarTexto(CUENTA_TOKEN)
        borrarTexto(CUENTA_PERFIL)
    }

    private fun borrarTexto(cuenta: String) {
        conConsultaBase(cuenta) { consulta ->
            SecItemDelete(consulta)
        }
    }

    private fun String.aConjunto(): Set<String> =
        takeIf(String::isNotBlank)?.split(",")?.toSet().orEmpty()

    private fun <T> conConsultaBase(
        cuentaValor: String,
        bloque: (CFMutableDictionaryRef) -> T
    ): T {
        val consulta = CFDictionaryCreateMutable(null, 0, null, null)
            ?: error("No se pudo crear la consulta de Keychain")
        val servicio = CFStringCreateWithCString(
            null,
            SERVICIO,
            kCFStringEncodingUTF8
        ) ?: error("No se pudo crear el identificador de servicio")
        val cuenta = CFStringCreateWithCString(
            null,
            cuentaValor,
            kCFStringEncodingUTF8
        ) ?: error("No se pudo crear el identificador de cuenta")
        return try {
            CFDictionarySetValue(consulta, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(consulta, kSecAttrService, servicio)
            CFDictionarySetValue(consulta, kSecAttrAccount, cuenta)
            bloque(consulta)
        } finally {
            CFRelease(cuenta)
            CFRelease(servicio)
            CFRelease(consulta)
        }
    }

    private companion object {
        const val SERVICIO = "pe.edu.upeu.pharmamobilee"
        const val CUENTA_TOKEN = "jwt"
        const val CUENTA_PERFIL = "perfil_sesion"
        const val SEPARADOR = "\u001F"
    }
}
