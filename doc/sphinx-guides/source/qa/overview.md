# Overview

```{contents} Contents:
:local: 
:depth: 3
```

## Introduction

This guide describes the testing process used by QA at IQSS and provides a reference for others filling in for that role. Please note that many variations are possible, and the main thing is to catch bugs and provide a good quality product to the user community.

## Workflow

The basic workflow is as follows. Bugs or feature requests are submitted to GitHub by the community or by team members as issues. These issues are prioritized and added to a two-week sprint that is reflected on the GitHub {ref}`kanban-board`. As developers work on these issues, a GitHub branch is produced, code is contributed, and a pull request is made to merge these new changes back into the common {ref}`develop branch <develop-branch>` and ultimately released as part of the product. Before a pull request is moved to QA, it must be reviewed by a member of the development team from a coding perspective, and it must pass automated tests. There it is tested manually, exercising the UI (using three common browsers) and any business logic it implements.  Depending on whether the code modifies existing code or is completely new, a smoke test of core functionality is performed and some basic regression testing of modified or related code is performed. Any documentation provided is used to understand the feature and any assertions made in that documentation are tested. Once this passes and any bugs that are found are corrected, and the automated tests are confirmed to be passing, the PR is merged into the develop, the PR is closed, and the branch is deleted (if it is local). At this point, the PR moves from the QA column automatically into the Done column and the process repeats with the next PR until it is decided to {doc}`make a release </developers/making-releases>`.

## Release Cadence and Sprints

A release likely spans multiple two-week sprints. Each sprint represents the priorities for that time and is sized so that the team can reasonably complete most of the work on time. This is a goal to help with planning, it is not a strict requirement. Some issues from the previous sprint may remain and likely be included in the next sprint but occasionally may be deprioritized and deferred to another time.

The decision to make a release can be based on the time since the last release, some important feature needed by the community or contractual deadline, or some other logical reason to package the work completed into a named release and posted to the releases section on GitHub.

## Performance Testing and Deployment

The final testing activity before producing a release is performance testing. This could be done throughout the release cycle but since it is time-consuming it is done once near the end. Using a load-generating tool named {ref}`Locust <locust>`, it loads the statistically most loaded pages, according to Google Analytics, that is 50% homepage and 50% some type of dataset page. Since dataset page weight also varies by the number of files, a selection of about 10 datasets with varying file counts is used. The pages are called randomly as a guest user with increasing levels of user load, from 1 user to 250 users. Typical daily loads in production are around the 50-user level. Though the simulated user level does have a modest amount of random think time before repeated calls, from 5-20 seconds, it is not a real-world load so direct comparisons to production are not reliable. Instead, we compare performance to prior versions of the product, and based on how that performed in production we have some idea whether this might be similar in performance or whether there is some undetected issue that appears under load, such as inefficient or too many DB queries per page.

Once the performance has been tested and recorded in a [Google spreadsheet](https://docs.google.com/spreadsheets/d/1lwPlifvgu3-X_6xLwq6Zr6sCOervr1mV_InHIWjh5KA/edit?usp=sharing) for this proposed version, the release will be prepared and posted.

## Making a Release

See {doc}`/developers/making-releases` in the Developer Guide.
