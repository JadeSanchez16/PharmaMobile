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

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class AlmacenSeguroIos : AlmacenSeguro {
    override fun guardarToken(token: String) = memScoped {
        borrarToken()
        val bytes = token.encodeToByteArray()
        val datos = bytes.usePinned { fijados ->
            CFDataCreate(
                allocator = null,
                bytes = fijados.addressOf(0).reinterpret(),
                length = bytes.size.toLong()
            )
        } ?: return@memScoped

        conConsultaBase { consulta ->
            CFDictionarySetValue(consulta, kSecValueData, datos)
            SecItemAdd(consulta, null)
        }
        CFRelease(datos)
    }

    override fun leerToken(): String? = memScoped {
        conConsultaBase { consulta ->
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
        conConsultaBase { consulta ->
            SecItemDelete(consulta)
        }
    }

    private fun <T> conConsultaBase(
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
            CUENTA,
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
        const val CUENTA = "jwt"
    }
}
