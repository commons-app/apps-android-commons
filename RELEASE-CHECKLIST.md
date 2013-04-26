To prevent sad pandas, please check the following items before making a release:

- Make sure that the APP POINTS TO COMMONS. This is very important, and should be
  ensured. The variables to check are `API_URL`, `IMAGE_URL_BASE`, `HOME_URL` and
  `EVENTLOG_WIKI`, in `CommonsApplication.java`. There is a patch for this named
  `upload-to-commons.patch` in the root directory
- Check for database schema migrations. If you modified the `DATABASE_VERSION` in
  `DBOpenHelper`, ensure that you have appropriately written recursive migrations
  in your appropriate `onUpgrade` methods. **Test** this before pushing out! You
  only need to test from the last database version, so should not be that hard!
  Do this on the final signed APK. You can use `adb install -r <apk>` to install
  it different versions of it.
- Bump the version numbers in AndroidManifest.xml
