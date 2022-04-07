package fr.free.nrw.commons.contributions;

import androidx.paging.DataSource.Factory;
import fr.free.nrw.commons.contributions.models.Contribution;
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
     * @param contribution
     * @return
     */
    public Completable deleteContributionFromDB(Contribution contribution) {
        return localDataSource.deleteContribution(contribution);
    }

    /**
     * Get contribution object with title
     * @param fileName
     * @return
     */
    public Contribution getContributionWithFileName(String fileName) {
        return localDataSource.getContributionWithFileName(fileName);
    }

    public Factory<Integer, Contribution> fetchContributions() {
        return localDataSource.getContributions();
    }

    public Single<List<Long>> save(List<Contribution> contributions) {
        return localDataSource.saveContributions(contributions);
    }

    public Completable save(Contribution contributions){
        return localDataSource.saveContributions(contributions);
    }

    public void set(String key, long value) {
        localDataSource.set(key,value);
    }

    public Completable updateContribution(Contribution contribution) {
        return localDataSource.updateContribution(contribution);
    }
}
