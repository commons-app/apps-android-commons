Thanks for considering to contribute to this project! A few guidelines for
people who want to contribute their code to this software are documented in
[this project's Wiki](https://github.com/commons-app/apps-android-commons/wiki/Contributing-Guidelines).
If you're not sure where to start head on to [this wiki page](https://github.com/commons-app/apps-android-commons/wiki/Volunteers-welcome!).

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

### Local Validation Before Submitting a Pull Request

Run the checks that are relevant to your changes before opening a pull request.
The commands below use the same Gradle wrapper and build variants used by the
pull request workflow.

#### Prerequisites

- Use JDK 17, which is the Java version configured by the project and used in
  GitHub Actions.
- Install the Android SDK and make API 36 available.
- Run Gradle through the repository wrapper (`./gradlew`), rather than relying
  on a separately installed Gradle version. The wrapper downloads the configured
  Gradle version if it is not already available.
- Ensure that `local.properties` exists in the repository root. The Gradle
  build reads it during configuration and reads `TEST_USER_NAME` and
  `TEST_USER_PASSWORD` from it to populate app build configuration. This local
  file is ignored by Git and must never be committed or published. Valid
  credentials are needed for login-based instrumentation tests, not for
  ordinary JVM unit tests or the other local validation commands below. Never
  commit or publish test credentials.

#### Recommended local checks

Choose checks relevant to your changes: run formatting and static analysis for
Kotlin changes, unit tests for changes that affect tested behavior, and Android
Lint for Android code or resource changes. The combined command below is a
convenient broader pre-PR check, not a requirement for every small,
documentation-only change.

| Command | What it checks |
| --- | --- |
| `./gradlew :app:ktlintCheck` | Kotlin formatting rules for files covered by the project's ktlint configuration. |
| `./gradlew :app:detekt` | Kotlin static analysis using the project's Detekt configuration and baseline. |
| `./gradlew :app:testBetaDebugUnitTest` | JVM unit tests for the Beta Debug variant. |
| `./gradlew :app:lintBetaDebug` | Android Lint for the Beta Debug variant. |

You can run the formatting and static-analysis checks together:

```sh
./gradlew :app:ktlintCheck :app:detekt --continue
```

For broader pre-PR validation, you can run all four checks in one Gradle
invocation:

```sh
./gradlew :app:ktlintCheck :app:detekt :app:testBetaDebugUnitTest :app:lintBetaDebug --continue
```

#### Optional build and coverage checks

These commands compile and package the corresponding debug APK. They do not
run the app or replace tests:

```sh
./gradlew :app:assembleBetaDebug
./gradlew :app:assembleProdDebug
```

To request the configured Beta Debug unit-test coverage task, use:

```sh
./gradlew -Pcoverage :app:testBetaDebugUnitTestCoverage
```

The task runs the unit tests and generates a coverage report when JaCoCo
execution data is available. Gradle may skip the report if that data is
unavailable; in that case, no report was produced.

#### Formatting fixes

`ktlintCheck` reports formatting violations without changing source files.
`ktlintFormat` applies formatting fixes and modifies source files:

```sh
./gradlew :app:ktlintFormat
```

Review the resulting Git diff before committing. Files listed in
`app/ktlint-legacy-files.txt` are intentionally excluded from ktlint for now;
eligible Kotlin files not on the list, including new files, are checked
automatically. Follow the repository's `.editorconfig` and fix new violations
rather than adding exclusions or changing rules to bypass them. After fixing a
legacy file, remove its path from the exclusion list so ktlint checks it again.

#### Understanding static-analysis results

Detekt uses `app/detekt-baseline.xml` to account for existing findings.
Address new findings in your changes; do not add findings to the baseline to
suppress them without justification. ktlint checks Kotlin files according to
the repository's configured rules and the intentional exclusions in
`app/ktlint-legacy-files.txt`.

#### How local checks relate to GitHub Actions

The pull request workflow currently validates string-resource XML, runs ktlint
and Detekt, runs Android Lint for Beta Debug, runs Beta Debug unit tests with
coverage, and assembles both Beta Debug and Prod Debug APKs. The XML validation
is an inline GitHub Actions check, not a repository-provided Gradle task.
Instrumentation tests are not currently active in pull request CI. Passing
local checks is useful, but does not guarantee that every CI check will pass.

#### Common problems

- If Gradle reports an incompatible Java version, check that it is using JDK
  17.
- If Android build tasks cannot find SDK components or report unaccepted
  licenses, check your local Android SDK setup and make API 36 available.
- If Gradle cannot find `local.properties`, restore your local configuration.
  Do not commit the file or include machine-specific configuration in your PR.
- If Gradle cannot download its distribution or dependencies, check your
  network connection and retry; a download failure is not by itself a code
  failure.
- Instrumentation tests need a connected Android device or emulator. Login-based
  flows also need valid test credentials configured locally; do not publish or
  commit them. These tests are separate from the JVM unit tests and are not
  currently run for pull requests.

#### Before opening a pull request

- Run the local checks relevant to your changes and review test failures and
  static-analysis findings.
- Run `git diff --check` and inspect `git diff`.
- Confirm your changes do not include credentials, local configuration,
  generated build outputs, or unrelated edits.

### Further reading

* [Importance of good commit messages](https://blog.oozou.com/commit-messages-matter-60309983c227?gi=c550a10d0f67)
