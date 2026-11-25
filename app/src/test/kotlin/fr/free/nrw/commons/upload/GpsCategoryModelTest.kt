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
    fun `addCategoriesFromLocation emits the new value`() {
        val expectedList =
            listOf(
                CategoryItem("category", "", "", false),
            )
        gpsCategoryModel.categoriesFromLocation
            .test()
            .also { gpsCategoryModel.addCategoriesFromLocation(expectedList) }
            .assertValues(emptyList(), expectedList)
    }

    @Test
    fun `addCategoriesFromLocation keeps the categories of the places before`() {
        val first = CategoryItem("first", "", "", false)
        val second = CategoryItem("second", "", "", false)
        gpsCategoryModel.addCategoriesFromLocation(listOf(first))
        gpsCategoryModel.addCategoriesFromLocation(listOf(second, first))

        gpsCategoryModel.categoriesFromLocation
            .test()
            .assertValue(listOf(first, second))
    }

    @Test
    fun `clear emits an empty value`() {
        gpsCategoryModel.categoriesFromLocation
            .test()
            .also { gpsCategoryModel.clear() }
            .assertValues(emptyList(), emptyList())
    }
}
