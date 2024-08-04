package fr.free.nrw.commons.contributions;

import androidx.paging.DataSource.Factory;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * The LocalDataSource class for Contributions
 */
class ContributionsLocalDataSource {

    private final ContributionDao contributionDao;
    private final JsonKvStore defaultKVStore;

    @Inject
    public ContributionsLocalDataSource(
        @Named("default_preferences") final JsonKvStore defaultKVStore,
        final ContributionDao contributionDao) {
        this.defaultKVStore = defaultKVStore;
        this.contributionDao = contributionDao;
    }

    /**
     * Fetch default number of contributions to be show, based on user preferences
     */
    public String getString(final String key) {
        return defaultKVStore.getString(key);
    }

    /**
     * Fetch default number of contributions to be show, based on user preferences
     */
    public long getLong(final String key) {
        return defaultKVStore.getLong(key);
    }

    /**
     * Get contribution object from cursor
     *
     * @param uri
     * @return
     */
    public Contribution getContributionWithFileName(final String uri) {
        final List<Contribution> contributionWithUri = contributionDao.getContributionWithTitle(
            uri);
        if (!contributionWithUri.isEmpty()) {
            return contributionWithUri.get(0);
        }
        return null;
    }

    /**
     * Remove a contribution from the contributions table
     *
     * @param contribution
     * @return
     */
    public Completable deleteContribution(final Contribution contribution) {
        return contributionDao.delete(contribution);
    }

    /**
     * Deletes contributions with specific states.
     *
     * @param states The states of the contributions to delete.
     * @return A Completable indicating the result of the operation.
     */
    public Completable deleteContributionsWithStates(List<Integer> states) {
        return contributionDao.deleteContributionsWithStates(states);
    }

    public Factory<Integer, Contribution> getContributions() {
        return contributionDao.fetchContributions();
    }

    /**
     * Fetches contributions with specific states.
     *
     * @param states The states of the contributions to fetch.
     * @return A DataSource factory for paginated contributions with the specified states.
     */
    public Factory<Integer, Contribution> getContributionsWithStates(List<Integer> states) {
        return contributionDao.getContributions(states);
    }

    /**
     * Fetches contributions with specific states sorted by the date the upload started.
     *
     * @param states The states of the contributions to fetch.
     * @return A DataSource factory for paginated contributions with the specified states sorted by
     * date upload started.
     */
    public Factory<Integer, Contribution> getContributionsWithStatesSortedByDateUploadStarted(
        List<Integer> states) {
        return contributionDao.getContributionsSortedByDateUploadStarted(states);
    }

    public Single<List<Long>> saveContributions(final List<Contribution> contributions) {
        final List<Contribution> contributionList = new ArrayList<>();
        for (final Contribution contribution : contributions) {
            final Contribution oldContribution = contributionDao.getContribution(
                contribution.getPageId());
            if (oldContribution != null) {
                contribution.setWikidataPlace(oldContribution.getWikidataPlace());
            }
            contributionList.add(contribution);
        }
        return contributionDao.save(contributionList);
    }

    public Completable saveContributions(Contribution contribution) {
        return contributionDao.save(contribution);
    }

    public void set(final String key, final long value) {
        defaultKVStore.putLong(key, value);
    }

    public Completable updateContribution(final Contribution contribution) {
        return contributionDao.update(contribution);
    }

    public Completable updateContributionsWithStates(List<Integer> states, int newState) {
        return contributionDao.updateContributionsWithStates(states, newState);
    }
}
