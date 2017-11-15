package fr.free.nrw.commons.utils;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import fr.free.nrw.commons.CommonsApplication;
import timber.log.Timber;

public class FileUtils {
    /**
     * Read and return the content of a resource file as string.
     *
     * @param fileName asset file's path (e.g. "/assets/queries/nearby_query.rq")
     * @return the content of the file
     */
    public static String readFromResource(String fileName) throws IOException {
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(
                            CommonsApplication.class.getResourceAsStream(fileName), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return buffer.toString();
    }

    /**
     * Deletes files.
     * @param file context
     */
    public static boolean deleteFile(File file) {
        boolean deletedAll = true;
        if (file != null) {
            if (file.isDirectory()) {
                String[] children = file.list();
                for (String child : children) {
                    deletedAll = deleteFile(new File(file, child)) && deletedAll;
                }
            } else {
                deletedAll = file.delete();
            }
        }

        return deletedAll;
    }

    public static File createAndGetAppLogsFile(String logs) {
        try {
            File commonsAppDirectory = new File(Environment.getExternalStorageDirectory().toString() + "/CommonsApp");
            if (!commonsAppDirectory.exists()) {
                commonsAppDirectory.mkdir();
            }

            File logsFile = new File(commonsAppDirectory,"logs.txt");
            if (logsFile.exists()) {
                //old logs file is useless
                logsFile.delete();
            }

            logsFile.createNewFile();

            FileOutputStream outputStream = new FileOutputStream(logsFile);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            outputStreamWriter.append(logs);
            outputStreamWriter.close();
            outputStream.flush();
            outputStream.close();

            return logsFile;
        } catch (IOException ioe) {
            Timber.e(ioe);
            return null;
        }
    }
}
