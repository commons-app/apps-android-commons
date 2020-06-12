package fr.free.nrw.commons.explore.depictions

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Single
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.Test

class PageableDepictionsDataSourceTest {

    @Test
    fun `loadFunction loads depictions`() {
        val depictsClient: DepictsClient = mock()
        whenever(depictsClient.searchForDepictions("test", 0, 1)).thenReturn(Single.just(emptyList()))
        val pageableDepictionsDataSource = PageableDepictionsDataSource(mock(), depictsClient)
        pageableDepictionsDataSource.onQueryUpdated("test")
        assertThat(pageableDepictionsDataSource.loadFunction.invoke(0, 1), Matchers.`is`(emptyList()))
    }
}

