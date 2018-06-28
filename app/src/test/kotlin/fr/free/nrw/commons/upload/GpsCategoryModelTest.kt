package fr.free.nrw.commons.upload

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GpsCategoryModelTest {

    private lateinit var testObject : GpsCategoryModel

    @Before
    fun setUp() {
        testObject = GpsCategoryModel()
    }

    @Test
    fun initiallyTheModelIsEmpty() {
        assertFalse(testObject.gpsCatExists)
        assertTrue(testObject.categoryList.isEmpty())
    }

    @Test
    fun addingCategoriesToTheModel() {
        testObject.add("one")
        assertTrue(testObject.gpsCatExists)
        assertFalse(testObject.categoryList.isEmpty())
        assertEquals(listOf("one"), testObject.categoryList)
    }

    @Test
    fun duplicatesAreIgnored() {
        testObject.add("one")
        testObject.add("one")
        assertEquals(listOf("one"), testObject.categoryList)
    }

    @Test
    fun modelProtectsAgainstExternalModification() {
        testObject.add("one")

        val list = testObject.categoryList
        list.add("two")

        assertEquals(listOf("one"), testObject.categoryList)
    }

    @Test
    fun clearingTheModel() {
        testObject.add("one")

        testObject.clear()
        assertFalse(testObject.gpsCatExists)
        assertTrue(testObject.categoryList.isEmpty())

        testObject.add("two")
        assertEquals(listOf("two"), testObject.categoryList)
    }

    @Test
    fun settingTheListHandlesNull() {
        testObject.add("one")

        testObject.categoryList = null

        assertFalse(testObject.gpsCatExists)
        assertTrue(testObject.categoryList.isEmpty())
    }

    @Test
    fun setttingTheListOverwritesExistingValues() {
        testObject.add("one")

        testObject.categoryList = listOf("two")

        assertEquals(listOf("two"), testObject.categoryList)
    }
}