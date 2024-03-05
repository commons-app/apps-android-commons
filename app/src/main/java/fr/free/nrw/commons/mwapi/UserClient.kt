package fr.free.nrw.commons.mwapi

import fr.free.nrw.commons.utils.DateUtil
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResult
import fr.free.nrw.commons.wikidata.mwapi.UserInfo
import io.reactivex.Single
import java.text.ParseException
import java.util.Date
import javax.inject.Inject

class UserClient @Inject constructor(private val userInterface: UserInterface) {
    /**
     * Checks to see if a user is currently blocked from Commons
     *
     * @return whether or not the user is blocked from Commons
     */
    fun isUserBlockedFromCommons(): Single<Boolean> =
        userInterface.getUserBlockInfo()
            .map(::processBlockExpiry)
            .single(false)

    @Throws(ParseException::class)
    private fun processBlockExpiry(response: MwQueryResponse): Boolean {
        val blockExpiry = response.query()?.userInfo()?.blockexpiry()
        return when {
            blockExpiry.isNullOrEmpty() -> false
            "infinite" == blockExpiry -> true
            else -> {
                val endDate = DateUtil.iso8601DateParse(blockExpiry)
                val current = Date()
                endDate.after(current)
            }
        }
    }
}
