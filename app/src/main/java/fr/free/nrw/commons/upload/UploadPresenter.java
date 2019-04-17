package fr.free.nrw.commons.upload;

import static fr.free.nrw.commons.upload.UploadModel.UploadItem;

import android.annotation.SuppressLint;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.repository.UploadRepository;
import io.reactivex.schedulers.Schedulers;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * The MVP pattern presenter of Upload GUI
 */
@Singleton
public class UploadPresenter implements UploadContract.UserActionListener {

    private static final UploadContract.View DUMMY = (UploadContract.View) Proxy.newProxyInstance(
            UploadContract.View.class.getClassLoader(),
            new Class[]{UploadContract.View.class}, (proxy, method, methodArgs) -> null);
    private final UploadRepository repository;
    private UploadContract.View view = DUMMY;

    private static final SimilarImageInterface SIMILAR_IMAGE = (SimilarImageInterface) Proxy.newProxyInstance(SimilarImageInterface.class.getClassLoader(),
            new Class[]{SimilarImageInterface.class}, (proxy, method, methodArgs) -> null);


    @Inject
    UploadPresenter(UploadRepository uploadRepository) {
        this.repository = uploadRepository;
    }


    /**
     * Called by the submit button in {@link UploadActivity}
     */
    @SuppressLint("CheckResult")
    @Override
    public void handleSubmit() {
        if (view.isLoggedIn()) {
            view.showProgress(true);
            repository.buildContributions()
                    .observeOn(Schedulers.io())
                    .subscribe(contribution -> {
                        repository.startUpload(contribution);
                        view.showProgress(false);
                        view.showMessage(R.string.uploading_started);
                        view.finish();
                    });
        }else{
            view.askUserToLogIn();
        }
    }

    /**
     * Called by the map button on the right card in {@link UploadActivity}
     */
    void openCoordinateMap() {
        GPSExtractor gpsObj = repository.getCurrentItem().getGpsCoords();
        if (gpsObj != null && gpsObj.imageCoordsExists) {
            view.launchMapActivity(gpsObj.getDecLatitude() + "," + gpsObj.getDecLongitude());
        }
    }

    public List<String> getImageTitleList() {
        List<String> titleList = new ArrayList<>();
        for (UploadItem item : repository.getUploads()) {
            if (item.getTitle().isSet()) {
                titleList.add(item.getTitle().toString());
            }
        }
        return titleList;
    }

    @Override public void onAttachView(UploadContract.View view) {
        this.view=view;
        repository.prepareService();
    }

    @Override public void onDetachView() {
        this.view=DUMMY;
        repository.cleanup();
    }

}