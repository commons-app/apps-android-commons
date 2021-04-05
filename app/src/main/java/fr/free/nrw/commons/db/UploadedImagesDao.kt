package fr.free.nrw.commons.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UploadedImagesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(uploadedImages: UploadedImages)

    @Query("SELECT * FROM already_uploaded_images_table")
    fun get():List<UploadedImages>
}