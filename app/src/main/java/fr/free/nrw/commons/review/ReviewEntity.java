package fr.free.nrw.commons.review;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reviewed-images")
public class ReviewEntity {
    @PrimaryKey
    @NonNull
    String filename;

    public ReviewEntity(String filename) {
        this.filename = filename;
    }
}
