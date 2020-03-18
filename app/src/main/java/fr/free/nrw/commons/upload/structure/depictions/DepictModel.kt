package fr.free.nrw.commons.upload.structure.depictions

import fr.free.nrw.commons.upload.depicts.DepictsInterface
import io.reactivex.Observable
import java.util.Locale
import javax.inject.Inject

/**
 * The model class for depictions in upload
 */
class DepictModel @Inject constructor(private val depictsInterface: DepictsInterface) {

  companion object {
    private const val SEARCH_DEPICTS_LIMIT = 25
  }

  val selectedDepictions = mutableListOf<DepictedItem>()

  fun onDepictItemClicked(depictedItem: DepictedItem) {
    if (depictedItem.isSelected) {
      selectDepictItem(depictedItem)
    } else {
      unselectDepiction(depictedItem)
    }
  }

  private fun unselectDepiction(depictedItem: DepictedItem) {
    selectedDepictions.remove(depictedItem)
  }

  private fun selectDepictItem(depictedItem: DepictedItem) {
    selectedDepictions.add(depictedItem)
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

  fun depictionsEntityIdList() = selectedDepictions.map { it.entityId }
}
