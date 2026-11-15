package fr.free.nrw.commons.explore.depictions.child

import com.nhaarman.mockitokotlin2.whenever
import depictedItem
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.mwapi.WikidataQueryClient
import io.reactivex.Single
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class PageableChildDepictionsDataSourceTest {
    @Mock
    lateinit var wikidataQueryClient: WikidataQueryClient

    @Mock
    lateinit var liveDataConverter: LiveDataConverter

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `loadFunction loads from api at position 0`() {
        val dataSource =
            PageableChildDepictionsDataSource(liveDataConverter, wikidataQueryClient)
        dataSource.onQueryUpdated("test")
        whenever(wikidataQueryClient.getChildDepictions("test", 0, 1))
            .thenReturn(Single.just(listOf(depictedItem())))
        Assert.assertEquals(dataSource.loadFunction(1, 0), listOf(depictedItem()))
    }
}
