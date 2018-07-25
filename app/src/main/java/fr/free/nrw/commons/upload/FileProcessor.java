package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.support.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.caching.CacheController;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.mwapi.CategoryApi;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static com.mapbox.mapboxsdk.Mapbox.getApplicationContext;

/**
 * Processing of the image file that is about to be uploaded via ShareActivity is done here
 */
public class FileProcessor implements SimilarImageDialogFragment.onResponse {

    @Inject
    CacheController cacheController;
    @Inject
    GpsCategoryModel gpsCategoryModel;
    @Inject
    CategoryApi apiCall;
    @Inject
    @Named("default_preferences")
    SharedPreferences prefs;
    private Uri mediaUri;
    private ContentResolver contentResolver;
    private GPSExtractor imageObj;
    private Context context;
    private String decimalCoords;
    private boolean haveCheckedForOtherImages = false;
    private String filePath;
    private String fileOrCopyPath=null;
    private boolean prefUseExtStorage;
    private boolean cacheFound;
    private GPSExtractor tempImageObj;
    private Set<String> prefRedactEXIFTags;
    private double prefLocationAccuracy;


    FileProcessor(Uri mediaUri, ContentResolver contentResolver, Context context) {
        this.mediaUri = mediaUri;
        Timber.d("mediaUri:"+ (this.mediaUri != null ? this.mediaUri.getPath() : "null"));
        this.contentResolver = contentResolver;
        this.context = context;
        ApplicationlessInjection.getInstance(context.getApplicationContext()).getCommonsApplicationComponent().inject(this);
        prefUseExtStorage = prefs.getBoolean("useExternalStorage", true);
        prefRedactEXIFTags = prefs.getStringSet("redactExifTags", Collections.emptySet() );
        prefLocationAccuracy = Double.valueOf(prefs.getString("locationAccuracy", "0"))/111300; //about 111300 meters in one degree
        Timber.d("prefLocationAccuracy:"+prefLocationAccuracy);
    }

    /**
     * Gets file path from media URI.
     * In older devices getPath() may fail depending on the source URI, creating and using a copy of the file seems to work instead.
     * If cleansing EXIF tags is enabled, it always copies the file.
     *
     * @return file path of media
     */
    @Nullable
    private String getPathOfMediaOrCopy() {
        if (fileOrCopyPath!=null)
            return fileOrCopyPath;
        filePath = FileUtils.getPath(context, mediaUri);
        Timber.d("Filepath: " + filePath);
        if (filePath == null || !prefRedactEXIFTags.isEmpty()) {
            try {
                ParcelFileDescriptor descriptor = contentResolver.openFileDescriptor(mediaUri, "r");
                if (descriptor != null) {
                    if (prefUseExtStorage) {
                        fileOrCopyPath = FileUtils.createCopyPath(descriptor);
                        return fileOrCopyPath;
                    }
                    fileOrCopyPath = getApplicationContext().getCacheDir().getAbsolutePath() + "/" + new Date().getTime() + ".jpg";
                    FileUtils.copy(descriptor.getFileDescriptor(), fileOrCopyPath);
                    Timber.d("Filepath (copied): %s", fileOrCopyPath);
                    return fileOrCopyPath;
                }
            } catch (IOException e) {
                Timber.w(e, "Error in file " + fileOrCopyPath);
                return null;
            }
        }
        fileOrCopyPath=filePath;
        return filePath;
    }

    /**
     * Processes file coordinates, either from EXIF data or user location
     *
     * @param gpsEnabled if true use GPS
     */
    GPSExtractor processFileCoordinates(boolean gpsEnabled) {
        Timber.d("Calling GPSExtractor");
        try {
            ParcelFileDescriptor descriptor = contentResolver.openFileDescriptor(mediaUri, "r");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (descriptor != null) {
                    imageObj = new GPSExtractor(descriptor.getFileDescriptor());
                }
            } else {
                String filePath = getPathOfMediaOrCopy();
                if (filePath != null) {
                    imageObj = new GPSExtractor(filePath);
                }
            }

            decimalCoords = imageObj.getCoords();
            if (decimalCoords == null || !imageObj.imageCoordsExists) {
                //Find other photos taken around the same time which has gps coordinates
                if (!haveCheckedForOtherImages)
                    findOtherImages();// Do not do repeat the process
            } else {
                useImageCoords();
            }

        } catch (FileNotFoundException e) {
            Timber.w("File not found: " + mediaUri, e);
        }
        return imageObj;
    }

    /**
     * @return The coordinates with reduced accuracy in "lat|long" format
     */
    String getDecimalCoords() {
        return decimalCoords;
    }

    /**
     * Reduces the accuracy of the coordinate according to location accuracy preference.
     *
     * @param input
     * @return The coordinate with reduced accuracy.
     */
    double anonymizeCoord(double input){
        double intermediate=Math.round(input/prefLocationAccuracy)*prefLocationAccuracy;
        return Math.round(intermediate*100000.0)/100000.0; //Round to 5th decimal place.
    }

    /**
     * Reduces the accuracy of file coordinates according to location accuracy preference.
     *
     * @return The coordinates with reduced accuracy in "lat|long" format
     */
    String getAnonymizedDecimalCoords(){
        Timber.d("Anonymizing coords with setting:"+prefLocationAccuracy);
        if(prefLocationAccuracy<0)
            return null;
        else if (prefLocationAccuracy==0)
            return decimalCoords;
        else{
            return  String.valueOf(anonymizeCoord(imageObj.getDecLatitude())) + "|"
                    + String.valueOf(anonymizeCoord(imageObj.getDecLongitude()));
        }
    }

    /**
     * Find other images around the same location that were taken within the last 20 sec
     *
     */
    private void findOtherImages() {
        Timber.d("filePath" + getPathOfMediaOrCopy());

        long timeOfCreation = new File(filePath).lastModified();//Time when the original image was created
        File folder = new File(filePath.substring(0, filePath.lastIndexOf('/')));
        File[] files = folder.listFiles();
        Timber.d("folderTime Number:" + files.length);


        for (File file : files) {
            if (file.lastModified() - timeOfCreation <= (120 * 1000) && file.lastModified() - timeOfCreation >= -(120 * 1000)) {
                //Make sure the photos were taken within 20seconds
                Timber.d("fild date:" + file.lastModified() + " time of creation" + timeOfCreation);
                tempImageObj = null;//Temporary GPSExtractor to extract coords from these photos
                ParcelFileDescriptor descriptor = null;
                try {
                    descriptor = contentResolver.openFileDescriptor(Uri.parse(file.getAbsolutePath()), "r");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (descriptor != null) {
                        tempImageObj = new GPSExtractor(descriptor.getFileDescriptor());
                    }
                } else {
                    if (filePath != null) {
                        tempImageObj = new GPSExtractor(file.getAbsolutePath());
                    }
                }

                if (tempImageObj != null) {
                    Timber.d("not null fild EXIF" + tempImageObj.imageCoordsExists + " coords" + tempImageObj.getCoords());
                    if (tempImageObj.getCoords() != null && tempImageObj.imageCoordsExists) {
                        // Current image has gps coordinates and it's not current gps locaiton
                        Timber.d("This file has image coords:" + file.getAbsolutePath());
                        SimilarImageDialogFragment newFragment = new SimilarImageDialogFragment();
                        Bundle args = new Bundle();
                        args.putString("originalImagePath", filePath);
                        args.putString("possibleImagePath", file.getAbsolutePath());
                        newFragment.setArguments(args);
                        newFragment.show(((AppCompatActivity) context).getSupportFragmentManager(), "dialog");
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
    public void useImageCoords() {
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
                cacheFound = false;
                apiCall.request(decimalCoords)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe(
                                gpsCategoryModel::setCategoryList,
                                throwable -> {
                                    Timber.e(throwable);
                                    gpsCategoryModel.clear();
                                }
                        );
                Timber.d("displayCatList size 0, calling MWAPI %s", displayCatList);
            } else {
                cacheFound = true;
                Timber.d("Cache found, setting categoryList in model to %s", displayCatList);
                gpsCategoryModel.setCategoryList(displayCatList);
            }
        } else {
            Timber.d("EXIF: no coords");
        }
    }

    boolean isCacheFound() {
        return cacheFound;
    }


    /**
    *Redacts EXIF data from the file.
    *
     * @return Uri of the new file.
    **/
    public Uri redactEXIFData() {
        try {
            Timber.d("Tags to be redacted:"+ Arrays.toString(prefRedactEXIFTags.toArray()));
            Timber.v("File path:"+getPathOfMediaOrCopy());
            if (!prefRedactEXIFTags.isEmpty() && getPathOfMediaOrCopy() != null) {
                ExifInterface exif = new ExifInterface(getPathOfMediaOrCopy());//Temporary EXIF interface to redact data.
                for (String tag : prefRedactEXIFTags) {
                    String oldValue = exif.getAttribute(tag);
                    if (oldValue != null && !oldValue.isEmpty()) {
                        Timber.d("Exif tag " + tag + " with value " + oldValue + " redacted.");
                        exif.setAttribute(tag, null);
                    }
                }
                if (prefLocationAccuracy<0) {
                    Timber.d("Setting EXIF coordinates to 0");
                    exif.setLatLong(0d, 0d);
                }else if (prefLocationAccuracy!=0){
                    exif.setLatLong(anonymizeCoord(imageObj.getDecLatitude()), anonymizeCoord(imageObj.getDecLongitude()));
                }
                exif.saveAttributes();
            }
        } catch (IOException e) {
            Timber.w(e);
			throw new RuntimeException("EXIF redaction failed.");
        }
        return Uri.parse("file://" + getPathOfMediaOrCopy());
    }

    /**
     * Calls the async task that detects if image is fuzzy, too dark, etc
     */
    void detectUnwantedPictures() {
        String imageMediaFilePath = FileUtils.getPath(context, mediaUri);
        DetectUnwantedPicturesAsync detectUnwantedPicturesAsync
                = new DetectUnwantedPicturesAsync(new WeakReference<>((Activity) context), imageMediaFilePath);
        detectUnwantedPicturesAsync.execute();
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
