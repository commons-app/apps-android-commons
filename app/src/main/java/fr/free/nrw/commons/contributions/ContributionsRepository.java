package fr.free.nrw.commons.contributions;

import android.database.Cursor;

import javax.inject.Inject;

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
    public int get(String uploadsShowing) {
        return localDataSource.get(uploadsShowing);
    }


    /**
     * Get contribution object from cursor from LocalDataSource
     * @param cursor
     * @return
     */
    public Contribution getContributionFromCursor(Cursor cursor) {
        return localDataSource.getContributionFromCursor(cursor);
    }

    /**
     * Deletes a failed upload from DB
     * @param contribution
     */
    public void deleteContributionFromDB(Contribution contribution) {
        localDataSource.deleteContribution(contribution);
    }
}
