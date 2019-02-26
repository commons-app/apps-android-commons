# Report for assignment 4

This report is written by several students from KTH in DD2480 Software Engineering for assignment 4.

NOTE: *The tasks for the assignment is described at the bottom of this report.*

## Project

**Name:**  `apps-android-commons`

**URL:**  *https://github.com/commons-app/apps-android-commons*

**Project Description:**
*The Wikimedia Commons Android app allows users to upload pictures from their Android phone/tablet to Wikimedia Commons.*

## Selected issue

**Title:** Refactor high complexity classes

**URL:** https://github.com/commons-app/apps-android-commons/issues/888

**Issue description:**
*Address the very high cyclomatic complexity we are seeing from Codacy by breaking up Activities & Fragments according to one of the well-respected architectural patterns (MVP / MVVM / etc)*

## Onboarding experience

The project provided a [quick start guide for developers](https://github.com/commons-app/apps-android-commons/wiki/Quick-start-guide-for-Developers) explaining how to run the code. However, the given guide did not work for some of our group members. 

When opening the `Android studio`  some of our group members were prompted to automatically update some programs which would cause the project not to work properly. This could have been documented in the quick start guide.

`Did it build as documented?
(See the assignment for details; if everything works out of the box,
there is no need to write much here.)`

## Requirements affected by functionality being refactored



## Existing test cases relating to refactored code



## The refactoring carried out

`(Link to) a UML diagram and its description`

## Test logs

Initially all the tests in the project passed.

## Effort Spent

*Here each group member describes how they spent their time during the project.*

### Jakob

### Jenny

### Fredrik

I spent roughly 2 hours on installing software needed for the project. The Android studio program needed were not in the standard repository of my Linux distribution. Getting the project to run on my computer also took some time. Following the quick guide given in the project did not work for me.

Finding suitable functions to refactor took long time because many of the functions were not so easy to understand. Making it difficult to separate the function into different parts to reduce complexity. Also some of the functions found were already taken by other group members. The actual refactoring barely took any time at all.

I assumed my functions had some kind of testing, which they did not have. Searching for non existent functions.

### Shiva
Initially I spent about 4-5 hours on searching through different projects that we could take up. It is possible that I even underestimated this time spent since the project that we chose for the previous lab ended up being not very appropriate for the task which made the task more difficult for us. So we tried to be very careful with project choice this time which meant a lot of searching through projects, familiarizing myself with the overall project and then deciding whether it could be an appropriate alternative for the task. The process of project choice and familiarizing ourselves with the projects were all being done remotely so we did not have any group meetings and we did all the communication through Slack. Once we did choose a project, the built instructions for me took quite some time. The instructions given by the project startup guide were actually not useful for me at all. Because I didn't do it their way and I did a different way to build the project which then meant that I had to figure out the small details of the project configuration process until successful build by myself. This then also took quite  a while. The reason I did it “my” way was because I was having trouble getting it to work when i followed the project’s startup guide. After successful build the rest of the time was spent on finding functions/classes that can be refactored further into having lower complexity as measured by their CCN, refactoring them and committing them to git. My commits then include the code refactored as well as the corresponding UML. 

### Philip

## Overall experience

## Tasks

1. *Identify an open source project with tasks where code requires 
   refactoring, by searching for such issues in the issue tracked (aka bug 
   tracker, ticket tracker). See "Selection criteria" below.*
2. *Identify an issue you will work on. The issue should be open, and have 
   no assignee (or you should get in touch with the assignee to ensure that
   the work has not been done yet).*
3. *Set up the project on your system(s). How good is the "onboarding" documentation?*
   1. *How easily can you build the project? Briefly describe if everything worked as documented or not:*
      1. *Did you have to install a lot of additional tools to build the software? Were those tools well documented?*
      2. *Were other components installed automatically by the build script?*
      3. *Did the build conclude automatically without errors?*
      4. *How well do examples and tests run on your system(s)?*
   2. Do you plan to continue or choose another project?
4. *Register the task you are working on [this spreadsheet (Links to an external site.)Links to an external site.](https://docs.google.com/spreadsheets/d/1dGXxvCLFQz4XM0fzIdtPLJf0G4fd2-mczKgSjcjLCrQ/edit?usp=sharing). Create an account on the issue tracker of the project, and register yourself as an assignee of that task.
   **Note:** It is OK if multiple groups choose the same project, but they have to 
   choose different issues/tasks. Maximum is three groups per project (one 
   group per issue!).*
5. *Identify requirements of the functions to be refactored. If the 
   requirements are not documented yet, try to describe them based on code 
   reviews and existing test cases. Create a project plan for testing these
   requirements, and refactoring the code.
   Footnote: If you choose a task where the goal is to refactor tests, add mechanisms that ensure that you have some kind of validation of the test behavior. This could 
   include coverage, test output, or mutations detected by the tests.*
6. *Analyze existing test cases that relate to code being refactored. Do 
   they contain good assertions? Do the assertions cover the requirements 
   as you understand them? If necessary, enhance existing unit tests with 
   properties derived from work done above, or write additional tests.*
7. *Carry out the unit tests, make sure they succeed (or fix the software if the fail justifiably). **Keep copies of the test logs.**
   If a test passes, make sure it was actually executed. You can use code 
   coverage measurement (especially if this is available in the project you
   are using), or temporarily inject a fault (mutation) in the code, and 
   see that at least one test fails.
   If you found a bug that is harder to fix than expected, document the issue (and leave an entry in the issue tracker), and attempt another refactoring, perhaps in a different 
   project.*
8. *Carry out the refactoring itself.*
9. *Run the tests on the new code. Test verdicts on the original and new 
   code should be the same. [Unless there is a good reason, such as a bug 
   fix that makes a previously failing test now pass.]*
10. *Create a patch in a format that is acceptable to the project.*
11. *Document your experience. **Keep track of how your team spends its time.** 
    If you were not able to finish all tasks, how much progress have you made?
    How much time do you think you would need to complete the task?
    If the process turned out to be less difficult than expected, attempt another refactoring (not necessarily in the same project).*




