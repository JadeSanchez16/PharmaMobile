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

class AlmacenSeguroAndroid(
    contexto: Context
) : AlmacenSeguro {
    private val preferencias = contexto.getSharedPreferences(
        ARCHIVO,
        Context.MODE_PRIVATE
    )

    override fun guardarToken(token: String) {
        val cifrador = Cipher.getInstance(TRANSFORMACION)
        cifrador.init(Cipher.ENCRYPT_MODE, obtenerLlave())
        val cifrado = cifrador.doFinal(token.encodeToByteArray())

        preferencias.edit()
            .putString(CLAVE_VECTOR, cifrador.iv.aBase64())
            .putString(CLAVE_TOKEN, cifrado.aBase64())
            .apply()
    }

    override fun leerToken(): String? = runCatching {
        val vector = preferencias.getString(CLAVE_VECTOR, null)?.desdeBase64()
            ?: return null
        val cifrado = preferencias.getString(CLAVE_TOKEN, null)?.desdeBase64()
            ?: return null
        val cifrador = Cipher.getInstance(TRANSFORMACION)
        cifrador.init(
            Cipher.DECRYPT_MODE,
            obtenerLlave(),
            GCMParameterSpec(128, vector)
        )
        cifrador.doFinal(cifrado).decodeToString()
    }.getOrElse {
        borrarToken()
        null
    }

    override fun borrarToken() {
        preferencias.edit()
            .remove(CLAVE_VECTOR)
            .remove(CLAVE_TOKEN)
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

    private fun String.desdeBase64(): ByteArray =
        Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        const val ARCHIVO = "sesion_segura"
        const val CLAVE_TOKEN = "jwt_cifrado"
        const val CLAVE_VECTOR = "jwt_vector"
        const val ALIAS = "pharmamobile_jwt_aes"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
    }
}
