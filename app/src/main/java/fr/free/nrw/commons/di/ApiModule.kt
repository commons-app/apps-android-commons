package fr.free.nrw.commons.di

import dagger.Module
import dagger.Provides
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.OkHttpConnectionFactory
import fr.free.nrw.commons.auth.login.LoginInterface
import fr.free.nrw.commons.wikidata.CoroutinesCommonsServiceFactory
import fr.free.nrw.commons.wikidata.cookies.CommonsCookieJar
import javax.inject.Singleton

@Module
@Suppress("unused")
class ApiModule {
    // NOTE: As an API client / interface is refactored to use coroutines, move it
    //       into this module and delete it from the RxJavaApiModule.

    @Provides
    @Singleton
    fun coroutinesServiceFactory(cookieJar: CommonsCookieJar): CoroutinesCommonsServiceFactory =
        CoroutinesCommonsServiceFactory(OkHttpConnectionFactory.getClient(cookieJar))

    @Provides
    @Singleton
    fun provideLoginInterface(serviceFactory: CoroutinesCommonsServiceFactory): LoginInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, LoginInterface::class.java)
}