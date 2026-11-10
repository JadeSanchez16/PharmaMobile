package pe.edu.upeu.pharmamobilee.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import pe.edu.upeu.pharmamobilee.domain.model.Sesion

class AlmacenSeguroAndroid(
    contexto: Context
) : AlmacenSeguro {
    private val preferencias = contexto.getSharedPreferences(
        ARCHIVO,
        Context.MODE_PRIVATE
    )

    override fun guardarToken(token: String) {
        guardarCifrado(token, CLAVE_TOKEN, CLAVE_VECTOR)
    }

    override fun leerToken(): String? = leerCifrado(CLAVE_TOKEN, CLAVE_VECTOR)

    override fun guardarPerfilSesion(sesion: Sesion) {
        guardarCifrado(
            listOf(
                sesion.usuario.aBase64(),
                sesion.nombreCompleto.aBase64(),
                sesion.roles.sorted().joinToString(","),
                sesion.permisos.sorted().joinToString(",")
            ).joinToString("|"),
            CLAVE_PERFIL,
            CLAVE_PERFIL_VECTOR
        )
    }

    override fun leerPerfilSesion(): Sesion? {
        val token = leerToken() ?: return null
        val partes = leerCifrado(CLAVE_PERFIL, CLAVE_PERFIL_VECTOR)
            ?.split("|", limit = 4)
            ?: return null
        if (partes.size != 4) return null
        return runCatching {
            Sesion(
                token = token,
                usuario = partes[0].desdeBase64().decodeToString(),
                nombreCompleto = partes[1].desdeBase64().decodeToString(),
                roles = partes[2].aConjunto(),
                permisos = partes[3].aConjunto()
            )
        }.getOrNull()
    }

    private fun guardarCifrado(
        valor: String,
        claveValor: String,
        claveVector: String
    ) {
        val cifrador = Cipher.getInstance(TRANSFORMACION)
        cifrador.init(Cipher.ENCRYPT_MODE, obtenerLlave())
        val cifrado = cifrador.doFinal(valor.encodeToByteArray())

        preferencias.edit()
            .putString(claveVector, cifrador.iv.aBase64())
            .putString(claveValor, cifrado.aBase64())
            .apply()
    }

    private fun leerCifrado(claveValor: String, claveVector: String): String? = runCatching {
        val vector = preferencias.getString(claveVector, null)?.desdeBase64()
            ?: return null
        val cifrado = preferencias.getString(claveValor, null)?.desdeBase64()
            ?: return null
        val cifrador = Cipher.getInstance(TRANSFORMACION)
        cifrador.init(
            Cipher.DECRYPT_MODE,
            obtenerLlave(),
            GCMParameterSpec(128, vector)
        )
        cifrador.doFinal(cifrado).decodeToString()
    }.getOrElse {
        preferencias.edit()
            .remove(claveVector)
            .remove(claveValor)
            .apply()
        null
    }

    override fun borrarToken() {
        preferencias.edit()
            .remove(CLAVE_VECTOR)
            .remove(CLAVE_TOKEN)
            .remove(CLAVE_PERFIL_VECTOR)
            .remove(CLAVE_PERFIL)
            .apply()
    }

    private fun obtenerLlave(): SecretKey {
        val almacen = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (almacen.getKey(ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        ).run {
            init(
                KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    private fun ByteArray.aBase64(): String =
        Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.aBase64(): String =
        encodeToByteArray().aBase64()

    private fun String.desdeBase64(): ByteArray =
        Base64.decode(this, Base64.NO_WRAP)

    private fun String.aConjunto(): Set<String> =
        takeIf(String::isNotBlank)?.split(",")?.toSet().orEmpty()

    private companion object {
        const val ARCHIVO = "sesion_segura"
        const val CLAVE_TOKEN = "jwt_cifrado"
        const val CLAVE_VECTOR = "jwt_vector"
        const val CLAVE_PERFIL = "perfil_cifrado"
        const val CLAVE_PERFIL_VECTOR = "perfil_vector"
        const val ALIAS = "pharmamobile_jwt_aes"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
    }
}
