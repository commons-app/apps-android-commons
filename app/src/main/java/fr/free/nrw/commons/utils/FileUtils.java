package fr.free.nrw.commons.utils;

import android.content.Context;
import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;

/**
 * Created for file operations
 */

public class FileUtils {

    /**
     * Saves file from source URI to destination.
     * @param sourceFileName Uri which points to file to be saved
     * @param destinationFilename where file will be located at
     * @return Uri points to file saved
     */
    public static Uri saveFileFromURI(String sourceFileName, String destinationFilename) {

        BufferedInputStream bufferedInputStream = null;
        BufferedOutputStream bufferedOutputStream = null;

        try {
            bufferedInputStream = new BufferedInputStream(new FileInputStream(sourceFileName));
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

    /**
     * Checks if directory exists
     * @param pathToCheck path of directory to check
     * @return true if directory exists, false otherwise
     */
    public static boolean checkIfDirectoryExists(String pathToCheck) {
        File director = new File(pathToCheck);
        if(director.exists() && director.isDirectory()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Creates new directory.
     * @param pathToCreateAt where directory will be created at
     * @return true if directory is created, false if an error occured, or already exists.
     */
    public static boolean createDirectory(String pathToCreateAt) {
        File directory = new File(pathToCreateAt);
        if (!directory.exists()) {
            return directory.mkdirs(); //true if directory is created
        } else {
            return false; //false if file already exists
        }
    }
}
