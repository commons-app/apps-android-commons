package fr.free.nrw.commons.profile.achievements;

import fr.free.nrw.commons.profile.achievements.FeaturedImages;

public class FeedbackResponse {

    private final String status;
    private final int uniqueUsedImages;
    private final int articlesUsingImages;
    private final int deletedUploads;
    private final FeaturedImages featuredImages;
    private final int thanksReceived;
    private final String user;
    private final int imagesEditedBySomeoneElse;


    public FeedbackResponse(String status,
                            int uniqueUsedImages,
                            int articlesUsingImages,
                            int deletedUploads,
                            FeaturedImages featuredImages,
                            int thanksReceived,
                            String user,
                            int imagesEditedBySomeoneElse) {
        this.status = status;
        this.uniqueUsedImages = uniqueUsedImages;
        this.articlesUsingImages = articlesUsingImages;
        this.deletedUploads = deletedUploads;
        this.featuredImages = featuredImages;
        this.thanksReceived = thanksReceived;
        this.user = user;
        this.imagesEditedBySomeoneElse = imagesEditedBySomeoneElse;
    }

    public String getStatus() {
        return status;
    }

    public int getUniqueUsedImages() {
        return uniqueUsedImages;
    }

    public int getArticlesUsingImages() {
        return articlesUsingImages;
    }

    public int getDeletedUploads() {
        return deletedUploads;
    }

    public FeaturedImages getFeaturedImages() {
        return featuredImages;
    }

    public int getThanksReceived() {
        return thanksReceived;
    }

    public String getUser() {
        return user;
    }

    public int getImagesEditedBySomeoneElse() {
        return imagesEditedBySomeoneElse;
    }
}
