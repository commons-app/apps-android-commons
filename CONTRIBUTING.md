Thaks for considering contributing to this project! Here are some
guidelines for people who want to contribute their code to this
software.

### Make separate commits for logically separate changes.

Always make a commit with complete commit message for a logically
separate change. It is a good discipline. For details about how to
write a good commit message, see the [Describe your changes well](#Describe your changes well).

Give an explanation for the change(s) that is detailed enough so
that people can judge if it is good thing to do, without reading
the actual patch text to determine how well the code does what
the explanation promises to do.

If your description starts to get too long, that's a sign that you
probably need to split up your commit to finer grained pieces.
That being said, commit messages which plainly describe the things that
help reviewers check the patch, and future maintainers understand
the code, are the most beautiful ones. Descriptions that summarize
the point in the subject well, and describe the motivation for the
change, the approach taken by the change, and if relevant how this
differs substantially from the prior version, are all good things
to have.

### Describe your changes well.

The first line of the commit message should be a short description (50
characters is the soft limit, see the DISCUSSION section found in the
man page of git commit(1)), and should skip the full stop.  It is also
good in most cases to prefix the first line with "area: " where the area
is a filename or identifier for the general area of the code being modified,
e.g.

  * readme: fix a broken link
  * Hygiene: add Nullable annotations to reduce risk of NPE

It's customary to start the remainder of the first line after "area: "
with a lower-case letter. E.g. "doc: clarify...", not "doc:
Clarify...", or "githooks.txt: improve...", not "githooks.txt:
Improve...".

The body should provide a meaningful commit message, which:

  * explains the problem the change tries to solve, i.e. what is wrong
    with the current code without the change.

  * justifies the way the change solves the problem, i.e. why the
    result with the change is better.

  * alternate solutions considered but discarded, if any.

Describe your changes in imperative mood, e.g. "make xyzzy do frotz"
instead of "[This change] makes xyzzy do frotz" or "[I] changed xyzzy
to do frotz", as if you are giving orders to the codebase to change
its behavior.  Try to make sure your explanation can be understood
without external resources. Instead of giving a URL to a Pull Request,
summarize the relevant points of the discussion.

If you want to reference a previous commit in the history of a stable
branch, use the format "abbreviated sha1 (subject, date)",
with the subject enclosed in a pair of double-quotes, like this:

    Commit f86a374 ("pack-bitmap.c: fix a memleak", 2015-03-30)
    noticed that ...

The "Copy commit summary" command of gitk can be used to obtain this
format, or this invocation of "git show":

    git show -s --date=short --pretty='format:%h ("%s", %ad)' <commit>


### Write tests for your code

Make sure that you have tests for the bug you are fixing.

When adding a new feature, make sure that you have new tests to show
the feature triggers the new behavior when it should, and to show the
feature does not trigger when it shouldn't. After any code change, make
sure that the entire test suite passes.

If you have an account at GitHub (and you can get one for free to work
on open source projects), you can use their Travis CI integration to
test your changes.


### Update the Wiki page

Do not forget to update the [Wiki page](https://github.com/commons-app/apps-android-commons/wiki)
on GitHub to describe the updated behavior and make sure that the resulting
Wiki page doesn't become stale as a consequence of your change.
