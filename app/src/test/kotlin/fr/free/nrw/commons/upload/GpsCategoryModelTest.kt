package fr.free.nrw.commons.upload

import fr.free.nrw.commons.category.CategoryItem
import org.junit.Before
import org.junit.Test

class GpsCategoryModelTest {
    lateinit var gpsCategoryModel: GpsCategoryModel

    @Before
    fun setUp() {
        gpsCategoryModel = GpsCategoryModel()
    }

    @Test
    fun `intial value is empty`() {
        gpsCategoryModel.categoriesFromLocation.test().assertValues(emptyList())
    }

    @Test
    fun `setCategoriesFromLocation emits the new value`() {
        val expectedList = listOf(
            CategoryItem("category", "", "", false))
        gpsCategoryModel.categoriesFromLocation.test()
            .also { gpsCategoryModel.setCategoriesFromLocation(expectedList) }
            .assertValues(emptyList(), expectedList)
    }

    @Test
    fun `clear emits an empty value`() {
        gpsCategoryModel.categoriesFromLocation.test()
            .also { gpsCategoryModel.clear() }
            .assertValues(emptyList(), emptyList())
    }
}
