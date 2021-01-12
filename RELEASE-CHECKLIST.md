To prevent sad pandas, please check the following items before making a release:

- Make sure that the APP POINTS TO COMMONS. This is very important, and should be
  ensured. The variables to check are `API_URL`, `IMAGE_URL_BASE`, `HOME_URL` and
  `EVENTLOG_WIKI`, in `CommonsApplication.java`. There is a patch for this named
  `upload-to-commons.patch` in the root directory.
- Check for database schema migrations. If you modified the `DATABASE_VERSION` in
  `DBOpenHelper`, ensure that you have appropriately written recursive migrations
  in your appropriate `onUpgrade` methods. **Test** this before pushing out! You
  only need to test from the last database version, so should not be that hard!
  Do this on the final signed APK. You can use `adb install -r <apk>` to install
  it different versions of it.
- Bump the version numbers in `AndroidManifest.xml`. `versionCode` is monotonically
  increasing integers. `versionName` is descriptive string for this version.
- Add the new release to the `CHANGELOG`. Make sure that the `CHANGELOG` additions
  and the `AndroidManifest.xml` additions go together on the same commit.
- Add an appropriately named git tag, such as `v1.0beta8`. Don't forget to push
  it to the repo :)
- Upload the new APK to the store using the dev console. Also modify the Changelog
  in the store listing to contain changes for the last two releases. 
- Email mobile-l with release announcement - short description of major new features
  and then a copy of the CHANGELOG. Forward that to commons-l and wikitech-l with
  a pithy note about how nobody reads mobile-l.
- Find someone with write access to `dumps.wikimedia.org` and get them to put the
  signed APK at `https://dumps.wikimedia.org/android/'. Add a link to the
  [Release History page][2].
- Respond to users at the [Feedback page on Commons][1].

[1]: http://commons.wikimedia.org/wiki/Commons_talk:Mobile_app
[2]: https://www.mediawiki.org/wiki/Mobile/Release_history#Commons
