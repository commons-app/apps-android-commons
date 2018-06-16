package fr.free.nrw.commons.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import timber.log.Timber;

/**
 * This class includes utility methods for uploading process of images.
 */

public class ContributionUtils {

    private static final String TEMP_UPLOADING_DIRECTORY =
            android.os.Environment.getExternalStorageDirectory().getPath()+
                    File.separatorChar+"UploadingByCommonsApp";

    /**
     * Saves images temporarily to a fixed folder and use URI of that file during upload process.
     * Otherwise, temporary URİ provided by content provider sometimes points to a null space and
     * consequently upload fails. See: issue #1400A and E.
     * Not: Saved image will be deleted, our directory will be empty after upload process.
     * @return URI of saved image
     */
    public static Uri saveFileBeingUploadedTemporarily(Uri URIfromContentProvider) {
        // TODO add exceptions for Google Drive URİ is needed
        Uri result = null;
        if (FileUtils.checkIfDirectoryExists(TEMP_UPLOADING_DIRECTORY)) {
            Log.d("deneme","saveFileBeingUploadedTemporarily checkIfDirectoryExists");

            String destinationFilename = TEMP_UPLOADING_DIRECTORY +File.separatorChar+"1_tmp";
            String sourceFileName = URIfromContentProvider.getPath();
            result = FileUtils.saveFileFromURI(sourceFileName, destinationFilename);
        } else { // If directory doesn't exist, create it and recursive call current method to check again
           if (FileUtils.createDirectory(TEMP_UPLOADING_DIRECTORY)) {
               Log.d("deneme","saveFileBeingUploadedTemporarily directoryCreated");

               Timber.d("saveFileBeingUploadedTemporarily() parameters: URI from Content Provider %s", URIfromContentProvider);
               saveFileBeingUploadedTemporarily(URIfromContentProvider); // If directory is created
           } else { //An error occurred to create directory
               Timber.e("saveFileBeingUploadedTemporarily() parameters: URI from Content Provider %s", URIfromContentProvider);
           }
        }
        return result;
    }
}
