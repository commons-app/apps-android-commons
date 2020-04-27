# Wikimedia Android data client
An Android library for communicating with Wikimedia projects, with Rx bindings and other utilities.

## Motivation and philosophy

Here are the purposes for creating this library:

* Encapsulate the various model structures that are returned by MediaWiki APIs,
as well as by REST APIs provided by Wikimedia services.

* Provide high-level bindings for Retrofit and RxJava for executing calls to MediaWiki APIs to
further simplify client integration, while also allowing customization and extension.

* Provide numerous common utility methods, so that they don't need to be duplicated.

## Integration with your app

Add the dependency to your Gradle file as usual:

```
implementation "com.dmitrybrant:wikimedia-android-data-client:0.0.18"
```

The only nontrivial point of integration with the library is the `AppAdapter` class:  You
need to create a class that inherits from `AppAdapter` and implement its methods.  The
methods are mostly self-explanatory, and deal with user account management, cookie storage,
and a few other customizations.

Once you create this class (suppose it's called `MyAppAdapter`), you should pass it into
the `AppAdapter` singleton when your app starts:

```
@Override
public void onCreate() {
    ...
    AppAdapter.set(new MyAppAdapter());
    ...
}
```

## Making calls to APIs

Notice that there is an interface called `Service` that contains a number of API definitions
for talking with a MediaWiki server. To use any of the functions in the interface, you should
use the `ServiceFactory` class. For example:

```
WikiSite wiki = new WikiSite("en.wikipedia.org");

Observable observable = ServiceFactory.get(wiki).fullTextSearch("foo");
```

That's it! Notice that most of the API calls return an `Observable` response which you can
feed into an Rx subscription.

Note: the `ServiceFactory` class contains automatic caching logic, so that multiple calls to
`get()` the service for the same `WikiSite` will be very efficient.

## Custom API calls

The `ServiceFactory` class also allows you to provide a service interface with custom
API functions. Suppose you create your own service interface that looks like this:

```
public interface MyInterface {

    @GET("action=myawesomeaction")
    Observable<MyAwesomeResponse> myAwesomeApiCall(@Query("parameter1") parameter);

}
```

You can then use it with `ServiceFactory` this way:

```
WikiSite wiki = new WikiSite("my.awesome.wiki");

Observable observable = ServiceFactory.get(wiki, "https://my.awesome.wiki/", MyInterface.class)
        .myAwesomeApiCall("foo");
```

## Utility methods

The library contains a potpourri of utility methods found under the `util` package. Feel free
to browse through them and use them as necessary.

## License

Copyright 2019 Wikimedia Foundation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
