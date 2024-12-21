package fr.free.nrw.commons.kvstore

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
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

class JsonKvStoreTest {
    private val context = mock<Context>()
    private val prefs = mock<SharedPreferences>()
    private val editor = mock<SharedPreferences.Editor>()

    private val gson = Gson()
    private val testData = Person(16, "Bob", true, Pet("Poodle", 2))
    private val expected = gson.toJson(testData)

    private lateinit var store: JsonKvStore

    @Before
    fun setUp() {
        whenever(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        store = JsonKvStore(context, "name", gson)
    }

    @Test
    fun putJson() {
        store.putJson("person", testData)

        verify(prefs).edit()
        verify(editor).putString("person", expected)
        verify(editor).apply()
    }

    @Test(expected = IllegalArgumentException::class)
    fun putJsonWithReservedKey() {
        store.putJson(KEY_VERSION, testData)
    }

    @Test
    fun getJson() {
        whenever(prefs.getString("key", null)).thenReturn(expected)

        val result = store.getJson<Person>("key")

        Assert.assertEquals(testData, result)
    }

    @Test
    fun getJsonInTheFuture() {
        whenever(prefs.getString("key", null)).thenReturn(expected)

        val resultOne: Person? = store.getJson("key")
        Assert.assertEquals(testData, resultOne)

        val resultTwo = store.getJson<Person?>("key")
        Assert.assertEquals(testData, resultTwo)
    }

    @Test
    fun getJsonHandlesMalformedJson() {
        whenever(prefs.getString("key", null)).thenReturn("junk")

        val result = store.getJson<Person>("key")

        Assert.assertNull(result)
    }

    data class Person(
        val age: Int, val name: String, val hasPets: Boolean, val pet: Pet?
    )

    data class Pet(
        val breed: String, val age: Int
    )
}