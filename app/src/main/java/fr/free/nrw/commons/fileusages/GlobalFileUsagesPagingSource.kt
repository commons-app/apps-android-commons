package fr.free.nrw.commons.fileusages

import androidx.paging.PagingSource
import androidx.paging.PagingState
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GlobalFileUsagesPagingSource @Inject constructor(
    private val okHttpJsonApiClient: OkHttpJsonApiClient
) : PagingSource<GlobalFileUsagesResponse.GlobalContinue, GlobalFileUsagesResponse.GlobalFileUsage>() {

    lateinit var fileName: String

    override fun getRefreshKey(
        state: PagingState<GlobalFileUsagesResponse.GlobalContinue, GlobalFileUsagesResponse.GlobalFileUsage>
    ): GlobalFileUsagesResponse.GlobalContinue? {
        return null
    }

    override suspend fun load(
        params: LoadParams<GlobalFileUsagesResponse.GlobalContinue>
    ): LoadResult<GlobalFileUsagesResponse.GlobalContinue, GlobalFileUsagesResponse.GlobalFileUsage> {
        return withContext(Dispatchers.IO) {
            try {
                val continueElement: GlobalFileUsagesResponse.GlobalContinue? = params.key

                val response = okHttpJsonApiClient.getGlobalFileUsages(
                    fileName,
                    params.loadSize,
                    continueElement
                )
                    .blockingFirst()

                val isContinued = response.continueResponse != null

                val nextKey = if (isContinued) {
                    response.continueResponse!!
                } else {
                    null
                }

                println("Test response for curernt page is nulL?  ${response == null}")
                println("Test continue response for curernt page ${response.continueResponse}")
                println("Test Loaded for current key $continueElement")
                println("Test Returned result ${response.continueResponse ?: "no key in the response"}")
                println("Test Next key $nextKey")

                // this comes null if there are no contributions to show for a file
                //TODO: handle this scenario
                val data = response.query.pages.first().globalUsage
                println(data)


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