package fr.free.nrw.commons.review;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity to store reviewed/skipped images identifier
 */
@Entity(tableName = "reviewed-images")
public class ReviewEntity {
    @PrimaryKey
    @NonNull
    String imageId;

    public ReviewEntity(String imageId) {
        this.imageId = imageId;
    }
}
