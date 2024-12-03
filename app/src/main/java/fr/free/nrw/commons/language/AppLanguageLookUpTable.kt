package fr.free.nrw.commons.language

import android.content.Context
import android.content.res.Resources
import android.text.TextUtils

import androidx.annotation.ArrayRes
import fr.free.nrw.commons.R
import java.lang.ref.SoftReference
import java.util.Arrays
import java.util.Locale


/** Immutable look up table for all app supported languages. All article languages may not be
 * present in this table as it is statically bundled with the app. */
class AppLanguageLookUpTable(context: Context) {

    companion object {
        const val SIMPLIFIED_CHINESE_LANGUAGE_CODE = "zh-hans"
        const val TRADITIONAL_CHINESE_LANGUAGE_CODE = "zh-hant"
        const val CHINESE_CN_LANGUAGE_CODE = "zh-cn"
        const val CHINESE_HK_LANGUAGE_CODE = "zh-hk"
        const val CHINESE_MO_LANGUAGE_CODE = "zh-mo"
        const val CHINESE_SG_LANGUAGE_CODE = "zh-sg"
        const val CHINESE_TW_LANGUAGE_CODE = "zh-tw"
        const val CHINESE_YUE_LANGUAGE_CODE = "zh-yue"
        const val CHINESE_LANGUAGE_CODE = "zh"
        const val NORWEGIAN_LEGACY_LANGUAGE_CODE = "no"
        const val NORWEGIAN_BOKMAL_LANGUAGE_CODE = "nb"
        const val TEST_LANGUAGE_CODE = "test"
        const val FALLBACK_LANGUAGE_CODE = "en" // Must exist in preference_language_keys.
    }

    private val resources: Resources = context.resources

    // Language codes for all app supported languages in fixed order. The special code representing
    // the dynamic system language is null.
    private var codesRef = SoftReference<List<String>>(null)

    // English names for all app supported languages in fixed order.
    private var canonicalNamesRef = SoftReference<List<String>>(null)

    // Native names for all app supported languages in fixed order.
    private var localizedNamesRef = SoftReference<List<String>>(null)

    /**
     * @return Nonnull immutable list. The special code representing the dynamic system language is
     *         null.
     */
    fun getCodes(): List<String> {
        var codes = codesRef.get()
        if (codes == null) {
            codes = getStringList(R.array.preference_language_keys)
            codesRef = SoftReference(codes)
        }
        return codes
    }

    fun getCanonicalName(code: String?): String? {
        var name = defaultIndex(getCanonicalNames(), indexOfCode(code), null)
        if (name.isNullOrEmpty() && !code.isNullOrEmpty()) {
            name = when (code) {
                Locale.CHINESE.language -> Locale.CHINESE.getDisplayName(Locale.ENGLISH)
                NORWEGIAN_LEGACY_LANGUAGE_CODE ->
                    defaultIndex(getCanonicalNames(), indexOfCode(NORWEGIAN_BOKMAL_LANGUAGE_CODE), null)
                else -> null
            }
        }
        return name
    }

    fun getLocalizedName(code: String?): String? {
        var name = defaultIndex(getLocalizedNames(), indexOfCode(code), null)
        if (name.isNullOrEmpty() && !code.isNullOrEmpty()) {
            name = when (code) {
                Locale.CHINESE.language -> Locale.CHINESE.getDisplayName(Locale.CHINESE)
                NORWEGIAN_LEGACY_LANGUAGE_CODE ->
                    defaultIndex(getLocalizedNames(), indexOfCode(NORWEGIAN_BOKMAL_LANGUAGE_CODE), null)
                else -> null
            }
        }
        return name
    }

    fun getCanonicalNames(): List<String> {
        var names = canonicalNamesRef.get()
        if (names == null) {
            names = getStringList(R.array.preference_language_canonical_names)
            canonicalNamesRef = SoftReference(names)
        }
        return names
    }

    fun getLocalizedNames(): List<String> {
        var names = localizedNamesRef.get()
        if (names == null) {
            names = getStringList(R.array.preference_language_local_names)
            localizedNamesRef = SoftReference(names)
        }
        return names
    }

    fun isSupportedCode(code: String?): Boolean {
        return getCodes().contains(code)
    }

    private fun <T> defaultIndex(list: List<T>, index: Int, defaultValue: T?): T? {
        return if (inBounds(list, index)) list[index] else defaultValue
    }

    /**
     * Searches #codes for the specified language code and returns the index for use in
     * #canonicalNames and #localizedNames.
     *
     * @param code The language code to search for. The special code representing the dynamic system
     *             language is null.
     * @return The index of the language code or -1 if the code is not supported.
     */
    private fun indexOfCode(code: String?): Int {
        return getCodes().indexOf(code)
    }

    /** @return Nonnull immutable list. */
    private fun getStringList(id: Int): List<String> {
        return getStringArray(id).toList()
    }

    private fun inBounds(list: List<*>, index: Int): Boolean {
        return index in list.indices
    }

    fun getStringArray(@ArrayRes id: Int): Array<String> {
        return resources.getStringArray(id)
    }
}
