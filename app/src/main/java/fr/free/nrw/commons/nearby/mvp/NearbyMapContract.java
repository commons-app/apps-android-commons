package fr.free.nrw.commons.nearby.mvp;

public interface NearbyMapContract {
    interface View extends NearbyContract.View {
        void showSearchThisAreaButton();
        void showInformationBottomSheet();
        void showFABs();
    }

    interface UserActions extends NearbyContract.UserActions {
        void searchThisArea();
        void recenterMap();
    }
}
