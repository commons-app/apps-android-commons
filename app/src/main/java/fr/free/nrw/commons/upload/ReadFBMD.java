package fr.free.nrw.commons.upload;

import fr.free.nrw.commons.utils.ImageUtils;
import io.reactivex.Single;
import java.io.FileInputStream;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * We want to discourage users from uploading images to Commons that were taken from Facebook. This
 * attempts to detect whether an image was downloaded from Facebook by heuristically searching for
 * metadata that is specific to images that come from Facebook.
 */
@Singleton
public class ReadFBMD {

  @Inject
  public ReadFBMD() {
  }

  public Single<Integer> processMetadata(String path) {
    return Single.fromCallable(() -> {
      try {
        int psBlockOffset;
        int fbmdOffset;

        try (FileInputStream fs = new FileInputStream(path)) {
          byte[] bytes = new byte[4096];
          fs.read(bytes);
          fs.close();
          String fileStr = new String(bytes);
          psBlockOffset = fileStr.indexOf("8BIM");
          fbmdOffset = fileStr.indexOf("FBMD");
        }

        if (psBlockOffset > 0 && fbmdOffset > 0
            && fbmdOffset > psBlockOffset && fbmdOffset - psBlockOffset < 0x80) {
          return ImageUtils.FILE_FBMD;
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      return ImageUtils.IMAGE_OK;
    });
  }
}

