package fr.free.nrw.commons.category

import android.content.Context
import com.google.gson.Gson
import fr.free.nrw.commons.kvstore.JsonKvStore
import io.reactivex.Observable
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.Spy
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.MwQueryResult

// class for testing CategoriesModel class
class CategoriesModelTest {
    @Mock
    internal var categoryInterface: CategoryInterface? = null

    @Mock
    internal var categoryItem: CategoryItem? = null

    @Spy
    internal lateinit var gson: Gson

    @Spy
    internal lateinit var categoryItemForSubstringSearch: CategoryItem

    @Mock
    internal var categoryDao: CategoryDao? = null

    @Mock
    internal var context: Context? = null

    @InjectMocks
    var categoryClient: CategoryClient? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        gson = Gson()
        categoryItemForSubstringSearch = CategoryItem("", false)
        MockitoAnnotations.initMocks(this)
    }

    // Test Case for verifying that Categories search (MW api calls) are case-insensitive
    @Test
    fun searchAllFoundCaseTest() {
        val mwQueryPage = Mockito.mock(MwQueryPage::class.java)
        Mockito.`when`(mwQueryPage.title()).thenReturn("Category:Test")
        val mwQueryResult = Mockito.mock(MwQueryResult::class.java)
        Mockito.`when`(mwQueryResult.pages()).thenReturn(listOf(mwQueryPage))
        val mockResponse = Mockito.mock(MwQueryResponse::class.java)
        Mockito.`when`(mockResponse.query()).thenReturn(mwQueryResult)
        val categoriesModel: CategoriesModel = CategoriesModel(categoryClient, null, null)

        Mockito.`when`(categoryInterface!!.searchCategoriesForPrefix(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()))
                .thenReturn(Observable.just(mockResponse))

        // Checking if both return "Test"
        val actualCategoryName = categoriesModel!!.searchAll("tes", null).blockingFirst()
        assertEquals("Test", actualCategoryName.getName())

        val actualCategoryNameCaps = categoriesModel!!.searchAll("Tes", null).blockingFirst()
        assertEquals("Test", actualCategoryNameCaps.getName())
    }

    /**
    * For testing the substring search algorithm for Categories search
    * To be more precise it tests the In Between substring( ex: searching `atte`
    * will give search suggestions: `Latte`, `Iced latte` e.t.c) which has been described
    * on github repo wiki:
    * https://github.com/commons-app/apps-android-commons/wiki/Category-suggestions-(readme)#user-content-3-category-search-when-typing-in-the-search-field-has-been-made-more-flexible
    */
    @Test
    fun searchAllFoundCaseTestForSubstringSearch() {
        val mockDaoList = mutableListOf<String>("")
        val mwQueryPage = Mockito.mock(MwQueryPage::class.java)
        Mockito.`when`(mwQueryPage.title()).thenReturn("Category:Test")
        val mwQueryResult = Mockito.mock(MwQueryResult::class.java)
        Mockito.`when`(mwQueryResult.pages()).thenReturn(listOf(mwQueryPage))
        val mockResponse = Mockito.mock(MwQueryResponse::class.java)
        Mockito.`when`(mockResponse.query()).thenReturn(mwQueryResult)
        Mockito.`when`(context!!.getSharedPreferences("", 0))
                .thenReturn(null)
        val directKvStore = Mockito.spy(JsonKvStore(context, "", gson))
        val categoriesModelForSubstringSearch = Mockito.spy(CategoriesModel(categoryClient, categoryDao, directKvStore))
        Mockito.doReturn(Observable.just(categoryItemForSubstringSearch)).`when`(categoriesModelForSubstringSearch).gpsCategories()
        Mockito.`when`(context!!.getSharedPreferences("", 0))
                .thenReturn(null)
        Mockito.`when`(categoryInterface!!.searchCategories(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()))
                .thenReturn(Observable.just(mockResponse))
        Mockito.doReturn(mockDaoList).`when`(categoryDao)?.recentCategories(25)
        Mockito.doReturn("Random Value").`when`(directKvStore).getString("Category", "")
        Mockito.`when`(categoryInterface!!.searchCategories(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()))
                .thenReturn(Observable.just(mockResponse))

        // Checking if both return "Test"
        val actualCategoryName = categoriesModelForSubstringSearch!!.searchAll(null, listOf<String>("tes")).blockingLast()
        assertEquals("Test", actualCategoryName.getName())

        val actualCategoryNameCaps = categoriesModelForSubstringSearch!!.searchAll(null, listOf<String>("Tes")).blockingLast()
        assertEquals("Test", actualCategoryNameCaps.getName())
    }
}
