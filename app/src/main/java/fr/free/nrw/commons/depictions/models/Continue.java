package fr.free.nrw.commons.depictions.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Model class for object obtained while parsing depiction response
 */
public class Continue {

  @SerializedName("sroffset")
  @Expose
  private Integer sroffset;
  @SerializedName("continue")
  @Expose
  private String _continue;

  /**
   * No args constructor for use in serialization
   */
  public Continue() {
  }

  /**
   * @param sroffset
   * @param _continue
   */
  public Continue(Integer sroffset, String _continue) {
    super();
    this.sroffset = sroffset;
    this._continue = _continue;
  }

  /**
   * gets sroffset from Continue object
   */
  public Integer getSroffset() {
    return sroffset;
  }

  public void setSroffset(Integer sroffset) {
    this.sroffset = sroffset;
  }

  /**
   * gets continue string from Continue object
   */
  public String getContinue() {
    return _continue;
  }

  public void setContinue(String _continue) {
    this._continue = _continue;
  }

}
