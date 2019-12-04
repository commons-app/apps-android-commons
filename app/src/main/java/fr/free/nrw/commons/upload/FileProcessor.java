package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.caching.CacheController;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.mwapi.CategoryApi;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.upload.SimilarImageDialogFragment.Callback;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Processing of the image filePath that is about to be uploaded via ShareActivity is done here
 */
@Singleton
public class FileProcessor implements Callback {

    @Inject
    CacheController cacheController;
    @Inject
    GpsCategoryModel gpsCategoryModel;
    @Inject
    CategoryApi apiCall;
    @Inject
    @Named("default_preferences")
    JsonKvStore defaultKvStore;
    private String filePath;
    private ContentResolver contentResolver;
    private GPSExtractor imageObj;
    private String decimalCoords;
    private ExifInterface exifInterface;
    private boolean haveCheckedForOtherImages = false;
    private GPSExtractor tempImageObj;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Inject
    public FileProcessor() {
    }

    public void cleanup() {
        compositeDisposable.clear();
    }

    void initFileDetails(@NonNull String filePath, ContentResolver contentResolver) {
        this.filePath = filePath;
        this.contentResolver = contentResolver;
        try {
            exifInterface = new ExifInterface(filePath);
        } catch (IOException e) {
            Timber.e(e);
        }
    }

    /**
     * Processes filePath coordinates, either from EXIF data or user location
     */
    GPSExtractor processFileCoordinates(SimilarImageInterface similarImageInterface, Context context) {
        // Redact EXIF data as indicated in preferences.
        redactExifTags(exifInterface, getExifTagsToRedact(context));

        Timber.d("Calling GPSExtractor");
        imageObj = new GPSExtractor(exifInterface);
        decimalCoords = imageObj.getCoords();
        if (decimalCoords == null || !imageObj.imageCoordsExists) {
            //Find other photos taken around the same time which has gps coordinates
            if (!haveCheckedForOtherImages)
                findOtherImages(similarImageInterface);// Do not do repeat the process
        } else {
            useImageCoords();
        }

        return imageObj;
    }

    /**
     * Gets EXIF Tags from preferences to be redacted.
     *
     * @param context application context
     * @return        tags to be redacted
     */
    private Set<String> getExifTagsToRedact(Context context) {
        Type setType = new TypeToken<Set<String>>() {}.getType();
        Set<String> prefManageEXIFTags = defaultKvStore.getJson(Prefs.MANAGED_EXIF_TAGS, setType);

        Set<String> redactTags = new HashSet<>(Arrays.asList(
                context.getResources().getStringArray(R.array.pref_exifTag_values)));
        Timber.d(redactTags.toString());

        if (prefManageEXIFTags != null) redactTags.removeAll(prefManageEXIFTags);

        return redactTags;
    }

    /**
     * Redacts EXIF metadata as indicated in preferences.
     *
     * @param exifInterface  ExifInterface object
     * @param redactTags     tags to be redacted
     */
    public static void redactExifTags(ExifInterface exifInterface, Set<String> redactTags) {
        if(redactTags.isEmpty()) return;

         Disposable disposable = Observable.fromIterable(redactTags)
                 .flatMap(tag -> Observable.fromArray(FileMetadataUtils.getTagsFromPref(tag)))
                 .forEach(tag -> {
                     Timber.d("Checking for tag: %s", tag);
                     String oldValue = exifInterface.getAttribute(tag);
                     if (oldValue != null && !oldValue.isEmpty()) {
                         Timber.d("Exif tag %s with value %s redacted.", tag, oldValue);
                         exifInterface.setAttribute(tag, null);
                     }
                 });
         CompositeDisposable disposables = new CompositeDisposable();
         disposables.add(disposable);
         disposables.clear();

         try {
             exifInterface.saveAttributes();
        } catch (IOException e) {
            Timber.w("EXIF redaction failed: %s", e.toString());
        }
    }

    /**
     * Find other images around the same location that were taken within the last 20 sec
     * @param similarImageInterface
     */
    private void findOtherImages(SimilarImageInterface similarImageInterface) {
        Timber.d("filePath" + filePath);

        long timeOfCreation = new File(filePath).lastModified();//Time when the original image was created
        File folder = new File(filePath.substring(0, filePath.lastIndexOf('/')));
        File[] files = folder.listFiles();
        Timber.d("folderTime Number:" + files.length);


        for (File file : files) {
            if (file.lastModified() - timeOfCreation <= (120 * 1000) && file.lastModified() - timeOfCreation >= -(120 * 1000)) {
                //Make sure the photos were taken within 20seconds
                Timber.d("fild date:" + file.lastModified() + " time of creation" + timeOfCreation);
                tempImageObj = null;//Temporary GPSExtractor to extract coords from these photos
                try {
                    tempImageObj = new GPSExtractor(contentResolver.openInputStream(Uri.fromFile(file)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (tempImageObj != null) {
                    tempImageObj = new GPSExtractor(file.getAbsolutePath());
                }
                if (tempImageObj != null) {
                    Timber.d("not null fild EXIF" + tempImageObj.imageCoordsExists + " coords" + tempImageObj.getCoords());
                    if (tempImageObj.getCoords() != null && tempImageObj.imageCoordsExists) {
                        // Current image has gps coordinates and it's not current gps locaiton
                        Timber.d("This filePath has image coords:" + file.getAbsolutePath());
                        similarImageInterface.showSimilarImageFragment(filePath, file.getAbsolutePath());
                        break;
                    }
                }
            }
        }
        haveCheckedForOtherImages = true; //Finished checking for other images
    }

    /**
     * Initiates retrieval of image coordinates or user coordinates, and caching of coordinates.
     * Then initiates the calls to MediaWiki API through an instance of CategoryApi.
     */
    @SuppressLint("CheckResult")
    private void useImageCoords() {
        if (decimalCoords != null) {
            Timber.d("Decimal coords of image: %s", decimalCoords);
            Timber.d("is EXIF data present:" + imageObj.imageCoordsExists + " from findOther image");

            // Only set cache for this point if image has coords
            if (imageObj.imageCoordsExists) {
                double decLongitude = imageObj.getDecLongitude();
                double decLatitude = imageObj.getDecLatitude();
                cacheController.setQtPoint(decLongitude, decLatitude);
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

    @Override
    public void onPositiveResponse() {
        imageObj = tempImageObj;
        decimalCoords = imageObj.getCoords();// Not necessary to use gps as image already ha EXIF data
        Timber.d("EXIF from tempImageObj");
        useImageCoords();
    }

    @Override
    public void onNegativeResponse() {
        Timber.d("EXIF from imageObj");
        useImageCoords();
    }
}