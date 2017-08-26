package fr.free.nrw.commons.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.free.nrw.commons.auth.WikiAccountAuthenticatorService;
import fr.free.nrw.commons.category.CategorizationFragment;
import fr.free.nrw.commons.contributions.ContributionsListFragment;
import fr.free.nrw.commons.media.MediaDetailFragment;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.nearby.NearbyListFragment;
import fr.free.nrw.commons.nearby.NoPermissionsFragment;
import fr.free.nrw.commons.settings.SettingsFragment;
import fr.free.nrw.commons.upload.MultipleUploadListFragment;
import fr.free.nrw.commons.upload.SingleUploadFragment;
import fr.free.nrw.commons.upload.UploadService;

@Module
public abstract class ServiceBuilderModule {

    @ContributesAndroidInjector
    abstract UploadService bindUploadService();

    @ContributesAndroidInjector
    abstract WikiAccountAuthenticatorService bindWikiAccountAuthenticatorService();

}
