package fr.free.nrw.commons.category

import categoryItem
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import depictedItem
import fr.free.nrw.commons.upload.GpsCategoryModel
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import media
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.MockitoAnnotations

// class for testing CategoriesModel class
class CategoriesModelTest {
    @Mock
    internal lateinit var categoryDao: CategoryDao

    @Mock
    internal lateinit var categoryClient: CategoryClient

    @Mock
    internal lateinit var gpsCategoryModel: GpsCategoryModel

    private lateinit var categoriesModel: CategoriesModel

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        categoriesModel = CategoriesModel(categoryClient, categoryDao, gpsCategoryModel)
    }

    // Test Case for verifying that Categories search (MW api calls)
    @Test
    fun searchAllFoundCaseTest() {
        val categoriesModel = CategoriesModel(categoryClient, mock(), mock())

        val expectedList =
            listOf(
                CategoryItem(
                    "Test",
                    "",
                    "",
                    false,
                ),
            )
        whenever(
            categoryClient.searchCategoriesForPrefix(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyInt(),
            ),
        ).thenReturn(Single.just(expectedList))

        // Checking if both return "Test"
        val expectedItems =
            expectedList.map {
                CategoryItem(
                    it.name,
                    it.description,
                    it.thumbnail,
                    false,
                )
            }
        var categoryTerm = "Test"
        categoriesModel
            .searchAll(categoryTerm, emptyList(), emptyList())
            .test()
            .assertValues(expectedItems)

        verify(categoryClient).searchCategoriesForPrefix(
            categoryTerm,
            CategoriesModel.SEARCH_CATS_LIMIT,
        )
        verify(categoryClient).searchCategoriesForPrefix(
            "Tests",
            CategoriesModel.SEARCH_CATS_LIMIT,
        )

        categoriesModel
            .searchAll(categoryTerm, emptyList(), emptyList())
            .test()
            .assertValues(expectedItems)
    }

    @Test
    fun `typed search includes the plural category missing from the singular prefix page`() {
        whenever(
            categoryClient.searchCategoriesForPrefix("Cat", CategoriesModel.SEARCH_CATS_LIMIT),
        ).thenReturn(
            Single.just(
                listOf(
                    categoryItem("Cat"),
                    categoryItem("Cat's Eye Nebula"),
                ),
            ),
        )
        whenever(
            categoryClient.searchCategoriesForPrefix("Cats", CategoriesModel.SEARCH_CATS_LIMIT),
        ).thenReturn(Single.just(listOf(categoryItem("Cats"))))

        val names =
            categoriesModel
                .searchAll("Cat", emptyList(), emptyList())
                .blockingFirst()
                .map { it.name }

        Assert.assertEquals(listOf("Cat", "Cats", "Cat's Eye Nebula"), names)
    }

    @Test
    fun `searchMore alternates the text search and the next prefix page`() {
        whenever(categoryClient.hasMoreTextSearchPages("Cats")).thenReturn(true)
        whenever(categoryClient.hasMorePrefixPages("Cats")).thenReturn(true)
        whenever(categoryClient.nextTextSearchPage("Cats", CategoriesModel.SEARCH_CATS_LIMIT))
            .thenReturn(Single.just(listOf(categoryItem("Black cats"))))
        whenever(categoryClient.nextPrefixPage("Cats", CategoriesModel.SEARCH_CATS_LIMIT))
            .thenReturn(Single.just(listOf(categoryItem("Cats in art"))))

        val pages = (1..3).map { categoriesModel.searchMore("Cats").blockingGet().map { it.name } }

        Assert.assertEquals(listOf(listOf("Black cats"), listOf("Cats in art"), listOf("Black cats")), pages)
    }

    @Test
    fun `searchMore finds a category that differs in case after the first word`() {
        // The prefix search has nothing for this, "Tokyo" is capitalised in the category name
        whenever(categoryClient.hasMoreTextSearchPages("Supermarkets in tokyo")).thenReturn(true)
        whenever(categoryClient.nextTextSearchPage("Supermarkets in tokyo", CategoriesModel.SEARCH_CATS_LIMIT))
            .thenReturn(Single.just(listOf(categoryItem("Supermarkets in Tokyo"))))

        val names = categoriesModel.searchMore("Supermarkets in tokyo").blockingGet().map { it.name }

        Assert.assertEquals(listOf("Supermarkets in Tokyo"), names)
    }

    @Test
    fun `searchMore skips an empty page and is empty once both searches are used up`() {
        whenever(categoryClient.hasMoreTextSearchPages("Cats")).thenReturn(true, false)
        whenever(categoryClient.hasMorePrefixPages("Cats")).thenReturn(true, true, false)
        whenever(categoryClient.nextTextSearchPage("Cats", CategoriesModel.SEARCH_CATS_LIMIT))
            .thenReturn(Single.just(emptyList()))
        whenever(categoryClient.nextPrefixPage("Cats", CategoriesModel.SEARCH_CATS_LIMIT))
            .thenReturn(Single.just(listOf(categoryItem("Cats in art"))))

        Assert.assertEquals(
            listOf("Cats in art"),
            categoriesModel.searchMore("Cats").blockingGet().map { it.name },
        )
        Assert.assertEquals(emptyList<CategoryItem>(), categoriesModel.searchMore("Cats").blockingGet())
    }

    @Test
    fun `searchAll with empty search terms creates results from gps, title search & recents`() {
        val gpsCategoryModel: GpsCategoryModel = mock()
        val depictedItem =
            depictedItem(
                commonsCategories =
                    listOf(
                        CategoryItem(
                            "depictionCategory",
                            "",
                            "",
                            false,
                        ),
                    ),
            )
        val depictedItemWithoutCategories =
            depictedItem(
                imageUrl = "testUrl",
            )

        whenever(gpsCategoryModel.categoriesFromLocation)
            .thenReturn(
                BehaviorSubject.createDefault(
                    listOf(
                        CategoryItem(
                            "gpsCategory",
                            "",
                            "",
                            false,
                        ),
                    ),
                ),
            )
        whenever(
            categoryClient.searchCategories(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyInt(),
            ),
        ).thenReturn(
            Single.just(
                listOf(
                    CategoryItem(
                        "titleSearch",
                        "",
                        "",
                        false,
                    ),
                ),
            ),
        )
        whenever(categoryDao.recentCategories(25)).thenReturn(
            listOf(
                CategoryItem(
                    "recentCategories",
                    "",
                    "",
                    false,
                ),
            ),
        )
        whenever(
            categoryClient.getCategoriesByName(
                "depictionCategory",
                "depictionCategory",
                25,
            ),
        ).thenReturn(
            Single.just(
                listOf(
                    CategoryItem(
                        "commonsCategories",
                        "",
                        "",
                        false,
                    ),
                ),
            ),
        )
        whenever(
            categoryClient.getCategoriesOfImage(
                "testUrl",
                25,
            ),
        ).thenReturn(
            Single.just(
                listOf(
                    CategoryItem(
                        "categoriesOfP18",
                        "",
                        "",
                        false,
                    ),
                ),
            ),
        )
        val imageTitleList = listOf("Test")
        CategoriesModel(categoryClient, categoryDao, gpsCategoryModel)
            .searchAll("", imageTitleList, listOf(depictedItem))
            .test()
            .assertValue(
                listOf(
                    categoryItem("commonsCategories"),
                    categoryItem("gpsCategory"),
                    categoryItem("titleSearch"),
                    categoryItem("recentCategories"),
                ),
            )
        CategoriesModel(categoryClient, categoryDao, gpsCategoryModel)
            .searchAll("", imageTitleList, listOf(depictedItemWithoutCategories))
            .test()
            .assertValue(
                listOf(
                    categoryItem("categoriesOfP18"),
                    categoryItem("gpsCategory"),
                    categoryItem("titleSearch"),
                    categoryItem("recentCategories"),
                ),
            )
        imageTitleList.forEach {
            verify(categoryClient, times(2)).searchCategories(it, CategoriesModel.SEARCH_CATS_LIMIT)
            verify(categoryClient).getCategoriesByName(any(), any(), any(), any())
            verify(categoryClient).getCategoriesOfImage(any(), any())
        }
    }

    @Test
    @Throws(Exception::class)
    fun testGetCategoriesByName() {
        categoriesModel.getCategoriesByName(listOf("Test"))
    }

    @Test
    @Throws(Exception::class)
    fun `Test buildCategories when it returns non empty list`() {
        whenever(
            categoryClient.getCategoriesByName(
                "Test",
                "Test",
                CategoriesModel.SEARCH_CATS_LIMIT,
            ),
        ).thenReturn(Single.just(listOf(categoryItem())))
        categoriesModel.buildCategories("Test")
    }

    @Test
    @Throws(Exception::class)
    fun `Test buildCategories when it returns empty list`() {
        whenever(
            categoryClient.getCategoriesByName(
                "Test",
                "Test",
                CategoriesModel.SEARCH_CATS_LIMIT,
            ),
        ).thenReturn(Single.just(emptyList()))
        categoriesModel.buildCategories("Test")
    }

    @Test
    @Throws(Exception::class)
    fun testGetSelectedExistingCategories() {
        categoriesModel.getSelectedExistingCategories()
    }

    @Test
    @Throws(Exception::class)
    fun testSetSelectedExistingCategories() {
        categoriesModel.setSelectedExistingCategories(mutableListOf("Test"))
    }

    @Test
    @Throws(Exception::class)
    fun `Test onCategoryItemClicked when media is null and item is selected`() {
        categoriesModel.onCategoryItemClicked(
            CategoryItem(
                "name",
                "des",
                "image",
                true,
            ),
            null,
        )
    }

    @Test
    @Throws(Exception::class)
    fun `Test onCategoryItemClicked when media is null and item is not selected`() {
        categoriesModel.onCategoryItemClicked(categoryItem(), null)
    }

    @Test
    @Throws(Exception::class)
    fun `Test onCategoryItemClicked when media is not null and item is selected and media contains category`() {
        categoriesModel.onCategoryItemClicked(
            CategoryItem(
                "categories",
                "des",
                "image",
                true,
            ),
            media(),
        )
    }

    @Test
    @Throws(Exception::class)
    fun `Test onCategoryItemClicked when media is not null and item is selected and media lacks category`() {
        categoriesModel.onCategoryItemClicked(
            CategoryItem(
                "name",
                "des",
                "image",
                true,
            ),
            media(),
        )
    }

    @Test
    @Throws(Exception::class)
    fun `Test onCategoryItemClicked when media is not null and item is not selected and media contains category`() {
        categoriesModel.onCategoryItemClicked(
            CategoryItem(
                "categories",
                "des",
                "image",
                false,
            ),
            media(),
        )
    }

    @Test
    @Throws(Exception::class)
    fun `Test onCategoryItemClicked when media is not null and item is not selected and media lacks category`() {
        categoriesModel.onCategoryItemClicked(
            CategoryItem(
                "name",
                "des",
                "image",
                false,
            ),
            media(),
        )
    }

    @Test
    fun `test valid input with XXXX in it between the expected range 20XX`() {
        val input = categoriesModel.isSpammyCategory("Amavenita (ship, 2014)")
        Assert.assertFalse(input)
    }

    @Test
    fun `test valid input with XXXXs in it between the expected range 20XXs`() {
        val input = categoriesModel.isSpammyCategory("Amavenita (ship, 2014s)")
        Assert.assertFalse(input)
    }

    @Test
    fun `test invalid category when have needing in the input`() {
        val input = categoriesModel.isSpammyCategory("Media needing categories as of 30 March 2017")
        Assert.assertTrue(input)
    }

    @Test
    fun `test invalid category when have taken on in the input`() {
        val input = categoriesModel.isSpammyCategory("Photographs taken on 2015-12-08")
        Assert.assertTrue(input)
    }

    @Test
    fun `test invalid category when have yy mm or yy mm dd in the input`() {
        // filtering based on [., /, -]  separators between the dates.
        val input = categoriesModel.isSpammyCategory("Image class 09.14")
        Assert.assertTrue(input)
    }

    @Test
    fun `test invalid category when have years not in 20XX range`() {
        val input = categoriesModel.isSpammyCategory("Japan in the 1400s")
        Assert.assertTrue(input)
    }
}
