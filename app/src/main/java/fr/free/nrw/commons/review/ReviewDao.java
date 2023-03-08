package fr.free.nrw.commons.review;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import kotlinx.coroutines.flow.Flow;


@Dao
public interface ReviewDao {
    @Query( "SELECT EXISTS (SELECT * from `reviewed-images` where fileName = (:image))")
    Boolean isReviewedAlready(String image);
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(ReviewEntity reviewEntity);
}