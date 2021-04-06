package fr.free.nrw.commons.db

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nguyenhoanglam.imagepicker.model.Image

@Entity(tableName = "already_uploaded_images_table")
data class UploadedImages(
    @PrimaryKey @ColumnInfo(name = "image_id")
    var imageId: Long,
    @ColumnInfo(name = "image_name")
    var imageName: String,
    @ColumnInfo(name = "image_uri")
    var imageUri: String,
    @ColumnInfo(name = "image_path")
    var path: String,
    @ColumnInfo(name = "bucket_id")
    var bucketId: Long,
    @ColumnInfo(name = "bucket_name")
    var bucketName: String
)

fun uploadedImagesToImages(uploadedImages: List<UploadedImages>): ArrayList<Image> {
    val images: ArrayList<Image> = ArrayList()
    for (uploadedImage in uploadedImages) {
        val image: Image = Image(
            id = uploadedImage.imageId,
            name = uploadedImage.imageName,
            uri = Uri.parse(uploadedImage.imageUri),
            path = uploadedImage.path,
            bucketId = uploadedImage.bucketId,
            bucketName = uploadedImage.bucketName
        )
        images.add(image)
    }
    return images
}

fun imageToUploadedImage(image: Image): UploadedImages {
    return UploadedImages(
        imageId = image.id,
        imageName = image.name,
        imageUri = image.uri.toString(),
        path = image.path,
        bucketId = image.bucketId,
        bucketName = image.bucketName
    )
}
