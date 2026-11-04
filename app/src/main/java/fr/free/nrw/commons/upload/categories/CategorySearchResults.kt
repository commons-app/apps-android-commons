package fr.free.nrw.commons.upload.categories

import android.text.TextUtils
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.repository.UploadRepository
import io.reactivex.Observable
import io.reactivex.Scheduler

/** Loads category suggestions and combines existing categories when editing media. */
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
            return repository
                .searchAll(term, getImageTitleList(), repository.getSelectedDepictions())
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
