package fr.free.nrw.commons.featured;

import android.graphics.Bitmap;

/**
 * Created by root on 09.01.2018.
 */

public class FeaturedImage {
    private Bitmap image;
    private String author;
    private String fileName;

    public FeaturedImage(Bitmap image, String author, String fileName) {
        this.image = image;
        this.author = author;
        this.fileName = fileName;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
