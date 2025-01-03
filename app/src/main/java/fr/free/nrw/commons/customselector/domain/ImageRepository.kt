package fr.free.nrw.commons.customselector.domain

import fr.free.nrw.commons.customselector.domain.model.Image
import kotlinx.coroutines.flow.Flow

interface ImageRepository {

    suspend fun getImagesFromDevice(): Flow<Image>

    suspend fun markAsNotForUpload(imageSHA: String)

    suspend fun unmarkAsNotForUpload(imageSHA: String)

    suspend fun isNotForUpload(imageSHA: String): Boolean
}