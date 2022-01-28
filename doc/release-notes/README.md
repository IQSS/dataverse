# Dataverse Release Notes

doc/sphinx-guides/source/developers/making-releases.rst documents the official process for making release notes. We have the following goals:

- As a developer, I want to express in my pull request when an addition to the release notes will be necessary.
- As a developer, I want to be aware of changes that should be made to my development environment after a pull request has been merged (examples are Solr schema changes or curl commands to reload metadata blocks)
- As a developer or Dataverse administrator, I want a single source of truth for release notes.

## release-notes Directory Process

- Create a Markdown file named after your branch (assuming your branch starts with an issue number as requested in doc/sphinx-guides/source/developers/version-control.rst) such as "5053-apis-custom-homepage.md".
- In the file you created, give instructions for non-SQL upgrade steps that must be taken to run the branch in your pull request. Examples include Solr schema updates or reloading metadata blocks. (SQL updates are handled separately as indicated in doc/sphinx-guides/source/developers/sql-upgrade-scripts.rst.)
- At release time, gather all the files into final release notes and make a `git rm` commit (or equivalent process in Github Desktop) to delete them to prevent clutter.
- Copy the markdown from the final release notes into the releases page on Github and delete the .md file from this directory (to ensure that there is only a single copy of the notes)
