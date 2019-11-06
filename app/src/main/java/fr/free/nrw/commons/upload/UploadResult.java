package fr.free.nrw.commons.upload;

import org.wikipedia.gallery.ImageInfo;

public class UploadResult {
    private final String result;
    private final String filekey;
    private final String filename;
    private final String sessionkey;
    private final ImageInfo imageinfo;

    public UploadResult(String result, String filekey, String filename, String sessionkey, ImageInfo imageinfo) {
        this.result = result;
        this.filekey = filekey;
        this.filename = filename;
        this.sessionkey = sessionkey;
        this.imageinfo = imageinfo;
    }

    public String getResult() {
        return result;
    }

    public String getFilekey() {
        return filekey;
    }

    public String getSessionkey() {
        return sessionkey;
    }

    public ImageInfo getImageinfo() {
        return imageinfo;
    }

    public String getFilename() {
        return filename;
    }
}
