package fr.free.nrw.commons.upload;

import android.content.Context;
import fr.free.nrw.commons.location.LatLng;
import io.reactivex.Observable;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

@Singleton
public class FileUtilsWrapper {

    @Inject
    public FileUtilsWrapper() {

    }

    public String getFileExt(String fileName) {
        return FileUtils.getFileExt(fileName);
    }

    public String getSHA1(InputStream is) {
        return FileUtils.getSHA1(is);
    }

    public FileInputStream getFileInputStream(String filePath) throws FileNotFoundException {
        return FileUtils.getFileInputStream(filePath);
    }

    public String getGeolocationOfFile(String filePath, LatLng inAppPictureLocation) {
        return FileUtils.getGeolocationOfFile(filePath, inAppPictureLocation);
    }


    /**
     * Takes a file as input and returns an Observable of files with the specified chunk size
     */
    public List<File> getFileChunks(Context context, File file, final int chunkSize)
        throws IOException {
        final byte[] buffer = new byte[chunkSize];

        //try-with-resources to ensure closing stream
        try (final FileInputStream fis = new FileInputStream(file);
            final BufferedInputStream bis = new BufferedInputStream(fis)) {
            final List<File> buffers = new ArrayList<>();
            int size;
            while ((size = bis.read(buffer)) > 0) {
                buffers.add(writeToFile(context, Arrays.copyOf(buffer, size), file.getName(),
                    getFileExt(file.getName())));
            }
            return buffers;
        }
    }

    /**
     * Create a temp file containing the passed byte data.
     */
    private File writeToFile(Context context, final byte[] data, final String fileName,
        String fileExtension)
        throws IOException {
        final File file = File.createTempFile(fileName, fileExtension, context.getCacheDir());
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            final FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
        } catch (final Exception throwable) {
            Timber.e(throwable, "Failed to create file");
        }
        return file;
    }
}
