package fr.free.nrw.commons.contributions;

import androidx.lifecycle.LiveData;
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
import java.util.List;

@Dao
public abstract class ContributionDao {

  @Query("SELECT * FROM contribution order by dateUploaded DESC")
  abstract DataSource.Factory<Integer, Contribution> fetchContributions();

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public abstract void save(Contribution contribution);

  @Transaction
  public void deleteAndSaveContribution(Contribution oldContribution,
      Contribution newContribution) {
    delete(oldContribution);
    save(newContribution);
  }

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public abstract Single<List<Long>> save(List<Contribution> contribution);

  @Delete
  public abstract void delete(Contribution contribution);

  @Query("SELECT * from contribution WHERE filename=:fileName")
  public abstract List<Contribution> getContributionWithTitle(String fileName);

  @Query("UPDATE contribution SET state=:state WHERE state in (:toUpdateStates)")
  public abstract Single<Integer> updateStates(int state, int[] toUpdateStates);

  @Query("Delete FROM contribution")
  public abstract void deleteAll();

  @Update
  public abstract Single<Integer> update(Contribution contribution);
}
