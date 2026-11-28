package fr.free.nrw.commons.auth.login

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import fr.free.nrw.commons.auth.login.LoginResult.EmailAuthResult
import fr.free.nrw.commons.auth.login.LoginResult.OAuthResult
import fr.free.nrw.commons.auth.login.LoginResult.ResetPasswordResult
import fr.free.nrw.commons.auth.login.LoginResult.Result
import fr.free.nrw.commons.wikidata.mwapi.MwServiceError

class LoginResponse {
    @SerializedName("error")
    val error: MwServiceError? = null

    /** API errors sent with `errorformat=plaintext`, e.g. `badtoken`. */
    @SerializedName("errors")
    val errors: List<MwServiceError>? = null

    @SerializedName("clientlogin")
    private val clientLogin: ClientLogin? = null

    @SerializedName("login")
    private val botLogin: BotLogin? = null

    /**
     * Returns the text of the first API error (or its code when the text is empty), looking at
     * [errors] first and then the legacy [error]. Returns null when there is no API error.
     */
    fun errorMessage(): String? =
        (errors?.firstOrNull() ?: error)?.let { e -> e.details.ifBlank { e.title }.ifBlank { null } }

    fun toLoginResult(password: String): LoginResult? =
        clientLogin?.toLoginResult(password) ?: botLogin?.toLoginResult(password)
}

internal class ClientLogin {
    private val status: String? = null
    private val requests: List<Request>? = null
    private val message: String? = null

    @SerializedName("username")
    private val userName: String? = null

    fun toLoginResult(password: String): LoginResult {
        var userMessage = message
        if (userMessage == "?userlogin-cannot-login?" || userMessage?.contains("cannot-login") == true) {
            userMessage = "Two-Factor Authentication is enabled. You must use a Bot Password to log in."
        }
        if ("UI" == status) {
            requests?.forEach { request ->
                request.id()?.let {
                    if (it.endsWith("TOTPAuthenticationRequest")) {
                        return OAuthResult(status, userName, password, message)
                    } else if (it.endsWith("EmailAuthAuthenticationRequest")) {
                        return EmailAuthResult(status, userName, password, message)
                    } else if (it.endsWith("PasswordAuthenticationRequest")) {
                        return ResetPasswordResult(status, userName, password, message)
                    }
                }
            }
        }
        return Result(status ?: "", userName, password, userMessage)
    }
}

@Suppress("UnusedPrivateProperty") // Fields deserialized by Gson via reflection
internal class Request {
    private val id: String? = null
    private val required: String? = null
    private val provider: String? = null
    private val account: String? = null
    internal val fields: Map<String, RequestField>? = null

    fun id(): String? = id
}

@Suppress("UnusedPrivateProperty") // Fields deserialized by Gson via reflection
internal class RequestField {
    private val type: String? = null
    private val label: String? = null
    internal val help: String? = null
}

internal class BotLogin {
    @SerializedName("result")
    private val result: String? = null

    @SerializedName("lgusername")
    private val userName: String? = null

    // here we use the JsonObject to cleanly extract the error text
    @SerializedName("reason")
    private val reason: JsonObject? = null

    fun toLoginResult(password: String): LoginResult {
        // handle successful login
        if (result == "Success") {
            return Result("PASS", userName, password, "Success")
        }

        var errorMessage = result ?: "Login Failed"
        if (reason != null && reason.has("text")) {
            errorMessage = reason.get("text").asString
        }
        if (reason != null &&
            reason.has("code") &&
            reason.get("code").asString == "api-login-fail-badsessionprovider"
        ) {
            errorMessage =
                "Session conflict: A previous bot is still logged in. " +
                "Please go to your device Settings -> Apps -> Commons -> Storage, " +
                "and tap 'Clear Data' before logging in."
        }
        return Result("FAIL", userName, password, errorMessage)
    }
}

class BotPermissionsResponse {
    @SerializedName("query")
    val query: BotQuery? = null
}

class BotQuery {
    @SerializedName("userinfo")
    val userInfo: BotUserInfo? = null
}

class BotUserInfo {
    @SerializedName("rights")
    val rights: List<String>? = null
}
