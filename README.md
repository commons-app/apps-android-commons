# Upload to Commons [![Build status](https://api.travis-ci.org/nicolas-raoul/apps-android-commons.svg)](https://travis-ci.org/nicolas-raoul/apps-android-commons)

Upload pictures from your Android phone/tablet to Wikimedia Commons.

Initially started by the Wikimedia Foundation, this app is now maintained by volunteers. Anyone is welcome to improve it, just choose among the [open issues](https://github.com/nicolas-raoul/apps-android-commons/issues) and send us a pull request :-)

## Use Android Studio or IntelliJ ##

### Import and Compile Commons Android App ##

[Download Android Studio][1] (recommended) or [IntelliJ][2].

1. Open Android Studio/IntelliJ. Open the project:
	``File`` > ``New`` > ``Project from Version Control...`` > ``Git``  
	or  
	(From Quick Start menu): ``Check out project from Version Control``
2. Enter ``https://github.com/nicolas-raoul/apps-android-commons/`` as Git Repository URL. Specify a (new) local directory you would like to clone into and select ``OK``.

## Build Manually ##

Note: It is much harder to build manually. We recommend you use Android Studio or IntelliJ IDEA, which both have gradle and all the android tools built in.

### Requirements ###

1. [Android SDK][3] (Level 23)
2. [Gradle][4]

### Build Instructions ###

1. Set the environment variable `ANDROID_HOME` to be the path to your Android SDK
2. Set the environment variable `JAVA_HOME` to the path to your Java SDK
3. Run `gradlew.bat assembleDebug` (Windows) or `./gradlew assembleDebug` (Mac / Linux) to build an unisgned apk
4. Alternatively, you can also connect your Android device via USB and install the app on it directly by running `gradlew.bat installDebug` (Windows) or `./gradlew installDebug` (Mac / Linux)

There are more thorough instructions on the [Android Developers website][5]

## License ##

This software is licensed under the [Apache License 2.0][6].

## Bugs ##

Please report any bug [on Github][7].

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

Thumbnail images are not currently cached.


[1]: https://developer.android.com/studio/index.html
[2]: http://www.jetbrains.com/idea/download/index.html
[3]: https://developer.android.com/sdk/index.html
[4]: http://gradle.org/gradle-download/
[5]: https://developer.android.com/studio/build/building-cmdline.html
[6]: https://www.apache.org/licenses/LICENSE-2.0
[7]: https://github.com/nicolas-raoul/apps-android-commons/issues
