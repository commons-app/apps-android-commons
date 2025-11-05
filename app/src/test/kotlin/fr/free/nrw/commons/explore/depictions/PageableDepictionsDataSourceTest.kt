package fr.free.nrw.commons.explore.depictions

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import fr.free.nrw.commons.explore.depictions.search.PageableDepictionsDataSource
import io.reactivex.Single
import org.junit.Assert
import org.junit.Test

class PageableDepictionsDataSourceTest {
    @Test
    fun `loadFunction loads depictions`() {
        val depictsClient: DepictsClient = mock()
        whenever(depictsClient.searchForDepictions("test", 0, 1))
            .thenReturn(Single.just(emptyList()))
        val pageableDepictionsDataSource = PageableDepictionsDataSource(mock(), depictsClient)
        pageableDepictionsDataSource.onQueryUpdated("test")
        Assert.assertEquals(
            pageableDepictionsDataSource.loadFunction.invoke(0, 1),
            emptyList<String>(),
        )
    }
}
