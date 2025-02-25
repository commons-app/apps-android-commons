package fr.free.nrw.commons.contributions

import androidx.paging.DataSource
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject

/**
 * The repository class for contributions
 */
class ContributionsRepository @Inject constructor(private val localDataSource: ContributionsLocalDataSource) {
    /**
     * Fetch default number of contributions to be show, based on user preferences
     */
    fun getString(key: String): String? {
        return localDataSource.getString(key)
    }

    /**
     * Deletes a failed upload from DB
     *
     * @param contribution
     * @return
     */
    fun deleteContributionFromDB(contribution: Contribution): Completable {
        return localDataSource.deleteContribution(contribution)
    }

    /**
     * Deletes contributions from the database with specific states.
     *
     * @param states The states of the contributions to delete.
     * @return A Completable indicating the result of the operation.
     */
    fun deleteContributionsFromDBWithStates(states: List<Int>): Completable {
        return localDataSource.deleteContributionsWithStates(states)
    }

    /**
     * Get contribution object with title
     *
     * @param fileName
     * @return
     */
    fun getContributionWithFileName(fileName: String): Contribution {
        return localDataSource.getContributionWithFileName(fileName)
    }

    fun fetchContributions(): DataSource.Factory<Int, Contribution> {
        return localDataSource.getContributions()
    }

    /**
     * Fetches contributions with specific states.
     *
     * @param states The states of the contributions to fetch.
     * @return A DataSource factory for paginated contributions with the specified states.
     */
    fun fetchContributionsWithStates(states: List<Int>): DataSource.Factory<Int, Contribution> {
        return localDataSource.getContributionsWithStates(states)
    }

    /**
     * Fetches contributions with specific states sorted by the date the upload started.
     *
     * @param states The states of the contributions to fetch.
     * @return A DataSource factory for paginated contributions with the specified states sorted by
     * date upload started.
     */
    fun fetchContributionsWithStatesSortedByDateUploadStarted(
        states: List<Int>
    ): DataSource.Factory<Int, Contribution> {
        return localDataSource.getContributionsWithStatesSortedByDateUploadStarted(states)
    }

    fun save(contributions: List<Contribution>): Single<List<Long>> {
        return localDataSource.saveContributions(contributions)
    }

    fun save(contributions: Contribution): Completable {
        return localDataSource.saveContributions(contributions)
    }

    operator fun set(key: String, value: Long) {
        localDataSource.set(key, value)
    }

    fun updateContribution(contribution: Contribution): Completable {
        return localDataSource.updateContribution(contribution)
    }

    /**
     * Updates the state of contributions with specific states.
     *
     * @param states   The current states of the contributions to update.
     * @param newState The new state to set.
     * @return A Completable indicating the result of the operation.
     */
    fun updateContributionsWithStates(states: List<Int>, newState: Int): Completable {
        return localDataSource.updateContributionsWithStates(states, newState)
    }
}
