package fr.free.nrw.commons.contributions;

import androidx.paging.DataSource.Factory;
import io.reactivex.Completable;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Single;

/**
 * The repository class for contributions
 */
public class ContributionsRepository {

    private ContributionsLocalDataSource localDataSource;

    @Inject
    public ContributionsRepository(ContributionsLocalDataSource localDataSource) {
        this.localDataSource = localDataSource;
    }

    /**
     * Fetch default number of contributions to be show, based on user preferences
     */
    public String getString(String key) {
        return localDataSource.getString(key);
    }

    /**
     * Deletes a failed upload from DB
     *
     * @param contribution
     * @return
     */
    public Completable deleteContributionFromDB(Contribution contribution) {
        return localDataSource.deleteContribution(contribution);
    }

    /**
     * Deletes contributions from the database with specific states.
     *
     * @param states The states of the contributions to delete.
     * @return A Completable indicating the result of the operation.
     */
    public Completable deleteContributionsFromDBWithStates(List<Integer> states) {
        return localDataSource.deleteContributionsWithStates(states);
    }

    /**
     * Get contribution object with title
     *
     * @param fileName
     * @return
     */
    public Contribution getContributionWithFileName(String fileName) {
        return localDataSource.getContributionWithFileName(fileName);
    }

    public Factory<Integer, Contribution> fetchContributions() {
        return localDataSource.getContributions();
    }

    /**
     * Fetches contributions with specific states.
     *
     * @param states The states of the contributions to fetch.
     * @return A DataSource factory for paginated contributions with the specified states.
     */
    public Factory<Integer, Contribution> fetchContributionsWithStates(List<Integer> states) {
        return localDataSource.getContributionsWithStates(states);
    }

    /**
     * Fetches contributions with specific states sorted by the date the upload started.
     *
     * @param states The states of the contributions to fetch.
     * @return A DataSource factory for paginated contributions with the specified states sorted by
     * date upload started.
     */
    public Factory<Integer, Contribution> fetchContributionsWithStatesSortedByDateUploadStarted(
        List<Integer> states) {
        return localDataSource.getContributionsWithStatesSortedByDateUploadStarted(states);
    }

    public Single<List<Long>> save(List<Contribution> contributions) {
        return localDataSource.saveContributions(contributions);
    }

    public Completable save(Contribution contributions) {
        return localDataSource.saveContributions(contributions);
    }

    public void set(String key, long value) {
        localDataSource.set(key, value);
    }

    public Completable updateContribution(Contribution contribution) {
        return localDataSource.updateContribution(contribution);
    }

    /**
     * Updates the state of contributions with specific states.
     *
     * @param states   The current states of the contributions to update.
     * @param newState The new state to set.
     * @return A Completable indicating the result of the operation.
     */
    public Completable updateContributionsWithStates(List<Integer> states, int newState) {
        return localDataSource.updateContributionsWithStates(states, newState);
    }
}
