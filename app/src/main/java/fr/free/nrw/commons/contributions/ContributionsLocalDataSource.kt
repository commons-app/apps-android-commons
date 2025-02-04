package fr.free.nrw.commons.contributions

import androidx.paging.DataSource
import fr.free.nrw.commons.kvstore.JsonKvStore
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Named

/**
 * The LocalDataSource class for Contributions
 */
class ContributionsLocalDataSource @Inject constructor(
    @param:Named("default_preferences") private val defaultKVStore: JsonKvStore,
    private val contributionDao: ContributionDao
) {
    /**
     * Fetch default number of contributions to be show, based on user preferences
     */
    fun getString(key: String): String? {
        return defaultKVStore.getString(key)
    }

    /**
     * Fetch default number of contributions to be show, based on user preferences
     */
    fun getLong(key: String): Long {
        return defaultKVStore.getLong(key)
    }

    /**
     * Get contribution object from cursor
     *
     * @param uri
     * @return
     */
    fun getContributionWithFileName(uri: String): Contribution {
        val contributionWithUri = contributionDao.getContributionWithTitle(uri)
        if (contributionWithUri.isNotEmpty()) {
            return contributionWithUri[0]
        }
        throw IllegalArgumentException("Contribution not found for URI: $uri")
    }

    /**
     * Remove a contribution from the contributions table
     *
     * @param contribution
     * @return
     */
    fun deleteContribution(contribution: Contribution): Completable {
        return contributionDao.delete(contribution)
    }

    /**
     * Deletes contributions with specific states.
     *
     * @param states The states of the contributions to delete.
     * @return A Completable indicating the result of the operation.
     */
    fun deleteContributionsWithStates(states: List<Int>): Completable {
        return contributionDao.deleteContributionsWithStates(states)
    }

    fun getContributions(): DataSource.Factory<Int, Contribution> {
        return contributionDao.fetchContributions()
    }

    /**
     * Fetches contributions with specific states.
     *
     * @param states The states of the contributions to fetch.
     * @return A DataSource factory for paginated contributions with the specified states.
     */
    fun getContributionsWithStates(states: List<Int>): DataSource.Factory<Int, Contribution> {
        return contributionDao.getContributions(states)
    }

    /**
     * Fetches contributions with specific states sorted by the date the upload started.
     *
     * @param states The states of the contributions to fetch.
     * @return A DataSource factory for paginated contributions with the specified states sorted by
     * date upload started.
     */
    fun getContributionsWithStatesSortedByDateUploadStarted(
        states: List<Int>
    ): DataSource.Factory<Int, Contribution> {
        return contributionDao.getContributionsSortedByDateUploadStarted(states)
    }

    fun saveContributions(contributions: List<Contribution>): Single<List<Long>> {
        val contributionList: MutableList<Contribution> = ArrayList()
        for (contribution in contributions) {
            val oldContribution = contributionDao.getContribution(
                contribution.pageId
            )
            if (oldContribution != null) {
                contribution.wikidataPlace = oldContribution.wikidataPlace
            }
            contributionList.add(contribution)
        }
        return contributionDao.save(contributionList)
    }

    fun saveContributions(contribution: Contribution): Completable {
        return contributionDao.save(contribution)
    }

    fun set(key: String, value: Long) {
        defaultKVStore.putLong(key, value)
    }

    fun updateContribution(contribution: Contribution): Completable {
        return contributionDao.update(contribution)
    }

    fun updateContributionsWithStates(states: List<Int>, newState: Int): Completable {
        return contributionDao.updateContributionsWithStates(states, newState)
    }
}
