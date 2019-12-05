package fr.free.nrw.commons.achievements;

/**
 * Represents Achievements class and stores all the parameters
 */
public class Achievements {
    private int uniqueUsedImages;
    private int thanksReceived;
    private int featuredImages;
    private int imagesUploaded;
    private int revertCount;

    public Achievements(){

    }

    /**
     * constructor for achievements class to set its data members
     * @param uniqueUsedImages
     * @param thanksReceived
     * @param featuredImages
     * @param imagesUploaded
     * @param revertCount
     */
    public Achievements(int uniqueUsedImages,
                        int thanksReceived,
                        int featuredImages,
                        int imagesUploaded,
                        int revertCount) {
        this.uniqueUsedImages = uniqueUsedImages;
        this.thanksReceived = thanksReceived;
        this.featuredImages = featuredImages;
        this.imagesUploaded = imagesUploaded;
        this.revertCount = revertCount;
    }

    /**
     * Get Achievements object from FeedbackResponse
     *
     * @param response
     * @return
     */
    public static Achievements from(FeedbackResponse response) {
        return new Achievements(response.getUniqueUsedImages(),
                response.getThanksReceived(),
                response.getFeaturedImages().getQualityImages()
                        + response.getFeaturedImages().getFeaturedPicturesOnWikimediaCommons(),
                0,
                response.getDeletedUploads());
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
