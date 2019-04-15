package fr.free.nrw.commons.upload.license;

import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.kvstore.BasicKvStore;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.upload.UploadModel;
import fr.free.nrw.commons.upload.license.IMediaLicense.View;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class MediaLicensePresenter implements IMediaLicense.UserActionListener {

    private IMediaLicense.View view;
    private UploadModel uploadModel;
    private final BasicKvStore defaultKvStore;

    @Inject
    public MediaLicensePresenter(UploadModel uploadModel,
            @Named("default_preferences") BasicKvStore defaultKvStore) {
        this.uploadModel = uploadModel;
        this.defaultKvStore = defaultKvStore;
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
    public void getLicenses() {
        List<String> licenses = uploadModel.getLicenses();
        view.setLicenses(licenses);
        String selectedLicense = defaultKvStore.getString(Prefs.DEFAULT_LICENSE,
                Prefs.Licenses.CC_BY_SA_4);//CC_BY_SA_4 is the default one used by the commons web app
        try {//I have to make sure that the stored default license was not one of the deprecated one's
            Utils.licenseNameFor(selectedLicense);
        } catch (IllegalStateException exception) {
            Timber.e(exception.getMessage());
            selectedLicense = Prefs.Licenses.CC_BY_SA_4;
            defaultKvStore.putString(Prefs.DEFAULT_LICENSE, Prefs.Licenses.CC_BY_SA_4);
        }
        view.setSelectedLicense(selectedLicense);

    }

    @Override
    public void selectLicense(String licenseName) {
        uploadModel.setSelectedLicense(licenseName);
        view.updateLicenseSummary(uploadModel.getSelectedLicense(), uploadModel.getCount());
    }
}
