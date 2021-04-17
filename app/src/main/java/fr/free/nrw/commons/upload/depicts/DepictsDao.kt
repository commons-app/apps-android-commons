package fr.free.nrw.commons.upload.depicts

import androidx.room.*

@Dao
interface DepictsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(depictedItem: Depicts);

    @Query("Select * From depicts_table order by lastUsed DESC")
    suspend fun getAllDepict(): List<Depicts>;

    @Query("Select * From depicts_table order by lastUsed DESC LIMIT :n OFFSET 10")
    suspend fun getItemToDelete(n: Int): List<Depicts>;

    @Delete
    suspend fun delete(depicts: Depicts);

}