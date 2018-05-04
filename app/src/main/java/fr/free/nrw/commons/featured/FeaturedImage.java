package fr.free.nrw.commons.featured;


import fr.free.nrw.commons.Media;

/**
 * Object to hold FeaturedImage
 */

public class FeaturedImage {
    private Media image;
    private String author;
    private String fileName;

    public FeaturedImage(Media image, String author, String fileName) {
        this.image = image;
        this.author = author;
        this.fileName = fileName;
    }

    public Media getImage() {
        return image;
    }

    public void setImage(Media image) {
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
