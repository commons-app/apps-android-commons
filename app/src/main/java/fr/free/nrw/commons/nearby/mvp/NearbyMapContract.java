package fr.free.nrw.commons.nearby.mvp;

/**
 * This interface defines specific View and UserActions for map
 * part of the nearby. On the other hand both extends methods
 * from parent View and UserActions where general methods are
 * defined (in Nearby Contract)
 */
public interface NearbyMapContract {

    interface View extends NearbyContract.View{
        void showSearchThisAreaButton();
        void showInformationBottomSheet();
        void showFABs();
    }

    interface UserActions extends NearbyContract.UserActions {
        void searchThisArea();
        void recenterMap();
    }
}
