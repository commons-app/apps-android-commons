package fr.free.nrw.commons.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.free.nrw.commons.contributions.ContributionsActivity;
import fr.free.nrw.commons.nearby.NearbyActivity;

@Module
public abstract class ActivityBuilder {

    @ContributesAndroidInjector()
    abstract ContributionsActivity bindSplashScreenActivity();

    @ContributesAndroidInjector()
    abstract NearbyActivity bindNearbyActivity();
}
