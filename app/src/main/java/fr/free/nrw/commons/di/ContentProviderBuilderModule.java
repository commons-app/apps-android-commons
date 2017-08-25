package fr.free.nrw.commons.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.auth.SignupActivity;
import fr.free.nrw.commons.category.CategoryContentProvider;
import fr.free.nrw.commons.contributions.ContributionsActivity;
import fr.free.nrw.commons.contributions.ContributionsContentProvider;
import fr.free.nrw.commons.modifications.ModificationsContentProvider;

@Module
public abstract class ContentProviderBuilderModule {

    @ContributesAndroidInjector
    abstract CategoryContentProvider bindCategoryContentProvider();

    @ContributesAndroidInjector
    abstract ContributionsContentProvider bindContributionsContentProvider();

    @ContributesAndroidInjector
    abstract ModificationsContentProvider bindModificationsContentProvider();

}
