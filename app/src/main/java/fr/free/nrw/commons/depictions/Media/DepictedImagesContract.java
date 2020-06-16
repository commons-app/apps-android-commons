package fr.free.nrw.commons.depictions.Media;

import android.widget.ListAdapter;
import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.Media;
import java.util.List;

/**
 * Contract with which DepictedImagesFragment and its presenter will talk to each other
 */
public interface DepictedImagesContract {

  interface View {

    /**
     * Handles the UI updates for no internet scenario
     */
    void handleNoInternet();

    /**
     * Handles the UI updates for a error scenario
     */
    void initErrorView();

    /**
     * Initializes the adapter with a list of Media objects
     *
     * @param mediaList List of new Media to be displayed
     */
    void setAdapter(List<Media> mediaList);

    /**
     * Seat caption to the image at the given position
     */
    void handleLabelforImage(String caption, int position);

    /**
     * Display snackbar
     */
    void showSnackBar();

    /**
     * Inform the view that there are no more items to be loaded for this search query or reset the
     * isLastPage for the current query
     *
     * @param isLastPage
     */
    void setIsLastPage(boolean isLastPage);

    /**
     * Set visibility of progressbar depending on the boolean value
     */
    void progressBarVisible(Boolean value);

    /**
     * It return an instance of gridView adapter which helps in extracting media details used by the
     * gridView
     *
     * @return GridView Adapter
     */
    ListAdapter getAdapter();

    /**
     * adds list to adapter
     */
    void addItemsToAdapter(List<Media> media);

    /**
     * Sets loading status depending on the boolean value
     */
    void setLoadingStatus(Boolean value);

    /**
     * Handles the success scenario On first load, it initializes the grid view. On subsequent
     * loads, it adds items to the adapter
     *
     * @param collection List of new Media to be displayed
     */
    void handleSuccess(List<Media> collection);

  }

  interface UserActionListener extends BasePresenter<View> {

    /**
     * Checks for internet connection and then initializes the grid view with first 10 images of
     * that depiction
     */
    void initList(String entityId);

    /**
     * Fetches more images for the item and adds it to the grid view adapter
     *
     * @param entityId
     */
    void fetchMoreImages(String entityId);

    /**
     * fetch captions for the image using filename and replace title of on the image thumbnail(if
     * captions are available) else show filename
     */
    void replaceTitlesWithCaptions(String title, int position);

    /**
     * add items to query list
     */
    void addItemsToQueryList(List<Media> collection);
  }
}
