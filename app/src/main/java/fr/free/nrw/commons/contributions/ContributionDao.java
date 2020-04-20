package fr.free.nrw.commons.contributions;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import androidx.room.Update;
import io.reactivex.disposables.Disposable;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public abstract class ContributionDao {

    @Query("SELECT * FROM contribution order by dateUploaded DESC")
    abstract LiveData<List<Contribution>> fetchContributions();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract Single<Long> save(Contribution contribution);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract Single<List<Long>> save(List<Contribution> contribution);

    @Delete
    public abstract Single<Integer> delete(Contribution contribution);

    @Query("SELECT * from contribution WHERE contentProviderUri=:uri")
    public abstract List<Contribution> getContributionWithUri(String uri);

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
