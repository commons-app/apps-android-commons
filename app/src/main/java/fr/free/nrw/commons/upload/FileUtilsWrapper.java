package fr.free.nrw.commons.upload;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FileUtilsWrapper {

    @Inject
    public FileUtilsWrapper() {

    }

    public String getSHA1(InputStream is) {
        return FileUtils.getSHA1(is);
    }

    public FileInputStream getFileInputStream(String filePath) throws FileNotFoundException {
        return FileUtils.getFileInputStream(filePath);
    }

    public String getGeolocationOfFile(String filePath) {
        return FileUtils.getGeolocationOfFile(filePath);
    }
}
