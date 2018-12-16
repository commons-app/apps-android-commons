package fr.free.nrw.commons.upload;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FileUtilsWrapper {

    @Inject
    public FileUtilsWrapper() {

    }

    public String createExternalCopyPathAndCopy(Uri uri, ContentResolver contentResolver) throws IOException {
        return FileUtils.createExternalCopyPathAndCopy(uri, contentResolver);
    }

    public String createCopyPathAndCopy(Uri uri, Context context) throws IOException {
        return FileUtils.createCopyPathAndCopy(uri, context);
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
}
