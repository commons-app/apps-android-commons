package fr.free.nrw.commons.media

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.free.nrw.commons.R
import fr.free.nrw.commons.fileusages.FileUsagesUiModel
import fr.free.nrw.commons.fileusages.toUiModel
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Show where file is being used on Commons and oher wikis.
 */
class MediaDetailViewModel(
    private val applicationContext: Context,
    private val okHttpJsonApiClient: OkHttpJsonApiClient
) :
    ViewModel() {

    private val _commonsContainerState =
        MutableStateFlow<FileUsagesContainerState>(FileUsagesContainerState.Initial)
    val commonsContainerState = _commonsContainerState.asStateFlow()

    private val _globalContainerState =
        MutableStateFlow<FileUsagesContainerState>(FileUsagesContainerState.Initial)
    val globalContainerState = _globalContainerState.asStateFlow()

    fun loadFileUsagesCommons(fileName: String) {

        viewModelScope.launch {

            _commonsContainerState.update { FileUsagesContainerState.Loading }

            try {
                val result =
                    okHttpJsonApiClient.getFileUsagesOnCommons(fileName, 10)

                val data = result?.query?.pages?.first()?.fileUsage?.map { it.toUiModel() }

                _commonsContainerState.update { FileUsagesContainerState.Success(data = data) }

            } catch (e: Exception) {

                _commonsContainerState.update {
                    FileUsagesContainerState.Error(
                        errorMessage = applicationContext.getString(
                            R.string.error_while_loading
                        )
                    )
                }

                Timber.e(e, javaClass.simpleName)

            }
        }

    }

    fun loadGlobalFileUsages(fileName: String) {

        viewModelScope.launch {

            _globalContainerState.update { FileUsagesContainerState.Loading }

            try {
                val result = okHttpJsonApiClient.getGlobalFileUsages(fileName, 10)

                val data = result?.query?.pages?.first()?.fileUsage?.map { it.toUiModel() }

                _globalContainerState.update { FileUsagesContainerState.Success(data = data) }

            } catch (e: Exception) {
                _globalContainerState.update {
                    FileUsagesContainerState.Error(
                        errorMessage = applicationContext.getString(
                            R.string.error_while_loading
                        )
                    )
                }

                Timber.e(e, javaClass.simpleName)

            }
        }

    }

    sealed class FileUsagesContainerState {
        object Initial : FileUsagesContainerState()
        object Loading : FileUsagesContainerState()
        data class Success(val data: List<FileUsagesUiModel>?) : FileUsagesContainerState()
        data class Error(val errorMessage: String) : FileUsagesContainerState()
    }

    class MediaDetailViewModelProviderFactory
    @Inject constructor(
        private val okHttpJsonApiClient: OkHttpJsonApiClient,
        private val applicationContext: Context
    ) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MediaDetailViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MediaDetailViewModel(applicationContext, okHttpJsonApiClient) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
