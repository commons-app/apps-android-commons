package fr.free.nrw.commons.depictions.Media;

import android.annotation.SuppressLint;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.explore.depictions.DepictsClient;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.media.MediaClient;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class DepictedImagesPresenter implements DepictedImagesContract.UserActionListener {

    private static final DepictedImagesContract.View DUMMY = (DepictedImagesContract.View) Proxy
            .newProxyInstance(
                    DepictedImagesContract.View.class.getClassLoader(),
                    new Class[]{DepictedImagesContract.View.class},
                    (proxy, method, methodArgs) -> null);
    private static int TIMEOUT_SECONDS = 15;
    DepictsClient depictsClient;
    MediaClient mediaClient;
    @Named("default_preferences")
    JsonKvStore depictionKvStore;
    private DepictedImagesContract.View view = DUMMY;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private String depictName = null;
    private String entityId = null;
    private List<Media> queryList = new ArrayList<>();

    @Inject
    public DepictedImagesPresenter(@Named("default_preferences") JsonKvStore depictionKvStore, DepictsClient depictsClient, MediaClient mediaClient) {
        this.depictionKvStore = depictionKvStore;
        this.depictsClient = depictsClient;
        this.mediaClient = mediaClient;
    }

    @Override
    public void onAttachView(DepictedImagesContract.View view) {
        this.view = view;
    }

    @Override
    public void onDetachView() {
        this.view = DUMMY;
    }

    /**
     * Checks for internet connection and then initializes the grid view with first 10 images of that depiction
     */
    @SuppressLint("CheckResult")
    @Override
    public void initList(String entityId) {
        view.setLoadingStatus(true);
        view.progressBarVisible(true);
        view.setIsLastPage(false);
        compositeDisposable.add(depictsClient.fetchImagesForDepictedItem(entityId, 25, 0)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .subscribe(this::handleSuccess, this::handleError));
    }

    /**
     * Fetches more images for the item and adds it to the grid view adapter
     */
    @SuppressLint("CheckResult")
    @Override
    public void fetchMoreImages() {
        view.progressBarVisible(true);
        compositeDisposable.add(depictsClient.fetchImagesForDepictedItem(entityId, 25, queryList.size())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .subscribe(this::handlePaginationSuccess, this::handleError));
    }

    private void handlePaginationSuccess(List<Media> media) {
        queryList.addAll(media);
        view.progressBarVisible(false);
        view.addItemsToAdapter(media);
    }

    /**
     * Logs and handles API error scenario
     *
     * @param throwable
     */
    public void handleError(Throwable throwable) {
        Timber.e(throwable, "Error occurred while loading images inside items");
        try {
            view.initErrorView();
            view.showSnackBar();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void handleSuccess(List<Media> collection) {
        if (collection == null || collection.isEmpty()) {
            if (queryList.isEmpty()) {
                view.initErrorView();
            } else {
                view.setIsLastPage(true);
            }
        } else {
            this.queryList.addAll(collection);
            view.handleSuccess(collection);
        }
    }

    @Override
    public void replaceTitlesWithCaptions(String displayTitle, int position) {
        compositeDisposable.add(mediaClient.getCaptionByFilename("File:" + displayTitle + ".jpg")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .subscribe(subscriber -> {
                    view.handleLabelforImage(subscriber, position);
                }));

    }

    @Override
    public void addItemsToQueryList(List<Media> collection) {
        queryList.addAll(collection);
    }

    /**
     * Query continue values determine the last page that was loaded for the particular keyword
     * This method resets those values, so that the results can be queried from the first page itself
     *
     * @param keyword
     */
    private void resetQueryContinueValues(String keyword) {
        depictionKvStore.remove("query_continue_" + keyword);
    }


}
