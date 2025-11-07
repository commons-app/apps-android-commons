package fr.free.nrw.commons.contributions

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.wikidata.model.WikiSite
import javax.inject.Named

/**
 * The Dagger Module for contributions-related providers
 */
@Module
@InstallIn(ActivityComponent::class)
class ContributionsProvidesModule {

    @Provides
    fun providesApplicationKvStore(
        @Named("default_preferences") kvStore: JsonKvStore
    ): JsonKvStore {
        return kvStore
    }

    @Provides
    fun providesLanguageWikipediaSite(
        @Named("language-wikipedia-wikisite") languageWikipediaSite: WikiSite
    ): WikiSite {
        return languageWikipediaSite
    }
}