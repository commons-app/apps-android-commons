package fr.free.nrw.commons.auth.login

import com.google.gson.annotations.SerializedName

data class OAuthProfileResponse(
    @SerializedName("sub") val id: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("email") val email: String?
)