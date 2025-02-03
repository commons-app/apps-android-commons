package fr.free.nrw.commons.contributions;

import javax.inject.Named;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import fr.free.nrw.commons.kvstore.JsonKvStore;

/**
 * The Dagger Module for contributions-related presenters and other dependencies
 */
@Module
public abstract class ContributionsModule {

    @Binds
    public abstract ContributionsContract.UserActionListener bindsContributionsPresenter(
        ContributionsPresenter presenter
    );

    @Provides
    static JsonKvStore providesApplicationKvStore(
        @Named("default_preferences") JsonKvStore kvStore
    ) {
        return kvStore;
    }
}
