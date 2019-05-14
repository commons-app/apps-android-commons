package fr.free.nrw.commons.nearby.mvp.contract;

/**
 * This interface defines specific View and UserActions for map
 * part of the nearby. On the other hand both extends methods
 * from parent View and UserActions where general methods are
 * defined (in Nearby Contract)
 */
public interface NearbyMapContract {

    interface View extends NearbyElementContract.View{
        void showSearchThisAreaButton();
        void showInformationBottomSheet();
        void showFABs();
    }

    interface UserActions extends NearbyElementContract.UserActions {
        void searchThisArea();
        void recenterMap();
    }
}
