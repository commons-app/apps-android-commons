package fr.free.nrw.commons.depictions;

import android.net.Uri;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "recent_depictions")
public class RecentDepictions {

  @PrimaryKey(autoGenerate = true)
  private int id;
  private Uri contentUri;
  private String name;
  private Date lastUsed;
  private int timesUsed;

  public RecentDepictions(final Uri contentUri, final String name, final Date lastUsed, final int timesUsed) {
    this.contentUri = contentUri;
    this.name = name;
    this.lastUsed = lastUsed;
    this.timesUsed = timesUsed;
  }

  public void setId(final int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }

  public Uri getContentUri() {
    return contentUri;
  }

  public String getName() {
    return name;
  }

  public Date getLastUsed() {
    return lastUsed;
  }

  public int getTimesUsed() {
    return timesUsed;
  }
}
