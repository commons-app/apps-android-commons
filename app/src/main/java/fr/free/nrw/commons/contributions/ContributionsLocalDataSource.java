package fr.free.nrw.commons.contributions;

import androidx.paging.DataSource.Factory;
import io.reactivex.Completable;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

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
            ContributionDao contributionDao) {
        this.defaultKVStore = defaultKVStore;
        this.contributionDao = contributionDao;
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
        if(!contributionWithUri.isEmpty()){
            return contributionWithUri.get(0);
        }
        return null;
    }

    /**
     * Remove a contribution from the contributions table
     * @param contribution
     * @return
     */
    public Completable deleteContribution(Contribution contribution) {
        return contributionDao.delete(contribution);
    }

    public Factory<Integer, Contribution> getContributions() {
        return contributionDao.fetchContributions();
    }

    public Single<List<Long>> saveContributions(List<Contribution> contributions) {
        List<Contribution> contributionList = new ArrayList<>();
        for(Contribution contribution: contributions) {
            Contribution oldContribution = contributionDao.getContribution(contribution.getPageId());
            if(oldContribution != null) {
                contribution.setWikidataPlace(oldContribution.getWikidataPlace());
            }
            contributionList.add(contribution);
        }
        return contributionDao.save(contributionList);
    }

    public void set(String key, long value) {
        defaultKVStore.putLong(key,value);
    }

    public Completable updateContribution(Contribution contribution) {
        return contributionDao.update(contribution);
    }
}
