package fr.free.nrw.commons.upload;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

import static fr.free.nrw.commons.upload.ExistingFileAsync.Result.DUPLICATE_PROCEED;
import static fr.free.nrw.commons.upload.ExistingFileAsync.Result.NO_DUPLICATE;

public class FileProcessor {


    /**
     * Check if file user wants to upload already exists on Commons
     */
    private void checkIfFileExists() {
        if (!useNewPermissions || storagePermitted) {
            if (!duplicateCheckPassed) {
                //Test SHA1 of image to see if it matches SHA1 of a file on Commons
                try {
                    InputStream inputStream = getContentResolver().openInputStream(mediaUri);
                    Timber.d("Input stream created from %s", mediaUri.toString());
                    String fileSHA1 = getSHA1(inputStream);
                    Timber.d("File SHA1 is: %s", fileSHA1);

                    ExistingFileAsync fileAsyncTask =
                            new ExistingFileAsync(new WeakReference<Activity>(this), fileSHA1, new WeakReference<Context>(this), result -> {
                                Timber.d("%s duplicate check: %s", mediaUri.toString(), result);
                                duplicateCheckPassed = (result == DUPLICATE_PROCEED || result == NO_DUPLICATE);

                                //TODO: 16/9/17 should we run DetectUnwantedPicturesAsync if DUPLICATE_PROCEED is returned? Since that means
                                //we are processing images that are already on server???...
                                if (duplicateCheckPassed) {
                                    //image can be uploaded, so now check if its a useless picture or not
                                    detectUnwantedPictures();
                                }
                            },mwApi);
                    fileAsyncTask.execute();
                } catch (IOException e) {
                    Timber.e(e, "IO Exception: ");
                }
            }
        } else {
            Timber.w("not ready for preprocessing: useNewPermissions=%s storage=%s location=%s",
                    useNewPermissions, storagePermitted, locationPermitted);
        }
    }

    /**
     * Calls the async task that detects if image is fuzzy, too dark, etc
     */
    private void detectUnwantedPictures() {
        String imageMediaFilePath = FileUtils.getPath(this,mediaUri);
        DetectUnwantedPicturesAsync detectUnwantedPicturesAsync
                = new DetectUnwantedPicturesAsync(new WeakReference<Activity>(this), imageMediaFilePath);
        detectUnwantedPicturesAsync.execute();
    }


    /**
     * Gets file path from media URI.
     * In older devices getPath() may fail depending on the source URI, creating and using a copy of the file seems to work instead.
     * @return file path of media
     */
    @Nullable
    private String getPathOfMediaOrCopy() {
        String filePath = FileUtils.getPath(getApplicationContext(), mediaUri);
        Timber.d("Filepath: " + filePath);
        if (filePath == null) {
            String copyPath = null;
            try {
                ParcelFileDescriptor descriptor = getContentResolver().openFileDescriptor(mediaUri, "r");
                if (descriptor != null) {
                    boolean useExtStorage = prefs.getBoolean("useExternalStorage", true);
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
    private void getFileCoordinates(boolean gpsEnabled) {
        Timber.d("Calling GPSExtractor");
        try {
            if (imageObj == null) {
                ParcelFileDescriptor descriptor = getContentResolver().openFileDescriptor(mediaUri, "r");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (descriptor != null) {
                        imageObj = new GPSExtractor(descriptor.getFileDescriptor(), this, prefs);
                    }
                } else {
                    String filePath = getPathOfMediaOrCopy();
                    if (filePath != null) {
                        imageObj = new GPSExtractor(filePath, this, prefs);
                    }
                }
            }

            if (imageObj != null) {
                decimalCoords = imageObj.getCoords(gpsEnabled);
                if (decimalCoords == null || !imageObj.imageCoordsExists){
                    //Find other photos taken around the same time which has gps coordinates
                    if(!haveCheckedForOtherImages)
                        findOtherImages(gpsEnabled);// Do not do repeat the process
                }
                else {
                    useImageCoords();
                }
            }
        } catch (FileNotFoundException e) {
            Timber.w("File not found: " + mediaUri, e);
        }
    }

    private void findOtherImages(boolean gpsEnabled) {
        Timber.d("filePath"+getPathOfMediaOrCopy());
        String filePath = getPathOfMediaOrCopy();
        long timeOfCreation = new File(filePath).lastModified();//Time when the original image was created
        File folder = new File(filePath.substring(0,filePath.lastIndexOf('/')));
        File[] files = folder.listFiles();
        Timber.d("folderTime Number:"+files.length);

        for(File file : files){
            if(file.lastModified()-timeOfCreation<=(120*1000) && file.lastModified()-timeOfCreation>=-(120*1000)){
                //Make sure the photos were taken within 20seconds
                Timber.d("fild date:"+file.lastModified()+ " time of creation"+timeOfCreation);
                tempImageObj = null;//Temporary GPSExtractor to extract coords from these photos
                ParcelFileDescriptor descriptor
                        = null;
                try {
                    descriptor = getContentResolver().openFileDescriptor(Uri.parse(file.getAbsolutePath()), "r");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (descriptor != null) {
                        tempImageObj = new GPSExtractor(descriptor.getFileDescriptor(),this, prefs);
                    }
                } else {
                    if (filePath != null) {
                        tempImageObj = new GPSExtractor(file.getAbsolutePath(), this, prefs);
                    }
                }

                if(tempImageObj!=null){
                    Timber.d("not null fild EXIF"+tempImageObj.imageCoordsExists +" coords"+tempImageObj.getCoords(gpsEnabled));
                    if(tempImageObj.getCoords(gpsEnabled)!=null && tempImageObj.imageCoordsExists){
//                       Current image has gps coordinates and it's not current gps locaiton
                        Timber.d("This fild has image coords:"+ file.getAbsolutePath());
//                       Create a dialog fragment for the suggestion
                        FragmentManager fragmentManager = getSupportFragmentManager();
                        SimilarImageDialogFragment newFragment = new SimilarImageDialogFragment();
                        Bundle args = new Bundle();
                        args.putString("originalImagePath",filePath);
                        args.putString("possibleImagePath",file.getAbsolutePath());
                        newFragment.setArguments(args);
                        newFragment.show(fragmentManager, "dialog");
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
            Timber.d("is EXIF data present:"+imageObj.imageCoordsExists+" from findOther image:"+(imageObj==tempImageObj));

            // Only set cache for this point if image has coords
            if (imageObj.imageCoordsExists) {
                double decLongitude = imageObj.getDecLongitude();
                double decLatitude = imageObj.getDecLatitude();
                cacheController.setQtPoint(decLongitude, decLatitude);
            }

            MwVolleyApi apiCall = new MwVolleyApi(this);

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

}
