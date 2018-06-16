package fr.free.nrw.commons.utils;

import android.content.Context;
import android.net.Uri;

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

/**
 * This class includes utility methods for uploading process of images.
 */

public class ContributionUtils {

    private static final String TEMP_UPLOADING_DIRECTORY =
            android.os.Environment.getExternalStorageDirectory().getPath()+
                    File.separatorChar+"UploadingByCommonsApp"+File.separatorChar;

    /**
     * Saves images temporarily to a fixed folder and use URI of that file during upload process.
     * Otherwise, temporary URİ provided by content provider sometimes points to a null space and
     * consequently upload fails. See: issue #1400A and E.
     * Not: Saved image will be deleted, our directory will be empty after upload process.
     * @return URI of saved image
     */
    public static Uri saveFileBeingUploadedTemporarily(Context context, URI URIfromContentProvider) {
        // TODO add exceptions for Google Drive URİ is needed
        checkIfDirectoryExists();

        String sourceFilename= URIfromContentProvider.getPath();
        String destinationFilename = TEMP_UPLOADING_DIRECTORY+"1_tmp";

        BufferedInputStream bufferedInputStream = null;
        BufferedOutputStream bufferedOutputStream = null;

        try {
            bufferedInputStream = new BufferedInputStream(new FileInputStream(sourceFilename));
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(destinationFilename, false));
            byte[] buffer = new byte[1024];
            bufferedInputStream.read(buffer);
            do {
                bufferedOutputStream.write(buffer);
            } while(bufferedInputStream.read(buffer) != -1);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedInputStream != null) bufferedInputStream.close();
                if (bufferedOutputStream != null) bufferedOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Uri.parse(destinationFilename);
    }
}
