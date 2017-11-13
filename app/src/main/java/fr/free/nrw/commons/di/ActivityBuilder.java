package fr.free.nrw.commons.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.free.nrw.commons.contributions.ContributionsActivity;

@Module
public abstract class ActivityBuilder {

    @ContributesAndroidInjector()
    abstract ContributionsActivity bindSplashScreenActivity();
}
