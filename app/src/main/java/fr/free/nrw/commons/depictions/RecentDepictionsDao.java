package fr.free.nrw.commons.depictions;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface RecentDepictionsDao {

  @Insert
  void insert(RecentDepictions recentDepictions);

  @Update
  void update(RecentDepictions recentDepictions);

  @Delete
  void delete(RecentDepictions recentDepictions);

  @Query("DELETE FROM recent_depictions")
  void deleteAllRecentDepictions();

  @Query("SELECT * FROM recent_depictions ORDER BY lastUsed DESC")
  LiveData<List<RecentDepictions>> getAllRecentDepictions();
}
