==================
Branching Strategy
==================

Goals
-----

The goals of the Dataverse branching strategy are twofold:

- have developers "stay together" in the same release branch (i.e. 4.0.1), resolving conflicts early

- allow for concurrent development in multiple branches, for example:

  - hot fix branch created from 4.0 tag (i.e. 4.0-patch.1)
  - bug fixes in an unreleased 4.0.1 release branch found in QA
  - feature development in an upcoming 4.0.2 release branch

Release branches that match milestones and future version numbers (i.e. 4.0.1, 4.0.2) are used to achieve these goals. Think of release branches as trains. If you miss the 4.0.1 train, hopefully you'll catch the 4.0.2! The goal is always to get the best code into each release.

Persistent Branches
-------------------

The "master" branch is the only persistent branch. Commits should never be made directly to master. Commits are only made to release branches. Release branches are merged in "master" just before tagging per :doc:`/developers/making-releases`.

Release Branches
----------------

When developers feel the code in a release branch (i.e. 4.0.1) is ready, it is passed to QA. At this point feature development in that release branch (i.e. 4.0.1) is frozen and a new release branch (i.e. 4.0.2) is created from that commit. The frozen release branch is sent to QA for testing.

Feature Development
-------------------

Developers who have push access to https://github.com/IQSS/dataverse are welcome to push commits directly to the branch corresponding to the milestone the issue tracking the feature has been assigned. For example, if you are working on https://github.com/IQSS/dataverse/issues/2088 and the issue has been assigned to milestone 4.0.1, you are welcome to push directly to the 4.0.1 branch.

Developers who do not have push access should first read https://github.com/IQSS/dataverse/blob/master/CONTRIBUTING.md for general guidelines about contributing code and contacting developers who have the ability to (hopefully!) merge in your contribution. You will likely be advised to submit a pull request against the current release branch that is not yet frozen. For example, if 4.0.1 is frozen (i.e. in QA) the pull request should be made against 4.0.2.

Fixing Bugs in Unreleased Code
------------------------------

Bugs in the release branch currently in QA (i.e. 4.0.1) will be fixed in that branch referencing an issue number. Assuming the fix is good and passes QA, the release branch in QA (i.e. 4.0.1) will be merged into the release branch currently under feature development (i.e. 4.0.2) to get the bug fix.

Fixing Bugs in Released code
----------------------------

Bugs found in released code will (like features) be assigned to milestones visible at https://github.com/IQSS/dataverse/milestones
