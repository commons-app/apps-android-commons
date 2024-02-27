package fr.free.nrw.commons.explore.depictions.child

import com.nhaarman.mockitokotlin2.whenever
import depictedItem
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.equalTo

class PageableChildDepictionsDataSourceTest {
    @Mock
    lateinit var okHttpJsonApiClient: OkHttpJsonApiClient
    @Mock
    lateinit var liveDataConverter: LiveDataConverter

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `loadFunction loads from api at position 0`() {
        val dataSource =
            PageableChildDepictionsDataSource(liveDataConverter, okHttpJsonApiClient)
        dataSource.onQueryUpdated("test")
        whenever(okHttpJsonApiClient.getChildDepictions("test", 0, 1))
            .thenReturn(Single.just(listOf(depictedItem())))
        assertThat(dataSource.loadFunction(1, 0), equalTo( listOf(depictedItem())))
    }
}
