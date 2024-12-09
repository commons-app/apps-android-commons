package fr.free.nrw.commons.upload.license;

import androidx.annotation.NonNull;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.repository.UploadRepository;
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
    public MediaLicensePresenter(final UploadRepository uploadRepository,
                @Named("default_preferences") final JsonKvStore defaultKVStore) {
        this.repository = uploadRepository;
        this.defaultKVStore = defaultKVStore;
    }

    @Override
    public void onAttachView(@NonNull final View view) {
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
        final List<String> licenses = repository.getLicenses();
        view.setLicenses(licenses);

        String selectedLicense = defaultKVStore.getString(Prefs.DEFAULT_LICENSE,
                Prefs.Licenses.CC_BY_SA_4);//CC_BY_SA_4 is the default one used by the commons web app
        try {//I have to make sure that the stored default license was not one of the deprecated one's
            Utils.licenseNameFor(selectedLicense);
        } catch (final IllegalStateException exception) {
            Timber.e(exception);
            selectedLicense = Prefs.Licenses.CC_BY_SA_4;
            defaultKVStore.putString(Prefs.DEFAULT_LICENSE, Prefs.Licenses.CC_BY_SA_4);
        }
        view.setSelectedLicense(selectedLicense);

    }

    /**
     * ask the repository to select a license for the current upload
     *
     * @param licenseName
     */
    @Override
    public void selectLicense(final String licenseName) {
        repository.setSelectedLicense(licenseName);
        view.updateLicenseSummary(repository.getSelectedLicense(), repository.getCount());
    }

    @Override
    public boolean isWLMSupportedForThisPlace() {
        return repository.isWMLSupportedForThisPlace();
    }
}
