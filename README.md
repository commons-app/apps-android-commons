# The Wikimedia Commons Android App #

## Build Requirements ##

1. [Android SDK][1] (Level 15)
2. [Maven][2]

## Build Instructions ##

1. Set the environment variable `ANDROID_HOME` to be the path to your Android SDK
2. Run `mvn install` to build
3. Run `mvn android:deploy` to deploy to a device
4. There is no step 4

**Note**: Currently uses a bunch of dependencies that are staged at `yuvi.in/blog/maven`. Will be migrated to either [Maven Central][4] or a Wikimedia staging server soon.

## License ##

This software is licensed under the [Apache License][5].

## Bugs? ##

This software has no bugs. You can dispute this statement at [bugzilla][3]

[1]: https://developer.android.com/sdk/index.html
[2]: https://maven.apache.org/
[3]: https://bugzilla.wikimedia.org/enter_bug.cgi?product=Commons%20App
[4]: http://search.maven.org/
[5]: https://www.apache.org/licenses/LICENSE-2.0
