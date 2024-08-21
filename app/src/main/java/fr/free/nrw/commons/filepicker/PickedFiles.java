package fr.free.nrw.commons.filepicker;

import android.content.ContentResolver;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import timber.log.Timber;

/**
 * PickedFiles.
 * Process the upload items.
 */
public class PickedFiles implements Constants {

    /**
     * Get Folder Name
     * @param context
     * @return default application folder name.
     */
    private static String getFolderName(@NonNull Context context) {
        return FilePicker.configuration(context).getFolderName();
    }

    /**
     * tempImageDirectory
     * @param context
     * @return temporary image directory to copy and perform exif changes.
     */
    private static File tempImageDirectory(@NonNull Context context) {
        File privateTempDir = new File(context.getCacheDir(), DEFAULT_FOLDER_NAME);
        if (!privateTempDir.exists()) privateTempDir.mkdirs();
        return privateTempDir;
    }

    /**
     * writeToFile
     * writes inputStream data to the destination file.
     * @param in input stream of source file.
     * @param file destination file
     */
    private static void writeToFile(InputStream in, File file) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    /**
     * Copy file function.
     * Copies source file to destination file.
     * @param src source file
     * @param dst destination file
     * @throws IOException (File input stream exception)
     */
    private static void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            writeToFile(in, dst);
        }
    }

    /**
     * Copy files in separate thread.
     * Copies all the uploadable files to the temp image folder on background thread.
     * @param context
     * @param filesToCopy uploadable file list to be copied.
     */
    static void copyFilesInSeparateThread(final Context context, final List<UploadableFile> filesToCopy) {
        new Thread(() -> {
            List<File> copiedFiles = new ArrayList<>();
            int i = 1;
            for (UploadableFile uploadableFile : filesToCopy) {
                File fileToCopy = uploadableFile.getFile();
                File dstDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), getFolderName(context));
                if (!dstDir.exists()) {
                    dstDir.mkdirs();
                }

                String[] filenameSplit = fileToCopy.getName().split("\\.");
                String extension = "." + filenameSplit[filenameSplit.length - 1];
                String filename = String.format("IMG_%s_%d.%s", new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()), i, extension);

                File dstFile = new File(dstDir, filename);
                try {
                    dstFile.createNewFile();
                    copyFile(fileToCopy, dstFile);
                    copiedFiles.add(dstFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                i++;
            }
            scanCopiedImages(context, copiedFiles);
        }).run();
    }

    /**
     * singleFileList.
     * converts a single uploadableFile to list of uploadableFile.
     * @param file uploadable file
     * @return
     */
    static List<UploadableFile> singleFileList(UploadableFile file) {
        List<UploadableFile> list = new ArrayList<>();
        list.add(file);
        return list;
    }

    /**
     * ScanCopiedImages
     * Scan copied images metadata using media scanner.
     * @param context
     * @param copiedImages copied images list.
     */
    static void scanCopiedImages(Context context, List<File> copiedImages) {
        String[] paths = new String[copiedImages.size()];
        for (int i = 0; i < copiedImages.size(); i++) {
            paths[i] = copiedImages.get(i).toString();
        }

        MediaScannerConnection.scanFile(context,
                paths, null,
                (path, uri) -> {
                    Timber.d("Scanned " + path + ":");
                    Timber.d("-> uri=%s", uri);
                });
    }

    /**
     * pickedExistingPicture
     * convert the image into uploadable file.
     * @param photoUri Uri of the image.
     * @return Uploadable file ready for tag redaction.
     */
    public static UploadableFile pickedExistingPicture(@NonNull Context context, Uri photoUri) throws IOException, SecurityException {// SecurityException for those file providers who share URI but forget to grant necessary permissions
        File directory = tempImageDirectory(context);
        File photoFile = new File(directory, UUID.randomUUID().toString() + "." + getMimeType(context, photoUri));
        if (photoFile.createNewFile()) {
            try (InputStream pictureInputStream = context.getContentResolver().openInputStream(photoUri)) {
                writeToFile(pictureInputStream, photoFile);
            }
        } else {
            throw new IOException("could not create photoFile to write upon");
        }
        return new UploadableFile(photoUri, photoFile);
    }

    /**
     * getCameraPictureLocation
     */
    static File getCameraPicturesLocation(@NonNull Context context) throws IOException {
        File dir = tempImageDirectory(context);
        return File.createTempFile(UUID.randomUUID().toString(), ".jpg", dir);
    }

    /**
     * To find out the extension of required object in given uri
     * Solution by http://stackoverflow.com/a/36514823/1171484
     */
    private static String getMimeType(@NonNull Context context, @NonNull Uri uri) {
        String extension;

        //Check uri format to avoid null
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            //If scheme is a content
            extension = MimeTypeMapWrapper.getExtensionFromMimeType(context.getContentResolver().getType(uri));
        } else {
            //If scheme is a File
            //This will replace white spaces with %20 and also other special characters. This will avoid returning null values on file name with spaces and special characters.
            extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());

        }

        return extension;
    }

    /**
     * GetUriToFile
     * @param file get uri of file
     * @return uri of requested file.
     */
    static Uri getUriToFile(@NonNull Context context, @NonNull File file) {
        String packageName = context.getApplicationContext().getPackageName();
        String authority = packageName + ".provider";
        return FileProvider.getUriForFile(context, authority, file);
    }

}