## Overview

At its core, dependency injection is just the principle of `"tell, don't ask"` put into practice; for instance, if a class needs to use the `MediaWikiApi`, it should be handed an instance of the class rather than reaching out to get it.  This has the effect of decoupling code, making it easier to test and reuse.

## Dependency Injection in the Commons app

We use Dagger 2 as our dependency injection engine.  Dagger is a fully static, compile-time dependency injection framework for both Java and Android.  Dagger aims to address many of the development and performance issues that have plagued reflection-based solutions that came before it, but it does come at something of a cost in complexity.

For more information about Dagger, take a look at the [Dagger user guide](https://google.github.io/dagger/users-guide.html).

## Dagger configuration in the Commons app

The top level `CommonsApplicationComponent` pulls together configuration for injection across the app.  The most important files to understand

- if you need to add a new Activity, look at `ActivityBuilderModule` and copy how injection is configured.  The `BaseActivity` class will take care of the rest.
- if you are adding a new Fragment, look at `FragmentBuilderModule`
- if you are adding a new ContentProvider, look at `ContentProviderBuilderModule`
- if you are adding a new Service, look at `ServiceBuilderModule`
- other dependencies are configured in `CommonsApplicationModule`

## "Provider" methods

Dagger will resolve the method arguments on provider methods in a module (or the constructor arguments when annotated with `@Inject`) and build the objects accordingly - either by calling another provider method or by looking for a constructor on a class that has the `@Inject` annotation.  Dagger takes care of managing singletons, just annotate with the `@Singleton` annotation.  For instance,

```java
@Provides
@Singleton
public SessionManager providesSessionManager(MediaWikiApi okHttpJsonApiClient) {
    return new SessionManager(application, okHttpJsonApiClient);
}
```

If your code injects an interface (in this case, `MediaWikiApi`) then Dagger needs to know which concrete class to use.  This comes by way of a provider method:

```java
@Provides
@Singleton
public MediaWikiApi provideMediaWikiApi() {
    return new ApacheHttpClientMediaWikiApi(BuildConfig.WIKIMEDIA_API_HOST);
}
```
