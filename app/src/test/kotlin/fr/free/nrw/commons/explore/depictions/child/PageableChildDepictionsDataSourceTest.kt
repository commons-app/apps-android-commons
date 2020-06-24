package fr.free.nrw.commons.explore.depictions.child

import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import depictedItem
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import io.reactivex.Single
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class PageableChildDepictionsDataSourceTest {
    @Mock
    lateinit var okHttpJsonApiClient: OkHttpJsonApiClient
    @Mock
    lateinit var liveDataConverter: LiveDataConverter

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun `loadFunction loads from api at position 0`() {
        val dataSource =
            PageableChildDepictionsDataSource(liveDataConverter, okHttpJsonApiClient)
        dataSource.onQueryUpdated("test")
        whenever(okHttpJsonApiClient.getChildDepictions("test"))
            .thenReturn(Single.just(listOf(depictedItem())))
        assertThat(dataSource.loadFunction(-1, 0), `is`(listOf(depictedItem())))
    }

    @Test
    fun `loadFunction loads nothing at any other position`() {
        val dataSource =
            PageableChildDepictionsDataSource(liveDataConverter, okHttpJsonApiClient)
        assertThat(dataSource.loadFunction(-1, 1), `is`(emptyList()))
        verifyZeroInteractions(okHttpJsonApiClient)
    }
}
