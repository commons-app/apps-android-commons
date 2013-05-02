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

## Set Up IntelliJ for Commons App Development ##

### Import and Compile CommonApp ##

[Download IntelliJ][6]

1. Clone the repository.
2. Open IntelliJ.
3. Import Project:  
	File -> Import Project  
	or  
	Select 'Import Project' from the Quick Start menu  
4. Navigate to the folder with the cloned repository and press 'OK'.
5. Select 'Import Project from external model' -> 'Maven' and press 'Next'.
6. Make sure 'Search for projects recursively' and 'Import Maven projects automatically' are checked. Select 'Next'.
7. This section needs no modification. Select 'Next'.
8. This section needs no modification. Select 'Next'.
9. Make sure the 'Android SDK home path' points to the 'android-sdk' folder. If the dropdown next to 'Java SDK' is empty, hit the '+' button avobe the sidebar and select 'JDK'. Navigate to your jdk folder, select it, and hit 'OK'. Now select the newly added JDK and hit 'Next'.
10. This section needs no modifications. Select 'Next'.
11. Select 'Finish'.
12. After the program opens select 'Make project' - there should be errors.
13. Neat the top of the file that is opened up, one of the offending lines should be "import android.support.v4.app.FragmentActivity;" - put your cursor on that line and hit 'alt'/'option'+'enter' to bring up the AutoFix dialog. Select the 'compatibility' option.
14. Select 'Make project' again. It should compile successfully.

## License ##

This software is licensed under the [Apache License][5].

## Bugs? ##

This software has no bugs. You can dispute this statement at [bugzilla][3]

[1]: https://developer.android.com/sdk/index.html
[2]: https://maven.apache.org/
[3]: https://bugzilla.wikimedia.org/enter_bug.cgi?product=Commons%20App
[4]: http://search.maven.org/
[5]: https://www.apache.org/licenses/LICENSE-2.0
