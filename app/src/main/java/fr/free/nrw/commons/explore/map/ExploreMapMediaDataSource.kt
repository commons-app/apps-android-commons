package fr.free.nrw.commons.explore.map

import android.util.Log
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.explore.depictions.search.LoadFunction
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.explore.paging.PageableBaseDataSource
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.media.WikidataMediaClient
import javax.inject.Inject

class ExploreMapMediaDataSource @Inject constructor(
    liveDataConverter: LiveDataConverter,
    private val mediaClient: MediaClient
    ) : PageableBaseDataSource<Media>(liveDataConverter) {
    fun setIsFromSearchActivity(isFromSearchActivity: Boolean) {
        this.isFromSearchActivity = isFromSearchActivity
    }
    override val loadFunction: LoadFunction<Media> = { loadSize: Int, startPosition: Int ->
        Log.d("deneme","loadFunction")
        //TODO: change this method
        // TODO: filter this result by location or display all of them on map
        if (isFromSearchActivity) {
            Log.d("deneme","isFromSearchActivity")
            mediaClient.getMediaListFromSearchWithLocation(query, loadSize, startPosition).blockingGet()
        } else {
            Log.d("deneme","not isFromSearchActivity")
            mediaClient.getMediaListFromGeoSearch(coordinate, limit).blockingGet()
        }
    }
}
