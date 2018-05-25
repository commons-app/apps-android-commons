package fr.free.nrw.commons.upload;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import fr.free.nrw.commons.caching.CacheController;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import timber.log.Timber;

import static com.mapbox.mapboxsdk.Mapbox.getApplicationContext;
import static fr.free.nrw.commons.upload.ExistingFileAsync.Result.DUPLICATE_PROCEED;
import static fr.free.nrw.commons.upload.ExistingFileAsync.Result.NO_DUPLICATE;
import static fr.free.nrw.commons.upload.FileUtils.getSHA1;

public class FileProcessor {

    private Uri mediaUri;
    private ContentResolver contentResolver;
    private GPSExtractor imageObj;
    private SharedPreferences prefs;
    private Context context;
    private String decimalCoords;
    private boolean haveCheckedForOtherImages = false;
    private String filePath;
    private boolean useExtStorage;
    private boolean cacheFound;

    @Inject
    CacheController cacheController;

    FileProcessor(Uri mediaUri, ContentResolver contentResolver, SharedPreferences prefs, Context context) {
        this.mediaUri = mediaUri;
        this.contentResolver = contentResolver;
        this.prefs = prefs;
        this.context = context;
        useExtStorage = prefs.getBoolean("useExternalStorage", true);
        ApplicationlessInjection.getInstance(context.getApplicationContext()).getCommonsApplicationComponent().inject(this);
    }

    /**
     * Gets file path from media URI.
     * In older devices getPath() may fail depending on the source URI, creating and using a copy of the file seems to work instead.
     * @return file path of media
     */
    @Nullable
    String getPathOfMediaOrCopy() {
        filePath = FileUtils.getPath(context, mediaUri);
        Timber.d("Filepath: " + filePath);
        if (filePath == null) {
            String copyPath = null;
            try {
                ParcelFileDescriptor descriptor = contentResolver.openFileDescriptor(mediaUri, "r");
                if (descriptor != null) {
                    if (useExtStorage) {
                        copyPath = FileUtils.createCopyPath(descriptor);
                        return copyPath;
                    }
                    copyPath = getApplicationContext().getCacheDir().getAbsolutePath() + "/" + new Date().getTime() + ".jpg";
                    FileUtils.copy(descriptor.getFileDescriptor(), copyPath);
                    Timber.d("Filepath (copied): %s", copyPath);
                    return copyPath;
                }
            } catch (IOException e) {
                Timber.w(e, "Error in file " + copyPath);
                return null;
            }
        }
        return filePath;
    }

    /**
     * Gets coordinates for category suggestions, either from EXIF data or user location
     * @param gpsEnabled if true use GPS
     */
    void getFileCoordinates(boolean gpsEnabled) {
        Timber.d("Calling GPSExtractor");
        try {
                ParcelFileDescriptor descriptor = contentResolver.openFileDescriptor(mediaUri, "r");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (descriptor != null) {
                        imageObj = new GPSExtractor(descriptor.getFileDescriptor(), context, prefs);
                    }
                } else {
                    String filePath = getPathOfMediaOrCopy();
                    if (filePath != null) {
                        imageObj = new GPSExtractor(filePath, context, prefs);
                    }
            }

                decimalCoords = imageObj.getCoords(gpsEnabled);
                if (decimalCoords == null || !imageObj.imageCoordsExists){
                    //Find other photos taken around the same time which has gps coordinates
                    if(!haveCheckedForOtherImages)
                        findOtherImages(gpsEnabled);// Do not do repeat the process
                }
                else {
                    useImageCoords();
                }

        } catch (FileNotFoundException e) {
            Timber.w("File not found: " + mediaUri, e);
        }
    }

    void findOtherImages(boolean gpsEnabled) {
        Timber.d("filePath"+getPathOfMediaOrCopy());

        long timeOfCreation = new File(filePath).lastModified();//Time when the original image was created
        File folder = new File(filePath.substring(0,filePath.lastIndexOf('/')));
        File[] files = folder.listFiles();
        Timber.d("folderTime Number:"+files.length);
        GPSExtractor tempImageObj;

        for(File file : files){
            if(file.lastModified()-timeOfCreation<=(120*1000) && file.lastModified()-timeOfCreation>=-(120*1000)){
                //Make sure the photos were taken within 20seconds
                Timber.d("fild date:"+file.lastModified()+ " time of creation"+timeOfCreation);
                tempImageObj = null;//Temporary GPSExtractor to extract coords from these photos
                ParcelFileDescriptor descriptor = null;
                try {
                    descriptor = contentResolver.openFileDescriptor(Uri.parse(file.getAbsolutePath()), "r");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (descriptor != null) {
                        tempImageObj = new GPSExtractor(descriptor.getFileDescriptor(), context, prefs);
                    }
                } else {
                    if (filePath != null) {
                        tempImageObj = new GPSExtractor(file.getAbsolutePath(), context, prefs);
                    }
                }

                if(tempImageObj!=null){
                    Timber.d("not null fild EXIF"+tempImageObj.imageCoordsExists +" coords"+tempImageObj.getCoords(gpsEnabled));
                    if(tempImageObj.getCoords(gpsEnabled)!=null && tempImageObj.imageCoordsExists){
//                       Current image has gps coordinates and it's not current gps locaiton
                        Timber.d("This file has image coords:"+ file.getAbsolutePath());
//                       Create a dialog fragment for the suggestion
                        SimilarImageDialogFragment newFragment = new SimilarImageDialogFragment();
                        Bundle args = new Bundle();
                        args.putString("originalImagePath",filePath);
                        args.putString("possibleImagePath",file.getAbsolutePath());
                        newFragment.setArguments(args);
                        newFragment.show(((AppCompatActivity)context).getSupportFragmentManager(), "dialog");
                        break;
                    }
                }
            }
        }
        haveCheckedForOtherImages = true; //Finished checking for other images
        return;
    }


    /**
     * Initiates retrieval of image coordinates or user coordinates, and caching of coordinates.
     * Then initiates the calls to MediaWiki API through an instance of MwVolleyApi.
     */
    public void useImageCoords() {
        if (decimalCoords != null) {
            Timber.d("Decimal coords of image: %s", decimalCoords);
            Timber.d("is EXIF data present:"+imageObj.imageCoordsExists+" from findOther image");

            // Only set cache for this point if image has coords
            if (imageObj.imageCoordsExists) {
                double decLongitude = imageObj.getDecLongitude();
                double decLatitude = imageObj.getDecLatitude();
                cacheController.setQtPoint(decLongitude, decLatitude);
            }

            MwVolleyApi apiCall = new MwVolleyApi(context);

            List<String> displayCatList = cacheController.findCategory();
            boolean catListEmpty = displayCatList.isEmpty();


            // If no categories found in cache, call MediaWiki API to match image coords with nearby Commons categories
            if (catListEmpty) {
                cacheFound = false;
                apiCall.request(decimalCoords);
                Timber.d("displayCatList size 0, calling MWAPI %s", displayCatList);
            } else {
                cacheFound = true;
                Timber.d("Cache found, setting categoryList in MwVolleyApi to %s", displayCatList);
                MwVolleyApi.setGpsCat(displayCatList);
            }
        }else{
            Timber.d("EXIF: no coords");
        }
    }

    boolean isCacheFound() {
        return cacheFound;
    }

}
