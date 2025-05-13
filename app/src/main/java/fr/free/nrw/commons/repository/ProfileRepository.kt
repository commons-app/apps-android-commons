package fr.free.nrw.commons.repository

import fr.free.nrw.commons.network.APIService
import fr.free.nrw.commons.profile.achievements.LevelController
import fr.free.nrw.commons.profile.model.UserAchievements
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

class ProfileRepository @Inject constructor(private val apiService: APIService) {

    fun getUserLevel(username: String) : Flow<UserAchievements> = flow {
        try {
            val uploadCountResponse = apiService.getImageUploadCount(username)
            val imagesUploaded = uploadCountResponse.body() ?:0

            val achievementResponse = apiService.getUserAchievements(username)

            val uniqueImages = achievementResponse.body()?.uniqueUsedImages ?: 0
            val articlesUsingImages = achievementResponse.body()?.articlesUsingImages ?: 0
            val thanksReceived = achievementResponse.body()?.thanksReceived ?: 0
            val featuredImages = achievementResponse.body()?.featuredImages?.featuredPicturesOnWikimediaCommons ?:0
            val qualityImages = achievementResponse.body()?.featuredImages?.qualityImages ?: 0
            val deletedUploads = achievementResponse.body()?.deletedUploads ?:0
            val revertCount = (imagesUploaded - deletedUploads) * 100 / imagesUploaded
            val imagesEditedBySomeoneElse = achievementResponse.body()?.imagesEditedBySomeoneElse ?:0

            val level = LevelController.LevelInfo.from(
                imagesUploaded = imagesUploaded,
                uniqueImagesUsed = uniqueImages,
                nonRevertRate = revertCount)

            emit(
                UserAchievements(
                    level = level,
                    articlesUsingImagesCount = articlesUsingImages,
                    featuredImagesCount = featuredImages,
                    imagesUploadedCount = imagesUploaded,
                    qualityImagesCount = qualityImages,
                    revertedCount = revertCount,
                    thanksReceivedCount = thanksReceived,
                    uniqueImagesCount = uniqueImages,
                    imagesEditedBySomeoneElseCount = imagesEditedBySomeoneElse
                )
            )

        }
        catch(e : Exception) {
            Timber.e(e.printStackTrace().toString())
        }
    }
}