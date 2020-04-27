package fr.free.nrw.commons.contributions;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;

@Dao
public abstract class ContributionDao {

    @Query("SELECT * FROM contribution order by dateUploaded DESC")
    abstract LiveData<List<Contribution>> fetchContributions();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract Single<Long> save(Contribution contribution);

    public Completable deleteAllAndSave(List<Contribution> contributions){
        return Completable.fromAction(() -> deleteAllAndSaveTransaction(contributions));
    }

    @Transaction
    public void deleteAllAndSaveTransaction(List<Contribution> contributions){
        deleteAll(Contribution.STATE_COMPLETED);
        save(contributions);
    }

    @Insert
    public abstract void save(List<Contribution> contribution);

    @Delete
    public abstract Single<Integer> delete(Contribution contribution);

    @Query("SELECT * from contribution WHERE filename=:fileName")
    public abstract List<Contribution> getContributionWithTitle(String fileName);

    @Query("UPDATE contribution SET state=:state WHERE state in (:toUpdateStates)")
    public abstract Single<Integer> updateStates(int state, int[] toUpdateStates);

    @Query("Delete FROM contribution")
    public abstract void deleteAll();

    @Query("Delete FROM contribution WHERE state = :state")
    public abstract void deleteAll(int state);

    @Update
    public abstract Single<Integer> update(Contribution contribution);
}
