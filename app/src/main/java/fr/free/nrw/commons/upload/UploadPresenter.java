package fr.free.nrw.commons.upload;

import static fr.free.nrw.commons.upload.UploadModel.UploadItem;

import android.annotation.SuppressLint;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.kvstore.BasicKvStore;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * The MVP pattern presenter of Upload GUI
 */
@Singleton
public class UploadPresenter implements IUpload.UserActionListener {

    private static final IUpload.View DUMMY = (IUpload.View) Proxy.newProxyInstance(IUpload.View.class.getClassLoader(),
            new Class[]{IUpload.View.class}, (proxy, method, methodArgs) -> null);
    private IUpload.View view = DUMMY;

    private static final SimilarImageInterface SIMILAR_IMAGE = (SimilarImageInterface) Proxy.newProxyInstance(SimilarImageInterface.class.getClassLoader(),
            new Class[]{SimilarImageInterface.class}, (proxy, method, methodArgs) -> null);

    private final UploadModel uploadModel;
    private final UploadController uploadController;

    @Inject
    UploadPresenter(UploadModel uploadModel,
            UploadController uploadController) {
        this.uploadModel = uploadModel;
        this.uploadController = uploadController;
    }


    /**
     * Called by the submit button in {@link UploadActivity}
     */
    @SuppressLint("CheckResult")
    @Override
    public void handleSubmit() {
        if (view.isLoggedIn()) {
            view.showProgress(true);
            uploadModel.buildContributions()
                    .observeOn(Schedulers.io())
                    .subscribe(new Consumer<Contribution>() {
                        @Override
                        public void accept(Contribution contribution) throws Exception {
                            uploadController.startUpload(contribution);
                            view.showProgress(false);
                            view.showMessage(R.string.uploading_started);
                            view.finish();
                        }
                    });
        }else{
            view.askUserToLogIn();
        }
    }

    /**
     * Called by the map button on the right card in {@link UploadActivity}
     */
    void openCoordinateMap() {
        GPSExtractor gpsObj = uploadModel.getCurrentItem().getGpsCoords();
        if (gpsObj != null && gpsObj.imageCoordsExists) {
            view.launchMapActivity(gpsObj.getDecLatitude() + "," + gpsObj.getDecLongitude());
        }
    }

    public List<String> getImageTitleList() {
        List<String> titleList = new ArrayList<>();
        for (UploadItem item : uploadModel.getUploads()) {
            if (item.getTitle().isSet()) {
                titleList.add(item.getTitle().toString());
            }
        }
        return titleList;
    }

    @Override public void onAttachView(IUpload.View view) {
        this.view=view;
        uploadController.prepareService();
    }

    @Override public void onDetachView() {
        this.view=DUMMY;
        uploadController.cleanup();
    }

}