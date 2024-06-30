package fr.free.nrw.commons.nearby;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import fr.free.nrw.commons.location.LatLng;
import io.reactivex.Completable;

@Dao
public abstract class PlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void saveSynchronous(Place place);

    @Query("SELECT * from place WHERE location=:l")
    public abstract Place getPlace(LatLng l);

    public Completable save(final Place place) {
        return Completable
            .fromAction(() -> {
                saveSynchronous(place);
            });
    }
}
