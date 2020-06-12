package fr.free.nrw.commons.depictions;

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
public abstract class RecentDepictionsDao {

  @Query("SELECT * FROM recent_depictions ORDER BY lastUsed DESC")
  abstract DataSource.Factory<Integer, RecentDepictions> fetchRecentDepictions();

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public abstract void saveSynchronous(RecentDepictions recentDepictions);

  public Completable save(final RecentDepictions recentDepictions) {
    return Completable
        .fromAction(() -> saveSynchronous(recentDepictions));
  }

  @Transaction
  public void deleteAndSaveRecentDepictions(final RecentDepictions oldRecentDepictions,
      final RecentDepictions newRecentDepictions) {
    deleteSynchronous(oldRecentDepictions);
    saveSynchronous(newRecentDepictions);
  }

  public Completable saveAndDelete(final RecentDepictions oldRecentDepictions,
      final RecentDepictions newRecentDepictions) {
    return Completable
        .fromAction(() -> deleteAndSaveRecentDepictions(oldRecentDepictions, newRecentDepictions));
  }

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public abstract Single<List<Long>> save(List<RecentDepictions> recentDepictions);

  @Delete
  public abstract void deleteSynchronous(RecentDepictions recentDepictions);

  public Completable delete(final RecentDepictions recentDepictions) {
    return Completable
        .fromAction(() -> deleteSynchronous(recentDepictions));
  }

  @Query("SELECT * from recent_depictions WHERE name=:name")
  public abstract List<RecentDepictions> getRecentDepictionsWithTitle(String name);

  @Query("Delete FROM recent_depictions")
  public abstract void deleteAll();

  @Update
  public abstract void updateSynchronous(RecentDepictions recentDepictions);

  public Completable update(final RecentDepictions recentDepictions) {
    return Completable
        .fromAction(() -> updateSynchronous(recentDepictions));
  }
}
