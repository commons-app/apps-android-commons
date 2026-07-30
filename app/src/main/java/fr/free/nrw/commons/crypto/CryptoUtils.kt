package fr.free.nrw.commons.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import timber.log.Timber
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Utility class for encrypting and decrypting data using the Android Keystore.
 * Uses AES-GCM (Advanced Encryption Standard with Galois/Counter Mode).
 */
object CryptoUtils {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "commons_cookie_store_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH = 12

    private fun getOrGenerateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts the given plaintext string using AES-GCM and Android KeyStore.
     */
    fun encrypt(plaintext: String?): String? {
        if (plaintext == null) return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrGenerateKey())
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = iv + ciphertext
            Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            Timber.e(e, "Error encrypting data")
            null
        }
    }

    /**
     * Decrypts the given base64 encrypted string using AES-GCM and Android KeyStore.
     * Returns the input string if decryption fails (useful for migration from plaintext).
     */
    fun decrypt(encryptedBase64: String?): String? {
        if (encryptedBase64 == null) return null
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.DEFAULT)
            if (combined.size <= IV_LENGTH) return encryptedBase64

            val iv = combined.copyOfRange(0, IV_LENGTH)
            val ciphertext = combined.copyOfRange(IV_LENGTH, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrGenerateKey(), spec)
            val plaintext = cipher.doFinal(ciphertext)
            String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            Timber.w(e, "Decryption failed, assuming plaintext (migration)")
            // It might be plaintext (before migration), so return it directly
            encryptedBase64
        }
    }
}
