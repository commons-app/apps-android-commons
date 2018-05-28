package fr.free.nrw.commons.achievements;

/**
 * represnts Achievements
 */
public class Achievements {
    private int uniqueUsedImages;
    private int articlesUsingImages;
    private int thanksReceived;
    private int imagesEditedBySomeoneElse;
    private int featuredImages;
    private int imagesUploaded;

    /**
     * Sets the unique images used
     *
     * @param uniqueUsedImages
     */
    public void setUniqueUsedImages(int uniqueUsedImages) {
        this.uniqueUsedImages = uniqueUsedImages;
    }

    /**
     * returns the unique images used
     *
     * @return
     */
    public int getUniqueUsedImages() {
        return uniqueUsedImages;
    }

    /**
     * sets the number of articles using images
     *
     * @param articlesUsingImages
     */
    public void setArticlesUsingImages(int articlesUsingImages) {
        this.articlesUsingImages = articlesUsingImages;
    }

    /**
     * returns the number of articles using images
     *
     * @return
     */
    public int getArticlesUsingImages() {
        return articlesUsingImages;
    }

    /**
     * sets the no of thanks received by user
     *
     * @param thanksReceived
     */
    public void setThanksReceived(int thanksReceived) {
        this.thanksReceived = thanksReceived;
    }

    /**
     * return the no of thanks received by user
     *
     * @return
     */
    public int getThanksReceived() {
        return thanksReceived;
    }

    /**
     * sets the no of images edited by someone else
     *
     * @param imagesEditedBySomeoneElse
     */
    public void setImagesEditedBySomeoneElse(int imagesEditedBySomeoneElse) {
        this.imagesEditedBySomeoneElse = imagesEditedBySomeoneElse;
    }

    /**
     * returns the no of images edited by someone else
     *
     * @return
     */
    public int getImagesEditedBySomeoneElse() {
        return imagesEditedBySomeoneElse;
    }

    /**
     *  sets the total of images featured
     * @param featuredImages
     */
    public void setFeaturedImages(int featuredImages) {
        this.featuredImages = featuredImages;
    }

    /**
     * returns the total count of images featured
     * @return
     */
    public int getFeaturedImages() {
        return featuredImages;
    }

    /**
     * to set the total number of images uploaded
     * @param imagesUploaded
     */
    public void setImagesUploaded(int imagesUploaded) {
        this.imagesUploaded = imagesUploaded;
    }

    /**
     * to get the total number of uploads
     * @return
     */
    public int getImagesUploaded() {
        return imagesUploaded;
    }
}
