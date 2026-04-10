package fr.free.nrw.commons.kvstore

import android.content.Context
import android.content.SharedPreferences
import com.nhaarman.mockitokotlin2.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString

class SecurePreferenceManagerTest {
    private val context = mock<Context>()
    private val oldPrefs = mock<SharedPreferences>()
    private val securePrefs = mock<SharedPreferences>()
    private val oldEditor = mock<SharedPreferences.Editor>()
    private val secureEditor = mock<SharedPreferences.Editor>()

    @Before
    fun setUp() {
        whenever(context.getSharedPreferences(anyString(), anyInt())).thenReturn(oldPrefs)
        whenever(oldPrefs.edit()).thenReturn(oldEditor)
        whenever(securePrefs.edit()).thenReturn(secureEditor)
        
        whenever(oldEditor.remove(anyString())).thenReturn(oldEditor)
        whenever(secureEditor.putString(anyString(), anyString())).thenReturn(secureEditor)
    }

    @Test
    fun testMigrationMovesDataAndClearsOld() {
        val cookieData = "{\"domain\": \"cookies\"}"
        whenever(oldPrefs.contains("cookie_store")).thenReturn(true)
        whenever(securePrefs.contains("cookie_store")).thenReturn(false)
        whenever(oldPrefs.getString("cookie_store", null)).thenReturn(cookieData)

        SecurePreferenceManager.migrateSensitiveData(context, "old_file", securePrefs)

        verify(secureEditor).putString("cookie_store", cookieData)
        verify(secureEditor).apply()
        verify(oldEditor).remove("cookie_store")
        verify(oldEditor).apply()
    }

    @Test
    fun testMigrationDoesNothingIfAlreadyMigrated() {
        whenever(oldPrefs.contains("cookie_store")).thenReturn(true)
        whenever(securePrefs.contains("cookie_store")).thenReturn(true)

        SecurePreferenceManager.migrateSensitiveData(context, "old_file", securePrefs)

        verify(secureEditor, never()).putString(any(), any())
        verify(oldEditor, never()).remove(any())
    }

    @Test
    fun testMigrationDoesNothingIfOldDataMissing() {
        whenever(oldPrefs.contains("cookie_store")).thenReturn(false)
        whenever(securePrefs.contains("cookie_store")).thenReturn(false)

        SecurePreferenceManager.migrateSensitiveData(context, "old_file", securePrefs)

        verify(secureEditor, never()).putString(any(), any())
        verify(oldEditor, never()).remove(any())
    }
}
