package fr.free.nrw.commons.customselector.data

import fr.free.nrw.commons.customselector.database.NotForUploadStatus
import fr.free.nrw.commons.customselector.database.NotForUploadStatusDao
import fr.free.nrw.commons.customselector.domain.ImageRepository
import fr.free.nrw.commons.customselector.domain.model.Image
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ImageRepositoryImpl @Inject constructor(
    private val mediaReader: MediaReader,
    private val notForUploadStatusDao: NotForUploadStatusDao
): ImageRepository {
    override suspend fun getImagesFromDevice(): Flow<Image> {
        return mediaReader.getImages()
    }

    override suspend fun markAsNotForUpload(imageSHA: String) {
        notForUploadStatusDao.insert(NotForUploadStatus(imageSHA))
    }

    override suspend fun unmarkAsNotForUpload(imageSHA: String) {
        notForUploadStatusDao.deleteWithImageSHA1(imageSHA)
    }

    override suspend fun isNotForUpload(imageSHA: String): Boolean {
        return notForUploadStatusDao.find(imageSHA) > 0
    }
}