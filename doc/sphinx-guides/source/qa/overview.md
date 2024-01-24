# Overview

```{contents} Contents:
:local: 
:depth: 3
```

## Introduction

This guide describes the testing process used by QA at IQSS and provides a reference for others filling in for that role. Please note that many variations are possible, and the main thing is to catch bugs and provide a good quality product to the user community.

## Workflow

The basic workflow is as follows. Bugs or feature requests are submitted to GitHub by the community or by team members as [issues](https://github.com/IQSS/dataverse/issues). These issues are prioritized and added to a two-week sprint that is reflected on the GitHub {ref}`kanban-board`. As developers work on these issues, a GitHub branch is produced, code is contributed, and a pull request is made to merge these new changes back into the common {ref}`develop branch <develop-branch>` and ultimately released as part of the product.

Before a pull request is moved to QA, it must be reviewed by a member of the development team from a coding perspective, and it must pass automated tests. There it is tested manually, exercising the UI (using three common browsers) and any business logic it implements.  

Depending on whether the code modifies existing code or is completely new, a smoke test of core functionality is performed and some basic regression testing of modified or related code is performed. Any documentation provided is used to understand the feature and any assertions made in that documentation are tested. Once this passes and any bugs that are found are corrected, and the automated tests are confirmed to be passing, the PR is merged into the develop branch, the PR is closed, and the branch is deleted (if it is local). At this point, the PR moves from the QA column automatically into the Merged column (where it might be discussed at the next standup) and the process repeats with the next PR until it is decided to {doc}`make a release </developers/making-releases>`.

The complete suggested workflow can be found at {doc}`qa-workflow`.

## Tips and Tricks

- Start testing simply, with the most obvious test. You don’t need to know all your tests upfront. As you gain comfort and understanding of how it works, try more tests until you are done. If it is a complex feature, jot down your tests in an outline format, some beforehand as a guide, and some after as things occur to you. Save the doc in a testing folder (on Google Drive). This potentially will help with future testing.
- When in doubt, ask someone. If you are confused about how something is working, it may be something you have missed, or it could be a documentation issue, or it could be a bug! Talk to the code reviewer and the contributor/developer for their opinion and advice.
- Always tail the server.log file while testing. Open a terminal window to the test instance and `tail -F server.log`. This helps you get a real-time sense of what the server is doing when you interact with the application and makes it easier to identify any stack trace on failure.
- When overloaded, QA the simple pull requests first to reduce the queue. It gives you a mental boost to complete something and reduces the perception of the amount of work still to be done.
- When testing a bug fix, try reproducing the bug on the demo server before testing the fix. That way you know you are taking the correct steps to verify that the fix worked.
- When testing an optional feature that requires configuration, do a smoke test without the feature configured and then with it configured. That way you know that folks using the standard config are unaffected by the option if they choose not to configure it.
- Back up your DB before applying an irreversible DB update when you are using a persistent/reusable platform. Just in case it fails, and you need to carry on testing something else you can use the backup.

## Release Cadence and Sprints

A release likely spans multiple two-week sprints. Each sprint represents the priorities for that time and is sized so that the team can reasonably complete most of the work on time. This is a goal to help with planning, it is not a strict requirement. Some issues from the previous sprint may remain and likely be included in the next sprint but occasionally may be deprioritized and deferred to another time.

The decision to make a release can be based on the time since the last release, some important feature needed by the community or contractual deadline, or some other logical reason to package the work completed into a named release and posted to the releases section on GitHub.

## Test API

The API test suite is added to and maintained by development. (See {doc}`/developers/testing` in the Developer Guide.) It is generally advisable for code contributors to add API tests when adding new functionality. The approach here is one of code coverage: exercise as much of the code base's code paths as possible, every time to catch bugs. 

This type of approach is often used to give contributing developers confidence that their code didn’t introduce any obvious, major issues and is run on each commit. Since it is a broad set of tests, it is not clear whether any specific, conceivable test is run but it does add a lot of confidence that the code base is functioning due to its reach and consistency. (See {doc}`/qa/test-automation` in the Developer Guide.)

## Making a Release

See {doc}`/developers/making-releases` in the Developer Guide.
