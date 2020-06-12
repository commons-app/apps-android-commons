package fr.free.nrw.commons.depictions;

import androidx.paging.DataSource.Factory;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * The LocalDataSource class for RecentDepictions
 */
class RecentDepictionsLocalDataSource {

  private final RecentDepictionsDao recentDepictionsDao;
  private final JsonKvStore defaultKVStore;

  @Inject
  public RecentDepictionsLocalDataSource(
      @Named("default_preferences") JsonKvStore defaultKVStore,
      RecentDepictionsDao recentDepictionsDao) {
    this.defaultKVStore = defaultKVStore;
    this.recentDepictionsDao = recentDepictionsDao;
  }

  /**
   * Fetch default number of recent depictions to be shown, based on user preferences
   */
  public String getString(String key) {
    return defaultKVStore.getString(key);
  }

  /**
   * Fetch default number of recent depictions to be shown, based on user preferences
   */
  public long getLong(String key) {
    return defaultKVStore.getLong(key);
  }

  /**
   * Get recent depiction object from cursor
   * @param uri
   * @return
   */
  public RecentDepictions getRecentDepictionsWithName(String uri) {
    List<RecentDepictions> recentDepictionsWithUri = recentDepictionsDao.getRecentDepictionsWithTitle(uri);
    if(!recentDepictionsWithUri.isEmpty()){
      return recentDepictionsWithUri.get(0);
    }
    return null;
  }

  /**
   * Remove a recent depiction from the recent_depictions table
   * @param recentDepictions
   * @return
   */
  public Completable deleteRecentDepictions(RecentDepictions recentDepictions) {
    return recentDepictionsDao.delete(recentDepictions);
  }

  public Factory<Integer, RecentDepictions> getRecentDepictions() {
    return recentDepictionsDao.fetchRecentDepictions();
  }

  public Single<List<Long>> saveRecentDepictions(List<RecentDepictions> recentDepictions) {
    return recentDepictionsDao.save(recentDepictions);
  }

  public void set(String key, long value) {
    defaultKVStore.putLong(key,value);
  }

  public Completable updateRecentDepictions(RecentDepictions recentDepictions) {
    return recentDepictionsDao.update(recentDepictions);
  }

}
