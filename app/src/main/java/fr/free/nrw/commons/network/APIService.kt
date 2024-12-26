package fr.free.nrw.commons.network

import fr.free.nrw.commons.profile.model.AchievementResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query


interface APIService {

    // https://tools.wmflabs.org/commons-android-app/tool-commons-android-app/uploadsbyuser.py?user=Devanonymous
    @GET("uploadsbyuser.py")
    suspend fun getImageUploadCount(
        @Query("user") username : String
    ) : Response<Int>


    // https://tools.wmflabs.org/commons-android-app/tool-commons-android-app//feedback.py?user=Devanonymous
    @GET("feedback.py")
    suspend fun getUserAchievements(
        @Query("user") username: String
    ) : Response<AchievementResponse>
}