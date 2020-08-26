package fr.free.nrw.commons.profile.leaderboard;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * GSON Response Class for Update Avatar API response
 */
public class UpdateAvatarResponse {

  /**
   * Status Code returned from the API
   * Example value - 200
   */
  @SerializedName("status")
  @Expose
  private String status;

  /**
   * Message returned from the API
   * Example value - Avatar Updated
   */
  @SerializedName("message")
  @Expose
  private String message;

  /**
   * Username returned from the API
   * Example value - Syced
   */
  @SerializedName("user")
  @Expose
  private String user;

  /**
   * @return the status code
   */
  public String getStatus() {
    return status;
  }

  /**
   * Sets the status code
   */
  public void setStatus(String status) {
    this.status = status;
  }

  /**
   * @return the message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Sets the message
   */
  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * @return the username
   */
  public String getUser() {
    return user;
  }

  /**
   * Sets the username
   */
  public void setUser(String user) {
    this.user = user;
  }

}
