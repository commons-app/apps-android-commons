package fr.free.nrw.commons.upload;

import dagger.Binds;
import dagger.Module;
import fr.free.nrw.commons.upload.categories.CategoriesPresenter;
import fr.free.nrw.commons.upload.categories.ICategories;
import fr.free.nrw.commons.upload.license.IMediaLicense;
import fr.free.nrw.commons.upload.license.MediaLicensePresenter;
import fr.free.nrw.commons.upload.mediaDetails.IUploadMediaDetails;
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaPresenter;

@Module
public abstract class UploadModule {

    @Binds
    public abstract IUpload.UserActionListener bindHomePresenter(UploadPresenter
            presenter);

    @Binds
    public abstract ICategories.UserActionListener bindsCategoriesPresenter(CategoriesPresenter
            presenter);

    @Binds
    public abstract IMediaLicense.UserActionListener bindsMediaLicensePresenter(
            MediaLicensePresenter
                    presenter);

    @Binds
    public abstract IUploadMediaDetails.UserActionListener bindsUploadMediaPresenter(
            UploadMediaPresenter
                    presenter);

}
