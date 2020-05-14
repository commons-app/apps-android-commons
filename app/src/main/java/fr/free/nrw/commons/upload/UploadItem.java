package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.net.Uri;
import androidx.annotation.Nullable;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.filepicker.MimeTypeMapWrapper;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.utils.ImageUtils;
import io.reactivex.subjects.BehaviorSubject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UploadItem {

  private final Uri mediaUri;
  private final String mimeType;
  private ImageCoordinates gpsCoords;
  private List<UploadMediaDetail> uploadMediaDetails;
  private final Place place;
  private final long createdTimestamp;
  private final String createdTimestampSource;
  private final BehaviorSubject<Integer> imageQuality;

  @SuppressLint("CheckResult")
  UploadItem(final Uri mediaUri,
      final String mimeType,
      final ImageCoordinates gpsCoords,
      final Place place,
      final long createdTimestamp,
      final String createdTimestampSource) {
    this.createdTimestampSource = createdTimestampSource;
    uploadMediaDetails = new ArrayList<>(Collections.singletonList(new UploadMediaDetail()));
    this.place = place;
    this.mediaUri = mediaUri;
    this.mimeType = mimeType;
    this.gpsCoords = gpsCoords;
    this.createdTimestamp = createdTimestamp;
    imageQuality = BehaviorSubject.createDefault(ImageUtils.IMAGE_WAIT);
  }

  public String getCreatedTimestampSource() {
    return createdTimestampSource;
  }

  public ImageCoordinates getGpsCoords() {
    return gpsCoords;
  }

  public List<UploadMediaDetail> getUploadMediaDetails() {
    return uploadMediaDetails;
  }

  public long getCreatedTimestamp() {
    return createdTimestamp;
  }

  public Uri getMediaUri() {
    return mediaUri;
  }

  public int getImageQuality() {
    return imageQuality.getValue();
  }

  public void setImageQuality(final int imageQuality) {
    this.imageQuality.onNext(imageQuality);
  }

  public Place getPlace() {
    return place;
  }

  public void setMediaDetails(final List<UploadMediaDetail> uploadMediaDetails) {
    this.uploadMediaDetails = uploadMediaDetails;
  }

  @Override
  public boolean equals(@Nullable final Object obj) {
    if (!(obj instanceof UploadItem)) {
      return false;
    }
    return mediaUri.toString().contains(((UploadItem) (obj)).mediaUri.toString());

  }

  @Override
  public int hashCode() {
    return mediaUri.hashCode();
  }

  /**
   * Choose a filename for the media. Currently, the caption is used as a filename. If several
   * languages have been entered, the first language is used.
   */
  public String getFileName() {
    return Utils.fixExtension(uploadMediaDetails.get(0).getCaptionText(),
        MimeTypeMapWrapper.getExtensionFromMimeType(mimeType));
  }

  public void setGpsCoords(final ImageCoordinates gpsCoords) {
    this.gpsCoords = gpsCoords;
  }

}
