package fr.free.nrw.commons.profile.model

import fr.free.nrw.commons.profile.achievements.LevelController

data class UserAchievements(
    val level: LevelController.LevelInfo,
    val articlesUsingImagesCount: Int = 0,
    val thanksReceivedCount: Int = 0,
    val featuredImagesCount: Int = 0,
    val qualityImagesCount: Int = 0,
    val imagesUploadedCount: Int = 0,
    val revertedCount: Int = 0,
    val uniqueImagesCount: Int = 0,
    val imagesEditedBySomeoneElseCount: Int = 0
)