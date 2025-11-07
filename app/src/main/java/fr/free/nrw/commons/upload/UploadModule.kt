package fr.free.nrw.commons.upload

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import fr.free.nrw.commons.upload.categories.CategoriesContract
import fr.free.nrw.commons.upload.categories.CategoriesPresenter
import fr.free.nrw.commons.upload.depicts.DepictsContract
import fr.free.nrw.commons.upload.depicts.DepictsPresenter
import fr.free.nrw.commons.upload.license.MediaLicenseContract
import fr.free.nrw.commons.upload.license.MediaLicensePresenter
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailsContract
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaPresenter

/**
 * The Dagger Module for upload related presenters and (some other objects maybe in future)
 */
@Module
@InstallIn(ActivityComponent::class)
abstract class UploadModule {
    @Binds
    abstract fun bindHomePresenter(presenter: UploadPresenter): UploadContract.UserActionListener

    @Binds
    abstract fun bindsCategoriesPresenter(presenter: CategoriesPresenter): CategoriesContract.UserActionListener

    @Binds
    abstract fun bindsMediaLicensePresenter(presenter: MediaLicensePresenter): MediaLicenseContract.UserActionListener

    @Binds
    abstract fun bindsUploadMediaPresenter(presenter: UploadMediaPresenter): UploadMediaDetailsContract.UserActionListener

    @Binds
    abstract fun bindsDepictsPresenter(presenter: DepictsPresenter): DepictsContract.UserActionListener
}
