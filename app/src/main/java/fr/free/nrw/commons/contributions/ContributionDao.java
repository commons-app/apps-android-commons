package fr.free.nrw.commons.contributions;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import io.reactivex.Single;

@Dao
public abstract class ContributionDao {

    @Query("SELECT * FROM contribution order by dateUploaded DESC")
    abstract LiveData<List<Contribution>> fetchContributions();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void save(Contribution contribution);

    @Transaction
    public void  deleteAllAndSave(List<Contribution> contributions){
        deleteAll();
        save(contributions);
    }

    @Insert
    public abstract void save(List<Contribution> contribution);

    @Delete
    public abstract Single<Integer> delete(Contribution contribution);

    @Query("SELECT * from contribution WHERE contentProviderUri=:uri")
    public abstract List<Contribution> getContributionWithUri(String uri);

    @Query("SELECT * from contribution WHERE filename=:fileName")
    public abstract List<Contribution> getContributionWithTitle(String fileName);

    @Query("UPDATE contribution SET state=:state WHERE state in (:toUpdateStates)")
    public abstract void updateStates(int state, int[] toUpdateStates);

    @Query("Delete FROM contribution")
    public abstract void deleteAll();
}
