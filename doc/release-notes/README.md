# Dataverse Release Notes

doc/sphinx-guides/source/developers/making-releases.rst documents the official process for making release notes but as indicated there, we are experimenting with a process with the following goals:

- As a developer, I want to express in my pull request when an addition to the release notes will be necessary.
- As a developer, I want to be aware of changes that should be made to my dev environment after a pull request has been merged. I already know to look in `scripts/database/upgrades` if I pull the latest code from the "develop" branch for updates as described in doc/sphinx-guides/source/developers/sql-upgrade-scripts.rst but I want a place to look for non-SQL updates that are required. These could be Solr schema changes or curl commands to reload metadata blocks, for example.

# release-notes directory process

- Create a Markdown file named after your branch (assuming your branch starts with an issue number as requested in doc/sphinx-guides/source/developers/version-control.rst) such as "4371-schemaorg.md".
- In the file you created, give instructions for non-SQL upgrade steps that must be taken to run the branch in your pull request. Examples include Solr schema updates or reloading metadata blocks.
- At release time, gather all the files into final release notes and make a `git rm` commit to delete them to prevent clutter.
