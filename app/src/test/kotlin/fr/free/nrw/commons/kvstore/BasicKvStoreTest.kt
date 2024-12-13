package fr.free.nrw.commons.kvstore

import android.content.Context
import android.content.SharedPreferences
import com.nhaarman.mockitokotlin2.atLeast
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.kvstore.BasicKvStore.Companion.KEY_VERSION
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock

class BasicKvStoreTest {
    private val context = mock<Context>()
    private val prefs = mock<SharedPreferences>()
    private val editor = mock<SharedPreferences.Editor>()
    private lateinit var store: BasicKvStore

    @Before
    fun setUp() {
        whenever(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        store = BasicKvStore(context, "name")
    }

    @Test
    fun versionUpdate() {
        whenever(prefs.getInt(KEY_VERSION, 0)).thenReturn(99)
        BasicKvStore(context, "name", 100, true)

        // It should clear itself and automatically put the new version number
        verify(prefs, atLeast(2)).edit()
        verify(editor).clear()
        verify(editor).putInt(KEY_VERSION, 100)
        verify(editor, atLeast(2)).apply()
    }

    @Test(expected = IllegalArgumentException::class)
    fun versionDowngradeNotAllowed() {
        whenever(prefs.getInt(KEY_VERSION, 0)).thenReturn(100)
        BasicKvStore(context, "name", 99, true)
    }

    @Test
    fun versionRedactedFromGetAll() {
        val all = mutableMapOf("key" to "value", KEY_VERSION to 100)
        whenever(prefs.all).thenReturn(all)

        val result = store.all
        Assert.assertEquals(mapOf("key" to "value"), result)
    }

    @Test
    fun getAllHandlesNull() {
        whenever(prefs.all).thenReturn(null)
        Assert.assertNull(store.all)
    }

    @Test
    fun getAllHandlesEmpty() {
        whenever(prefs.all).thenReturn(emptyMap())
        Assert.assertNull(store.all)
    }

    @Test
    fun getString() {
        whenever(prefs.getString("key", null)).thenReturn("value")
        Assert.assertEquals("value", store.getString("key"))
    }

    @Test
    fun getBoolean() {
        whenever(prefs.getBoolean("key", false)).thenReturn(true)
        Assert.assertTrue(store.getBoolean("key"))
    }

    @Test
    fun getLong() {
        whenever(prefs.getLong("key", 0L)).thenReturn(100)
        Assert.assertEquals(100L, store.getLong("key"))
    }

    @Test
    fun getInt() {
        whenever(prefs.getInt("key", 0)).thenReturn(100)
        Assert.assertEquals(100, store.getInt("key"))
    }

    @Test
    fun getStringWithDefault() {
        whenever(prefs.getString("key", "junk")).thenReturn("value")
        Assert.assertEquals("value", store.getString("key", "junk"))
    }

    @Test
    fun getBooleanWithDefault() {
        whenever(prefs.getBoolean("key", true)).thenReturn(true)
        Assert.assertTrue(store.getBoolean("key", true))
    }

    @Test
    fun getLongWithDefault() {
        whenever(prefs.getLong("key", 22L)).thenReturn(100)
        Assert.assertEquals(100L, store.getLong("key", 22L))
    }

    @Test
    fun getIntWithDefault() {
        whenever(prefs.getInt("key", 22)).thenReturn(100)
        Assert.assertEquals(100, store.getInt("key", 22))
    }

    @Test
    fun putAllStrings() {
        store.putAllStrings(
            mapOf(
                "one" to "fish",
                "two" to "fish",
                "red" to "fish",
                "blue" to "fish"
            )
        )

        verify(prefs).edit()
        verify(editor).putString("one", "fish")
        verify(editor).putString("two", "fish")
        verify(editor).putString("red", "fish")
        verify(editor).putString("blue", "fish")
        verify(editor).apply()
    }

    @Test(expected = IllegalArgumentException::class)
    fun putAllStringsWithReservedKey() {
        store.putAllStrings(
            mapOf(
                "this" to "that",
                KEY_VERSION to "something"
            )
        )
    }

    @Test
    fun putString() {
        store.putString("this" , "that")

        verify(prefs).edit()
        verify(editor).putString("this", "that")
        verify(editor).apply()
    }

    @Test
    fun putBoolean() {
        store.putBoolean("this" , true)

        verify(prefs).edit()
        verify(editor).putBoolean("this", true)
        verify(editor).apply()
    }

    @Test
    fun putLong() {
        store.putLong("this" , 123L)

        verify(prefs).edit()
        verify(editor).putLong("this", 123L)
        verify(editor).apply()
    }

    @Test
    fun putInt() {
        store.putInt("this" , 16)

        verify(prefs).edit()
        verify(editor).putInt("this", 16)
        verify(editor).apply()
    }

    @Test(expected = IllegalArgumentException::class)
    fun putStringWithReservedKey() {
        store.putString(KEY_VERSION, "that")
    }

    @Test(expected = IllegalArgumentException::class)
    fun putBooleanWithReservedKey() {
        store.putBoolean(KEY_VERSION, true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun putLongWithReservedKey() {
        store.putLong(KEY_VERSION, 33L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun putIntWithReservedKey() {
        store.putInt(KEY_VERSION, 12)
    }

    @Test
    fun testContains() {
        whenever(prefs.contains("key")).thenReturn(true)
        Assert.assertTrue(store.contains("key"))
    }

    @Test
    fun containsRedactsVersion() {
        whenever(prefs.contains(KEY_VERSION)).thenReturn(true)
        Assert.assertFalse(store.contains(KEY_VERSION))
    }

    @Test
    fun remove() {
        store.remove("key")

        verify(prefs).edit()
        verify(editor).remove("key")
        verify(editor).apply()
    }

    @Test(expected = IllegalArgumentException::class)
    fun removeWithReservedKey() {
        store.remove(KEY_VERSION)
    }

    @Test
    fun clearAllPreservesVersion() {
        whenever(prefs.getInt(KEY_VERSION, 0)).thenReturn(99)

        store.clearAll()

        verify(prefs).edit()
        verify(editor).clear()
        verify(editor).putInt(KEY_VERSION, 99)
        verify(editor).apply()
    }
}