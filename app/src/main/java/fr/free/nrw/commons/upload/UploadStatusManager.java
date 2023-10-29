package fr.free.nrw.commons.upload;
import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import timber.log.Timber;

public class UploadStatusManager {
    private static final String FILE_NAME = "upload_status.txt";

    /**
     * Writes the hashmap of unfinished uploads to a file.
     *
     * @param context The application context.
     * @param uploads The map of unfinished uploads.
     */
    public static void writeUnfinishedUploads(Context context, Map<String, Boolean> uploads) {
        try {
            FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(uploads);
            oos.close();
            fos.close();
        } catch (IOException e) {
            Timber.e(e, "Failed to write unfinished uploads to file");
        }
    }

    /**
     * Reads the hashmap of unfinished uploads from a file.
     *
     * @param context The application context.
     * @return The map of unfinished uploads, or an empty map if reading fails.
     */
    public static Map<String, Boolean> readUnfinishedUploads(Context context) {
        try {
            FileInputStream fis = context.openFileInput(FILE_NAME);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Map<String, Boolean> uploads = (Map<String, Boolean>) ois.readObject();
            ois.close();
            fis.close();
            return uploads;
        } catch (IOException | ClassNotFoundException e) {
            Timber.e(e, "Failed to read unfinished uploads from file");
            return new HashMap<>();  // Return an empty map if reading fails
        }
    }
}

