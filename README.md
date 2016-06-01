# Upload to Commons #

Upload pictures from your Android phone/tablet to Wikimedia Commons.

Initially started by the Wikimedia Foundation, this app is now maintained by volunteers. Anyone is welcome to improve it, just choose among the [open issues](https://github.com/nicolas-raoul/apps-android-commons/issues) and send us a pull request :-)

## Build Requirements ##

1. [Android SDK][1] (Level 23)
2. [Maven][2]

## Build Instructions ##

1. Set the environment variable `ANDROID_HOME` to be the path to your Android SDK
2. Run `mvn install` to build
3. Run `cd commons && mvn android:deploy` to deploy to a device

**Note**: Currently uses a bunch of dependencies that are staged at `yuvi.in/blog/maven`. Will be migrated to either [Maven Central][4] or a Wikimedia staging server soon.

## Set Up IntelliJ or Android Studio for Commons Android App Development ##

### Import and Compile Commons Android App ##

[Download IntelliJ][6] or [Download Android Studio 1.5.2][7]. (Note: The steps below currently only work on Android Studio 1.5.2 and below)

1. Clone the repository.
2. Open IntelliJ/Android Studio. Tick the box for the Maven Integration plugin by selecting:  
 	``File`` > ``Settings`` > ``Plugins`` > ``Maven Integration``  
	or  
	(From Quick Start menu): ``Configure`` > ``Plugins`` > ``Maven Integration``
3. Import Project:  
	``File`` > ``Import Project``  
	or  
	(From Quick Start menu): ``Import Project (Eclipse ADT, Gradle, etc.)``
4. Navigate to the folder with the cloned repository (named apps-android-commons). Select ``OK``.
5. Select ``Import Project from external model`` > ``Maven``. Select ``Next``.
6. Tick the boxes ``Search for projects recursively`` and ``Import Maven projects automatically``. Select ``Next``.
7. Select ``Next``.
8. Select ``Next``.
9. Click ``Maven Android API 23 Platform`` or ``Android API 23 Platform`` in the sidebar. Make sure the ``Android SDK home path`` points to the ``/Android/Sdk`` folder. Make sure the ``Java SDK`` is set to 1.8 or higher.  
    If there are no options for the ``Java SDK``, click the ``+`` button above the sidebar and select 'JDK'. Navigate to your JDK folder, select it, and hit ``OK``, and then select the newly added JDK.  
    Select ``Next``.
10. Select ``Next``.
11. Select ``Finish``.
12. Set the Module SDKs.  
    Select the ``Dependencies`` tab on the right pane.  
    Set the modules as follows:  

	| Name                                                  | Module SDK                            |
	|-------------------------------------------------------|---------------------------------------|
	| commons                                               | Project SDK (Android API 23 Platform) |
	| commons-parent                                        | Project SDK (Android API 23 Platform) |
	| ~apklib-com.actionbarsherlock_actionbarsherlock_4.4.0 | Maven Android API 14 Platform         |
	| ~apklib-com.viewpagerindicator_library_2.4.1          | Maven Android API 16 Platform         |

    If certain modules are not available, install the correct API levels through the SDK manager. To do this do the following:  
    
    * Click ``Cancel``. Navigate to ``File`` > ``Settings`` > ``Appearance & Behaviour`` > ``System Settings`` > ``Android SDK``.  
    * Tick the boxes for API levels ``14``, ``16``, and ``23`` (or Android ``4.0``, ``4.1.2`` and ``6.0``).  
    * Then click ``OK``, and allow it to download the new APIs. Once it has finished, click ``File`` > ``Project Structure`` > ``Project Settings`` > ``Modules``, and repeat step 15.
13. Select ``commons``. Click the green ``+`` button on the right. Select ``JARs or directories...``. Choose the ``apps-android-commons/lib`` folder. Select ``OK``.
14. Select ``OK`` to save your changes to the project structre settings.
15. To test it worked, check if it builds (Select ``commons`` on the projects panel. Select ``Build`` > ``Make Module 'commons'``). If there are no errors (warnings are OK) you're set!

## License ##

This software is licensed under the [Apache License][5].

## Bugs ##

Please report any bug [on Github][3].

## Code Structure ##

Key breakdowns:

Activities started within the UI:
* ContributionsActivity (ContributionsListFragment, MediaDetailPagerFragment, MediaDetailFragment) - main "my uploads" list and detail view
* LoginActivity - login screen when setting up an account
* SettingsActivity - settings screen
* AboutActivity - about screen

Activities receiving intents:
* ShareActivity (SingleUploadFragment, CategorizationFragment) - handles receiving a file from another app, accepting a title/desc, and slating it for upload
* MultipleShareActivity (MultipleUploadListFragment, CategorizationFragment) - handles receiving a batch of multiple files from another app, accepting a title/desc, and slating them for upload

Services:
* WikiAccountAuthenticatorService - authentication service
* UploadService - performs actual file uploads in background
* ContributionsSyncService - polls for updated contributions list from server
* ModificationsSyncService - pushes category additions up to server

Content providers:
* ContributionsContentProvider - private storage for local copy of user's contribution list
* ModificationsContentProvider - private storage for pending category and template modifications
* CategoryContentProvider - private storage for recently used categories


## On-Device Storage ##

Account credentials are encapsulated in an account provider. Currently only one Wikimedia Commons account is supported at a time. (Question: what is the actual storage for credentials?)

Preferences are stored in Android's SharedPreferences.

Information about past and pending uploads is stored in the Contributions content provider, which uses an SQLite database on the backend.

A list of recently-used categories is stored in the Categories content provider, which uses an SQLite database on the backend.

Captured files are not currently stored within the app, but are passed by content: or file: URI from other apps.

Thumbnail images are not currently cached. (?)


[1]: https://developer.android.com/sdk/index.html
[2]: https://maven.apache.org/
[3]: https://github.com/nicolas-raoul/apps-android-commons/issues
[4]: http://search.maven.org/
[5]: https://www.apache.org/licenses/LICENSE-2.0
[6]: http://www.jetbrains.com/idea/download/index.html
[7]: https://sites.google.com/a/android.com/tools/download/studio/builds/1-5-2
