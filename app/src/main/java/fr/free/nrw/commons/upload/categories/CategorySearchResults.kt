package fr.free.nrw.commons.upload.categories

import android.text.TextUtils
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.category.CATEGORY_PREFIX
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.Observable
import io.reactivex.Scheduler
import timber.log.Timber

class CategorySearchResults(
    private val repository: UploadRepository,
    private val ioScheduler: Scheduler,
) {
    /**
     * If media is null : Fetches categories from server according to the term
     * Else : Fetches existing categories by their name, fetches categories from server according
     * to the term and combines both in a list
     */
    fun searchResults(
        term: String,
        media: Media?,
    ): Observable<List<CategoryItem>> {
        if (media == null) {
            val search = repository.searchAll(term, getImageTitleList(), repository.getSelectedDepictions())
            val location = if (term.isEmpty()) repository.getLocationOfCreation() else null
            val results =
                if (location == null) {
                    search
                } else {
                    Observable.combineLatest(search, locationCategories(location)) { found, fromLocation ->
                        (fromLocation + found).distinctBy(CategoryItem::name)
                    }
                }
            return results
                .subscribeOn(ioScheduler)
                .map {
                    it.filter { categoryItem ->
                        !repository.isSpammyCategory(categoryItem.name) ||
                            categoryItem.name == term
                    }
                }
        } else {
            return Observable
                .zip(
                    repository
                        .getCategories(repository.getSelectedExistingCategories())
                        .map { list ->
                            list.map {
                                CategoryItem(it.name, it.description, it.thumbnail, true)
                            }
                        },
                    repository.searchAll(term, getImageTitleList(), repository.getSelectedDepictions()),
                ) { it1, it2 ->
                    it1 + it2
                }.subscribeOn(ioScheduler)
                .map {
                    it.filter { categoryItem ->
                        !repository.isSpammyCategory(categoryItem.name) ||
                            categoryItem.name == term
                    }
                }.map { it.filterNot { categoryItem -> categoryItem.thumbnail == "hidden" } }
        }
    }

    /** Resolves the selected place's linked Commons categories, falling back to its label. */
    private fun locationCategories(location: DepictedItem): Observable<List<CategoryItem>> {
        val linkedNames =
            location.commonsCategories
                .map { it.name.removePrefix(CATEGORY_PREFIX).trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        val label = location.name.trim()
        val linked = if (linkedNames.isEmpty()) Observable.just(emptyList()) else repository.getCategories(linkedNames)
        return linked
            .switchMap { categories ->
                if (categories.isEmpty() && label.isNotEmpty() && label !in linkedNames) {
                    repository.getCategories(listOf(label))
                } else {
                    Observable.just(categories)
                }
            }.map { categories ->
                categories.map { CategoryItem(it.name, it.description, it.thumbnail, false) }
            }.onErrorReturn { error ->
                Timber.w(error, "Could not load categories for the selected photo location")
                emptyList()
            }
    }

    /**
     * Returns image title list from UploadItem
     * @return
     */
    private fun getImageTitleList(): List<String> =
        repository
            .getUploads()
            .map { it.uploadMediaDetails[0].captionText }
            .filterNot { TextUtils.isEmpty(it) }
}
