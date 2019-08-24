package fr.free.nrw.commons.explore.depictions;

import com.pedrogomez.renderers.RVRendererAdapter;

import java.util.List;

import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;

/**
 * The contract with with SearchDepictionsFragment and its presenter would talk to each other
 */
public interface SearchDepictionsFragmentContract {

    interface View {
        /**
         * Handles the UI updates for a error scenario
         */
        void initErrorView();

        /**
         * Handles the UI updates for no internet scenario
         */
        void handleNoInternet();

        /**
         * If a non empty list is successfully returned from the api then modify the view
         * like hiding empty labels, hiding progressbar and notifying the apdapter that list of items has been fetched from the API
         */
        void onSuccess(List<DepictedItem> mediaList);

        /**
         * load depictions
         */
        void loadingDepictions();

        /**
         * clear adapter
         */
        void clearAdapter();

        /**
         * show snackbar
         */
        void showSnackbar();

        /**
         * @return adapter
         */
        RVRendererAdapter<DepictedItem> getAdapter();

        void onImageUrlFetched(String response, int position);

        /**
         * Inform the view that there are no more items to be loaded for this search query
         * or reset the isLastPage for the current query
         * @param isLastPage
         */
        void setIsLastPage(boolean isLastPage);
    }

    interface UserActionListener extends BasePresenter<View> {

        /**
         * Called when user selects "Items" from Search Activity
         * to load the list of depictions from API
         *
         * @param query string searched in the Explore Activity
         * @param reInitialise
         */
        void updateDepictionList(String query, int pageSize, boolean reInitialise);

        /**
         * This method saves Search Query in the Recent Searches Database.
         */
        void saveQuery();

        /**
         * Whenever a new query is initiated from the search activity clear the previous adapter
         * and add new value of the query
         */
        void initializeQuery(String query);

        /**
         * @return query
         */
        String getQuery();

        /**
         * After all the depicted items are loaded fetch thumbnail image for all the depicted items (if available)
         */
        void fetchThumbnailForEntityId(String entityId,int position);
    }
}
