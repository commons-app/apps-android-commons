package fr.free.nrw.commons.achievements;

import android.util.Log;

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
    private int revertCount;

    public Achievements(){

    }

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

    public int getImagesUploaded() {
        return imagesUploaded;
    }

    public int getFeaturedImages() {
        return featuredImages;
    }

    public int getImagesEditedBySomeoneElse() {
        return imagesEditedBySomeoneElse;
    }

    public int getThanksReceived() {
        return thanksReceived;
    }

    public int getArticlesUsingImages() {
        return articlesUsingImages;
    }

    public int getUniqueUsedImages() {
        return uniqueUsedImages;
    }

    public void setImagesUploaded(int imagesUploaded) {
        this.imagesUploaded = imagesUploaded;
    }

    public void setFeaturedImages(int featuredImages) {
        this.featuredImages = featuredImages;
    }

    public void setImagesEditedBySomeoneElse(int imagesEditedBySomeoneElse) {
        this.imagesEditedBySomeoneElse = imagesEditedBySomeoneElse;
    }

    public void setThanksReceived(int thanksReceived) {
        this.thanksReceived = thanksReceived;
    }

    public void setArticlesUsingImages(int articlesUsingImages) {
        this.articlesUsingImages = articlesUsingImages;
    }

    public void setUniqueUsedImages(int uniqueUsedImages) {
        this.uniqueUsedImages = uniqueUsedImages;
    }

    public int getRevertCount() {
        return revertCount;
    }

    public void setRevertCount(int revertCount) {
        this.revertCount = revertCount;
    }

    public int getNotRevertPercentage(){
        try {
            return ((imagesUploaded - revertCount) * 100)/imagesUploaded;
        } catch (ArithmeticException divideByZero ){
           return 100;
        }
    }

}
