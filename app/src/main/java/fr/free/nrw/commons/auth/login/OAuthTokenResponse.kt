package fr.free.nrw.commons.auth.login

import com.google.gson.annotations.SerializedName

data class OAuthTokenResponse(
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("token_type") val tokenType: String?,
    @SerializedName("expires_in") val expiresIn: Long?,
    @SerializedName("error") val error: String?,
    @SerializedName("message") val message: String?
)