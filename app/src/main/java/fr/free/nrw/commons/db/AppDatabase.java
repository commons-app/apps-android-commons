package fr.free.nrw.commons.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.contributions.ContributionDao;

@Database(entities = {Contribution.class}, version = 2, exportSchema = false)
@TypeConverters({Converters.class})
abstract public class AppDatabase extends RoomDatabase {
    public abstract ContributionDao getContributionDao();
}
