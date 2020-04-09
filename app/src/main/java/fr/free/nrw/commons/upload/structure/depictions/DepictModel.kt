package fr.free.nrw.commons.upload.structure.depictions

import fr.free.nrw.commons.upload.depicts.DepictsInterface
import io.reactivex.Observable
import java.util.*
import javax.inject.Inject

/**
 * The model class for depictions in upload
 */
class DepictModel @Inject constructor(private val depictsInterface: DepictsInterface) {

  companion object {
    private const val SEARCH_DEPICTS_LIMIT = 25
  }

  /**
   * Search for depictions
   */
  fun searchAllEntities(query: String?): Observable<DepictedItem> {
    return depictsInterface.searchForDepicts(
        query, "$SEARCH_DEPICTS_LIMIT", Locale.getDefault().language,
        Locale.getDefault().language, "0"
      )
      .flatMap { Observable.fromIterable(it.search) }
      .map(::DepictedItem)
  }

}
