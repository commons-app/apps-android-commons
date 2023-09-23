package fr.free.nrw.commons.upload;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.exifinterface.media.ExifInterface;

import fr.free.nrw.commons.location.LatLng;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import timber.log.Timber;

public class FileUtils {

    /**
     * Get SHA1 of filePath from input stream
     */
    static String getSHA1(InputStream is) {

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            Timber.e(e, "Exception while getting Digest");
            return "";
        }

        byte[] buffer = new byte[8192];
        int read;
        try {
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            // Fill to 40 chars
            output = String.format("%40s", output).replace(' ', '0');
            Timber.i("File SHA1: %s", output);

            return output;
        } catch (IOException e) {
            Timber.e(e, "IO Exception");
            return "";
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Timber.e(e, "Exception on closing MD5 input stream");
            }
        }
    }

    /**
     * Get Geolocation of filePath from input filePath path
     */
    static String getGeolocationOfFile(String filePath, LatLng inAppPictureLocation) {

        try {
            ExifInterface exifInterface = new ExifInterface(filePath);
            ImageCoordinates imageObj = new ImageCoordinates(exifInterface, inAppPictureLocation);
            if (imageObj.getDecimalCoords() != null) { // If image has geolocation information in its EXIF
                return imageObj.getDecimalCoords();
            } else {
                return "";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }


    /**
     * Read and return the content of a resource filePath as string.
     *
     * @param fileName asset filePath's path (e.g. "/queries/nearby_query.rq")
     * @return the content of the filePath
     */
    public static String readFromResource(String fileName) throws IOException {
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = null;
        try {
            InputStream inputStream = FileUtils.class.getResourceAsStream(fileName);
            if (inputStream == null) {
                throw new FileNotFoundException(fileName);
            }
            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
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
     *
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

    public static String getMimeType(Context context, Uri uri) {
        String mimeType;
        if (uri.getScheme()!=null && uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver cr = context.getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());
        }
        return mimeType;
    }

    static String getFileExt(String fileName) {
        //Default filePath extension
        String extension = ".jpg";

        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1);
        }
        return extension;
    }

    static FileInputStream getFileInputStream(String filePath) throws FileNotFoundException {
        return new FileInputStream(filePath);
    }

    public static boolean recursivelyCreateDirs(String dirPath) {
        File fileDir = new File(dirPath);
        if (!fileDir.exists()) {
            return fileDir.mkdirs();
        }
        return true;
    }

    /**
     * Check if file exists in local dirs
     */
    public static boolean fileExists(Uri localUri) {
        try {
            File file = new File(localUri.getPath());
            return file.exists();
        } catch (Exception e) {
            Timber.d(e);
            return false;
        }
    }
}
