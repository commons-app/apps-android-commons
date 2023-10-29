package fr.free.nrw.commons.upload;
import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class UploadStatusManager {
    private static final String UPLOAD_STATUS_FILE = "upload_status.txt";

    /**
     * Sets the upload status to the specified value.
     *
     * This method writes the provided status to a file named 'upload_status.txt'.
     * If the file doesn't exist, it will be created.
     *
     * @param context The application context, used to access the file system.
     * @param status  The status to write to the file, should be "yes" or "no".
     */
    public static void setUploadStatus(Context context, String status) {
        File file = new File(context.getFilesDir(), "upload_status.txt");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(status.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Retrieves the current upload status from the file.
     *
     * This method reads the status from a file named 'upload_status.txt'.
     * If the file doesn't exist or an error occurs, "no" is returned as a default value.
     *
     * @param context The application context, used to access the file system.
     * @return The current upload status, "yes" or "no".
     */
    public static String getUploadStatus(Context context) {
        File file = new File(context.getFilesDir(), "upload_status.txt");
        if (!file.exists()) {
            return "no";
        }
        try (FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr)) {
            return br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return "no";  // default value in case of an error
        }
    }
}


