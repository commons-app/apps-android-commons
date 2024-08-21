# Wikimedia Commons for Android

## v5.0.2

- Enhanced multi-upload functionality with user prompts to clarify that all images would share the
  same category and depictions.
- Show Wikidata description on currently active Nearby pin to provide more useful information.
- Improve the visibility of map markers by dynamically adjusting their colors based on the app's
  theme. The map markers will now appear lighter when the app is in dark mode and darker when the
  app is in light mode. This change aims to enhance marker visibility and improve the overall user
  experience.
- Added information on where user feedback is posted, helping users track existing feedback and
  monitor their own submissions.
- Enhanced the edit location screen of the upload screen by centering the map on the picture's
  location from metadata when editing, or on the device's GPS location if metadata is unavailable,
  improving accuracy and user experience.
- Ensured the 'Add Location' button is renamed to 'Edit Location' when copying the location of a
  recently uploaded image, enhancing clarity and user experience.
- Added a ProgressBar to the media detail screen to indicate image loading status, enhancing user
  experience by showing loading progress until the image is fully loaded.
- Fixed an issue where caption and description fields would intermittently disappear when using
  voice input, ensuring text remains visible and stable across all entries.
- Fixed a crash that occurred when attempting to remove multiple instances of caption/description
  fields after initially adding them.
- Improve the text in the prompt shown when skipping login to sound more natural.
- Modified feedback addition logic to append new sections at the bottom of the page, ensuring
  auto-archiving of sections functions correctly on the feedback page.
- Resolved issue where the app failed to clear cookies upon logout.

## v5.0.1

Same as v5.0.0 except this fixes some R8 rules to ensure that the release
variants of the app work as intended.

## v5.0.0

### What's Changed

- Redesigned the map feature to **replace Mapbox with the osmdroid library**.
  Key elements like pin visualization and user-centered display are still
  included in this redesign. This is done to guard against possible misuse of
  the Mapbox token and, more crucially, to keep the app from becoming dependent
  on a service that charges for usage but offers a free tier.

  With this change, the app retrieves the map tiles from [Wikimedia maps](https://maps.wikimedia.org).
- Add the ability to **export locations of nearby missing pictures in GPX and
  KML formats**. This allows users to browse the locations with desired radius
  for offline use in their favourite map apps like OsmAnd or Maps.me, enhancing
  accessibility  and offline functionality.
- **Limited the uploads via the custom image picker** to a maximum of 20.
- Added two menu choices for **transparent image backgrounds**, giving users the
  option of either a black or white background, increasing adaptability to
  various theme settings.

  User customization option has been provided with the
  ability to save background color selections permanently on a per image basis.
- Implemented functionality to **automatically resume uploads** that become
  stuck due to app termination or device reboot.
- Added a **compass arrow in the Nearby banner** shown in the "Contributions"
  screen to guide users towards the nearest item, thus providing the missing
  directional cues. The arrow dynamically adjusts based on device rotation,
  aligning with the calculated bearing towards the  target location. Further,
  the distance and direction are updated as the user moves.
- Implemented **voice input feature** for caption and description fields,
  enabling users to dictate text directly into these fields.
- Improved various flows in the app to **redirect users to the login page** and
  display a  persistent message **if their session becomes invalid** due to a
  password  change, enhancing user guidance and security measures.

### Revamps and refactorings

- **Revamped initial upload screen layout and the description edit screen layout**
  for enhanced user experience and ensuring better symmetry in the design.
- **Replaced Butterknife with ViewBinding** in various places of the app.
- Transferred essential code from **the redundant data-client module** to the
  main Commons app code, enabling its integration and facilitating the removal
  of the redundant module. Further, convert various parts of the code to Kotlin.
- **Revamped the various location permission flows** to ensure consistency for
  the sake of a better user experience.

### Bug fixes and various changes

- Resolved an issue where paused uploads that were subsequently cancelled were
  still being uploaded.
- Fixed an issue where some user information such as upload count were not
  displayed in the "Contributions" and "Profile" screens.
- Fixed the long-standing broken *"Picture of the Day" widget* to restore its
  usability.
- Resolved an issue where some categories were hidden at the top of Upload
  Wizard suggestions.
- Resolved an issue where there was a grey empty screen at Upload wizard when
  the app was denied the files permission.
- Implemented logic to bypass media in Peer Review if the current reviewer is
  also the user who uploaded the media.
- Corrected arrow image behaviour in the first upload screen: now displays down
  arrow when details card is fully visible, aligning with expected user
  interaction.
- Updated app icon to improve visibility and recognition on F-Droid.
- Fixed issue causing all pictures to disappear and activity to reload fully in
  the custom image selector after marking a picture as 'not for  upload', now
  ensuring only the selected picture is removed as expected.

What's listed here is only a subset of all the changes. Check the full-list of
the changes in [this link](https://github.com/commons-app/apps-android-commons/compare/v4.2.1...v5.0.0).
Alternatively, checkout [this release on GitHub releases page](https://github.com/commons-app/apps-android-commons/releases/tag/v5.0.0)
for an exhaustive list of changes and the various contributors who contributed the same.

## v4.2.1

- Provide the ability to edit an image to losslessly rotate it while uploading
- Fix a bug in v4.2.0 where the nearby places were not loading
- Fix a bug where editing depictions was showing a progress bar indefinitely
- In the upload screen, use different map icons to indicate if image is being uploaded with location
  metadata
- For nearby uploads, it is no longer possible to deselect the item's category and depiction
- The Mapbox account key used by the app has been changed
- Category search now shows exact matches without any discrepancies
- Various bug and crash fixes

## v4.2.0
- Dark mode colour improvements
- Enhancements done to address location metadata loss including the metadata loss that occurs in
  latest Android versions
- Enhancements done to address the issue where uploads get stuck in queued state
- Fix the inability to upload via the in-app camera option
- Provide the ability to optionally include location metadata for in-app camera uploads in case the
  device camera app does not provide location metadata
- Use geo location URL that works consistently across all map applications
- Fix crash when clicking on location target icon while trying to edit the location of an upload
- Fix crash that occurs randomly while returning to the app after leaving it in the background
- Fix crash in Sign up activity on Android version 5.0 and 5.1
- Android 13 compatibility changes

## v4.1.0
- Location of pictures uploaded via custom picture selector are now recognized
- Improvements to the custom picture selector
- Ensure the WLM pictures are associated with the correct templates for each year
- Only show pictures uploaded via app in peer review
- Improve the variety of images show in peer review
- Allow going to current location in location edit dialog while uploading a picture
- Switch to using MapLibre instead of Mapbox and thereby disable telemetry sent to Mapbox
- Fixed various bugs

## v4.0.5
- Bumped min SDK to 29 to try and solve Google policy issue
- Reverted dialog
- Note: This encompasses versions 1031, 1032, and 1033, due to the Play Store's requirements to overwrite all the tracks with a post-fix version (otherwise no single track can be published)

## v4.0.4
- Added dialog for Google's location policy

## v4.0.3
- Added "Report" button for Google UGC policy

## v4.0.2
- Fixed bug with wrong dates taken from EXIF
- Fixed various crashes

## v4.0.1
- Fixed bug with no browser found
- Updated Mapbox SDK to fix hamburger crash

## v4.0.0
- Added map showing nearby Commons pictures
- Added custom SPARQL queries
- Added user profiles
- Added custom picture selector
- Various bugfixes
- Updated target SDK to 30

## v3.1.1
- Optimized Nearby query
- Added Sweden's property for WLM 2021
- Added link to wiki explaining how to contribute to WLM through app
- Fixed various bugs and crashes

## v3.1.0
- Added Wiki Loves Monuments integration for WLM 2021

## v3.0.2
- Fixed crash when uploading high res image
- Fixed crash when viewing images in Explore

## v3.0.1
- Pre-fill desc in Nearby uploads with Wikidata item's label + description
- Improved ACRA crash reporting
- Fixed various crashes

## v3.0.0
- Added Structured Data to upload workflow, users can now add depicts
- Added Leaderboard in Achievements screen
- Added to-do system for images with no categories/descriptions or with associated Wikipedia articles that have no pictures
- Users can now modify and add categories to their uploads from the media details view
- New UI for main screen
- Limited connection mode added, users can now pause and resume uploads

## v2.13.1
- Added OpenStreetMap attribution
- Fixed various crashes
- Fixed SQLite error in Nearby map
- Fixed issue with Nearby uploads not being associated with Wikidata p18

## v2.13.0
- New media details UI, ability to zoom and pan around image
- Added suggestions for a place that needs photos if user uploads a photo that is near one of them
- Modifications and fixes to Nearby filters based on user feedback
- Multiple crash and bug fixes

## v2.12.3
- Fixed issue with EXIF data, including coords, being removed from uploads

## v2.12.2
- Fixed crash on startup 

## v2.12.1
- Fixed issue with Nearby loading in wrong location
- Various crash fixes

## v2.12.0
- Completed codebase overhaul 
- Added filters for place type and place state to Nearby
- Switched to using new data client library, aimed at fixing failed uploads
- Fixed 2FA not working
- Fixed issues with upload date and deletion notifications

## v2.11.0
- Refactored upload process, explore/media details, and peer review to use MVP architecture
- Refactored all AsyncTasks to use RxAndroid
- Partial migration to Retrofit
- Allow users to remove EXIF tags from their uploads if desired
- Multiple crash and bug fixes

## v2.10.2
- Fixed remaining issues with date image taken
- Fixed database crash

## v2.10.1
- Fixed "stuck before category selection screen" bug
- Fixed notification taps
- Fixed crash while uploading images
- Fixed crash while loading contributions
- Fixed sporadic issue with date image was taken

## v2.10.0
- Added option to search for places that need pictures in any location
- Added coordinate check for images submitted via Nearby
- Added news about ongoing campaigns
- Easy retry for failed uploads
- Javadocs for Nearby package
- Optimized Nearby query for faster loading
- Allow users to dismiss notifications
- Various bugfixes for Explore, Notifications and Nearby
- Fixed uploads getting stuck in "receiving shared content" phase
- Fixed empty notifications bell icon in main screen

## v2.9.0
- New main screen UI with Nearby tab
- New upload UI and flow
- Multiple uploads
- Send Log File revamp
- Fixed issues with wrong "image taken" date
- Fixed default zoom level in Nearby map
- Incremented target SDK to 27, with corresponding notification channel fix
- Removed several redundant libraries to reduce bloat

## v2.8.5
- Fixed issues with sporadic upload failures due to wrong mimeType

## v2.8.4
- Hotfix for constant upload crashes for Oreo users

## v2.8.3
- Fixed issues with session tokens not being cleared in 2FA, which should reduce p18 edit failures as well
- Fixed crash caused by bug in fetching revert count
- Fixed crash potentially caused by Traceur library

## v2.8.2
- Fixed bug with uploads sent via Share being given .jpeg extensions and overwriting files of the same name

## v2.8.1
- Fixed bug with category edits not being sent to server

## v2.8.0
- Fixed failed uploads by modifying auth token
- Fixed crashes during upload by storing file temporarily
- Added automatic Wikidata p18 edits upon Nearby upload
- Added Explore feature to browse other Commons images, including featured images
- Added Achievements feature to see current level and upload stats
- Added quiz for users with high deletion rates
- Added first run tutorial for Nearby
- Various small improvements to ShareActivity UI

## v2.7.2
- Modified subtext for "automatically get current location" setting to emphasize that it will reveal user's location

## v2.7.1
- Fixed UI and permission issues with Nearby
- Fixed issue with My Recent Uploads being empty
- Fixed blank category issue when uploading directly from Nearby
- Various crash fixes

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
