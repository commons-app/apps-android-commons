package fr.free.nrw.commons.delete

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Commons API calls needed to request the speedy deletion of a file
 */
interface SpeedyDeletionInterface {
    /**
     * Checks whether a file is used on any wiki, including Commons itself. One use is enough to
     * know that the file is used, so at most one page of each kind is asked for.
     *
     * @param title the file title, such as "File:Example.jpg"
     */
    @GET(
        "w/api.php?action=query&format=json&formatversion=2&prop=globalusage|fileusage" +
            "&gulimit=1&fulimit=1",
    )
    fun getFileUsage(
        @Query("titles") title: String,
    ): Single<FileUsageResponse>
}
