package fr.free.nrw.commons.fileusages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FileUsagesViewModel(private val okHttpJsonApiClient: OkHttpJsonApiClient) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    private var media: Media? = null

    private val _screenState: MutableStateFlow<FileUsagesScreenState> =
        MutableStateFlow(FileUsagesScreenState())
    val screenState = _screenState.asStateFlow()

    init {
//        getCommonsFileUsages()
    }

    fun setMedia(media: Media?) {
        this.media = media
    }

    fun getCommonsFileUsages() {
        if (media != null) {
            println("file name is ${media!!.filename}")
//            val fileName = media!!.filename
            // for testing
            val fileName = "File:Commons-logo.svg"
            _screenState.update { it.copy(isCommonsScreenLoading = true) }
            compositeDisposable.add(
                okHttpJsonApiClient.getFileUsagesOnCommons(fileName)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { fileUsageResponse ->
                        //TODO assuming page is always going to be just one.
                        _screenState.update {
                            it.copy(
                                isCommonsScreenLoading = false,
                                commonsFileUsagesList = fileUsageResponse.query.pages.first().fileUsage
                            )
                        }
                    }
            )
        } else {
            println("media object is null")
        }
    }

    fun getOtherWikisUsages() {
        if (media != null) {
            //            val fileName = media!!.filename
            // for testing
            val fileName = "File:Commons-logo.svg"
            _screenState.update { it.copy(isOtherWikisScreenLoading = true) }
            compositeDisposable.add(
                okHttpJsonApiClient.getGlobalFileUsages(fileName)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { fileUsageResponse ->
                        //TODO assuming page is always going to be just one.
                        _screenState.update {
                            it.copy(
                                isOtherWikisScreenLoading = false,
                                otherWikisUsagesList = fileUsageResponse.query.pages.first().globalUsage
                            )
                        }
                    }
            )
        }else{
            println("media object is null")
        }
    }

    fun disposeNetworkOperations() {
        compositeDisposable.clear()
    }

}

data class FileUsagesScreenState(
    //TODO: should we have separate loading indicators?
    val isCommonsScreenLoading: Boolean = false,
    val isOtherWikisScreenLoading: Boolean = false,
    val commonsFileUsagesList: List<FileUsage>? = null,
    val otherWikisUsagesList: List<GlobalFileUsage>? = null
)

class FileUsagesViewModelProviderFactory(private val okHttpJsonApiClient: OkHttpJsonApiClient) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FileUsagesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FileUsagesViewModel(okHttpJsonApiClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}