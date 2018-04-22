package fr.free.nrw.commons.mwapi.response;

import com.google.gson.annotations.SerializedName;

@SuppressWarnings({"WeakerAccess", "unused"})
public class LoginResponse {
    @SerializedName("status")
    public String status;
    @SerializedName("message")
    public String message;
    @SerializedName("messagecode")
    public String messageCode;
    @SerializedName("username")
    public String username;

    public String getStatusCodeToReturn() {
        if (status.equals("PASS")) {
            return status;
        } else if (status.equals("FAIL")) {
            return messageCode;
        }
        /* else if (
                status.equals("UI")
                        && loginApiResult.getString("/api/clientlogin/requests/_v/@id").equals("TOTPAuthenticationRequest")
                        && loginApiResult.getString("/api/clientlogin/requests/_v/@provider").equals("Two-factor authentication (OATH).")
                ) {
            return "2FA";
        }*/

        // UI, REDIRECT, RESTART
        return "genericerror-" + status;
    }

    @Override
    public String toString() {
        return "LoginResponse{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", messageCode='" + messageCode + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
