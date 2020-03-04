package fr.free.nrw.commons.contributions;

import androidx.lifecycle.LiveData;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.db.AppDatabase;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import io.reactivex.Single;

/**
 * The LocalDataSource class for Contributions
 */
class ContributionsLocalDataSource {

    private final ContributionDao contributionDao;
    private final JsonKvStore defaultKVStore;

    @Inject
    public ContributionsLocalDataSource(
            @Named("default_preferences") JsonKvStore defaultKVStore,
            AppDatabase appDatabase) {
        this.defaultKVStore = defaultKVStore;
        this.contributionDao = appDatabase.getContributionDao();
    }

    /**
     * Fetch default number of contributions to be show, based on user preferences
     */
    public String getString(String key) {
        return defaultKVStore.getString(key);
    }

    /**
     * Fetch default number of contributions to be show, based on user preferences
     */
    public long getLong(String key) {
       return defaultKVStore.getLong(key);
    }

    /**
     * Get contribution object from cursor
     * @param uri
     * @return
     */
    public Contribution getContributionWithFileName(String uri) {
        List<Contribution> contributionWithUri = contributionDao.getContributionWithTitle(uri);
        if(null!=contributionWithUri && contributionWithUri.size()>0){
            return contributionWithUri.get(0);
        }
        return null;
    }

    /**
     * Remove a contribution from the contributions table
     * @param contribution
     * @return
     */
    public Single<Integer> deleteContribution(Contribution contribution) {
        return contributionDao.delete(contribution);
    }

    public LiveData<List<Contribution>> getContributions() {
        return contributionDao.fetchContributions();
    }

    public void saveContributions(List<Contribution> contributions) {
        contributionDao.deleteAllAndSave(contributions);
    }

    public void set(String key, long value) {
        defaultKVStore.putLong(key,value);
    }
}
