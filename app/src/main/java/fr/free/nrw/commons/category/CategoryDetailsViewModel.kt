package fr.free.nrw.commons.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.free.nrw.commons.bookmarks.category.BookmarkCategoriesDao
import fr.free.nrw.commons.bookmarks.category.BookmarksCategoryModal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModal for [CategoryDetailsActivity]
 */
class CategoryDetailsViewModel(
    private val bookmarkCategoriesDao: BookmarkCategoriesDao
) : ViewModel() {

    private val _bookmarkState = MutableStateFlow(false)
    val bookmarkState = _bookmarkState.asStateFlow()


    /**
     * Used to check if bookmark exists for the given category in DB
     * based on that bookmark state is updated
     * @param categoryName
     */
    fun onCheckIfBookmarked(categoryName: String) {
        viewModelScope.launch {
            val isBookmarked = bookmarkCategoriesDao.doesExist(categoryName)
            _bookmarkState.update {
                isBookmarked
            }
        }
    }

    /**
     * Handles event when bookmark button is clicked from view
     * based on that category is bookmarked or removed in/from in the DB
     * and bookmark state is update as well
     * @param categoryName
     */
    fun onBookmarkClick(categoryName: String) {
        if (_bookmarkState.value) {
            deleteBookmark(categoryName)
            _bookmarkState.update {
                false
            }
        } else {
            addBookmark(categoryName)
            _bookmarkState.update {
                true
            }
        }
    }


    /**
     * Add bookmark into DB
     *
     * @param categoryName
     */
    private fun addBookmark(categoryName: String) {
        viewModelScope.launch {
            val categoryItem = BookmarksCategoryModal(
                categoryName = categoryName
            )

            bookmarkCategoriesDao.insert(categoryItem)
        }
    }


    /**
     * Delete bookmark from DB
     *
     * @param categoryName
     */
    private fun deleteBookmark(categoryName: String) {
        viewModelScope.launch {
            bookmarkCategoriesDao.delete(
                BookmarksCategoryModal(
                    categoryName = categoryName
                )
            )
        }
    }

    /**
     * View model factory to create [CategoryDetailsViewModel]
     *
     * @property bookmarkCategoriesDao
     * @constructor Create empty View model factory
     */
    class ViewModelFactory @Inject constructor(
        private val bookmarkCategoriesDao: BookmarkCategoriesDao
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            if (modelClass.isAssignableFrom(CategoryDetailsViewModel::class.java)) {
                CategoryDetailsViewModel(bookmarkCategoriesDao) as T
            } else {
                throw IllegalArgumentException("Unknown class name")
            }
    }
}
