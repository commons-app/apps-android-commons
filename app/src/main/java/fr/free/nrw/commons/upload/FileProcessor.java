package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.List;

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
    private String filePath;
    private ContentResolver contentResolver;
    private GPSExtractor imageObj;
    private Context context;
    private String decimalCoords;
    private ExifInterface exifInterface;
    private boolean useExtStorage;
    private boolean haveCheckedForOtherImages = false;
    private GPSExtractor tempImageObj;

    FileProcessor(@NonNull String filePath, ContentResolver contentResolver, Context context) {
        this.filePath = filePath;
        this.contentResolver = contentResolver;
        this.context = context;
        ApplicationlessInjection.getInstance(context.getApplicationContext()).getCommonsApplicationComponent().inject(this);
        try {
            exifInterface=new ExifInterface(filePath);
        } catch (IOException e) {
            Timber.e(e);
        }
        useExtStorage = prefs.getBoolean("useExternalStorage", true);
    }

    /**
     * Processes file coordinates, either from EXIF data or user location
     */
    GPSExtractor processFileCoordinates() {
        Timber.d("Calling GPSExtractor");
        imageObj = new GPSExtractor(exifInterface);
        decimalCoords = imageObj.getCoords();
        if (decimalCoords == null || !imageObj.imageCoordsExists) {
            //Find other photos taken around the same time which has gps coordinates
            if (!haveCheckedForOtherImages)
                findOtherImages();// Do not do repeat the process
        } else {
            useImageCoords();
        }

        return imageObj;
    }

    String getDecimalCoords() {
        return decimalCoords;
    }

    /**
     * Find other images around the same location that were taken within the last 20 sec
     */
    private void findOtherImages() {
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
                ParcelFileDescriptor descriptor = null;
                try {
                    descriptor = contentResolver.openFileDescriptor(Uri.fromFile(file), "r");
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
