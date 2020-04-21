package fr.free.nrw.commons.upload.structure.depictions

import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.upload.depicts.DepictsInterface
import io.reactivex.Observable
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The model class for depictions in upload
 */
@Singleton
class DepictModel @Inject constructor(private val depictsInterface: DepictsInterface) {

  var nearbyPlaces: MutableList<Place>? = null

  companion object {
    private const val SEARCH_DEPICTS_LIMIT = 25
  }

  /**
   * Search for depictions
   */
  fun searchAllEntities(query: String): Observable<DepictedItem> {
    if(query.isBlank()){
      return Observable.fromIterable(nearbyPlaces?.map { DepictedItem(it) } ?: emptyList<DepictedItem>())
    }
    return networkItems(query)
  }

  private fun networkItems(query: String): Observable<DepictedItem> {
    val language = Locale.getDefault().language
    return depictsInterface.searchForDepicts(
      query, "$SEARCH_DEPICTS_LIMIT", language, language, "0"
    )
      .flatMap { Observable.fromIterable(it.search) }
      .map(::DepictedItem)
  }

    fun cleanUp() {
        nearbyPlaces = null
    }

}
