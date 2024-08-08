package fr.free.nrw.commons.contributions;

import android.database.sqlite.SQLiteException;
import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.Calendar;
import java.util.List;
import timber.log.Timber;

@Dao
public abstract class ContributionDao {

    @Query("SELECT * FROM contribution order by media_dateUploaded DESC")
    abstract DataSource.Factory<Integer, Contribution> fetchContributions();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void saveSynchronous(Contribution contribution);

    public Completable save(final Contribution contribution) {
        return Completable
            .fromAction(() -> {
                contribution.setDateModified(Calendar.getInstance().getTime());
                if (contribution.getDateUploadStarted() == null) {
                    contribution.setDateUploadStarted(Calendar.getInstance().getTime());
                }
                saveSynchronous(contribution);
            });
    }

    @Transaction
    public void deleteAndSaveContribution(final Contribution oldContribution,
        final Contribution newContribution) {
        deleteSynchronous(oldContribution);
        saveSynchronous(newContribution);
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract Single<List<Long>> save(List<Contribution> contribution);

    @Delete
    public abstract void deleteSynchronous(Contribution contribution);

    /**
     * Deletes contributions with specific states from the database.
     *
     * @param states The states of the contributions to delete.
     * @throws SQLiteException If an SQLite error occurs.
     */
    @Query("DELETE FROM contribution WHERE state IN (:states)")
    public abstract void deleteContributionsWithStatesSynchronous(List<Integer> states)
        throws SQLiteException;

    public Completable delete(final Contribution contribution) {
        return Completable
            .fromAction(() -> deleteSynchronous(contribution));
    }

    /**
     * Deletes contributions with specific states from the database.
     *
     * @param states The states of the contributions to delete.
     * @return A Completable indicating the result of the operation.
     */
    public Completable deleteContributionsWithStates(List<Integer> states) {
        return Completable
            .fromAction(() -> deleteContributionsWithStatesSynchronous(states));
    }

    @Query("SELECT * from contribution WHERE media_filename=:fileName")
    public abstract List<Contribution> getContributionWithTitle(String fileName);

    @Query("SELECT * from contribution WHERE pageId=:pageId")
    public abstract Contribution getContribution(String pageId);

    @Query("SELECT * from contribution WHERE state IN (:states) order by media_dateUploaded DESC")
    public abstract Single<List<Contribution>> getContribution(List<Integer> states);

    /**
     * Gets contributions with specific states in descending order by the date they were uploaded.
     *
     * @param states The states of the contributions to fetch.
     * @return A DataSource factory for paginated contributions with the specified states.
     */
    @Query("SELECT * from contribution WHERE state IN (:states) order by media_dateUploaded DESC")
    public abstract DataSource.Factory<Integer, Contribution> getContributions(
        List<Integer> states);

    /**
     * Gets contributions with specific states in ascending order by the date the upload started.
     *
     * @param states The states of the contributions to fetch.
     * @return A DataSource factory for paginated contributions with the specified states.
     */
    @Query("SELECT * from contribution WHERE state IN (:states) order by dateUploadStarted ASC")
    public abstract DataSource.Factory<Integer, Contribution> getContributionsSortedByDateUploadStarted(
        List<Integer> states);

    @Query("SELECT COUNT(*) from contribution WHERE state in (:toUpdateStates)")
    public abstract Single<Integer> getPendingUploads(int[] toUpdateStates);

    @Query("Delete FROM contribution")
    public abstract void deleteAll() throws SQLiteException;

    @Update
    public abstract void updateSynchronous(Contribution contribution);

    /**
     * Updates the state of contributions with specific states.
     *
     * @param states   The current states of the contributions to update.
     * @param newState The new state to set.
     */
    @Query("UPDATE contribution SET state = :newState WHERE state IN (:states)")
    public abstract void updateContributionsState(List<Integer> states, int newState);

    public Completable update(final Contribution contribution) {
        return Completable
            .fromAction(() -> {
                contribution.setDateModified(Calendar.getInstance().getTime());
                updateSynchronous(contribution);
            });
    }

    /**
     * Updates the state of contributions with specific states asynchronously.
     *
     * @param states   The current states of the contributions to update.
     * @param newState The new state to set.
     * @return A Completable indicating the result of the operation.
     */
    public Completable updateContributionsWithStates(List<Integer> states, int newState) {
        return Completable
            .fromAction(() -> {
                updateContributionsState(states, newState);
            });
    }
}
