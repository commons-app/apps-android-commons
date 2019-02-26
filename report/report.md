# Report for assignment 4

This report is written by KTH  students (Group 9) taking the course DD2480 Software Engineering, assignment 4.

## Project

**Name:**  `apps-android-commons`

**URL:**  *https://github.com/commons-app/apps-android-commons*

**Project Description:**
*The Wikimedia Commons Android app allows users to upload pictures from their Android phone/tablet to Wikimedia Commons.*

## Selected issue

**Title:** Refactor high complexity classes

**URL:** *https://github.com/commons-app/apps-android-commons/issues/888*

**Issue description:**
*Address the very high cyclomatic complexity we are seeing from Codacy by breaking up Activities & Fragments according to one of the well-respected architectural patterns (MVP / MVVM / etc).*

## Onboarding experience

The project provided a [quick start guide for developers](https://github.com/commons-app/apps-android-commons/wiki/Quick-start-guide-for-Developers) explaining how to run the code. However, the given guide did not work for some of our group members. 

When opening the `Android studio`  some of our group members were prompted to automatically update some programs which would cause the project not to work properly. This could have been documented in the quick start guide.

## Requirements affected by functionality being refactored

Most of the group memebers focused on refactoring by reducing complexity. This in turn was done by lowering the complexity of some high complexity functions. The complexity in this case would refer to the CCN of the particular function as well as its average class CCN. 


## Existing test cases relating to refactored code

Some group members did not find particular unit test cases for the functions they refactored. This could be problematic if the refactoring would somehow change any functionality aspect of a function. 

## The refactoring carried out

The UML diagrams are attached as .jpg files in the UML folder under report map in the project repository. The refactoring carried out aimed to reduce CCN measurement of particular functions with relatively high CCN. The refactoring was done by a variety of techniques such as splitting up a function into several functions some of which are helper functions to the main function being refactored. 

## Test logs

Initially all the tests in the project passed. After every refactoring merge, the tests were reexecuted locally to make sure that they still pass once the refactoring has been introduced to the code. 

## Effort Spent

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

The issue that our group focused on required high complexity classes to be refactored in terms of their CCN measurements. Our group then addressed this issue by particularly reducing CCN of high complexity functions. The project is a very large project and there could be a lot more refactoring that could be done and CCN to be reduced. The project team members tried to address as much complexity as possible given the limited time-span. 





