package fr.free.nrw.commons.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.free.nrw.commons.AboutActivity;
import fr.free.nrw.commons.WelcomeActivity;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.auth.SignupActivity;
import fr.free.nrw.commons.contributions.ContributionsActivity;
import fr.free.nrw.commons.nearby.NearbyActivity;
import fr.free.nrw.commons.settings.SettingsActivity;
import fr.free.nrw.commons.upload.MultipleShareActivity;
import fr.free.nrw.commons.upload.ShareActivity;

@Module
public abstract class ActivityBuilderModule {

    @ContributesAndroidInjector
    abstract ContributionsActivity bindContributionsActivity();

    @ContributesAndroidInjector
    abstract MultipleShareActivity bindMultipleShareActivity();

    @ContributesAndroidInjector
    abstract ShareActivity bindShareActivity();

    @ContributesAndroidInjector
    abstract LoginActivity bindLoginActivity();

    @ContributesAndroidInjector
    abstract SignupActivity bindSignupActivity();

    @ContributesAndroidInjector
    abstract NearbyActivity bindNearbyActivity();

    @ContributesAndroidInjector
    abstract AboutActivity bindAboutActivity();

    @ContributesAndroidInjector
    abstract SettingsActivity bindSettingsActivity();

    @ContributesAndroidInjector
    abstract WelcomeActivity bindWelcomeActivity();

}
