package fr.free.nrw.commons.wikidata.cookies

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.kvstore.JsonKvStore
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock

class CommonsCookieStorageTest {
    private val context = mock<Context>()
    private val sharedPreferences = mock<SharedPreferences>()
    private val editor = mock<SharedPreferences.Editor>()

    private lateinit var preferences: JsonKvStore
    private lateinit var storage: CommonsCookieStorage

    @Before
    fun setUp() {
        whenever(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences)
        whenever(sharedPreferences.edit()).thenReturn(editor)
        preferences = JsonKvStore(context, "cookies", Gson())
        storage = CommonsCookieStorage(preferences)
    }

    @Test
    fun loadRemovesMalformedCookieStore() {
        whenever(sharedPreferences.getString("cookie_store", null)).thenReturn("malformed")

        storage.load()

        assertTrue(storage.domains.isEmpty())
        verify(editor).remove("cookie_store")
        verify(editor).apply()
    }

    @Test
    fun loadRemovesNullCookieStore() {
        whenever(sharedPreferences.getString("cookie_store", null)).thenReturn("null")

        storage.load()

        assertTrue(storage.domains.isEmpty())
        verify(editor).remove("cookie_store")
        verify(editor).apply()
    }

    @Test
    fun loadRemovesTruncatedCookieStore() {
        assertInvalidStore("{\"commons.wikimedia.org\":[")
    }

    @Test
    fun loadRemovesNonObjectCookieStores() {
        listOf("[]", "42", "true", "\"string\"").forEach(::assertInvalidStore)
    }

    @Test
    fun loadRemovesCookieStoreWithInvalidDomainValue() {
        assertInvalidStore("{\"commons.wikimedia.org\":\"not-an-array\"}")
    }

    @Test
    fun loadRemovesEmptyCookieStores() {
        listOf("", "   ").forEach(::assertInvalidStore)
    }

    @Test
    fun loadAcceptsValidEmptyCookieStore() {
        whenever(sharedPreferences.getString("cookie_store", null)).thenReturn("{}")

        storage.load()

        assertTrue(storage.domains.isEmpty())
        verify(editor, never()).remove("cookie_store")
    }

    @Test
    fun loadAcceptsValidCookieStore() {
        whenever(sharedPreferences.getString("cookie_store", null))
            .thenReturn(
                "{\"commons.wikimedia.org\":[" +
                    "\"session=abc; path=/; domain=commons.wikimedia.org\"]}",
            )

        storage.load()

        assertTrue(storage.domains.contains("commons.wikimedia.org"))
        assertTrue(storage["commons.wikimedia.org"].size == 1)
        verify(editor, never()).remove("cookie_store")
    }

    @Test
    fun loadIgnoresInvalidCookieInOtherwiseValidStore() {
        whenever(sharedPreferences.getString("cookie_store", null))
            .thenReturn("{\"commons.wikimedia.org\":[\"not-a-cookie\"]}")

        storage.load()

        assertTrue(storage.domains.contains("commons.wikimedia.org"))
        assertTrue(storage["commons.wikimedia.org"].isEmpty())
        verify(editor, never()).remove("cookie_store")
    }

    @Test
    fun loadPreservesUnrelatedPreferencesWhenRecovering() {
        whenever(sharedPreferences.getString("cookie_store", null)).thenReturn("malformed")

        storage.load()

        verify(editor).remove("cookie_store")
        verify(editor, never()).clear()
    }

    @Test
    fun loadCanRunAgainAfterRecovering() {
        whenever(sharedPreferences.getString("cookie_store", null))
            .thenReturn("malformed", null)

        storage.load()
        storage.load()

        assertTrue(storage.domains.isEmpty())
        verify(editor).remove("cookie_store")
    }

    private fun assertInvalidStore(value: String) {
        whenever(sharedPreferences.getString("cookie_store", null)).thenReturn(value)

        storage.load()

        assertTrue(storage.domains.isEmpty())
        verify(editor, atLeastOnce()).remove("cookie_store")
        verify(editor, atLeastOnce()).apply()
    }
}
