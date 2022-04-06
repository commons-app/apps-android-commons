package fr.free.nrw.commons.upload;

import dagger.Binds;
import dagger.Module;
import fr.free.nrw.commons.upload.categories.CategoriesContract;
import fr.free.nrw.commons.upload.categories.CategoriesPresenter;
import fr.free.nrw.commons.upload.depicts.DepictsContract;
import fr.free.nrw.commons.upload.depicts.DepictsPresenter;
import fr.free.nrw.commons.upload.license.MediaLicenseContract;
import fr.free.nrw.commons.upload.license.MediaLicensePresenter;
import fr.free.nrw.commons.upload.mediadetails.UploadMediaDetailsContract;
import fr.free.nrw.commons.upload.mediadetails.UploadMediaPresenter;

/**
 * The Dagger Module for upload related presenters and (some other objects maybe in future)
 */
@Module
public abstract class UploadModule {

    @Binds
    public abstract UploadContract.UserActionListener bindHomePresenter(UploadPresenter
                                                                                presenter);

    @Binds
    public abstract CategoriesContract.UserActionListener bindsCategoriesPresenter(
        CategoriesPresenter presenter);

    @Binds
    public abstract MediaLicenseContract.UserActionListener bindsMediaLicensePresenter(
            MediaLicensePresenter
                    presenter);

    @Binds
    public abstract UploadMediaDetailsContract.UserActionListener bindsUploadMediaPresenter(
            UploadMediaPresenter
                    presenter);

    @Binds
    public abstract DepictsContract.UserActionListener bindsDepictsPresenter(
            DepictsPresenter
            presenter
    );
}
