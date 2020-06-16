package fr.free.nrw.commons.upload;

import androidx.exifinterface.media.ExifInterface;
import fr.free.nrw.commons.utils.ImageUtils;
import io.reactivex.Single;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * We try to minimize uploads from the Commons app that might be copyright violations. If an image
 * does not have any Exif metadata, then it was likely downloaded from the internet, and is probably
 * not an original work by the user. We detect these kinds of images by looking for the presence of
 * some basic Exif metadata.
 */
@Singleton
public class EXIFReader {

  @Inject
  public EXIFReader() {
  }

  public Single<Integer> processMetadata(String path) {
    try {
      ExifInterface exif = new ExifInterface(path);
      if (exif.getAttribute(ExifInterface.TAG_MAKE) != null
          || exif.getAttribute(ExifInterface.TAG_DATETIME) != null) {
        return Single.just(ImageUtils.IMAGE_OK);
      }
    } catch (Exception e) {
      return Single.just(ImageUtils.FILE_NO_EXIF);
    }
    return Single.just(ImageUtils.FILE_NO_EXIF);
  }
}

