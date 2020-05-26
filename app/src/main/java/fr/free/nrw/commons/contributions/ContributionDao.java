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
import io.reactivex.functions.Action;
import java.util.List;
import java.util.concurrent.Callable;

@Dao
public abstract class ContributionDao {

  @Query("SELECT * FROM contribution order by dateUploaded DESC")
  abstract DataSource.Factory<Integer, Contribution> fetchContributions();

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public abstract void saveSyncronous(Contribution contribution);

  public Completable save(final Contribution contribution) {
    return Completable
        .fromAction(() -> saveSyncronous(contribution));
  }

  @Transaction
  public void deleteAndSaveContribution(final Contribution oldContribution,
      final Contribution newContribution) {
    deleteSyncronous(oldContribution);
    saveSyncronous(newContribution);
  }

  public Completable saveAndDelete(final Contribution oldContribution,
      final Contribution newContribution) {
    return Completable
        .fromAction(() -> deleteAndSaveContribution(oldContribution, newContribution));
  }

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public abstract Single<List<Long>> save(List<Contribution> contribution);

  @Delete
  public abstract void deleteSyncronous(Contribution contribution);

  public Completable delete(final Contribution contribution) {
    return Completable
        .fromAction(() -> deleteSyncronous(contribution));
  }

  @Query("SELECT * from contribution WHERE filename=:fileName")
  public abstract List<Contribution> getContributionWithTitle(String fileName);

  @Query("UPDATE contribution SET state=:state WHERE state in (:toUpdateStates)")
  public abstract Single<Integer> updateStates(int state, int[] toUpdateStates);

  @Query("Delete FROM contribution")
  public abstract void deleteAll();

  @Update
  public abstract void updateSyncronous(Contribution contribution);

  public Completable update(final Contribution contribution) {
    return Completable
        .fromAction(() -> updateSyncronous(contribution));
  }
}
