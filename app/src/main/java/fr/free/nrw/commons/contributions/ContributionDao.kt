package fr.free.nrw.commons.contributions

import android.database.sqlite.SQLiteException
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.reactivex.Completable
import io.reactivex.Single
import java.util.Calendar

@Dao
abstract class ContributionDao {
    @Query("SELECT * FROM contribution order by media_dateUploaded DESC")
    abstract fun fetchContributions(): DataSource.Factory<Int, Contribution>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveSynchronous(contribution: Contribution)

    fun save(contribution: Contribution): Completable {
        return Completable
            .fromAction {
                contribution.dateModified = Calendar.getInstance().time
                if (contribution.dateUploadStarted == null) {
                    contribution.dateUploadStarted = Calendar.getInstance().time
                }
                saveSynchronous(contribution)
            }
    }

    @Transaction
    open fun deleteAndSaveContribution(
        oldContribution: Contribution,
        newContribution: Contribution
    ) {
        deleteSynchronous(oldContribution)
        saveSynchronous(newContribution)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun save(contribution: List<Contribution>): Single<List<Long>>

    @Delete
    abstract fun deleteSynchronous(contribution: Contribution)

    /**
     * Deletes contributions with specific states from the database.
     *
     * @param states The states of the contributions to delete.
     * @throws SQLiteException If an SQLite error occurs.
     */
    @Query("DELETE FROM contribution WHERE state IN (:states)")
    @Throws(SQLiteException::class)
    abstract fun deleteContributionsWithStatesSynchronous(states: List<Int>)

    fun delete(contribution: Contribution): Completable {
        return Completable
            .fromAction { deleteSynchronous(contribution) }
    }

    /**
     * Deletes contributions with specific states from the database.
     *
     * @param states The states of the contributions to delete.
     * @return A Completable indicating the result of the operation.
     */
    fun deleteContributionsWithStates(states: List<Int>): Completable {
        return Completable
            .fromAction { deleteContributionsWithStatesSynchronous(states) }
    }

    @Query("SELECT * from contribution WHERE media_filename=:fileName")
    abstract fun getContributionWithTitle(fileName: String): List<Contribution>

    @Query("SELECT * from contribution WHERE pageId=:pageId")
    abstract fun getContribution(pageId: String): Contribution

    @Query("SELECT * from contribution WHERE state IN (:states) order by media_dateUploaded DESC")
    abstract fun getContribution(states: List<Int>): Single<List<Contribution>>

    /**
     * Gets contributions with specific states in descending order by the date they were uploaded.
     *
     * @param states The states of the contributions to fetch.
     * @return A DataSource factory for paginated contributions with the specified states.
     */
    @Query("SELECT * from contribution WHERE state IN (:states) order by media_dateUploaded DESC")
    abstract fun getContributions(
        states: List<Int>
    ): DataSource.Factory<Int, Contribution>

    /**
     * Gets contributions with specific states in ascending order by the date the upload started.
     *
     * @param states The states of the contributions to fetch.
     * @return A DataSource factory for paginated contributions with the specified states.
     */
    @Query("SELECT * from contribution WHERE state IN (:states) order by dateUploadStarted ASC")
    abstract fun getContributionsSortedByDateUploadStarted(
        states: List<Int>
    ): DataSource.Factory<Int, Contribution>

    @Query("SELECT COUNT(*) from contribution WHERE state in (:toUpdateStates)")
    abstract fun getPendingUploads(toUpdateStates: IntArray): Single<Int>

    @Query("Delete FROM contribution")
    @Throws(SQLiteException::class)
    abstract fun deleteAll()

    @Update
    abstract fun updateSynchronous(contribution: Contribution)

    /**
     * Updates the state of contributions with specific states.
     *
     * @param states   The current states of the contributions to update.
     * @param newState The new state to set.
     */
    @Query("UPDATE contribution SET state = :newState WHERE state IN (:states)")
    abstract fun updateContributionsState(states: List<Int>, newState: Int)

    fun update(contribution: Contribution): Completable {
        return Completable.fromAction {
            contribution.dateModified = Calendar.getInstance().time
            updateSynchronous(contribution)
        }
    }



    /**
     * Updates the state of contributions with specific states asynchronously.
     *
     * @param states   The current states of the contributions to update.
     * @param newState The new state to set.
     * @return A Completable indicating the result of the operation.
     */
    fun updateContributionsWithStates(states: List<Int>, newState: Int): Completable {
        return Completable
            .fromAction {
                updateContributionsState(states, newState)
            }
    }
}
