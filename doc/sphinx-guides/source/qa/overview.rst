Overview
========

.. contents:: |toctitle|
    :local:


Introduction
------------
This document describes the testing process used by QA at IQSS and provides a guide for others filling in for that role. Please note that many variations are possible, and the main thing is to catch bugs and provide a good quality product to the user community.

Workflow
--------
The basic workflow is bugs or feature requests are submitted to GitHub by the community or by team members as issues. These issues are prioritized and added to a two-week sprint that is reflected on the GitHub Kanban board. As developers work on these issues, a GitHub branch is produced, code is contributed, and a pull request is made to merge these new changes back into the common develop branch and ultimately released as part of the product. Before a pull request is merged it must be reviewed by a member of the development team from a coding perspective, it must pass automated integration tests before moving to QA. There it is tested manually, exercising the UI using three common browser types and any business logic it implements.  Depending on whether the code modifies existing code or is completely new, a smoke test of core functionality is performed and some basic regression testing of modified or related code is performed. Any documentation provided is used to understand the feature and any assertions are tested. Once this passes and any bugs that are found are corrected, the automated integration tests are confirmed to be passing, the PR is merged into development, the PR is closed, and the branch is deleted. At this point, the pr moves from the QA column automatically into the Done column and the process repeats with the next pr until it is decided to make a release.

Release Cadence and Sprints
---------------------------
A release likely spans multiple two-week sprints. Each sprint represents the priorities for that time and is sized so that the team can reasonably complete most of the work on time. This is a goal to help with planning, it is not a strict requirement. Some issues from the previous sprint may remain and likely be included in the next sprint but occasionally may be deprioritized and deferred to another time.

The decision to make a release can be based on the time since the last release, some important feature needed by the community or contractual deadline, or some other logical reason to package the work completed into a named release and posted to the releases section on GitHub.

Performance Testing and Deployment
----------------------------------
The final testing activity before producing a release is performance testing. This could be done throughout the release cycle but since it is time-consuming it is done once near the end. Using a load-generating tool named Locust, it loads the statistically most loaded pages, according to Google Analytics, that is 50% homepage and 50% some type of dataset page. Since dataset page weight also varies by the number of files, a selection of about 10 datasets with varying file counts is used. The pages are called randomly as a guest user with increasing levels of user load, from 1 user to 250 users. Typical daily loads in production are around the 50-user level. Though the simulated user level does have a modest amount of random think time before repeated calls, from 5-20 seconds (I believe), it is not a real-world load so direct comparisons to production are not reliable. Instead, we compare performance to prior versions of the product and based on how that performed in production we have some idea whether this might be similar in performance or whether there is some undetected issue that appears under load, such as inefficient or too many DB queries per page.

Once the performance has been tested and recorded in a Google spreadsheet for this proposed version, the release will be prepared and posted.

Preparing the release consists of writing and reviewing the release notes compiled from individual notes in PRs that have been merged for this release. A PR is made for the notes and merged. Next, increment the version numbers in certain code files, produce a PR with those changes, and merge that into the common development branch. Last, a PR is made to merge and develop into the master branch. Once that is merged a guide build with the new release version is made from the master branch. Last, a release war file is built from the master and an installer is built from the master branch and includes the newly built war file. 

Publishing the release consists of creating a new draft release on GitHub, posting the release notes, uploading the .war file and the installer .zip file, and any ancillary files used to configure this release. The latest link for the guides should be updated on the guides server to point to the newest version. Once that is all in place, specify the version name and the master branch at the top of the GitHub draft release and publish. This will tag the master branch with the version number and make the release notes and files available to the public.

Once released, post to Dataverse general about the release and when possible, deploy to demo and production.

