package fr.free.nrw.commons.depictions.Media;

import android.widget.ListAdapter;

import java.util.List;

import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.Media;

/**
 * Contract with which DepictedImagesFragment and it's presenter will talk to each other
 */
public interface DepictedImagesContract {

    interface View {

        void handleNoInternet();

        void initErrorView();

        void setAdapter(List<Media> mediaList);

        void handleLabelforImage(String s, int position);

        void showSnackBar();

        void setIsLastPage(boolean isLastPage);

        void progressBarVisible(Boolean value);

        ListAdapter getAdapter();

        void addItemsToAdapter(List<Media> media);

        void setLoadingStatus(Boolean value);

        void handleSuccess(List<Media> collection);

    }

    interface UserActionListener extends BasePresenter<View> {

        void initList(String entityId);

        void fetchMoreImages();

        void replaceTitlesWithCaptions(String title, int position);

        void addItemsToQueryList(List<Media> collection);
    }
}
