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

    public Completable delete(final Contribution contribution) {
        return Completable
            .fromAction(() -> deleteSynchronous(contribution));
    }

    @Query("SELECT * from contribution WHERE media_filename=:fileName")
    public abstract List<Contribution> getContributionWithTitle(String fileName);

    @Query("SELECT * from contribution WHERE pageId=:pageId")
    public abstract Contribution getContribution(String pageId);

    @Query("SELECT * from contribution WHERE state IN (:states) order by media_dateUploaded DESC")
    public abstract Single<List<Contribution>> getContribution(List<Integer> states);

    @Query("SELECT COUNT(*) from contribution WHERE state in (:toUpdateStates)")
    public abstract Single<Integer> getPendingUploads(int[] toUpdateStates);

    @Query("Delete FROM contribution")
    public abstract void deleteAll() throws SQLiteException;

    @Update
    public abstract void updateSynchronous(Contribution contribution);

    public Completable update(final Contribution contribution) {
        return Completable
            .fromAction(() -> {
                contribution.setDateModified(Calendar.getInstance().getTime());
                updateSynchronous(contribution);
            });
    }
}
