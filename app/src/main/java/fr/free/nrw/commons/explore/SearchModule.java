package fr.free.nrw.commons.explore;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import fr.free.nrw.commons.explore.depictions.DepictsClient;
import fr.free.nrw.commons.explore.depictions.SearchDepictionsDataSourceFactory;
import fr.free.nrw.commons.explore.depictions.SearchDepictionsDataSourceFactoryFactory;
import fr.free.nrw.commons.explore.depictions.SearchDepictionsFragmentContract;
import fr.free.nrw.commons.explore.depictions.SearchDepictionsFragmentPresenter;

/**
 * The Dagger Module for explore:depictions related presenters and (some other objects maybe in future)
 */
@Module
public abstract class SearchModule {

    @Binds
    public abstract SearchDepictionsFragmentContract.UserActionListener bindsSearchDepictionsFragmentPresenter(
            SearchDepictionsFragmentPresenter
                    presenter
    );

    @Provides
    static public SearchDepictionsDataSourceFactoryFactory providesSearchDepictionsFactoryFactory(
        DepictsClient depictsClient){
        return (query, loadingStates) -> new SearchDepictionsDataSourceFactory(depictsClient, query, loadingStates);
    }
}
