To prevent sad pandas, please check the following items before making a release:

- Make sure that the APP POINTS TO COMMONS. This is very important, and should be
  ensured. The variables to check are `API_URL`, `IMAGE_URL_BASE`, `HOME_URL` and
  `EVENTLOG_WIKI`, in `CommonsApplication.java`. Preferred way to do this is to have
  a staved `git stash` that applies these, and then apply them (and make a branch)
  before pushing them out
- Check for database schema migrations. If you modified the `DATABASE_VERSION` in
  `DBOpenHelper`, ensure that you have appropriately written recursive migrations
  in your appropriate `onUpgrade` methods. **Test** this before pushing out! You
  only need to test from the last database version, so should not be that hard!
