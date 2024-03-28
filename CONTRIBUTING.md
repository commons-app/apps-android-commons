Thanks for considering to contribute to this project! A few guidelines for
people who want to contribute their code to this software are documented in
[this project's Wiki](https://github.com/commons-app/apps-android-commons/wiki/Contributing-Guidelines).
If you're not sure where to start head on
to [this wiki page](https://github.com/commons-app/apps-android-commons/wiki/Volunteers-welcome!).

Here's a gist of the guidelines,

1. Make separate commits for logically separate changes

2. Describe your changes well in the commit message

The first line of the commit message should be a short description of what has
changed. It is also good to prefix the first line with "area: " where the "area"
is a filename or identifier for the general area of the code being modified.
The body should provide a meaningful commit message.

1. Write Javadocs

   We require contributors to include Javadocs for all new methods and classes
   submitted via PRs (after 1 May 2018). This is aimed at making it easier for
   new contributors to dive into our codebase, especially those who are new to
   Android development. A few things to note:

    - This should not replace the need for code that is easily-readable in
      and of itself
    - Please make sure that your Javadocs are reasonably descriptive, not just
      a copy of the method name
    - Please do not use `@author` tags - we aim for collective code ownership,
      and if needed, Git allows us to see who wrote something without needing
      to add these tags (`git blame`)

2. Write tests for your code (if possible)

3. Make sure the Wiki pages don't become stale by updating them (if needed)

### Further reading

* [Importance of good commit messages](https://blog.oozou.com/commit-messages-matter-60309983c227?gi=c550a10d0f67)
