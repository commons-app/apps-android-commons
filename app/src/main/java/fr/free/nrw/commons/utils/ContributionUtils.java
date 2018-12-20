package fr.free.nrw.commons.utils;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

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

        if (checkIfDirectoryExists(TEMP_EXTERNAL_DIRECTORY)) {
            String destinationFilename = decideTempDestinationFileName();
            result = saveFileFromURI(context, URIfromContentProvider, destinationFilename);
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
     * @param tempFileUri
     */
    public static void removeTemporaryFile(Uri tempFileUri) {
        //TODO: do I have to notify file system about deletion?
        File tempFile = new File(tempFileUri.getPath());
        if (tempFile.exists()) {
            boolean isDeleted = tempFile.delete();
            Timber.e("removeTemporaryFile() parameters: URI tempFileUri %s, deleted status %b", tempFileUri, isDeleted);
        }
    }

    private static String decideTempDestinationFileName() {
        int i = 0;
        while (new File(TEMP_EXTERNAL_DIRECTORY + File.separatorChar + i + "_tmp").exists()) {
            i++;
        }

        // Use time stamp for file name, so that two temporary file never has same file name
        // to prevent previous file reference bug
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);

        // For multiple uploads, time randomisation should be combined with another random
        // parameter, since they created at same time
        int multipleUploadRandomParameter = new Random().nextInt(100);
        return TEMP_EXTERNAL_DIRECTORY + File.separatorChar + timeStamp + multipleUploadRandomParameter + "_tmp";
    }

    public static void emptyTemporaryDirectory() {
        File dir = new File(TEMP_EXTERNAL_DIRECTORY);
        if (dir.isDirectory())
        {
            String[] children = dir.list();
            if (children != null && children.length >0) {
                for (int i = 0; i < children.length; i++)
                {
                    new File(dir, children[i]).delete();
                }
            }
        }
    }

    /**
     * Saves file from source URI to destination.
     * @param sourceUri Uri which points to file to be saved
     * @param destinationFilename where file will be located at
     * @return Uri points to file saved
     */
    private static Uri saveFileFromURI(Context context, Uri sourceUri, String destinationFilename) {
        File file = new File(destinationFilename);
        if (file.exists()) {
            file.delete();
        }

        InputStream in = null;
        OutputStream out = null;
        try {
            in = context.getContentResolver().openInputStream(sourceUri);
            out = new FileOutputStream(new File(destinationFilename));

            byte[] buf = new byte[1024];
            int length;
            while ((length = in.read(buf)) > 0) {
                out.write(buf, 0, length);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if (in != null) in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return Uri.parse("file://" + destinationFilename);
    }

    /**
     * Checks if directory exists
     * @param pathToCheck path of directory to check
     * @return true if directory exists, false otherwise
     */
    private static boolean checkIfDirectoryExists(String pathToCheck) {
        File dir = new File(pathToCheck);
        return dir.exists() && dir.isDirectory();
    }
}
