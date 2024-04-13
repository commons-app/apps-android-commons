package fr.free.nrw.commons.di

import dagger.Module
import dagger.Provides
import fr.free.nrw.commons.BetaConstants
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.OkHttpConnectionFactory
import fr.free.nrw.commons.actions.PageEditInterface
import fr.free.nrw.commons.actions.ThanksInterface
import fr.free.nrw.commons.auth.csrf.CsrfTokenInterface
import fr.free.nrw.commons.category.CategoryInterface
import fr.free.nrw.commons.media.MediaDetailInterface
import fr.free.nrw.commons.media.MediaInterface
import fr.free.nrw.commons.media.PageMediaInterface
import fr.free.nrw.commons.media.WikidataMediaInterface
import fr.free.nrw.commons.mwapi.UserInterface
import fr.free.nrw.commons.notification.NotificationInterface
import fr.free.nrw.commons.review.ReviewInterface
import fr.free.nrw.commons.upload.UploadInterface
import fr.free.nrw.commons.upload.WikiBaseInterface
import fr.free.nrw.commons.upload.depicts.DepictsInterface
import fr.free.nrw.commons.wikidata.RxCommonsServiceFactory
import fr.free.nrw.commons.wikidata.WikidataConstants
import fr.free.nrw.commons.wikidata.WikidataInterface
import fr.free.nrw.commons.wikidata.cookies.CommonsCookieJar
import fr.free.nrw.commons.wikidata.model.WikiSite
import javax.inject.Named
import javax.inject.Singleton

@Module
@Suppress("unused")
@Deprecated("Start migrating toward Kotlin coroutines")
class RxJavaApiModule {
    // NOTE: As an API client / interface is refactored to use coroutines, move it
    //       out of this module and put it into the ApiModule.

    @Provides
    @Singleton
    //TODO: delete this once the coroutine migration is completed for the API layer
    fun rxJavaServiceFactory(cookieJar: CommonsCookieJar): RxCommonsServiceFactory =
        RxCommonsServiceFactory(OkHttpConnectionFactory.getClient(cookieJar))

    @Provides
    @Singleton
    fun provideReviewInterface(serviceFactory: RxCommonsServiceFactory): ReviewInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, ReviewInterface::class.java)

    @Provides
    @Singleton
    fun provideDepictsInterface(serviceFactory: RxCommonsServiceFactory): DepictsInterface =
        serviceFactory.create(BuildConfig.WIKIDATA_URL, DepictsInterface::class.java)

    @Provides
    @Singleton
    fun provideWikiBaseInterface(serviceFactory: RxCommonsServiceFactory): WikiBaseInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, WikiBaseInterface::class.java)

    @Provides
    @Singleton
    fun provideUploadInterface(serviceFactory: RxCommonsServiceFactory): UploadInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, UploadInterface::class.java)

    @Provides
    @Singleton
    @Named("commons-page-edit-service")
    fun providePageEditService(serviceFactory: RxCommonsServiceFactory): PageEditInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, PageEditInterface::class.java)

    @Provides
    @Singleton
    @Named("wikidata-page-edit-service")
    fun provideWikiDataPageEditService(serviceFactory: RxCommonsServiceFactory): PageEditInterface =
        serviceFactory.create(BuildConfig.WIKIDATA_URL, PageEditInterface::class.java)

    @Provides
    @Singleton
    fun provideMediaInterface(serviceFactory: RxCommonsServiceFactory): MediaInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, MediaInterface::class.java)

    @Provides
    @Singleton
    fun provideWikidataMediaInterface(serviceFactory: RxCommonsServiceFactory): WikidataMediaInterface =
        serviceFactory.create(BetaConstants.COMMONS_URL, WikidataMediaInterface::class.java)

    @Provides
    @Singleton
    fun providesMediaDetailInterface(serviceFactory: RxCommonsServiceFactory): MediaDetailInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, MediaDetailInterface::class.java)

    @Provides
    @Singleton
    fun provideCategoryInterface(serviceFactory: RxCommonsServiceFactory): CategoryInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, CategoryInterface::class.java)

    @Provides
    @Singleton
    fun provideThanksInterface(serviceFactory: RxCommonsServiceFactory): ThanksInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, ThanksInterface::class.java)

    @Provides
    @Singleton
    fun provideNotificationInterface(serviceFactory: RxCommonsServiceFactory): NotificationInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, NotificationInterface::class.java)

    @Provides
    @Singleton
    fun provideUserInterface(serviceFactory: RxCommonsServiceFactory): UserInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, UserInterface::class.java)

    @Provides
    @Singleton
    fun provideWikidataInterface(serviceFactory: RxCommonsServiceFactory): WikidataInterface =
        serviceFactory.create(BuildConfig.WIKIDATA_URL, WikidataInterface::class.java)

    /**
     * Add provider for PageMediaInterface It creates a retrofit service for the wiki site using
     * device's current language
     */
    @Provides
    @Singleton
    fun providePageMediaInterface(
        @Named(WikidataConstants.NAMED_LANGUAGE_WIKI_PEDIA_WIKI_SITE) wikiSite: WikiSite,
        serviceFactory: RxCommonsServiceFactory
    ): PageMediaInterface = serviceFactory.create(wikiSite.url(), PageMediaInterface::class.java)

    @Provides
    @Singleton
    fun provideCsrfTokenInterface(serviceFactory: RxCommonsServiceFactory): CsrfTokenInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, CsrfTokenInterface::class.java)

}