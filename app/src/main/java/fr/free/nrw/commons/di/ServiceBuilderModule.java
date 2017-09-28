package fr.free.nrw.commons.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.free.nrw.commons.auth.WikiAccountAuthenticatorService;
import fr.free.nrw.commons.upload.UploadService;

@Module
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class ServiceBuilderModule {

    @ContributesAndroidInjector
    abstract UploadService bindUploadService();

    @ContributesAndroidInjector
    abstract WikiAccountAuthenticatorService bindWikiAccountAuthenticatorService();

}
