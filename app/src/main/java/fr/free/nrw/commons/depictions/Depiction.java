package fr.free.nrw.commons.depictions;
import android.net.Uri;
import java.util.Date;

/**
 * Represents a Depiction
 */
public class Depiction {
  private Uri contentUri;
  private String name;
  private Date lastUsed;
  private int timesUsed;

  public Depiction() {
  }

  public Depiction(Uri contentUri, String name, Date lastUsed, int timesUsed) {
    this.contentUri = contentUri;
    this.name = name;
    this.lastUsed = lastUsed;
    this.timesUsed = timesUsed;
  }

  /**
   * Gets name
   *
   * @return name
   */
  public String getName() {
    return name;
  }

  /**
   * Modifies name
   *
   * @param name Depiction name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets last used date
   *
   * @return Last used date
   */
  public Date getLastUsed() {
    // warning: Date objects are mutable.
    return (Date)lastUsed.clone();
  }

  /**
   * Generates new last used date
   */
  private void touch() {
    lastUsed = new Date();
  }

  /**
   * Gets no. of times the Depiction is used
   *
   * @return no. of times used
   */
  public int getTimesUsed() {
    return timesUsed;
  }

  /**
   * Increments timesUsed by 1 and sets last used date as now.
   */
  public void incTimesUsed() {
    timesUsed++;
    touch();
  }

  /**
   * Gets the content URI for this Depiction
   *
   * @return content URI
   */
  public Uri getContentUri() {
    return contentUri;
  }

  /**
   * Modifies the content URI - marking this Depiction as already saved in the database
   *
   * @param contentUri the content URI
   */
  public void setContentUri(Uri contentUri) {
    this.contentUri = contentUri;
  }

}