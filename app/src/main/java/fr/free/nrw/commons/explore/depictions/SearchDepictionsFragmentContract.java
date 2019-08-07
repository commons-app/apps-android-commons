package fr.free.nrw.commons.explore.depictions;

import java.util.List;

import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem;

public interface SearchDepictionsFragmentContract {

    interface View {
        void initErrorView();

        void handleNoInternet();

        void onSuccess(List<DepictedItem> mediaList);

        void loadingDepictions();

        void clearAdapter();

        void showSnackbar();
    }

    interface UserActionListener extends BasePresenter<View> {

        void addDepictionsToList();

        void updateDepictionList(String query);

        void saveQuery();

        void initializeQuery(String query);

        String getQuery();
    }
}
