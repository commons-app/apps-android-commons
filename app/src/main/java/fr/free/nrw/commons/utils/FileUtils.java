package fr.free.nrw.commons.utils;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created for file operations
 */

public class FileUtils {

    /**
     * Saves file from source URI to destination.
     * @param sourceUri Uri which points to file to be saved
     * @param destinationFilename where file will be located at
     * @return Uri points to file saved
     */
    public static Uri saveFileFromURI(Context context, Uri sourceUri, String destinationFilename) {
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
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(out != null) {
                    out.close();
                }
                if(in != null) {
                    in.close();
                }
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
    public static boolean checkIfDirectoryExists(String pathToCheck) {
        File director = new File(pathToCheck);
        if (director.exists() && director.isDirectory()) {
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
