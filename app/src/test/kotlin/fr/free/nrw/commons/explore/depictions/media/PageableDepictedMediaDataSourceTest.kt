package fr.free.nrw.commons.explore.depictions.media

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.media.MediaClient
import io.reactivex.Single
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class PageableDepictedMediaDataSourceTest{
    @Test
    fun `loadFunction loads Media`() {
        val mediaClient = mock<MediaClient>()
        whenever(mediaClient.fetchImagesForDepictedItem("test",0,1))
            .thenReturn(Single.just(emptyList()))
        val pageableDepictedMediaDataSource = PageableDepictedMediaDataSource(mock(), mediaClient)
        pageableDepictedMediaDataSource.onQueryUpdated("test")
        assertThat(pageableDepictedMediaDataSource.loadFunction(0,1), `is`(emptyList()))
    }
}
