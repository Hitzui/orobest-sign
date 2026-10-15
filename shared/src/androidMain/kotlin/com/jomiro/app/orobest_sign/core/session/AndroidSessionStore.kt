package com.jomiro.app.orobest_sign.core.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AndroidSessionStore(context: Context) : SessionStore {
    private val preferences = context.applicationContext.getSharedPreferences("sign_session", Context.MODE_PRIVATE)
    private val alias = "orobest_sign_session_v1"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
        }.generateKey()
    }

    override suspend fun readToken(): String? = withContext(Dispatchers.IO) {
        val stored = preferences.getString("token", null) ?: return@withContext null
        try {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            require(bytes.size > 28)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            cipher.doFinal(bytes.copyOfRange(12, bytes.size)).decodeToString(throwOnInvalidSequence = true)
        } catch (_: GeneralSecurityException) {
            clear()
            null
        } catch (_: IllegalArgumentException) {
            clear()
            null
        }
    }

    override suspend fun writeToken(token: String): Unit = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.iv + cipher.doFinal(token.encodeToByteArray())
        check(preferences.edit().putString("token", Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit()) {
            "No se pudo guardar la sesión"
        }
    }

    override suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        check(preferences.edit().remove("token").commit()) { "No se pudo eliminar la sesión" }
    }
}
