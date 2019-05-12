package fr.free.nrw.commons.nearby.mvp;

public interface NearbyContract {

    interface View {
        void showPlaces();
    }

    interface UserActions {
        void uploadImageGallery();
        void uploadImageCamera();
        void bookmarkItem();
        void getDirections();
        void seeWikidataItem();
        void seeWikipediaArticle();
        void rotateScreen();
    }
}
