package fr.free.nrw.commons.contributions

import dagger.Module
import dagger.Provides
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.wikidata.model.WikiSite
import javax.inject.Named

/**
 * The Dagger Module for contributions-related providers
 */
@Module
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