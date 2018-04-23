# Wikimedia Commons for Android

## v2.7.0
- New Nearby Places UI with direct uploads (and associated category suggestions)
- Added two-factor authentication login
- Added Notifications activity to display user talk messages
- Added real-time location tracking in Nearby
- Added "rate us", "translate", and FB link in About
- Improvements to UI of navigation drawer, tutorial, media details view, login activity and Settings
- Added option to nominate picture for deletion in media details view
- Too many bug and crash fixes to mention!

## v2.6.7
- Added null checks to prevent frequent crashes in ModificationsSyncAdapter

## v2.6.6
- Refactored Dagger to fix crashes encountered in production
- Fixed "?" displaying in description of Nearby places
- Database-related cleanup and tests
- Optimized dimens.xml
- Fixed issue where map opens with incorrect coordinates

## v2.6.5 beta
- Changed "send log" feature to only send logs to private Google group forum
- Switched to using Wikimedia maps server instead of Mapbox for privacy reasons
- Removed event logging from app for privacy reasons
- Fixed crash caused by rapidly switching from Nearby map to list while loading

## v2.6.4 beta
- Excluded httpclient and commons-logging to fix release build errors
- Fixed crashes caused by Fresco and Dagger

## v2.6.3 beta
- Same as 2.6.2 except with localizations added for Google Code-In 

## v2.6.2 beta
- Reverted temporarily to last stable version while working on crash fix

## v2.6.1 beta
- Failed attempt to fix crashes in release build with the previous beta release

## v2.6.0 beta
- Multiple bugfixes for location updates and list/map loading in Nearby
- Multiple fixes for various crashes and memory leaks
- Added several unit tests
- Modified About page to include WMF disclaimer and modified Privacy Policy link to point to our individual privacy policy
- Added option for users to send logs to developers (has to be manually activated by user)
- Converted PNGs to WebPs
- Improved login screen with new design and privacy policy link
- Improved category display, if a category has an exact name entered, it will be shown first
- New UI for Nearby list
- Added product flavors for production and the beta-cluster Wikimedia servers 
- Various improvements to navigation flow and backstack

## v2.5.0 beta 
- Added one-time popup for beta users to provide feedback on IEG renewal proposal
- Added link to Commons policies in ShareActivity
- Various string fixes
- Switched to using vector icons for map markers
- Added filter for irrelevant categories
- Fixed various crashes
- Incremented target SDK to 25
- Improved appearance of navigation drawer
- Replaced proprietary app image in tutorial with one that isn't Telegram
- Fixed camera issue with FileProvider
- Added RxJava library, migrated to Java 8
- Various code and continuous integration optimizations

## v2.4.2 beta
- Added option to launch tutorial again from nav drawer
- Added marker for current location in Nearby map
- Fixed various strings
- Added check for location permissions when launching Nearby
- Temporary fix for API 25 camera crash
- App should now display accurate upload count
- Updated Gradle from 3.3 to 4.0

## v2.4.1 beta
- Fixed crash with uploading multiple photos
- Fixed memory leaks
- Fixed issues with Nearby places list and map

## v2.4
- Fixed memory issue with loading contributions on main screen
- Deleted images don't show up on contributions list 
- Added Fresco library for image loading and LeakCanary for memory profiling
- Added navigation drawer and overhauled action bar
- Added logout functionality
- Fixed various issues with map of Nearby places

## v2.3 beta
- Add map of Nearby places
- Add overlay dialog when a Nearby place is tapped
- Set default number of uploads to display in Main activity as 100, and add option in Settings to change it
- Detect when 2FA is used for login and display message
- Display date uploaded and image coordinates in image details page
- Display message when GPS is turned off, and when no Nearby items are found

## v2.2.2
- Hotfix for Nearby localization issue

## v2.2.1
- Hotfix for Settings crash

## v2.2 beta (will not be released to Production due to bugs with Settings)
- Revamped Nearby to query Wikidata by default instead of Wiki Needs Pictures
- Added action bar to About screen
- Fixed crash related to fragment transaction state loss
- Moved Feedback menu item below Settings
- Various code optimizations and refactoring

## v2.1
- Added beta opt in link to Settings
- Added Codacy and Butterknife support
- Added Light theme for day/outdoor use
- Added Material icons
- Reordered overflow menu items
- Added credits to About page
- Fixed lint issues
- Fixed various crashes

## v2.0.2 
- Make "View in browser" direct to mobile website 

## v2.0.1
- Disabled minify again (reenabling test failed)
- Hotfix for ShareAction bug

## v2.0
- Modified Share button in media details fragment to allow user to choose different apps
- Added CC-BY 4.0 and CC-BY-SA 4.0 to license options
- Added selection pane for licenses on title/desc screen
- Switched to using material design for login form fields
- Added Checkstyle support
- Reenabled minify in Gradle
- Other minor code optimizations

## v1.44
- Attempted fix for GPS suggestions issue

## v1.43
- Added translations for multiple languages
- Minor code optimization

## v1.42
- Fixed language mappings; successful translatewiki integration
- Various translations added

## v1.41
- Bumped min SDK and removed escaped characters for translatewiki.net integration
- Added check for whether file already exists on Commons

## v1.40
- Added new pages to tutorial

## v1.39
- Fix for Korean translations crash
- Various minor fixes

## v1.38
- Added filter for suggested categories containing years (other than current or previous year)
- Attempted fix for issues with categories not being saved

## v1.37
- Added category suggestions based on entered title

## v1.36
- Fixed Ukranian translations

## v1.35
- Fixed issues with GPS category suggestions

## v1.34
- Added button to use previous title/desc

## v1.33
- Fixed crash when back button pressed before Nearby list is loaded
- Fixed crash when Nearby list is loaded without network connection
- Added no args constructor for GPS category suggestions

## v1.32
- Use Quadtree source instead of JAR, for F-Droid compatibility
- Fixed GPS extractor not being called

## v1.31
- Fixed bug with geolocation category suggestions not being displayed
- Fixed bug with (0,0) being recorded as image location occasionally

## v1.30
- Fixed {{Location|null}} template bug

## v1.29
- Added new icons to Nearby
- Added link to website on About

## v1.28
- Added geocoding template from GPS data stored in image
- Fixed bug with doubled list view in Nearby
- Further attempts to reduce overwrites

## v1.27
- New feature: List of nearby places without photos

## v1.26
- Fixed bug with overwriting files when multiple images selected

## v1.25
- Added in-app signup feature for new users
- Fixed crash when reading GPS coordinates

## v1.24
- Moved  from bits/event.gif to wikimedia/beacon
- Fixed issue with needing to tap gallery again after giving permissions

## v1.23
- Added warning if image is submitted without categories
- Added check if back button is pressed at category selection screen

## v1.22
- Fixed various crashes
- Crash reports now go to private mailing list to protect user info

## v1.21
- Fixed Google Photos multiple share crash

## v1.20
- Hotfix for data=null crash

## v1.19
- Fixed adapter crash
- Attempt at fixing Google Photos crash

## v1.18
- Fixed various crashes
- Fixed camera and gallery for API 23

## v1.17
- Fixed various crashes
- Fixed 'Desc/license/categories empty' bug

## v1.16
- Fixed various crashes
- Reduced APK size
- Fixed 'waiting for first sync' bug

## v1.15
- Added material design logo

## v1.14
- Migrated to Gradle
- Fixed API 23 permission crash
- Fixed "Template:According to EXIF data" analyzing EXIF data incorrectly

## v1.13
- Fixed prettyLicense and mediaUri crashes

## v1.12
- Further bug fixes for Polish language
- Added Javadocs

## v1.11
- Bugfix for Polish language crash

## v1.10
- Bugfix for null location crash

## v1.9
- Bugfix for null pages array crash
- New feature: Added option to use GPS to find nearby categories if picture is not geotagged

## v1.8
- New feature: Improved category search function (not limited to prefix search now)

## v1.7
- Fixed bug with uploading images in Marshmallow
- Fixed links in About page

## v1.6
- Bugfix for invalid images

## v1.5
- Caches area and associated categories
- Increased search radius for nearby categories

## v1.4
- New feature: Suggests nearby Commons categories

## v1.3
- Removed 'send usage reports' setting
- Fixed package naming issue
- Added 'sign up' button
- Removed unused 'campaigns' shortcut

## v1.0 beta 11
- New Launcher Icon
- Fix bug with licensing templates
- i18n updates

## v1.0 beta 10
- Successfully reached double digit beta number
- Honeycomb fixes
- Fix crash when uploading multiple files
- Make thumbnail loading faster
- i18n updates

## v1.0 beta 9
- Sharper, higher resolution image thumbnails used
- Better caching mechanism in place for image thumbnails
- Allow users to pick between various CC licenses
- Display tutorial on first use explaining Commons
- Experimental checks to avoid filename duplication
- Experimental support for RTL flipping with Android 4.2
- Add option to download full resolution image to phone
- Fix 'flickering' on Android 2.3
- Various minor bug fixes
- i18n updates

## v1.0 beta 8
- Disable menu items for share and open browser when upload incomplete
- Show recently-used categories
- Prevent upload status from overlapping with the title of upload
- Make template removal work properly
- Relicense to Apache License
- i18n updates

## v1.0 beta 7
- Added opt out from EventLogging
- Remove {{Uncategorized}} template after adding categories
- Be more consistent and proactive in syncing modifications (adding categories)
- Add a minimal About page
- Add option to send feedback via email from within the app
- i18n updates

## v1.0 beta 6
- Add categorization
- Add a 'Modifications Sync' framework for doing eventual-consistent page edits
- More consistent designb between single and multiple upload
- i18n updates

## v1.0 beta 5.1
- Emergency release, since beta5 uploaded to testwiki

## v1.0 beta 5

- Fix bug setting descriptions and author info on multiple image uploads to 'null'

## v1.0 beta 4
- Switched properly to Holo Dark theme
- Multiple uploads support! Select multiple images from gallery and send 'em here!
- Reduce naming related upload errors
- Update UIL
- General refactoring for a slightly cleaner codebase
- i18n updates

## v1.0 beta 3
- Fix reported crashes
- i18n updates

## v1.0 beta 2

- Fix bug with non-ASCII characters
- Preserve user and description information across upload restarts
- Rudimentary OGG uploading support (when shared from another app only)
- Transparent images now have a white background
- UI improvements for Login


## v1.0 beta 1

- Upload images to commons by taking a picture, picking from Gallery, or sharing from another application
- Queue up and upload multiple images to commons at the same time
- View all your contributions to commons
