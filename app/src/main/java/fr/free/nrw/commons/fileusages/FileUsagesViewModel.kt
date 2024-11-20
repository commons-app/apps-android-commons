package fr.free.nrw.commons.fileusages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val ITEMS_PER_PAGE = 30

class FileUsagesViewModel(
    private val fileUsagesPagingSource: FileUsagesPagingSource,
    private val globalFileUsagesPagingSource: GlobalFileUsagesPagingSource,
    private val okHttpJsonApiClient: OkHttpJsonApiClient
) : ViewModel() {
    private val _screenState: MutableStateFlow<FileUsagesScreenState> =
        MutableStateFlow(FileUsagesScreenState())
    val screenState = _screenState.asStateFlow()

    lateinit var fileUsagesPagingData: Flow<PagingData<FileUsagesResponse.FileUsage>>

    lateinit var globalFileUsagesPagingData: Flow<PagingData<UiModel>>

    fun setFileName(fileName: String?) {
        // for testing
        val testingFileName = "File:Ratargul 785 retouched.jpg"
        // get the file name and use it to create paging source
        //TODO: [Parry} handle if null
        if (fileName != null) {
            if (!::fileUsagesPagingData.isInitialized) {
                initFileUsagesPagingData()
                initGlobalFileUsagesPagingData()
                fileUsagesPagingSource.fileName = testingFileName
                globalFileUsagesPagingSource.fileName = testingFileName
            }

        }
    }

    private fun initFileUsagesPagingData() {
        fileUsagesPagingData =
            Pager(config = PagingConfig(pageSize = ITEMS_PER_PAGE),
                pagingSourceFactory = { fileUsagesPagingSource })
                .flow.cachedIn(viewModelScope)
    }

    private fun initGlobalFileUsagesPagingData() {
        globalFileUsagesPagingData =
            Pager(config = PagingConfig(pageSize = ITEMS_PER_PAGE),
                pagingSourceFactory = { globalFileUsagesPagingSource })
                .flow.cachedIn(viewModelScope).map {
                    it.map {
                        UiModel.ItemModel(
                            item = GlobalUsageItem(
                                group = it.wiki,
                                title = it.title
                            )
                        )
                    }.insertSeparators { before, after ->
                        // what about when after item is null (i.e when last item)
                        // also does direction would affect this logic???
                        if (before == null || before.item.group != after?.item?.group) {
                            if(after != null){
                                UiModel.HeaderModel(group = after.item.group)
                            } else null
                        } else null
                    }
                }
    }

    fun getOtherWikisUsage() {
//            val fileName = media!!.filename
        // for testing
        val fileName = "File:Commons-logo.svg"
        _screenState.update { it.copy(isCommonsScreenLoading = true) }
        okHttpJsonApiClient.getGlobalFileUsages(fileName, null)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { globalResponse ->
                _screenState.update {
                    it.copy(
                        isCommonsScreenLoading = false,
                        otherWikisUsagesList = globalResponse.query.pages.first().globalUsage
                    )
                }
            }
    }

}

data class FileUsagesScreenState(
    //TODO: should we have separate loading indicators?
    val isCommonsScreenLoading: Boolean = false,
    val isOtherWikisScreenLoading: Boolean = false,
    val commonsFileUsagesList: List<FileUsagesResponse.FileUsage>? = null,
    val otherWikisUsagesList: List<GlobalFileUsage>? = null
)

class FileUsagesViewModelProviderFactory
@Inject constructor(
    private val fileUsagesPagingSource: FileUsagesPagingSource,
    private val globalFileUsagesPagingSource: GlobalFileUsagesPagingSource,
    private val okHttpJsonApiClient: OkHttpJsonApiClient
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FileUsagesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FileUsagesViewModel(
                fileUsagesPagingSource,
                globalFileUsagesPagingSource,
                okHttpJsonApiClient
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}