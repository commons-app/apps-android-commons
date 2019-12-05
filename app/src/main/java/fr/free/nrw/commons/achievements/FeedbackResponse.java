package fr.free.nrw.commons.achievements;

public class FeedbackResponse {

    private final int uniqueUsedImages;
    private final int articlesUsingImages;
    private final int deletedUploads;
    private final FeaturedImages featuredImages;
    private final int thanksReceived;
    private final String user;


    public FeedbackResponse(int uniqueUsedImages,
                            int articlesUsingImages,
                            int deletedUploads,
                            FeaturedImages featuredImages,
                            int thanksReceived,
                            String user) {
        this.uniqueUsedImages = uniqueUsedImages;
        this.articlesUsingImages = articlesUsingImages;
        this.deletedUploads = deletedUploads;
        this.featuredImages = featuredImages;
        this.thanksReceived = thanksReceived;
        this.user = user;
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

}
