package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import timber.log.Timber;

import static com.mapbox.mapboxsdk.Mapbox.getApplicationContext;

public class FileUtils {

    /**
     * Get SHA1 of file from input stream
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
     * Get Geolocation of file from input file path
     */
    static String getGeolocationOfFile(String filePath) {

        try {
            ExifInterface exifInterface=new ExifInterface(filePath);
            GPSExtractor imageObj = new GPSExtractor(exifInterface);
            if (imageObj.imageCoordsExists) { // If image has geolocation information in its EXIF
                return imageObj.getCoords();
            } else {
                return "";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * In older devices getPath() may fail depending on the source URI. Creating and using a copy of the file seems to work instead.
     *
     * @return path of copy
     */
    @NonNull
    static String createExternalCopyPathAndCopy(Uri uri, ContentResolver contentResolver) throws IOException {
        FileDescriptor fileDescriptor = contentResolver.openFileDescriptor(uri, "r").getFileDescriptor();
        String copyPath = Environment.getExternalStorageDirectory().toString() + "/CommonsApp/" + new Date().getTime() + "." + getFileExt(uri, contentResolver);
        File newFile = new File(Environment.getExternalStorageDirectory().toString() + "/CommonsApp");
        newFile.mkdir();
        FileUtils.copy(fileDescriptor, copyPath);
        Timber.d("Filepath (copied): %s", copyPath);
        return copyPath;
    }

    /**
     * In older devices getPath() may fail depending on the source URI. Creating and using a copy of the file seems to work instead.
     *
     * @return path of copy
     */
    @NonNull
    static String createCopyPathAndCopy(Uri uri, Context context) throws IOException {
        FileDescriptor fileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r").getFileDescriptor();
        String copyPath = context.getCacheDir().getAbsolutePath() + "/" + new Date().getTime() + "." + getFileExt(uri, context.getContentResolver());
        FileUtils.copy(fileDescriptor, copyPath);
        Timber.d("Filepath (copied): %s", copyPath);
        return copyPath;
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
    // Can be safely suppressed, checks for isKitKat before running isDocumentUri
    @SuppressLint("NewApi")
    @Nullable
    public static String getPath(Context context, Uri uri) {

        String returnPath = null;
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    returnPath = Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) { // DownloadsProvider

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/document"), Long.valueOf(id));

                returnPath = getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) { // MediaProvider

                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                switch (type) {
                    case "image":
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        break;
                    case "video":
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                        break;
                    case "audio":
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                        break;
                    default:
                        break;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                returnPath = getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            returnPath = getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            returnPath = uri.getPath();
        }

        if (returnPath == null) {
            //fetching path may fail depending on the source URI and all hope is lost
            //so we will create and use a copy of the file, which seems to work
            String copyPath = null;
            try {
                ParcelFileDescriptor descriptor
                        = context.getContentResolver().openFileDescriptor(uri, "r");
                if (descriptor != null) {

                    SharedPreferences sharedPref = PreferenceManager
                            .getDefaultSharedPreferences(context);
                    boolean useExtStorage = sharedPref.getBoolean("useExternalStorage", true);
                    if (useExtStorage) {
                        copyPath = Environment.getExternalStorageDirectory().toString()
                                + "/CommonsApp/" + new Date().getTime() + ".jpg";
                        File newFile = new File(Environment.getExternalStorageDirectory().toString() + "/CommonsApp");
                        newFile.mkdir();
                        FileUtils.copy(
                                descriptor.getFileDescriptor(),
                                copyPath);
                        Timber.d("Filepath (copied): %s", copyPath);
                        return copyPath;
                    }
                    copyPath = context.getCacheDir().getAbsolutePath()
                            + "/" + new Date().getTime() + ".jpg";
                    FileUtils.copy(
                            descriptor.getFileDescriptor(),
                            copyPath);
                    Timber.d("Filepath (copied): %s", copyPath);
                    return copyPath;
                }
            } catch (IOException e) {
                Timber.w(e, "Error in file " + copyPath);
                return null;
            }
        } else {
            return returnPath;
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    @Nullable
    private static String getDataColumn(Context context, Uri uri, String selection,
                                        String[] selectionArgs) {

        Cursor cursor = null;
        final String column = MediaStore.Images.ImageColumns.DATA;
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch (IllegalArgumentException e) {
            Timber.d(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * Check if the URI is owned by the current app.
     */
    public static boolean isSelfOwned(Context context, Uri uri) {
        return uri.getAuthority().equals(context.getPackageName() + ".provider");
    }

    /**
     * Copy content from source file to destination file.
     *
     * @param source      stream copied from
     * @param destination stream copied to
     * @throws IOException thrown when failing to read source or opening destination file
     */
    public static void copy(@NonNull FileInputStream source, @NonNull FileOutputStream destination)
            throws IOException {
        FileChannel sourceChannel = source.getChannel();
        FileChannel destinationChannel = destination.getChannel();
        sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
    }

    /**
     * Copy content from source file to destination file.
     *
     * @param source      file descriptor copied from
     * @param destination file path copied to
     * @throws IOException thrown when failing to read source or opening destination file
     */
    private static void copy(@NonNull FileDescriptor source, @NonNull String destination)
            throws IOException {
        copy(new FileInputStream(source), new FileOutputStream(destination));
    }


    /**
     * Read and return the content of a resource file as string.
     *
     * @param fileName asset file's path (e.g. "/queries/nearby_query.rq")
     * @return the content of the file
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

    public static File createAndGetAppLogsFile(String logs) {
        try {
            File commonsAppDirectory = new File(Environment.getExternalStorageDirectory().toString() + "/CommonsApp");
            if (!commonsAppDirectory.exists()) {
                commonsAppDirectory.mkdir();
            }

            File logsFile = new File(commonsAppDirectory, "logs.txt");
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

    public static String getFilename(Uri uri, ContentResolver contentResolver) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN)
            return "";
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    static String getFileExt(String fileName){
        //Default file extension
        String extension=".jpg";

        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i+1);
        }
        return extension;
    }

    private static String getFileExt(Uri uri, ContentResolver contentResolver) {
        return getFileExt(getFilename(uri, contentResolver));
    }

    public static FileInputStream getFileInputStream(String filePath) throws FileNotFoundException {
        return new FileInputStream(filePath);
    }
}