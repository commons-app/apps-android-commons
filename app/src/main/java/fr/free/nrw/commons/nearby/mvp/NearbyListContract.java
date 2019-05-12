package fr.free.nrw.commons.nearby.mvp;

/**
 * This interface defines specific View and UserActions for list
 * part of the nearby. On the other hand both extends methods
 * from parent View and UserActions where general methods are
 * defined (in Nearby Contract)
 */
public interface NearbyListContract {

    interface View extends NearbyContract.View {
        // Even if this is empty for now, I keep this one for code consistency
    }

    interface UserActions extends NearbyContract.UserActions {
        void expandItem();
    }
}
