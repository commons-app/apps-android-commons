package fr.free.nrw.commons.achievements;

import android.util.Log;

/**
 * represnts Achievements class ans stores all the parameters
 */
public class Achievements {
    private int uniqueUsedImages;
    private int articlesUsingImages;
    private int thanksReceived;
    private int imagesEditedBySomeoneElse;
    private int featuredImages;
    private int imagesUploaded;
    private int revertCount;

    public Achievements(){

    }

    /**
     * constructor for achievements class to set its data members
     * @param uniqueUsedImages
     * @param articlesUsingImages
     * @param thanksReceived
     * @param imagesEditedBySomeoneElse
     * @param featuredImages
     * @param imagesUploaded
     * @param revertCount
     */
    public Achievements(int uniqueUsedImages,
                        int articlesUsingImages,
                        int thanksReceived,
                        int imagesEditedBySomeoneElse,
                        int featuredImages,
                        int imagesUploaded,
                        int revertCount) {
        this.uniqueUsedImages = uniqueUsedImages;
        this.articlesUsingImages = articlesUsingImages;
        this.thanksReceived = thanksReceived;
        this.imagesEditedBySomeoneElse = imagesEditedBySomeoneElse;
        this.featuredImages = featuredImages;
        this.imagesUploaded = imagesUploaded;
        this.revertCount = revertCount;
    }

    /**
     * Builder class for Achievements class
     */
    public class AchievementsBuilder {
        private int nestedUniqueUsedImages;
        private int nestedArticlesUsingImages;
        private int nestedThanksReceived;
        private int nestedImagesEditedBySomeoneElse;
        private int nestedFeaturedImages;
        private int nestedImagesUploaded;
        private int nestedRevertCount;

        public AchievementsBuilder setUniqueUsedImages(int uniqueUsedImages) {
            this.nestedUniqueUsedImages = uniqueUsedImages;
            return this;
        }

        public AchievementsBuilder setArticlesUsingImages(int articlesUsingImages) {
            this.nestedArticlesUsingImages = articlesUsingImages;
            return this;
        }

        public AchievementsBuilder setThanksReceived(int thanksReceived) {
            this.nestedThanksReceived = thanksReceived;
            return this;
        }

        public AchievementsBuilder setImagesEditedBySomeoneElse(int imagesEditedBySomeoneElse) {
            this.nestedImagesEditedBySomeoneElse = imagesEditedBySomeoneElse;
            return this;
        }

        public AchievementsBuilder setFeaturedImages(int featuredImages) {
            this.nestedFeaturedImages = featuredImages;
            return this;
        }

        public AchievementsBuilder setImagesUploaded(int imagesUploaded) {
            this.nestedImagesUploaded = imagesUploaded;
            return this;
        }

        public AchievementsBuilder setRevertCount( int revertCount){
            this.nestedRevertCount = revertCount;
            return this;
        }

        public Achievements createAchievements(){
            return new Achievements(nestedUniqueUsedImages,
                    nestedArticlesUsingImages,
                    nestedThanksReceived,
                    nestedImagesEditedBySomeoneElse,
                    nestedFeaturedImages,
                    nestedImagesUploaded,
                    nestedRevertCount);
        }

    }

    /**
     * getter function to get count of images uploaded
     * @return
     */
    public int getImagesUploaded() {
        return imagesUploaded;
    }

    /**
     * getter function to get count of featured images
     * @return
     */
    public int getFeaturedImages() {
        return featuredImages;
    }

    /**
     * getter function to get count of thanks received
     * @return
     */
    public int getThanksReceived() {
        return thanksReceived;
    }

    /**
     * getter function to get count of unique images used by wiki
     * @return
     */
    public int getUniqueUsedImages() {
        return uniqueUsedImages;
    }

    /**
     * setter function to count of images uploaded
     * @param imagesUploaded
     */
    public void setImagesUploaded(int imagesUploaded) {
        this.imagesUploaded = imagesUploaded;
    }

    /**
     * setter function to set count of featured images
     * @param featuredImages
     */
    public void setFeaturedImages(int featuredImages) {
        this.featuredImages = featuredImages;
    }

    /**
     * setter function to set the count of images edited by someone
     * @param imagesEditedBySomeoneElse
     */
    public void setImagesEditedBySomeoneElse(int imagesEditedBySomeoneElse) {
        this.imagesEditedBySomeoneElse = imagesEditedBySomeoneElse;
    }

    /**
     * setter function to set count of thanks received
     * @param thanksReceived
     */
    public void setThanksReceived(int thanksReceived) {
        this.thanksReceived = thanksReceived;
    }

    /**
     * setter function to count of articles using images uploaded
     * @param articlesUsingImages
     */
    public void setArticlesUsingImages(int articlesUsingImages) {
        this.articlesUsingImages = articlesUsingImages;
    }

    /**
     * setter function to set count of uniques images used by wiki
     * @param uniqueUsedImages
     */
    public void setUniqueUsedImages(int uniqueUsedImages) {
        this.uniqueUsedImages = uniqueUsedImages;
    }

    /**
     * to set count of images reverted
     * @param revertCount
     */
    public void setRevertCount(int revertCount) {
        this.revertCount = revertCount;
    }

    /**
     * used to calculate the percentages of images that haven't been reverted
     * @return
     */
    public int getNotRevertPercentage(){
        try {
            return ((imagesUploaded - revertCount) * 100)/imagesUploaded;
        } catch (ArithmeticException divideByZero ){
           return 100;
        }
    }
}
