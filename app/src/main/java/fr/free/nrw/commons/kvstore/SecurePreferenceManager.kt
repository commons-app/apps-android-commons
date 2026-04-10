package fr.free.nrw.commons.kvstore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber
import java.io.IOException
import java.security.GeneralSecurityException

class SecurePreferenceManager {
    companion object {
        private const val NEW_FILE = "preferences_v2"
        private const val KEY_COOKIE_STORE = "cookie_store"

        @JvmStatic
        @Throws(GeneralSecurityException::class, IOException::class)
        fun get(context: Context, oldFile: String): SharedPreferences {
            // 1. Initialize the Master Key (stored in Android Keystore)
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            // 2. Initialize the EncryptedSharedPreferences (preferences_v2)
            val securePrefs = EncryptedSharedPreferences.create(
                context,
                NEW_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            // 3. Perform migration if needed
            migrateSensitiveData(context, oldFile, securePrefs)

            return securePrefs
        }

        internal fun migrateSensitiveData(
            context: Context,
            oldFile: String,
            securePrefs: SharedPreferences
        ) {
            val oldPrefs = context.getSharedPreferences(oldFile, Context.MODE_PRIVATE)

            // Only migrate if the old file contains the key and the new one doesn't yet
            if (oldPrefs.contains(KEY_COOKIE_STORE) && !securePrefs.contains(KEY_COOKIE_STORE)) {
                val plaintextCookie = oldPrefs.getString(KEY_COOKIE_STORE, null)

                if (plaintextCookie != null) {
                    Timber.d("Migrating cookie_store to encrypted shared preferences")
                    // Write to the encrypted store
                    securePrefs.edit()
                        .putString(KEY_COOKIE_STORE, plaintextCookie)
                        .apply()

                    // CRITICAL: Wipe the plaintext version immediately
                    oldPrefs.edit()
                        .remove(KEY_COOKIE_STORE)
                        .apply()
                }
            }
        }
    }
}
