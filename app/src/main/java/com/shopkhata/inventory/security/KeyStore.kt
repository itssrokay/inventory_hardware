package com.shopkhata.inventory.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.shopkhata.inventory.BuildConfig
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Only encrypted ciphertext is persisted. The Keystore key never leaves Android. */
class ApiKeyStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("secure_api", Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("inventory-openai", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("inventory-openai", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun save(value: String) {
        require(value.trim().startsWith("sk-")) { "Enter a valid OpenAI API key." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(value.trim().toByteArray())
        check(prefs.edit().putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit()) { "Could not save the API key." }
    }
    fun read(): String? = try {
        prefs.getString("ciphertext", null)?.let { encoded ->
            val iv = Base64.decode(prefs.getString("iv", ""), Base64.NO_WRAP)
            Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv)) }
                .doFinal(Base64.decode(encoded, Base64.NO_WRAP)).toString(Charsets.UTF_8)
        }
    } catch (_: Exception) { null }
    fun clear() { prefs.edit().clear().apply() }
    fun importDebugKey() {
        if (!BuildConfig.DEBUG) return
        val pending = File(context.filesDir, "openai-key-import")
        if (pending.exists()) try { save(pending.readText()) } finally { pending.delete() }
    }
}
