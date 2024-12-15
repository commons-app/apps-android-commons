package fr.free.nrw.commons.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.free.nrw.commons.bookmarks.category.BookmarkCategoriesDao
import fr.free.nrw.commons.bookmarks.models.BookmarksCategoryModal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class CategoryDetailsViewModel(
    private val bookmarkCategoriesDao: BookmarkCategoriesDao
) : ViewModel() {

    private val _bookmarkState = MutableStateFlow(false)
    val bookmarkState = _bookmarkState.asStateFlow()

    fun onCheckIfBookmarked(categoryName: String) {
        viewModelScope.launch {
            val isBookmarked = bookmarkCategoriesDao.doesExist(categoryName)
            _bookmarkState.update {
                isBookmarked
            }
        }
    }

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

    private fun addBookmark(categoryName: String) {
        viewModelScope.launch {
            // TODO [Parry] view only knows about `name` see if we can have more data
            val categoryItem = BookmarksCategoryModal(
                categoryName = categoryName
            )

            bookmarkCategoriesDao.insert(categoryItem)
        }
    }

    private fun deleteBookmark(categoryName: String) {
        viewModelScope.launch {
            bookmarkCategoriesDao.delete(
                BookmarksCategoryModal(
                    categoryName = categoryName
                )
            )
        }
    }

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