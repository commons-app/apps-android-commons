package fr.free.nrw.commons.explore.depictions.parent

import com.nhaarman.mockitokotlin2.whenever
import depictedItem
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import io.reactivex.Single
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class PageableParentDepictionsDataSourceTest {
    @Mock
    lateinit var okHttpJsonApiClient: OkHttpJsonApiClient

    @Mock
    lateinit var liveDataConverter: LiveDataConverter

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `loadFunction loads from api`() {
        val dataSource =
            PageableParentDepictionsDataSource(liveDataConverter, okHttpJsonApiClient)
        dataSource.onQueryUpdated("test")
        whenever(okHttpJsonApiClient.getParentDepictions("test", 0, 1))
            .thenReturn(Single.just(listOf(depictedItem())))
        Assert.assertEquals(dataSource.loadFunction(1, 0), listOf(depictedItem()))
    }
}

