package fr.free.nrw.commons.fileusages

import androidx.paging.PagingSource
import androidx.paging.PagingState
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FileUsagesPagingSource @Inject constructor(
    private val okHttpJsonApiClient: OkHttpJsonApiClient
) : PagingSource<FileUsagesResponse.FileUsagesContinue, FileUsagesResponse.FileUsage>() {

    lateinit var fileName: String


    override fun getRefreshKey(
        state: PagingState<FileUsagesResponse.FileUsagesContinue, FileUsagesResponse.FileUsage>
    ): FileUsagesResponse.FileUsagesContinue? {
        return null
    }

    override suspend fun load(
        params: LoadParams<FileUsagesResponse.FileUsagesContinue>
    ): LoadResult<FileUsagesResponse.FileUsagesContinue, FileUsagesResponse.FileUsage> {
        return withContext(Dispatchers.IO) {
            try {
                val continueElement: FileUsagesResponse.FileUsagesContinue? = params.key


                println("load with size ${params.loadSize}")
                val response = okHttpJsonApiClient.getFileUsagesOnCommons(
                    fileName,
                    params.loadSize,
                    continueElement
                ).blockingFirst()

                val isContinued = response.continueResponse != null

                val nextKey = if (isContinued) {
                    response.continueResponse
                } else {
                    null
                }

                // this comes null if there are no contributions to show for a file
                //TODO: handle this scenario
                val data = response.query.pages.first().fileUsage

                if (data == null) {
                    throw IllegalStateException("No contributions for this file")
                } else {
                    LoadResult.Page(
                        data = data,
                        prevKey = null,
                        nextKey = nextKey
                    )
                }

            } catch (e: Throwable) {
                println("error ${e.message}")
                LoadResult.Error(e)
            }
        }
    }
}

