package fr.free.nrw.commons.upload.license;

import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.repository.UploadRepository;
import fr.free.nrw.commons.settings.Licenses.CC_BY_SA_4;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.upload.license.MediaLicenseContract.View;
import java.lang.reflect.Proxy;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

/**
 * Added JavaDocs for MediaLicensePresenter
 */
public class MediaLicensePresenter implements MediaLicenseContract.UserActionListener {

    private static final MediaLicenseContract.View DUMMY = (MediaLicenseContract.View) Proxy
            .newProxyInstance(
                    MediaLicenseContract.View.class.getClassLoader(),
                    new Class[]{MediaLicenseContract.View.class},
                    (proxy, method, methodArgs) -> null);

    private final UploadRepository repository;
    private final JsonKvStore defaultKVStore;
    private MediaLicenseContract.View view = DUMMY;

    @Inject
    public MediaLicensePresenter(UploadRepository uploadRepository,
                @Named("default_preferences") JsonKvStore defaultKVStore) {
        this.repository = uploadRepository;
        this.defaultKVStore = defaultKVStore;
    }

    @Override
    public void onAttachView(View view) {
        this.view = view;
    }

    @Override
    public void onDetachView() {
        this.view = DUMMY;
    }

    /**
     * asks the repository for the available licenses, and informs the view on the same
     */
    @Override
    public void getLicenses() {
        List<String> licenses = repository.getLicenses();
        view.setLicenses(licenses);

        //CC_BY_SA_4 is the default one used by the commons web app
        final String defaultLicenseId = CC_BY_SA_4.INSTANCE.getId();
        String selectedLicense = defaultKVStore.getString(Prefs.DEFAULT_LICENSE, defaultLicenseId);
        try {//I have to make sure that the stored default license was not one of the deprecated one's
            Utils.licenseNameFor(selectedLicense);
        } catch (IllegalStateException exception) {
            Timber.e(exception.getMessage());
            selectedLicense = defaultLicenseId;
            defaultKVStore.putString(Prefs.DEFAULT_LICENSE, defaultLicenseId);
        }
        view.setSelectedLicense(selectedLicense);
    }

    /**
     * ask the repository to select a license for the current upload
     *
     * @param licenseName
     */
    @Override
    public void selectLicense(String licenseName) {
        repository.setSelectedLicense(licenseName);
        view.updateLicenseSummary(repository.getSelectedLicense(), repository.getCount());
    }

    @Override
    public boolean isWLMSupportedForThisPlace() {
        return repository.isWMLSupportedForThisPlace();
    }
}
