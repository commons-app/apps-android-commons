package fr.free.nrw.commons.explore.depictions.media

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.media.WikidataMediaClient
import io.reactivex.Single
import org.junit.Assert
import org.junit.Test

class PageableDepictedMediaDataSourceTest{
    @Test
    fun `loadFunction loads Media`() {
        val mediaClient = mock<WikidataMediaClient>()
        whenever(mediaClient.fetchImagesForDepictedItem("test",0,1))
            .thenReturn(Single.just(emptyList()))
        val pageableDepictedMediaDataSource = PageableDepictedMediaDataSource(mock(), mediaClient)
        pageableDepictedMediaDataSource.onQueryUpdated("test")
        Assert.assertEquals(pageableDepictedMediaDataSource.loadFunction(0,1), emptyList<String>())
    }
}
