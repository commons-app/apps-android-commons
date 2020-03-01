package fr.free.nrw.commons.contributions;

import android.database.Cursor;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.kvstore.JsonKvStore;

/**
 * The LocalDataSource class for Contributions
 */
class ContributionsLocalDataSource {

    private final ContributionDao contributionsDao;
    private final JsonKvStore defaultKVStore;

    @Inject
    public ContributionsLocalDataSource(
            @Named("default_preferences") JsonKvStore defaultKVStore,
            ContributionDao contributionDao) {
        this.defaultKVStore = defaultKVStore;
        this.contributionsDao = contributionDao;
    }

    /**
     * Fetch default number of contributions to be show, based on user preferences
     */
    public int get(String key) {
        return defaultKVStore.getInt(key);
    }

    /**
     * Get contribution object from cursor
     * @param cursor
     * @return
     */
    public Contribution getContributionFromCursor(Cursor cursor) {
        return contributionsDao.fromCursor(cursor);
    }

    /**
     * Remove a contribution from the contributions table
     * @param contribution
     */
    public void deleteContribution(Contribution contribution) {
        contributionsDao.delete(contribution);
    }
}
