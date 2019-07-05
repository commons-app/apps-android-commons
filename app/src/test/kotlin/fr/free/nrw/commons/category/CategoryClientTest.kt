package fr.free.nrw.commons.category

import io.reactivex.Observable
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.*
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.MwQueryResult

class CategoryClientTest {
    @Mock
    internal var categoryInterface: CategoryInterface? = null

    @InjectMocks
    var categoryClient: CategoryClient? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun searchCategoriesFound() {
        val mwQueryPage = Mockito.mock(MwQueryPage::class.java)
        Mockito.`when`(mwQueryPage.title()).thenReturn("Category:Test")
        val mwQueryResult = Mockito.mock(MwQueryResult::class.java)
        Mockito.`when`(mwQueryResult.pages()).thenReturn(listOf(mwQueryPage))
        val mockResponse = Mockito.mock(MwQueryResponse::class.java)
        Mockito.`when`(mockResponse.query()).thenReturn(mwQueryResult)

        Mockito.`when`(categoryInterface!!.searchCategories(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt()))
                .thenReturn(Observable.just(mockResponse))

        val actualCategoryName = categoryClient!!.searchCategories("tes", 10).blockingFirst()
        Assert.assertEquals("Test", actualCategoryName)
    }

    @Test
    fun searchCategoriesNull() {
        val mwQueryResult = Mockito.mock(MwQueryResult::class.java)
        Mockito.`when`(mwQueryResult.pages()).thenReturn(null)
        val mockResponse = Mockito.mock(MwQueryResponse::class.java)
        Mockito.`when`(mockResponse.query()).thenReturn(mwQueryResult)

        Mockito.`when`(categoryInterface!!.searchCategories(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt()))
                .thenReturn(Observable.just(mockResponse))
        categoryClient!!.searchCategories("tes", 10).subscribe(
                { Assert.fail("SearchCategories returned element when it shouldn't have.") },
                { s -> throw s })
    }
}