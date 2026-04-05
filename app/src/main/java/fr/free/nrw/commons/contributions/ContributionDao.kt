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
    protected abstract fun fetchContributionsInternal(): DataSource.Factory<Int, ContributionRoomEntity>

    fun fetchContributions(): DataSource.Factory<Int, Contribution> =
        fetchContributionsInternal().map { fromEntity(it) }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun saveSynchronousInternal(contribution: ContributionRoomEntity)

    fun saveSynchronous(contribution: Contribution) =
        saveSynchronousInternal(toEntity(contribution))

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
    protected abstract fun saveInternal(contribution: List<ContributionRoomEntity>): Single<List<Long>>

    fun save(contribution: List<Contribution>): Single<List<Long>> =
        saveInternal(contribution.map { toEntity(it) })

    @Delete
    protected abstract fun deleteSynchronousInternal(contribution: ContributionRoomEntity)

    fun deleteSynchronous(contribution: Contribution) =
        deleteSynchronousInternal(toEntity(contribution))

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
    protected abstract fun getContributionWithTitleInternal(fileName: String): List<ContributionRoomEntity>

    fun getContributionWithTitle(fileName: String): List<Contribution> =
        getContributionWithTitleInternal(fileName).map { fromEntity(it) }

    @Query("SELECT * from contribution WHERE pageId=:pageId")
    protected abstract fun getContributionInternal(pageId: String): ContributionRoomEntity

    fun getContribution(pageId: String): Contribution =
        fromEntity(getContributionInternal(pageId))

    @Query("SELECT * from contribution WHERE state IN (:states) order by media_dateUploaded DESC")
    protected abstract fun getContributionInternal(states: List<Int>): Single<List<ContributionRoomEntity>>

    fun getContribution(states: List<Int>): Single<List<Contribution>> =
        getContributionInternal(states).map { entities -> entities.map { fromEntity(it) } }

    @Query("SELECT * from contribution WHERE state IN (:states) order by media_dateUploaded DESC")
    protected abstract fun getContributionsInternal(
        states: List<Int>
    ): DataSource.Factory<Int, ContributionRoomEntity>

    fun getContributions(states: List<Int>): DataSource.Factory<Int, Contribution> =
        getContributionsInternal(states).map { fromEntity(it) }

    @Query("SELECT * from contribution WHERE state IN (:states) order by dateUploadStarted ASC")
    protected abstract fun getContributionsSortedByDateUploadStartedInternal(
        states: List<Int>
    ): DataSource.Factory<Int, ContributionRoomEntity>

    fun getContributionsSortedByDateUploadStarted(states: List<Int>): DataSource.Factory<Int, Contribution> =
        getContributionsSortedByDateUploadStartedInternal(states).map { fromEntity(it) }

    @Query("SELECT COUNT(*) from contribution WHERE state in (:toUpdateStates)")
    abstract fun getPendingUploads(toUpdateStates: IntArray): Single<Int>

    @Query("Delete FROM contribution")
    @Throws(SQLiteException::class)
    abstract fun deleteAll()

    @Update
    protected abstract fun updateSynchronousInternal(contribution: ContributionRoomEntity)

    fun updateSynchronous(contribution: Contribution) =
        updateSynchronousInternal(toEntity(contribution))

    @Query("UPDATE contribution SET state = :newState WHERE state IN (:states)")
    abstract fun updateContributionsState(states: List<Int>, newState: Int)

    fun update(contribution: Contribution): Completable {
        return Completable.fromAction {
            contribution.dateModified = Calendar.getInstance().time
            updateSynchronous(contribution)
        }
    }

    fun updateContributionsWithStates(states: List<Int>, newState: Int): Completable {
        return Completable
            .fromAction {
                updateContributionsState(states, newState)
            }
    }

    private fun toEntity(contribution: Contribution): ContributionRoomEntity =
        ContributionRoomEntity(
            media = contribution.media,
            pageId = contribution.pageId,
            state = contribution.state,
            transferred = contribution.transferred,
            decimalCoords = contribution.decimalCoords,
            dateCreatedSource = contribution.dateCreatedSource,
            wikidataPlace = contribution.wikidataPlace,
            chunkInfo = contribution.chunkInfo,
            errorInfo = contribution.errorInfo,
            depictedItems = contribution.depictedItems,
            mimeType = contribution.mimeType,
            localUri = contribution.localUri,
            dataLength = contribution.dataLength,
            dateCreated = contribution.dateCreated,
            dateCreatedString = contribution.dateCreatedString,
            dateModified = contribution.dateModified,
            dateUploadStarted = contribution.dateUploadStarted,
            hasInvalidLocation = contribution.hasInvalidLocation,
            contentUri = contribution.contentUri,
            countryCode = contribution.countryCode,
            imageSHA1 = contribution.imageSHA1,
            retries = contribution.retries
        )

    private fun fromEntity(entity: ContributionRoomEntity): Contribution =
        Contribution(
            media = entity.media,
            pageId = entity.pageId,
            state = entity.state,
            transferred = entity.transferred,
            decimalCoords = entity.decimalCoords,
            dateCreatedSource = entity.dateCreatedSource,
            wikidataPlace = entity.wikidataPlace,
            chunkInfo = entity.chunkInfo,
            errorInfo = entity.errorInfo,
            depictedItems = entity.depictedItems,
            mimeType = entity.mimeType,
            localUri = entity.localUri,
            dataLength = entity.dataLength,
            dateCreated = entity.dateCreated,
            dateCreatedString = entity.dateCreatedString,
            dateModified = entity.dateModified,
            dateUploadStarted = entity.dateUploadStarted,
            hasInvalidLocation = entity.hasInvalidLocation,
            contentUri = entity.contentUri,
            countryCode = entity.countryCode,
            imageSHA1 = entity.imageSHA1,
            retries = entity.retries
        )
}
