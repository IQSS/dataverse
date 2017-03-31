==================
Branching Strategy
==================

.. contents:: :local:

Goals
-----

The goals of the Dataverse branching strategy are:

- allow for concurrent development
- only ship stable code

We follow a simplified "git flow" model described at http://nvie.com/posts/a-successful-git-branching-model/ involving a "master" branch, a "develop" branch, and feature branches such as "1234-bug-fix".

Branches
--------

The "master" Branch
~~~~~~~~~~~~~~~~~~~

The "`master <https://github.com/IQSS/dataverse/tree/master>`_" branch represents released versions of Dataverse. As mentioned in the :doc:`making-releases` section, at release time we update the master branch to include all the code for that release. Commits are never made directly to master. Rather, master is updated only when we merge code into it from the "develop" branch.

The "develop" Branch
~~~~~~~~~~~~~~~~~~~~

The "`develop <https://github.com/IQSS/dataverse>`_" branch represents code that was stable enough to merge from a "feature" branch (described below) and that will make it into the next release. Like master, commits are never made to the develop branch. The develop branch is where integration occurs. Your goal is have your code merged into the develop branch after it has been reviewed.

Feature Branches
~~~~~~~~~~~~~~~~

Feature branches are used for both developing features and fixing bugs. They are named after the GitHub issue they are meant to address, which means the branch should not be created until the GitHub issue exists. For example, if https://github.com/IQSS/dataverse/issues/1234 had a title of "Bug fix", you would name your branch "1234-bug-fix" or some other short "slug" with the issue number at the start.

Always create your feature branch from the latest code in develop, pulling if necessary. Push your feature branch either to your fork of Dataverse or to the main repo at IQSS if you have write access. Create a pull request based on the feature branch you pushed. As mentioned in https://github.com/IQSS/dataverse/blob/develop/CONTRIBUTING.md if you do not have access to advance your pull request into the "Code Review" column at https://waffle.io/IQSS/dataverse you should reach out to ask for it to be moved on your behalf.
