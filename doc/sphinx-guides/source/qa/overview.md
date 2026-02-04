# Overview

```{contents} Contents:
:local: 
:depth: 3
```

## Introduction

This guide describes the testing process used by QA at IQSS and provides a reference for others filling in for that role. Please note that many variations are possible, and the main thing is to catch bugs and provide a good quality product to the user community.

## Workflow

Here is a brief description of our workflow: 

### Issue Submission and Prioritization: 
- Members of the community or the development team submit bugs or request features through GitHub as [Issues](https://github.com/IQSS/dataverse/issues).
- These Issues are prioritized and added to a two-week-long sprint that can be tracked on the {ref}`kanban-board`.

### Development Process:
- Developers will work on a solution on a separate branch
- Once a developer completes their work, they submit a [Pull Request](https://github.com/IQSS/dataverse/pulls) (PR).
- The PR is reviewed by a developer from the team.
- During the review, the reviewer may suggest coding or documentation changes to the original developer.

### Quality Assurance (QA) Testing:
- The QA tester performs a smoke test of core functionality and regression testing.
- Documentation is used to understand the feature and validate any assertions made.
- If no documentation is provided in the PR, the tester may refer to the original bug report to determine the desired outcome of the changes.
- Once the branch is assumed to be safe, it is merged into the develop branch.

### Final Steps:
- The PR and the Issue are closed and assigned the “merged” status.
- It is good practice to delete the branch if it is local.
- The content from the PR becomes part of the codebase for {doc}`future releases </developers/making-releases>`.

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
