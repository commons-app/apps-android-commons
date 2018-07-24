package fr.free.nrw.commons.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.free.nrw.commons.category.CategoryContentProvider;
import fr.free.nrw.commons.contributions.ContributionsContentProvider;
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesContentProvider;
import fr.free.nrw.commons.modifications.ModificationsContentProvider;

@Module
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class ContentProviderBuilderModule {

    @ContributesAndroidInjector
    abstract ContributionsContentProvider bindContributionsContentProvider();

    @ContributesAndroidInjector
    abstract ModificationsContentProvider bindModificationsContentProvider();

    @ContributesAndroidInjector
    abstract CategoryContentProvider bindCategoryContentProvider();

    @ContributesAndroidInjector
    abstract RecentSearchesContentProvider bindRecentSearchesContentProvider();

}
