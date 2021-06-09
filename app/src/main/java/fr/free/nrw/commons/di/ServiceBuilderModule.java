package fr.free.nrw.commons.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.free.nrw.commons.auth.WikiAccountAuthenticatorService;

/**
 * This Class Represents the Module for dependency injection (using dagger)
 * so, if a developer needs to add a new Service to the commons app
 * then that must be mentioned here to inject the dependencies 
 */
@Module
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class ServiceBuilderModule {

    @ContributesAndroidInjector
    abstract WikiAccountAuthenticatorService bindWikiAccountAuthenticatorService();

}
