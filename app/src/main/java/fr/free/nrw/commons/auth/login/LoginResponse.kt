package fr.free.nrw.commons.auth.login

import com.google.gson.annotations.SerializedName
import fr.free.nrw.commons.auth.login.LoginResult.OAuthResult
import fr.free.nrw.commons.auth.login.LoginResult.ResetPasswordResult
import fr.free.nrw.commons.auth.login.LoginResult.Result
import org.wikipedia.dataclient.mwapi.MwServiceError

class LoginResponse {
    @SerializedName("error")
    val error: MwServiceError? = null

    @SerializedName("clientlogin")
    private val clientLogin: ClientLogin? = null

    fun toLoginResult(password: String): LoginResult? {
        return clientLogin?.toLoginResult(password)
    }
}

internal class ClientLogin {
    private val status: String? = null
    private val requests: List<Request>? = null
    private val message: String? = null

    @SerializedName("username")
    private val userName: String? = null

    fun toLoginResult(password: String): LoginResult {
        var userMessage = message
        if ("UI" == status) {
            if (requests != null) {
                for (req in requests) {
                    if ("MediaWiki\\Extension\\OATHAuth\\Auth\\TOTPAuthenticationRequest" == req.id()) {
                        return OAuthResult(status, userName, password, message)
                    } else if ("MediaWiki\\Auth\\PasswordAuthenticationRequest" == req.id()) {
                        return ResetPasswordResult(status, userName, password, message)
                    }
                }
            }
        } else if ("PASS" != status && "FAIL" != status) {
            //TODO: String resource -- Looks like needed for others in this class too
            userMessage = "An unknown error occurred."
        }
        return Result(status ?: "", userName, password, userMessage)
    }
}

internal class Request {
    private val id: String? = null
    private val required: String? = null
    private val provider: String? = null
    private val account: String? = null
    private val fields: Map<String, RequestField>? = null

    fun id(): String? = id
}

internal class RequestField {
    private val type: String? = null
    private val label: String? = null
    private val help: String? = null
}
