package fr.free.nrw.commons.nearby.mvp.contract;

public interface NearbyParentFragmentContract {

    interface View {
        void setListFragmentExpanded();
        void refreshView();
    }

    interface UserActions {
        void displayListFragmentExpanded();
        void locationChangedSlightly();
        void locationChangedMedium();
        void locationChangedSignificantly();
    }
}
