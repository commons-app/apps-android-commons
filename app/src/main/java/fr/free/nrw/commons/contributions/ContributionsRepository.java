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

}
