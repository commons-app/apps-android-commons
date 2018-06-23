package fr.free.nrw.commons.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;

import timber.log.Timber;

/**
 * This class includes utility methods for uploading process of images.
 */

public class ContributionUtils {

    /*private static String TEMP_UPLOADING_DIRECTORY_BODY =
            android.os.Environment.getExternalStorageDirectory().getPath()+
                    File.separatorChar+"UploadingByCommonsApp";*/
    // This file will be folder of files which are uploading now will be stored in
    private static final String TEMP_UPLOADING_DIRECTORY_BODY = "/Uploading_Now";

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

        if (FileUtils.checkIfDirectoryExists(context.getFilesDir()+TEMP_UPLOADING_DIRECTORY_BODY)) {
            String destinationFilename = context.getFilesDir()+ TEMP_UPLOADING_DIRECTORY_BODY +File.separatorChar+"1_tmp";
            String sourceFileName = URIfromContentProvider.getPath();

            result = FileUtils.saveFileFromURI(context, URIfromContentProvider, destinationFilename);
        } else { // If directory doesn't exist, create it and recursive call current method to check again

            File file = new File(context.getFilesDir(),TEMP_UPLOADING_DIRECTORY_BODY);
            if (file.mkdirs()) {
               Timber.d("saveFileBeingUploadedTemporarily() parameters: URI from Content Provider %s", URIfromContentProvider);
               result = saveFileBeingUploadedTemporarily(context, URIfromContentProvider); // If directory is created
            } else { //An error occurred to create directory
               Timber.e("saveFileBeingUploadedTemporarily() parameters: URI from Content Provider %s", URIfromContentProvider);
            }
        }
        Log.d("deneme","result Uri is:"+result);
        return result;
    }

    public static void emptyTemporaryDirectory(Context context) {
        File dir = new File(context.getFilesDir()+TEMP_UPLOADING_DIRECTORY_BODY);
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
