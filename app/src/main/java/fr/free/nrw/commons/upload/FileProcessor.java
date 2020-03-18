package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import androidx.exifinterface.media.ExifInterface;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.caching.CacheController;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.mwapi.CategoryApi;
import fr.free.nrw.commons.settings.Prefs;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

/**
 * Processing of the image filePath that is about to be uploaded via ShareActivity is done here
 */
public class FileProcessor {

  private final Context context;
  private final ContentResolver contentResolver;
  private final CacheController cacheController;
  private final GpsCategoryModel gpsCategoryModel;
  private final JsonKvStore defaultKvStore;
  private final CategoryApi apiCall;

  private final CompositeDisposable compositeDisposable = new CompositeDisposable();

  @Inject
  public FileProcessor(Context context, ContentResolver contentResolver,
      CacheController cacheController, GpsCategoryModel gpsCategoryModel,
      @Named("default_preferences") JsonKvStore defaultKvStore, CategoryApi apiCall) {
    this.context = context;
    this.contentResolver = contentResolver;
    this.cacheController = cacheController;
    this.gpsCategoryModel = gpsCategoryModel;
    this.defaultKvStore = defaultKvStore;
    this.apiCall = apiCall;
  }

  public void cleanup() {
    compositeDisposable.clear();
  }

  /**
   * Processes filePath coordinates, either from EXIF data or user location
   */
  GPSExtractor processFileCoordinates(SimilarImageInterface similarImageInterface,
      String filePath) {
    ExifInterface exifInterface = null;
    try {
      exifInterface = new ExifInterface(filePath);
    } catch (IOException e) {
      Timber.e(e);
    }
    // Redact EXIF data as indicated in preferences.
    redactExifTags(exifInterface, getExifTagsToRedact());

    Timber.d("Calling GPSExtractor");
    GPSExtractor originalImageExtractor = new GPSExtractor(exifInterface);
    String decimalCoords = originalImageExtractor.getCoords();
    if (decimalCoords == null || !originalImageExtractor.imageCoordsExists) {
      //Find other photos taken around the same time which has gps coordinates
      findOtherImages(originalImageExtractor, new File(filePath),
          similarImageInterface);// Do not do repeat the process
    } else {
      useImageCoords(originalImageExtractor);
    }

    return originalImageExtractor;
  }

  /**
   * Gets EXIF Tags from preferences to be redacted.
   *
   * @return tags to be redacted
   */
  private Set<String> getExifTagsToRedact() {
    Set<String> prefManageEXIFTags = defaultKvStore.getStringSet(Prefs.MANAGED_EXIF_TAGS);

    Set<String> redactTags = new HashSet<>(Arrays.asList(
        context.getResources().getStringArray(R.array.pref_exifTag_values)));
    Timber.d(redactTags.toString());

    if (prefManageEXIFTags != null) {
      redactTags.removeAll(prefManageEXIFTags);
    }

    return redactTags;
  }

  /**
   * Redacts EXIF metadata as indicated in preferences.
   *
   * @param exifInterface ExifInterface object
   * @param redactTags    tags to be redacted
   */
  private void redactExifTags(ExifInterface exifInterface, Set<String> redactTags) {
    compositeDisposable.add(
        Observable.fromIterable(redactTags)
            .flatMap(tag -> Observable.fromArray(FileMetadataUtils.getTagsFromPref(tag)))
            .subscribe(
                (tag) -> redactTag(exifInterface, tag),
                Timber::d,
                () -> save(exifInterface)
            ));
  }

  private void save(ExifInterface exifInterface) {
    try {
      exifInterface.saveAttributes();
    } catch (IOException e) {
      Timber.w("EXIF redaction failed: %s", e.toString());
    }
  }

  private void redactTag(ExifInterface exifInterface, String tag) {
    Timber.d("Checking for tag: %s", tag);
    String oldValue = exifInterface.getAttribute(tag);
    if (oldValue != null && !oldValue.isEmpty()) {
      Timber.d("Exif tag %s with value %s redacted.", tag, oldValue);
      exifInterface.setAttribute(tag, null);
    }
  }

  /**
   * Find other images around the same location that were taken within the last 20 sec
   *
   * @param originalImageExtractor
   * @param fileBeingProcessed
   * @param similarImageInterface
   */
  private void findOtherImages(
      GPSExtractor originalImageExtractor,
      File fileBeingProcessed,
      SimilarImageInterface similarImageInterface) {
    //Time when the original image was created
    long timeOfCreation = fileBeingProcessed.lastModified();
    File[] files = fileBeingProcessed.getParentFile().listFiles();
    Timber.d("folderTime Number:" + files.length);

    for (File file : files) {
      if (file.lastModified() - timeOfCreation <= (120 * 1000)
          && file.lastModified() - timeOfCreation >= -(120 * 1000)) {
        //Make sure the photos were taken within 20seconds
        Timber.d("fild date:" + file.lastModified() + " time of creation" + timeOfCreation);
        GPSExtractor similarPictureExtractor = createGpsExtractor(file);
        Timber.d("not null fild EXIF" + similarPictureExtractor.imageCoordsExists + " coords"
            + similarPictureExtractor.getCoords());
        if (similarPictureExtractor.getCoords() != null
            && similarPictureExtractor.imageCoordsExists) {
          // Current image has gps coordinates and it's not current gps locaiton
          Timber.d("This filePath has image coords:" + file.getAbsolutePath());
          similarImageInterface.showSimilarImageFragment(
              fileBeingProcessed.getPath(),
              file.getAbsolutePath(),
              originalImageExtractor,
              similarPictureExtractor
          );
          break;
        }
      }
    }
  }

  @NotNull
  private GPSExtractor createGpsExtractor(File file) {
    try {
      return new GPSExtractor(contentResolver.openInputStream(Uri.fromFile(file)));
    } catch (IOException e) {
      e.printStackTrace();
      return new GPSExtractor(file.getAbsolutePath());
    }
  }

  /**
   * Initiates retrieval of image coordinates or user coordinates, and caching of coordinates. Then
   * initiates the calls to MediaWiki API through an instance of CategoryApi.
   *
   * @param gpsExtractor
   */
  @SuppressLint("CheckResult")
  public void useImageCoords(GPSExtractor gpsExtractor) {
    useImageCoords(gpsExtractor, gpsExtractor.getCoords());
  }

  private void useImageCoords(GPSExtractor gpsExtractor, String decimalCoords) {
    if (decimalCoords != null) {
      Timber.d("Decimal coords of image: %s", decimalCoords);
      Timber.d("is EXIF data present:" + gpsExtractor.imageCoordsExists +
          " from findOther image");

      // Only set cache for this point if image has coords
      if (gpsExtractor.imageCoordsExists) {
        cacheController.setQtPoint(gpsExtractor.getDecLongitude(), gpsExtractor.getDecLatitude());
      }

      List<String> displayCatList = cacheController.findCategory();
      boolean catListEmpty = displayCatList.isEmpty();

      // If no categories found in cache, call MediaWiki API to match image coords with nearby Commons categories
      if (catListEmpty) {
        compositeDisposable.add(apiCall.request(decimalCoords)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe(
                gpsCategoryModel::setCategoryList,
                throwable -> {
                  Timber.e(throwable);
                  gpsCategoryModel.clear();
                }
            ));
        Timber.d("displayCatList size 0, calling MWAPI %s", displayCatList);
      } else {
        Timber.d("Cache found, setting categoryList in model to %s", displayCatList);
        gpsCategoryModel.setCategoryList(displayCatList);
      }
    } else {
      Timber.d("EXIF: no coords");
    }
  }
}
