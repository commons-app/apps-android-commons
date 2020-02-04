package fr.free.nrw.commons.contributions;

import android.text.TextUtils;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.db.AppDatabase;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import io.reactivex.Observable;

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
    public int get(String key) {
        String uploads = defaultKVStore.getString(key);
        if(TextUtils.isEmpty(uploads)){
            return 0;
        }
        return Integer.parseInt(uploads);
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
     */
    public void deleteContribution(Contribution contribution) {
        contributionDao.delete(contribution);
    }

    public Observable<List<Contribution>> getContributions(int numberOfContributions) {
        return contributionDao.fetchContributions(numberOfContributions);
    }
}
