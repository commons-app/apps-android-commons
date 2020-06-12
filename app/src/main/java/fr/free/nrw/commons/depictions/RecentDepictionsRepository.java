package fr.free.nrw.commons.depictions;

import androidx.paging.DataSource.Factory;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;
import javax.inject.Inject;

/**
 * The repository class for recent depictions
 */
public class RecentDepictionsRepository {

  private RecentDepictionsLocalDataSource localDataSource;

  @Inject
  public RecentDepictionsRepository(RecentDepictionsLocalDataSource localDataSource) {
    this.localDataSource = localDataSource;
  }

  /**
   * Fetch default number of recent depictions to be shown, based on user preferences
   */
  public String getString(String key) {
    return localDataSource.getString(key);
  }

  /**
   * Deletes a recent depiction from DB
   * @param recentDepictions
   * @return
   */
  public Completable deleteRecentDepictionsFromDB(RecentDepictions recentDepictions) {
    return localDataSource.deleteRecentDepictions(recentDepictions);
  }

  /**
   * Get recent depiction object with title
   * @param name
   * @return
   */
  public RecentDepictions getRecentDepictionsWithName(String name) {
    return localDataSource.getRecentDepictionsWithName(name);
  }

  public Factory<Integer, RecentDepictions> fetchRecentDepictions() {
    return localDataSource.getRecentDepictions();
  }

  public Single<List<Long>> save(List<RecentDepictions> recentDepictions) {
    return localDataSource.saveRecentDepictions(recentDepictions);
  }

  public void set(String key, long value) {
    localDataSource.set(key,value);
  }

  public Completable updateRecentDepictions(RecentDepictions recentDepictions) {
    return localDataSource.updateRecentDepictions(recentDepictions);
  }

}
