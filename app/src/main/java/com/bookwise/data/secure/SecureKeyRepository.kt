package com.bookwise.data.secure

import android.content.Context
import android.util.Base64
import androidx.core.content.edit

class SecureKeyRepository(private val context: Context, private val keyStoreManager: KeyStoreManager) {

    private val prefs = context.getSharedPreferences("bookwise_secure", Context.MODE_PRIVATE)

    fun hasKey(): Boolean = prefs.contains(KEY_CIPHER)

    fun saveKey(plain: String) {
        val cipher = keyStoreManager.encrypt(plain.toByteArray(Charsets.UTF_8))
        prefs.edit { putString(KEY_CIPHER, Base64.encodeToString(cipher, Base64.NO_WRAP)) }
    }

    fun loadKey(): String? {
        val b64 = prefs.getString(KEY_CIPHER, null) ?: return null
        return try {
            String(keyStoreManager.decrypt(Base64.decode(b64, Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    fun clearKey() {
        prefs.edit { remove(KEY_CIPHER) }
    }

    companion object {
        private const val KEY_CIPHER = "api_key_cipher"
    }
}
