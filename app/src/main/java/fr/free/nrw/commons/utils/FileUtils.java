package fr.free.nrw.commons.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import fr.free.nrw.commons.CommonsApplication;

public class FileUtils {
    /**
     * Read and return the content of a resource file as string.
     *
     * @param fileName asset file's path (e.g. "/assets/queries/nearby_query.rq")
     * @return the content of the file
     */
    public static String readFromResource(String fileName) throws IOException {
        StringBuffer buffer = new StringBuffer();
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


}
