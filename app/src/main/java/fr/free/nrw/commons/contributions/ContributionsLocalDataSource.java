package fr.free.nrw.commons.contributions;

import androidx.paging.DataSource.Factory;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/**
 * The LocalDataSource class for Contributions
 */
class ContributionsLocalDataSource {

    private final ContributionDao contributionDao;

    @Inject
    public ContributionsLocalDataSource(ContributionDao contributionDao) {
        this.contributionDao = contributionDao;
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


}
