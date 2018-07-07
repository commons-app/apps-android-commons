package fr.free.nrw.commons.utils;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;

import timber.log.Timber;

/**
 * This class includes utility methods for uploading process of images.
 */

public class ContributionUtils {

    private static String TEMP_EXTERNAL_DIRECTORY =
            android.os.Environment.getExternalStorageDirectory().getPath()+
                    File.separatorChar+"UploadingByCommonsApp";

    /**
     * Saves images temporarily to a fixed folder and use Uri of that file during upload process.
     * Otherwise, temporary Uri provided by content provider sometimes points to a null space and
     * consequently upload fails. See: issue #1400A and E.
     * Not: Saved image will be deleted, our directory will be empty after upload process.
     * @return URI of saved image
     */
    public static Uri saveFileBeingUploadedTemporarily(Context context, Uri URIfromContentProvider) {
        // TODO add exceptions for Google Drive URİ is needed
        Uri result = null;

        if (FileUtils.checkIfDirectoryExists(TEMP_EXTERNAL_DIRECTORY)) {
            String destinationFilename = decideTempDestinationFileName();
            result = FileUtils.saveFileFromURI(context, URIfromContentProvider, destinationFilename);
        } else { // If directory doesn't exist, create it and recursive call current method to check again

            File file = new File(TEMP_EXTERNAL_DIRECTORY);
            if (file.mkdirs()) {
               Timber.d("saveFileBeingUploadedTemporarily() parameters: URI from Content Provider %s", URIfromContentProvider);
               result = saveFileBeingUploadedTemporarily(context, URIfromContentProvider); // If directory is created
            } else { //An error occurred to create directory
               Timber.e("saveFileBeingUploadedTemporarily() parameters: URI from Content Provider %s", URIfromContentProvider);
            }
        }
        return result;
    }

    /**
     * Removes temp file created during upload
     * @param context
     * @param tempFileUri
     * @param contentProviderUri
     */
    public static void removeTemporaryFile(Context context, Uri tempFileUri, Uri contentProviderUri) {
        //TODO: do I have to notify file system about deletion?
        File tempFile = new File(tempFileUri.getPath());
        if (tempFile.exists()) {
            boolean isDeleted= tempFile.delete();
            Timber.e("removeTemporaryFile() parameters: URI tempFileUri %s, deleted status %b", tempFileUri, isDeleted);
        }
    }

    private static String decideTempDestinationFileName() {
        int i = 0;
        while (true) {
            if (new File(TEMP_EXTERNAL_DIRECTORY +File.separatorChar+i+"_tmp").exists()) {
                // This file is in use, try enother file
                i++;
            } else {
                return TEMP_EXTERNAL_DIRECTORY +File.separatorChar+i+"_tmp";
            }
        }
    }

    public static void emptyTemporaryDirectory() {
        File dir = new File(TEMP_EXTERNAL_DIRECTORY);
        if (dir.isDirectory())
        {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++)
            {
                new File(dir, children[i]).delete();
            }
        }
    }
}
