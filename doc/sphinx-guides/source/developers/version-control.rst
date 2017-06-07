==================
Version Control
==================

.. contents:: |toctitle|
	:local:

Branching Strategy
------------------

Goals
~~~~~

The goals of the Dataverse branching strategy are:

- allow for concurrent development
- only ship stable code

We follow a simplified "git flow" model described at http://nvie.com/posts/a-successful-git-branching-model/ involving a "master" branch, a "develop" branch, and feature branches such as "1234-bug-fix".

Branches
~~~~~~~~

The "master" Branch
*******************

The "`master <https://github.com/IQSS/dataverse/tree/master>`_" branch represents released versions of Dataverse. As mentioned in the :doc:`making-releases` section, at release time we update the master branch to include all the code for that release. Commits are never made directly to master. Rather, master is updated only when we merge code into it from the "develop" branch.

The "develop" Branch
********************

The "`develop <https://github.com/IQSS/dataverse>`_" branch represents code that was stable enough to merge from a "feature" branch (described below) and that will make it into the next release. Like master, commits are never made to the develop branch. The develop branch is where integration occurs. Your goal is have your code merged into the develop branch after it has been reviewed.

Feature Branches
****************

Feature branches are used for both developing features and fixing bugs. They are named after the GitHub issue they are meant to address, which means the branch should not be created until the GitHub issue exists. For example, if https://github.com/IQSS/dataverse/issues/1234 had a title of "Bug fix", you would name your branch "1234-bug-fix" or some other short "slug" with the issue number at the start.

Always create your feature branch from the latest code in develop, pulling if necessary. Push your feature branch either to your fork of Dataverse or to the main repo at IQSS if you have write access. Create a pull request based on the feature branch you pushed. As mentioned in https://github.com/IQSS/dataverse/blob/develop/CONTRIBUTING.md if you do not have access to advance your pull request into the "Code Review" column at https://waffle.io/IQSS/dataverse you should reach out to ask for it to be moved on your behalf.

Using Git
---------
This section explains step-by-step some of the most important processes involved in using Git to contribute to Dataverse.

Contribution Checklist
~~~~~~~~~~~~~~~~~~~~~~
To make a contribution to Dataverse's code:

1. Find or create a GitHub issue explaining what you'd like to contribute.

**e.g.** `Issue #3728 <https://github.com/IQSS/dataverse/issues/3728>`_ points out a typo in a page of Dataverse's documentation.

2. Create a new branch that you will commit your change to.

**e.g.**  *3728-doc-apipolicy-fix*

3. Make your change in the form of a commit to that branch. Make sure the title of your commit includes a reference to the number of the issue it relates to.

**e.g.** *Fixed BlockedApiPolicy [ref: #3728]*

4. Make a pull request to get approval to merge your changes into the Develop branch.

**For more details** see our `Pull Request Template <https://github.com/IQSS/dataverse/blob/develop/PULL_REQUEST_TEMPLATE.md>`_.

Merge Conflict Resolution Checklist
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
When merging a branch to Develop, you may encounter a merge conflict. Here is a checklist for how to resolve this situation. Note that this checklist presumes that you're using the free software tools GitHub Desktop and Netbeans. There are other tools that can accomplish this, but your checklist may look different.

**In GitHub Desktop:**

1. Sync from Develop
2. Open specific branch that's having the merge conflict.
3. Click "Update from develop"

**In Netbeans:**

4. Click Window -> Favorites and open your local Dataverse project folder in the Favorites panel
5. In this file browser, you can follow the red cylinder icon to find files with merge conflicts
6. Double click the red merge conflicted file
7. Right click on the red tab for that file and select Git -> Resolve Conflicts
8. Resolve on right or left (if you select "both" you can do finer edits after)
9. Save all changes

**In GitHub Desktop:**

10. Commit the merge (append issue number to end, e.g. #3728) and leave note about what was resolved.