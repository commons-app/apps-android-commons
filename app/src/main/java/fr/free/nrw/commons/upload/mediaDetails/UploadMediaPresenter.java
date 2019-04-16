package fr.free.nrw.commons.upload.mediaDetails;

import static fr.free.nrw.commons.utils.ImageUtils.EMPTY_TITLE;
import static fr.free.nrw.commons.utils.ImageUtils.FILE_NAME_EXISTS;
import static fr.free.nrw.commons.utils.ImageUtils.IMAGE_KEEP;
import static fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.kvstore.BasicKvStore;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.repository.LocalDataSource;
import fr.free.nrw.commons.upload.UploadModel;
import fr.free.nrw.commons.upload.UploadModel.UploadItem;
import fr.free.nrw.commons.upload.mediaDetails.IUploadMediaDetails.UserActionListener;
import fr.free.nrw.commons.upload.mediaDetails.IUploadMediaDetails.View;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class UploadMediaPresenter implements UserActionListener {

    private final UploadModel uploadModel;
    private IUploadMediaDetails.View view;
    private LocalDataSource localDataSource;

    private BasicKvStore defaultKvStore;
    private JsonKvStore directKvStore;

    @Inject
    public UploadMediaPresenter(UploadModel uploadModel,
            @Named("default_preferences") BasicKvStore defaultKvStore,
            @Named("direct_nearby_upload_prefs") JsonKvStore directKvStore) {
        this.defaultKvStore = defaultKvStore;
        this.directKvStore = directKvStore;
        this.uploadModel = uploadModel;
    }

    @Override
    public void onAttachView(View view) {
        this.view = view;
    }

    @Override
    public void onDetachView() {
        this.view = null;
    }

    @Override
    public void receiveImage(UploadableFile uploadableFile, String source, Place place) {
        view.showProgress(true);
        Observable<UploadItem> uploadItemObservable = uploadModel
                .preProcessImage(uploadableFile, place, source, view);

        uploadItemObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(uploadItem ->
                        {
                            view.onImageProcessed(uploadItem, place);
                            view.showProgress(false);
                        },
                        throwable -> Timber.e(throwable, "Error occurred in processing images"));
    }

    @Override
    public void verifyImageQuality(UploadItem uploadItem, boolean validateTitle) {
        view.showProgress(true);
        uploadModel.getImageQuality(uploadItem, true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(imageResult -> {
                            view.showProgress(false);
                            handleImageResult(imageResult);
                        },
                        throwable -> {
                            view.showProgress(false);
                            view.showMessage("" + throwable.getLocalizedMessage(),
                                    R.color.color_error);
                            Timber.e(throwable, "Error occurred while handling image");
                        });
    }

    @Override
    public void setUploadItem(int index,UploadItem uploadItem) {
        uploadModel.updateUploadItem(index,uploadItem);
    }

    private void handleImageResult(Integer imageResult) {
        if (imageResult == IMAGE_KEEP || imageResult == IMAGE_OK) {
            view.onImageValidationSuccess();
        } else {
            handleBadImage(imageResult);
        }
    }

    private void handleBadImage(Integer errorCode) {
        Timber.d("Handle bad picture with error code %d", errorCode);
        if (errorCode
                >= 8) { // If location of image and nearby does not match, then set shared preferences to disable wikidata edits
            directKvStore.putBoolean("Picture_Has_Correct_Location", false);
        }

        switch (errorCode) {
            case EMPTY_TITLE:
                Timber.d("Title is empty. Showing toast");
                view.showMessage(R.string.add_title_toast, R.color.color_error);
                break;
            case FILE_NAME_EXISTS:
                Timber.d("Trying to show duplicate picture popup");
                view.showDuplicatePicturePopup();
                break;
            default:
                view.showBadImagePopup(errorCode);
        }
    }
}
