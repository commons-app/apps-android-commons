package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import androidx.exifinterface.media.ExifInterface;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.caching.CacheController;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.mwapi.CategoryApi;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static fr.free.nrw.commons.filepicker.Constants.DEFAULT_FOLDER_NAME;

/**
 * Processing of the image filePath that is about to be uploaded via ShareActivity is done here
 */
@Singleton
public class FileProcessor implements SimilarImageDialogFragment.onResponse {

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
    FileProcessor() {
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
        redactMetadata(context);

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
     * Redacts EXIF and XMP metadata as indicated in preferences.
     *
     */
    @SuppressLint("CheckResult")
    private void redactMetadata(Context context) {
        Set<String> prefManageEXIFTags = defaultKvStore.getStringSet("manageExifTags");
        boolean prefKeepXmp = defaultKvStore.getBoolean("keepXmp", true);

        try {
            if (!prefKeepXmp) {
                String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(filePath)).toString());
                String newFilePath = context.getCacheDir().getAbsolutePath() + "/"
                        + DEFAULT_FOLDER_NAME + "/"
                        + UUID.randomUUID().toString() + "." + extension;

                FileInputStream inStream = new FileInputStream(filePath);
                FileOutputStream outStream = new FileOutputStream(newFilePath);
                FileChannel inChannel = inStream.getChannel();
                FileChannel outChannel = outStream.getChannel();
                inChannel.transferTo(0, inChannel.size(), outChannel);
                inStream.close();
                outStream.close();
                // Overwrites original file while removing XMP data.
                // inputPath - newFilePath, the copy of FilePath
                // outputPath - original FilePath
                FileMetadataUtils.removeXmpAndWriteToFile(newFilePath, filePath);
            }

            Set<String> redactTags = new HashSet<>(Arrays.asList(
                    context.getResources().getStringArray(R.array.pref_exifTag_values)));

            Timber.d(redactTags.toString());
            redactTags.removeAll(prefManageEXIFTags);

            if (!redactTags.isEmpty()) {
                //noinspection ResultOfMethodCallIgnored
                Observable.fromIterable(redactTags)
                        .flatMap(FileMetadataUtils::getTagsFromPref)
                        .forEach(tag -> {
                            Timber.d("Checking for tag:%s", tag);
                            String oldValue = exifInterface.getAttribute(tag);
                            if (oldValue != null && !oldValue.isEmpty()) {
                                Timber.d("Exif tag %s with value %s redacted.", tag, oldValue);
                                exifInterface.setAttribute(tag, null);
                            }
                        });
                exifInterface.saveAttributes();
            }
        } catch (IOException e) {
            Timber.w(e);
            throw new RuntimeException("EXIF/XMP redaction failed.");
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